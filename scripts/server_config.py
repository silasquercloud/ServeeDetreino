#!/usr/bin/env python3
"""Prepare the local Spigot 1.8.8 properties while preserving unrelated settings."""
import argparse
from datetime import datetime, timezone
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
PROPERTIES = ROOT/'server/server.properties'
DEFAULTS = ROOT/'config/server.properties'
REQUIRED = {'online-mode': 'false', 'white-list': 'false', 'server-port': '25565', 'server-ip': ''}


def entries(text):
    result = {}
    for line in text.splitlines():
        line = line.strip()
        if line and not line.startswith(('#', '!')):
            match = re.match(r'([^=:\s]+)\s*[=:]\s*(.*)', line)
            if match:
                result[match[1]] = match[2].strip()
    return result


def patched(text):
    lines = []
    written = set()
    for line in text.splitlines():
        match = re.match(r'^\s*([^#!\s=:]+)\s*[=:]', line)
        key = match[1] if match else None
        if key in REQUIRED:
            if key not in written:
                lines.append(key+'='+REQUIRED[key])
                written.add(key)
        else:
            lines.append(line)
    for key, value in REQUIRED.items():
        if key not in written:
            lines.append(key+'='+value)
    return '\n'.join(lines)+'\n'


def apply():
    PROPERTIES.parent.mkdir(parents=True, exist_ok=True)
    previous = PROPERTIES.read_text(encoding='utf-8') if PROPERTIES.exists() else None
    current = patched(previous if previous is not None else DEFAULTS.read_text(encoding='utf-8'))
    if previous != current:
        if previous is not None:
            timestamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S-%f')
            backup = PROPERTIES.with_name('server.properties.backup-'+timestamp)
            backup.write_text(previous, encoding='utf-8')
            print('Backup da configuração anterior: '+backup.name)
        temporary = PROPERTIES.with_suffix('.tmp')
        temporary.write_text(current, encoding='utf-8')
        temporary.replace(PROPERTIES)
    print('Configuração pronta: online-mode=false | whitelist desligada | porta local 25565')


def check():
    if not PROPERTIES.exists():
        raise RuntimeError('Instale o servidor pela opção 2 do menu.')
    values = entries(PROPERTIES.read_text(encoding='utf-8'))
    for key in ('online-mode', 'server-port', 'server-ip'):
        if values.get(key) != REQUIRED[key]:
            raise RuntimeError(f'Configuração {key} incompatível com este setup. Atualize pela opção 2 do menu.')
    return values


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['apply', 'check'])
    args = parser.parse_args()
    try:
        apply() if args.command == 'apply' else check()
    except (OSError, RuntimeError) as error:
        parser.exit(1, 'Erro: '+str(error)+'\n')
