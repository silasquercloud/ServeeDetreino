#!/usr/bin/env python3
"""Prepare/activate BWFE for the next manual start. Never starts Minecraft."""
import argparse, datetime, hashlib, json, os, pathlib, re, shutil, subprocess, sys, tempfile, urllib.request, zipfile
import template, playit, server_config
from runtime_lock import server_lock
from update import check_processes, stop_if_managed
ROOT=template.ROOT
PINS={'Citizens':('2.0.35','https://ci.citizensnpcs.co/job/Citizens2/3478/artifact/dist/target/Citizens-2.0.35-b3478.jar'),
      'Parties':('3.2.14',None),'LastLoginAPI':('1.7.4',None)}

def descriptor(path):
    with zipfile.ZipFile(path) as z:
        if z.testzip():raise ValueError('JAR corrompido: '+path.name)
        text=z.read('plugin.yml').decode('utf-8')
        def field(key):
            m=re.search(r'^'+key+r':\s*([^\n\r]+)',text,re.M)
            return m.group(1).strip().strip('\"\x27') if m else ''
        main=field('main').replace('.','/')+'.class'
        data=z.read(main)
        if data[:4]!=b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8],'big')>61:raise ValueError('Plugin exige Java superior a 17: '+path.name)
        return field('name'),field('version'),text,z.namelist()

def download(url,target):
    req=urllib.request.Request(url,headers={'User-Agent':'ServeeDetreino-BWFE-installer/1.0'})
    with urllib.request.urlopen(req,timeout=25) as response,target.open('wb') as out:
        size=0
        while True:
            data=response.read(1024*1024)
            if not data:break
            size+=len(data)
            if size>64*1024*1024:raise ValueError('Download excede 64 MiB')
            out.write(data)

def download_citizens(url, target):
    """Try the individual artifact, then Jenkins' archive of the SAME build."""
    try:
        download(url, target)
        return
    except Exception as direct_error:
        print('Citizens: download direto indisponível; tentando ZIP oficial do build 3478.', flush=True)
        archive = target.with_suffix('.zip')
        try:
            download('https://ci.citizensnpcs.co/job/Citizens2/3478/artifact/*zip*/archive.zip', archive)
            with zipfile.ZipFile(archive) as z:
                matches = [i for i in z.infolist() if pathlib.PurePosixPath(i.filename).name == 'Citizens-2.0.35-b3478.jar']
                if len(matches) != 1 or matches[0].file_size > 64*1024*1024:
                    raise ValueError('ZIP oficial sem o JAR esperado ou tamanho inválido')
                target.write_bytes(z.read(matches[0]))
        except Exception as archive_error:
            try:
                download_citizens_actions(target)
            except Exception as actions_error:
                raise ValueError(f'Direto: {direct_error}; ZIP oficial: {archive_error}; GitHub: {actions_error}') from actions_error
        finally:
            archive.unlink(missing_ok=True)

def download_citizens_actions(target):
    if not shutil.which('gh'):
        raise ValueError('GitHub CLI (gh) ausente; use o Codespace ou imports/plugins/')
    repo='silasquercloud/ServeeDetreino'
    result=subprocess.run(['gh','api',f'repos/{repo}/actions/workflows/citizens-cache.yml/runs?status=success&branch=main&per_page=1'],capture_output=True,text=True,timeout=30)
    if result.returncode:raise ValueError('Sem acesso ao artefato GitHub; confira gh auth status')
    runs=json.loads(result.stdout).get('workflow_runs',[])
    if not runs:raise ValueError('Ainda não existe download bem-sucedido no Actions; consulte a aba Actions do repositório')
    with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
        result=subprocess.run(['gh','run','download',str(runs[0]['id']),'-R',repo,'-n','citizens-2.0.35-b3478','-D',tmp],capture_output=True,text=True,timeout=120)
        if result.returncode:raise ValueError('Não foi possível baixar o artefato do Actions')
        candidate=pathlib.Path(tmp)/'Citizens-2.0.35-b3478.jar'
        actual,version,_,files=descriptor(candidate)
        if actual!='Citizens' or '2.0.35' not in version or '3478' not in version or not any('v1_8_R3' in f for f in files):
            raise ValueError('Artefato não corresponde ao Citizens 3478 com suporte 1.8.8')
        shutil.copy2(candidate,target)
        print('Citizens obtido pelo GitHub Actions.',flush=True)

def dependencies(root,offline=False):
    installed={descriptor(p)[0] for p in (root/'plugins').glob('*.jar')}
    issues=[]
    for name,(version,url) in PINS.items():
        if name in installed:continue
        print('Verificando dependência: '+name,flush=True)
        local=list((ROOT/'imports/plugins').glob('*.jar'))
        source=next((p for p in local if descriptor(p)[0]==name),None)
        try:
            with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
                candidate=pathlib.Path(tmp)/(name+'.jar')
                if source:shutil.copy2(source,candidate)
                elif offline:raise ValueError('Download desativado nesta verificação')
                else:
                    if not url:
                        api=f'https://hangar.papermc.io/api/v1/projects/{name}/versions/{version}'
                        with urllib.request.urlopen(api,timeout=25) as r:meta=json.load(r)
                        if not any(v == '1.8' or v.startswith('1.8.') for v in meta.get('platformDependencies',{}).get('PAPER',[])):raise ValueError('Hangar não confirma suporte 1.8 nesta versão')
                        url=meta['downloads']['PAPER']['downloadUrl']
                    if name=='Citizens':download_citizens(url,candidate)
                    else:download(url,candidate)
                actual,ver,_,files=descriptor(candidate)
                if actual!=name or version not in ver:raise ValueError(f'Esperado {name} {version}, recebido {actual} {ver}')
                if name=='Citizens' and not any('v1_8_R3' in f for f in files):raise ValueError('Citizens sem adaptador 1.8_R3')
                shutil.copy2(candidate,root/'plugins'/(name+'-'+version+'.jar'))
                print('Instalado '+name+' '+ver,flush=True)
        except Exception as e:issues.append(name+': '+str(e)+f'. Alternativa: coloque o JAR {version} em imports/plugins/.')
    return issues

