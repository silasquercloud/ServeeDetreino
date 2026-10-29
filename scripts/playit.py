#!/usr/bin/env python3
"""Playit 1.1.0 portable daemon + CLI. Credentials stay outside tracked source."""
import argparse
import hashlib
import json
import ipaddress
import os
import pathlib
import platform
import re
import subprocess
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
PRIVATE = ROOT / '.private'
TOOLS = ROOT / '.tools'
SECRET = PRIVATE / 'playit.secret'
SOCKET = PRIVATE / 'playit.sock'
READY = PRIVATE / 'playit-ready.json'
VERSION = '1.1.0'
HASHES = {
    'amd64': {'playit': '1ccd3fe491d4f54e71504da001c16fa4718832a5cb7ee2461dce2816c3fbbc07', 'playit-cli': 'd0d61355cadae033595e5671e7b5cf9200f434b4796ce5ccaf55ec77139e18ba'},
    'aarch64': {'playit': 'd072bd00a076b99fddc81204523d452d1e25f90545c35b224e4009b41ff0cbb2', 'playit-cli': '4695f3cc2b48545d1781888c8549cde8f12344e9152fc1bd62b65012432e2a39'},
}


def arch():
    machine = platform.machine().lower()
    result = {'x86_64': 'amd64', 'amd64': 'amd64', 'aarch64': 'aarch64', 'arm64': 'aarch64'}.get(machine)
    if platform.system() != 'Linux' or not result:
        raise RuntimeError('Este setup automático suporta Linux x64 e ARM64. Veja docs/PLAYIT.md.')
    return result


def ensure_tools():
    machine = arch()
    TOOLS.mkdir(exist_ok=True)
    for name, digest in HASHES[machine].items():
        path = TOOLS / name
        if not path.exists():
            asset = f'{name}-linux-{machine}'
            url = f'https://github.com/playit-cloud/playit-agent/releases/download/v{VERSION}/{asset}'
            print(f'Baixando {asset} v{VERSION} do repositório oficial...', flush=True)
            request = urllib.request.Request(url, headers={'User-Agent': 'ServeeDetreino/1.0 (https://github.com/silasquercloud/ServeeDetreino)'})
            with urllib.request.urlopen(request, timeout=120) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != digest:
                raise RuntimeError('Checksum incorreto para ' + name)
            temporary = path.with_suffix('.download')
            temporary.write_bytes(data)
            temporary.chmod(0o700)
            temporary.replace(path)
        if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            raise RuntimeError(f'{path.name} foi alterado. Remova-o de .tools e repita o setup.')
        path.chmod(0o700)


def cli(*args, **kwargs):
    return subprocess.run([str(TOOLS/'playit-cli'), '--socket-path', str(SOCKET), *args], **kwargs)


def phase(output):
    match = re.search(r'^\s*Phase: (.+)$', output, re.MULTILINE)
    return match.group(1).strip() if match else None


def status():
    result = cli('status', capture_output=True, text=True, timeout=10)
    return phase(result.stdout)


def stop(process):
    if process is not None and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def start_agent():
    PRIVATE.mkdir(mode=0o700, exist_ok=True)
    PRIVATE.chmod(0o700)
    if SOCKET.exists():
        if status() is not None:
            raise RuntimeError('Já há um agente neste projeto. Pare a execução anterior antes de iniciar outra.')
        SOCKET.unlink()  # stale socket only; never delete a live agent's socket
    log = open(PRIVATE/'playit.log', 'a', encoding='utf-8')
    os.chmod(PRIVATE/'playit.log', 0o600)
    try:
        process = subprocess.Popen([str(TOOLS/'playit'), '--secret-path', str(SECRET), '--socket-path', str(SOCKET)], stdout=log, stderr=subprocess.STDOUT)
    finally:
        log.close()
    try:
        for _ in range(60):
            if process.poll() is not None:
                raise RuntimeError('Playit encerrou ao iniciar. Consulte o log local .private/playit.log; não publique esse arquivo.')
            if SOCKET.exists():
                current = status()
                if current is not None and current != 'starting':
                    return process
            time.sleep(0.25)
        raise RuntimeError('O agente não disponibilizou o socket. Verifique .private/playit.log.')
    except BaseException:
        stop(process)
        raise


def wait_running(process, timeout=45):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError('O Playit encerrou antes de ficar pronto.')
        current = status()
        if current == 'running':
            return
        if current in ('invalid secret', 'disabled over limit', 'error', 'waiting for secret'):
            raise RuntimeError('Playit: ' + current + '. Refaça o setup ou consulte seu painel.')
        time.sleep(0.5)
    raise RuntimeError('O agente não ficou ativo. Verifique sua rede e o painel Playit.')


def valid_address(address):
    # Only an address to display, never interpreted as a command or used as a URL.
    if not isinstance(address, str):
        return False
    match = re.fullmatch(r'(?=.{3,253}$)([A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?)(?::([0-9]{1,5}))?', address)
    if not match or '.' not in match[1] or '..' in match[1]:
        return False
    try:
        if not ipaddress.ip_address(match[1]).is_global:
            return False
    except ValueError:
        if match[1].lower().endswith(('.local', '.localhost')):
            return False
    return not match[2] or 1 <= int(match[2]) <= 65535


