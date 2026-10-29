#!/usr/bin/env python3
"""Fast-forward project sources and atomically update the game plugin only."""
from datetime import datetime, timezone
import pathlib
import re
import shutil
import subprocess
import sys
import zipfile
from runtime_lock import server_lock

ROOT = pathlib.Path(__file__).resolve().parents[1]


def execute(args, capture=False):
    return subprocess.run(args, cwd=ROOT, check=True, text=True, capture_output=capture)


def update():
    destination = ROOT/'server/plugins/TreinoHub-1.0.0.jar'
    if not (ROOT/'server/server.jar').exists() or not destination.exists():
        raise RuntimeError('Instale primeiro o servidor completo pela opção 2.')
    with server_lock(ROOT):
        # Older project launchers did not hold server.lock; also detect their live agent.
        socket = ROOT/'.private/playit.sock'
        cli = ROOT/'.tools/playit-cli'
        if socket.exists() and cli.is_file():
            status = execute([str(cli), '--socket-path', str(socket), 'status'], capture=True).stdout
            if re.search(r'^\s*Phase:', status, re.MULTILINE):
                raise RuntimeError('Playit/servidor ainda está ativo. Pare com stop antes de atualizar.')
        execute(['git', 'rev-parse', '--show-toplevel'], capture=True)
        dirty = execute(['git', 'status', '--porcelain', '--untracked-files=no'], capture=True).stdout.strip()
        if dirty:
            raise RuntimeError('Há arquivos de código modificados localmente. Salve suas alterações no Git antes de atualizar; nada foi apagado.')
        print('Buscando atualização do repositório (fast-forward, sem reset)...', flush=True)
        execute(['git', 'pull', '--ff-only'])
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
        print('Atualização concluída: TreinoHub + TreinoAntiLag. Backups em server/backups/'+stamp+'-*')
        print('Playit, server.properties, EULA, mundos e configurações dos plugins foram preservados.')
        print('Base mantida: Spigot 1.8.8 / cliente 1.8.9. Escolha 3 no menu para ligar.')


def main():
    try:
        update()
    except (OSError, RuntimeError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        print('Atualização não concluída: '+str(error), file=sys.stderr)
        print('O atualizador não reseta o Git nem substitui o plugin se a compilação falhar.', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
