"""Private, generation-bound stop requests; no public RCON port or PID killing."""
import json
import os
import pathlib
import secrets
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]


class Control:
    def __init__(self, root=ROOT):
        self.directory = root/'.private'
        self.state = self.directory/'minecraft-control.json'
        self.request = self.directory/'minecraft-stop.json'
        self.token = secrets.token_hex(24)

    def __enter__(self):
        self.directory.mkdir(mode=0o700, exist_ok=True)
        self.request.unlink(missing_ok=True)
        temporary = self.state.with_suffix('.tmp')
        temporary.write_text(json.dumps({'generation': self.token, 'pid': os.getpid()}))
        temporary.chmod(0o600)
        temporary.replace(self.state)
        return self

    def requested(self):
        try:
            return json.loads(self.request.read_text()).get('generation') == self.token
        except (OSError, ValueError):
            return False

    def __exit__(self, *args):
        self.state.unlink(missing_ok=True)
        self.request.unlink(missing_ok=True)


def request_stop(root=ROOT, timeout=120):
    directory = root/'.private'
    state = directory/'minecraft-control.json'
    try:
        data = json.loads(state.read_text())
        os.kill(int(data['pid']), 0)  # existence check only; no signal is sent
        generation = data['generation']
    except (OSError, ValueError, KeyError):
        raise RuntimeError('Nenhum launcher compatível está ativo. Se iniciou antes desta atualização, digite stop no console original.')
    request = directory/'minecraft-stop.json'
    temporary = request.with_suffix('.tmp')
    temporary.write_text(json.dumps({'generation': generation}))
    temporary.chmod(0o600)
    temporary.replace(request)
    print('Solicitando stop ao console. Aguardando salvar e encerrar Minecraft + Playit...', flush=True)
    deadline = time.monotonic()+timeout
    while time.monotonic() < deadline:
        try:
            current = json.loads(state.read_text())
        except FileNotFoundError:
            print('Servidor e Playit encerrados. Agora pode atualizar.')
            return
        if current.get('generation') != generation:
            raise RuntimeError('O launcher mudou durante a parada. Confira o console antes de atualizar.')
        time.sleep(0.25)
    raise RuntimeError('O servidor ainda está encerrando. Confira o console; nenhum processo foi morto à força.')


if __name__ == '__main__':
    try:
        request_stop()
    except (OSError, RuntimeError, ValueError) as error:
        raise SystemExit(str(error))
