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
        source = ROOT/'target/TreinoHub-1.0.0.jar'
        with zipfile.ZipFile(source) as artifact:
            if artifact.testzip() is not None or 'plugin.yml' not in artifact.namelist():
                raise RuntimeError('O novo plugin é inválido. O JAR instalado foi mantido.')
        backups = ROOT/'server/backups'
        backups.mkdir(exist_ok=True)
        stamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S-%f')
        backup = backups/('TreinoHub-'+stamp+'.jar')
        shutil.copy2(destination, backup)
        temporary = destination.with_suffix('.jar.new')
        try:
            shutil.copy2(source, temporary)
            temporary.replace(destination)
        finally:
            temporary.unlink(missing_ok=True)
        print('Atualização concluída. Backup do plugin anterior: '+backup.name)
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