def hard_dependencies(root):
    descriptors=[descriptor(p) for p in (root/'plugins').glob('*.jar')];names=[d[0] for d in descriptors];issues=[]
    for name in set(names):
        if names.count(name)>1:issues.append('Plugin duplicado: '+name)
    for name,version,text,files in descriptors:
        m=re.search(r'^depend:\s*(\[[^\]]*\]|\n(?:[ \t]*-[^\n]*\n?)+)',text,re.M)
        if m:
            deps=re.findall(r'[A-Za-z][A-Za-z0-9_-]*',m.group(1))
            for dep in deps:
                if dep not in names:issues.append(name+' requer '+dep)
    return issues

def prepare(offline=False,archive=None):
    stop_if_managed()
    with server_lock(ROOT):
        check_processes()
        java=subprocess.run(['java','-version'],capture_output=True,text=True)
        match=re.search(r'version "(\d+)',java.stderr+java.stdout)
        if java.returncode or not match or int(match.group(1))!=17:raise ValueError('Use o Java 17 preparado pelo bootstrap. Execute bash atualizar.sh.')
        active=(ROOT/'server/.bwfe-template.json').exists()
        root=ROOT/'server' if active else template.STAGE
        if not root.exists():
            choices=[p for folder in [ROOT,ROOT/'imports'] for p in folder.glob('*.zip') if p.name.startswith('BWFE')]
            chosen=pathlib.Path(archive) if archive else choices[0] if len(choices)==1 else None
            if chosen is None or not chosen.is_file():raise ValueError('Envie BWFE (25.1.0).zip para imports/ no Codespace e execute novamente. O anexo do chat não aparece automaticamente no Codespace.')
            template.prepare(chosen)
        status=template.report(root)
        if status['arenas_configuradas'] != 46:
            raise ValueError('Esperadas 46 configurações BWFE; encontradas '+str(status['arenas_configuradas']))
        print('Arenas: '+str(46-len(status['mapas_ausentes']))+'/46 presentes; importando as ausentes.',flush=True)
        if status['mapas_ausentes']:
            archive_path=ROOT/'maps/arenas-bwfe-original.zip'
            with zipfile.ZipFile(archive_path) as src,tempfile.TemporaryDirectory(dir=ROOT) as tmp:
                subset=pathlib.Path(tmp)/'missing.zip'
                with zipfile.ZipFile(subset,'w',zipfile.ZIP_DEFLATED) as out:
                    for item in src.infolist():
                        if pathlib.PurePosixPath(item.filename).parts[0] in status['mapas_ausentes']:out.writestr(item,src.read(item))
                template.add_maps(subset, root)
        issues=dependencies(root,offline)+hard_dependencies(root)
        status=template.report(root)
        if status['mapas_ausentes']:issues.append('Mapas ausentes: '+str(len(status['mapas_ausentes'])))
        if status['plugins_ausentes']:issues.append('Plugins ausentes: '+', '.join(status['plugins_ausentes']))
        if not status['lobby_presente'] or not status['server_jar']:issues.append('Lobby ou server.jar ausente')
        with zipfile.ZipFile(root/'server.jar') as z:
            if not any('net/minecraft/server/v1_8_R3/' in n for n in z.namelist()):
                digest=hashlib.sha256((root/'server.jar').read_bytes()).hexdigest()
                known=digest=='4077e740bb39a8fe06ea0786a0624c786888be37dce11ca7aee799b7861fa886'
                if not known:issues.append('server.jar não reconhecido como NMS 1.8_R3 ou launcher original BWFE')
                else:print('PandaSpigot Paperclip 1.8.8 original conferido; prepara o JAR final no primeiro início.')
        try:playit.check_ready()
        except Exception as e:issues.append('Playit: '+str(e)+' Use opção 1 do menu.')
        if issues:
            print('\nPENDÊNCIAS — instalação anterior preservada:\n- '+'\n- '.join(issues));return 1
        properties=root/'server.properties';properties.write_text(server_config.patched(properties.read_text()))
        if not active:
            backup=ROOT/'.private'/('server-before-bwfe-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
            previous=ROOT/'server'
            if previous.exists():previous.rename(backup)
            try:root.rename(previous)
            except OSError:
                if backup.exists():backup.rename(previous)
                raise
            print('Instalação anterior preservada em '+str(backup))
        print('\nPRONTO PARA INICIAR O TESTE. Java 17, mapas, JARs e configuração Playit verificados.\nMinecraft não foi iniciado. Use bash menu.sh → opção 3.\nCompatibilidade de arquivos verificada; confira os plugins no console durante o primeiro início.')
        return 0

def main():
    p=argparse.ArgumentParser();p.add_argument('--offline',action='store_true');p.add_argument('--archive');a=p.parse_args()
    try:return prepare(a.offline,a.archive)
    except Exception as e:print('PREPARAÇÃO NÃO CONCLUÍDA: '+str(e));return 1
if __name__=='__main__':raise SystemExit(main())

