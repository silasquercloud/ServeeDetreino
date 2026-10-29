#!/usr/bin/env python3
"""Fast-forward project sources and atomically update the game plugin only."""
from datetime import datetime, timezone
import pathlib
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from runtime_lock import server_lock
from server_control import request_stop

ROOT = pathlib.Path(__file__).resolve().parents[1]


def execute(args, capture=False):
    return subprocess.run(args, cwd=ROOT, check=True, text=True, capture_output=capture)


def stop_if_managed():
    state = ROOT/'.private/minecraft-control.json'
    if state.exists():
        try:
            data = json.loads(state.read_text())
            os.kill(int(data['pid']), 0)
        except (OSError, ValueError, KeyError):
            return
        request_stop(ROOT)


def check_processes():
    # Also protect servers launched before the current lock/control protocol.
    server_dir = (ROOT/'server').resolve()
    proc = pathlib.Path('/proc')
    if proc.exists():
        for entry in proc.iterdir():
            if not entry.name.isdigit():
                continue
            try:
                if (entry/'cwd').resolve() != server_dir:
                    continue
                args = (entry/'cmdline').read_bytes().split(b'\0')
                if b'-jar' in args and any(arg == b'server.jar' or arg.endswith(b'/server.jar') for arg in args):
                    raise RuntimeError('Minecraft ainda ligado. Digite stop no console original e aguarde encerrar antes de atualizar.')
            except (OSError, ValueError):
                continue
    socket = ROOT/'.private/playit.sock'
    cli = ROOT/'.tools/playit-cli'
    if socket.exists() and cli.is_file():
        # A leftover socket or unavailable CLI is not evidence of a running server.
        try:
            result = subprocess.run([str(cli), '--socket-path', str(socket), 'status'],
                                    cwd=ROOT, text=True, capture_output=True, timeout=10)
        except (OSError, subprocess.SubprocessError):
            return
        if re.search(r'^\s*Phase:', result.stdout, re.MULTILINE):
            raise RuntimeError('Playit antigo ainda ativo. Encerre pelo console original antes de atualizar.')


def update(fetch=True):
    if not (ROOT/'server/server.jar').is_file():
        raise RuntimeError('Instale primeiro o servidor completo pela opção 2.')
    stop_if_managed()
    if fetch:
        with server_lock(ROOT):
            check_processes()
            execute(['git', 'rev-parse', '--show-toplevel'], capture=True)
            dirty = execute(['git', 'status', '--porcelain', '--untracked-files=no'], capture=True).stdout.strip()
            if dirty:
                raise RuntimeError('Código modificado localmente: \n'+dirty+'\nSuas alterações foram preservadas. Salve-as no Git antes de atualizar.')
            print('Buscando atualização do repositório (fast-forward, sem reset)...', flush=True)
            execute(['git', 'pull', '--ff-only'])
        # Start the NEW updater/environment, not the Python code loaded before pull.
        execute(['bash', str(ROOT/'scripts/update.sh'), '--install-current'])
        return
    with server_lock(ROOT):
        check_processes()
        (ROOT/'server/plugins').mkdir(exist_ok=True)
        print('Compilando e testando o plugin atualizado...', flush=True)
        execute([sys.executable, 'scripts/build.py'])
        names = ['TreinoHub-1.0.0.jar', 'TreinoAntiLag-1.0.0.jar']
        for name in names:
            with zipfile.ZipFile(ROOT/'target'/name) as artifact:
                if artifact.testzip() is not None or 'plugin.yml' not in artifact.namelist():
                    raise RuntimeError('O novo plugin é inválido: '+name+'. Os JARs instalados foram mantidos.')
        backups = ROOT/'server/backups'
        backups.mkdir(exist_ok=True)
        stamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S-%f')
        originals = {}
        staged = []
        try:
            for name in names:
                dest = ROOT/'server/plugins'/name
                backup = backups/(stamp+'-'+name)
                originals[dest] = backup if dest.exists() else None
                if dest.exists():
                    shutil.copy2(dest, backup)
                temporary = dest.with_suffix('.jar.new')
                staged.append((temporary, dest))
                shutil.copy2(ROOT/'target'/name, temporary)
            replaced = []
            try:
                for temporary, dest in staged:
                    temporary.replace(dest)
                    replaced.append(dest)
            except OSError:
                for dest in reversed(replaced):
                    if originals[dest] is None:
                        dest.unlink(missing_ok=True)
                    else:
                        shutil.copy2(originals[dest], dest)
                raise
        finally:
            for temporary, dest in staged:
                temporary.unlink(missing_ok=True)
        receipt = {'installed_at': stamp, 'plugins': {}}
        for name in names:
            path = ROOT/'server/plugins'/name
            source = ROOT/'target'/name
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            if digest != hashlib.sha256(source.read_bytes()).hexdigest():
                raise RuntimeError('A verificação do plugin instalado falhou: '+name)
            with zipfile.ZipFile(path) as archive:
                descriptor = archive.read('plugin.yml').decode('utf-8')
            version = re.search(r'^version:\s*(.+)$', descriptor, re.MULTILINE)
            value = version[1].strip() if version else 'não informada'
            receipt['plugins'][name] = {'version': value, 'sha256': digest}
            print('Instalado: '+name+' | versão '+value+' | SHA-256 '+digest[:12], flush=True)
        (ROOT/'.private/update-installed.json').write_text(json.dumps(receipt, indent=2)+'\n')
        print('Atualização concluída: TreinoHub + TreinoAntiLag. Backups em server/backups/'+stamp+'-*')
        print('Playit, server.properties, EULA, mundos e configurações dos plugins foram preservados.')
        print('Base mantida: Spigot 1.8.8 / cliente 1.8.9. Escolha 3 no menu para ligar.')


def main():
    try:
        update(fetch='--install-current' not in sys.argv[1:])
    except (OSError, RuntimeError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        print('Atualização não concluída: '+str(error), file=sys.stderr)
        print('Envie a mensagem acima se precisar de diagnóstico. O atualizador não reseta o Git nem substitui o plugin se a compilação falhar.', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
