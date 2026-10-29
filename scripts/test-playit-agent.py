#!/usr/bin/env python3
"""Real unclaimed-agent smoke test: no account, tunnel or secret is created."""
import importlib.util
import pathlib
import tempfile

source = pathlib.Path(__file__).with_name('playit.py')
spec = importlib.util.spec_from_file_location('playit_smoke', source)
playit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(playit)

playit.ensure_tools()
with tempfile.TemporaryDirectory(prefix='treino-ipc-') as temporary:
    playit.PRIVATE = pathlib.Path(temporary)
    playit.SECRET = playit.PRIVATE/'secret'
    playit.SOCKET = playit.PRIVATE/'agent.sock'
    playit.READY = playit.PRIVATE/'ready.json'
    agent = None
    try:
        agent = playit.start_agent()
        assert playit.status() == 'waiting for secret', 'Unexpected unclaimed-agent lifecycle'
        assert not playit.SECRET.exists(), 'Test must not create a credential'
        print('Playit 1.1.0: real daemon + CLI + private IPC OK; waiting for user claim.')
    finally:
        playit.stop(agent)
