#!/usr/bin/env python3
"""Stage the user's BWFE archive without distributing or overwriting its assets."""
import argparse, json, pathlib, re, shutil, stat, tempfile, zipfile
from runtime_lock import server_lock
ROOT=pathlib.Path(__file__).resolve().parents[1]
STAGE=ROOT/'template-server'
REQUIRED={'BedWars2023','Citizens','Parties','LastLoginAPI','ProtocolLib','Vault','PlaceholderAPI'}

def extract(archive, destination):
    with zipfile.ZipFile(archive) as z:
        total=0
        for info in z.infolist():
            name=info.filename.replace('\\','/')
            path=pathlib.PurePosixPath(name)
            total+=info.file_size
            if path.is_absolute() or '..' in path.parts or ':' in name or stat.S_ISLNK(info.external_attr>>16):
                raise ValueError('Caminho inseguro no ZIP: '+name)
            if total>8*1024**3:raise ValueError('ZIP excede 8 GiB descompactados')
        for info in z.infolist():
            target=destination.joinpath(*pathlib.PurePosixPath(info.filename.replace('\\','/')).parts)
            if info.is_dir():target.mkdir(parents=True,exist_ok=True)
            else:
                target.parent.mkdir(parents=True,exist_ok=True)
                with z.open(info) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)

def report(root):
    plugins={}
    for jar in sorted((root/'plugins').glob('*.jar')):
        try:
            with zipfile.ZipFile(jar) as z:s=z.read('plugin.yml').decode('utf-8')
            name=re.search(r'^name:\s*[\"\x27]?([^\"\x27\n\r]+)',s,re.M).group(1).strip()
            plugins[name]=jar.name
        except (OSError,ValueError,KeyError,AttributeError,zipfile.BadZipFile):raise ValueError('JAR inválido: '+jar.name)
    arenas=list((root/'plugins/BedWars2023/Arenas').glob('*.yml'))
    missing=[]
    for arena in arenas:
        # BW2023 arena file stem is the world name; require actual region data.
        world=root/arena.stem
        backup=root/'plugins/BedWars2023/ArenaBackups'/arena.stem
        if not any(p.is_dir() and any((p/'region').glob('*.mca')) for p in [world,backup]):missing.append(arena.stem)
    return {'plugins':len(plugins),'arenas_configuradas':len(arenas),'mapas_ausentes':missing,
            'plugins_ausentes':sorted(REQUIRED-set(plugins)),
            'lobby_presente':(root/'spawn/level.dat').is_file(),
            'server_jar':(root/'server.jar').is_file(),
            'compativel_em_execucao':False}

def prepare(archive):
    if STAGE.exists():raise ValueError('template-server já existe. Use check; a importação não sobrescreve arquivos.')
    with tempfile.TemporaryDirectory(dir=ROOT,prefix='.bwfe-import-') as temporary:
        temp=pathlib.Path(temporary);extract(archive,temp/'outer')
        nested=temp/'outer/Server Files.zip'
        if not nested.is_file():raise ValueError('Esperado ZIP BWFE com Server Files.zip')
        extract(nested,temp/'server');server=temp/'server'
        if not (server/'plugins/BedWars2023/config.yml').is_file():raise ValueError('Template BedWars2023 não encontrado')
        props=server/'server.properties';s=props.read_text()
        for key,value in {'server-port':'25565','query.port':'25565','server-ip':'0.0.0.0','online-mode':'false','use-native-transport':'false','max-players':'32','view-distance':'6','enable-rcon':'false'}.items():
            s=re.sub(r'^'+re.escape(key)+r'=.*$',key+'='+value,s,flags=re.M) if re.search(r'^'+re.escape(key)+'=',s,re.M) else s+'\n'+key+'='+value+'\n'
        props.write_text(s);(server/'eula.txt').write_text('eula=false\n')
        (server/'.bwfe-template.json').write_text(json.dumps({'template':'BWFE 25.1.0','java':17,'minecraft':'1.8.8','status':'staged'},indent=2))
        shutil.move(str(server),STAGE)
    print(json.dumps(report(STAGE),indent=2,ensure_ascii=False))

def add_maps(archive, root=None):
    root = STAGE if root is None else root
    if not root.exists():raise ValueError('Importe primeiro o BWFE')
    expected={p.stem for p in (root/'plugins/BedWars2023/Arenas').glob('*.yml')}
    with tempfile.TemporaryDirectory(dir=ROOT,prefix='.bwfe-maps-') as temporary:
        temp=pathlib.Path(temporary);extract(archive,temp)
        found=[p.parent for p in temp.rglob('level.dat') if p.parent.name in expected]
        if not found:raise ValueError('ZIP sem mundos correspondentes às arenas configuradas')
        if any((root/p.name).exists() for p in found):raise ValueError('Um dos mapas já existe; arquivos preservados')
        for p in found:shutil.copytree(p,root/p.name)
    print(json.dumps(report(root),indent=2,ensure_ascii=False))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('action',choices=['import','maps','check']);parser.add_argument('archive',nargs='?');a=parser.parse_args()
    try:
        with server_lock(ROOT):
            if a.action=='check':
                if not STAGE.exists():raise ValueError('Template ainda não importado')
                print(json.dumps(report(STAGE),indent=2,ensure_ascii=False))
            elif not a.archive:raise ValueError('Informe o caminho do ZIP')
            elif a.action=='import':prepare(pathlib.Path(a.archive))
            else:add_maps(pathlib.Path(a.archive))
        return 0
    except (OSError,ValueError,RuntimeError,zipfile.BadZipFile) as e:print('Não concluído:',e);return 1
if __name__=='__main__':raise SystemExit(main())