def connection_info(data):
    address = data['public_address']
    host, separator, suffix = address.rpartition(':')
    if not separator:
        host = address
    port = int(suffix) if separator else data.get('public_port')
    if port is not None:
        try:
            if isinstance(port, bool):
                raise ValueError('Porta pública inválida.')
            port = int(port)
        except (TypeError, ValueError) as error:
            raise ValueError('Porta pública inválida. Reconfigure pela opção 1.') from error
        if not 1 <= port <= 65535:
            raise ValueError('Porta pública inválida. Reconfigure o Playit pela opção 1.')
    return {'host': host, 'port': port, 'connect': host+':'+str(port) if port else host}


def print_connection(data):
    info = connection_info(data)
    print('  IP / host público: ' + info['host'])
    print('  Porta pública:    ' + (str(info['port']) if info['port'] else 'automática via endereço/SRV (não informada)'))
    print('  Cole no Minecraft: ' + info['connect'])
    print('  Destino local:    127.0.0.1:25565')
    return info


def check_ready():
    if not READY.exists() or not SECRET.exists() or SECRET.stat().st_size == 0:
        raise RuntimeError('Configure o Playit ANTES de instalar/iniciar: bash menu.sh (opção 1)')
    data = json.loads(READY.read_text())
    if not isinstance(data, dict):
        raise RuntimeError('Setup incompleto. Reconfigure o Playit pela opção 1.')
    if data.get('local_address') != '127.0.0.1:25565' or not valid_address(data.get('public_address', '')):
        raise RuntimeError('Setup incompleto. Execute python3 scripts/playit.py setup novamente.')
    connection_info(data)
    return data


def setup():
    ensure_tools()
    agent = None
    try:
        agent = start_agent()
        current = status()
        if current == 'waiting for secret':
            print('\nPLAYIT • PASSO 1/3 — abra o link mostrado a seguir e vincule o agente à sua conta Playit.\n', flush=True)
            cli('setup', check=True)
        wait_running(agent)
        if not SECRET.exists() or SECRET.stat().st_size == 0:
            raise RuntimeError('O Playit não salvou a credencial. Setup não concluído.')
        SECRET.chmod(0o600)
        print('\nPLAYIT • PASSO 2/3 — em https://playit.gg/account/tunnels crie um túnel:')
        print('  Tipo: Minecraft Java / TCP | Agente: este computador')
        print('  Local address: 127.0.0.1 | Local port: 25565')
        print('  Salve e copie o endereço público do túnel. O servidor local ainda estará offline nesta etapa.')
        while True:
            address = input('\nCole o endereço público (sem https://): ').strip()
            if valid_address(address):
                break
            print('Endereço inválido. Cole somente o hostname/IP público e a porta, se houver.')
        port = connection_info({'public_address': address})['port']
        if port is None:
            while True:
                answer = input('Porta PÚBLICA mostrada no painel (Enter = endereço automático/SRV): ').strip()
                if not answer:
                    break
                if answer.isdigit() and 1 <= int(answer) <= 65535:
                    port = int(answer)
                    break
                print('Use uma porta de 1 a 65535, ou Enter se o endereço usa SRV.')
        READY.write_text(json.dumps({'version': VERSION, 'local_address': '127.0.0.1:25565', 'public_address': address, 'public_port': port}, indent=2)+'\n')
        READY.chmod(0o600)
        print('\nPLAYIT • PASSO 3/3 — agente vinculado e endereço cadastrado. Escolha a opção 2 do menu para instalar.')
        print_connection(check_ready())
        print('A conexão pública será testada depois que o Minecraft estiver iniciado.')
    finally:
        stop(agent)
        if SOCKET.exists() and agent is not None and agent.poll() is not None:
            SOCKET.unlink(missing_ok=True)


def run():
    data = check_ready()
    ensure_tools()
    agent = server = None
    try:
        agent = start_agent()
        wait_running(agent)
        print('\nPlayit ativo. Dados do túnel cadastrado:', flush=True)
        print_connection(data)
        print('Iniciando Minecraft. Espere a mensagem Done antes de conectar.\n', flush=True)
        environment = os.environ.copy()
        environment['TREINO_DIRECT'] = '1'
        server = subprocess.Popen(['bash', str(ROOT/'scripts/start.sh')], cwd=ROOT, env=environment)
        while server.poll() is None:
            if agent.poll() is not None:
                raise RuntimeError('Playit encerrou. O servidor será parado para reiniciar os dois juntos.')
            time.sleep(0.5)
        if server.returncode:
            raise RuntimeError(f'O Minecraft encerrou com código {server.returncode}. Veja server/logs/latest.log.')
    finally:
        stop(server)
        stop(agent)
        if SOCKET.exists() and agent is not None and agent.poll() is not None:
            SOCKET.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description='Setup e execução integrada do Playit oficial')
    parser.add_argument('command', choices=['setup', 'check', 'run'])
    args = parser.parse_args()
    os.umask(0o077)
    try:
        if args.command == 'setup':
            setup()
        elif args.command == 'check':
            check_ready()
        else:
            run()
    except KeyboardInterrupt:
        print('\nExecução encerrada.')
        return 130
    except (RuntimeError, OSError, ValueError, subprocess.SubprocessError, EOFError) as error:
        print('Erro: ' + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
