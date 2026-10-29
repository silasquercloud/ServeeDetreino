#!/usr/bin/env python3
"""Keep server start and updates mutually exclusive on this Linux installation."""
import contextlib
import fcntl
import os


@contextlib.contextmanager
def server_lock(root):
    directory = root/'.private'
    directory.mkdir(mode=0o700, exist_ok=True)
    directory.chmod(0o700)
    with open(directory/'server.lock', 'a+') as handle:
        os.chmod(directory/'server.lock', 0o600)
        try:
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise RuntimeError('Servidor ou atualização em execução. Use stop no console antes de atualizar/iniciar novamente.') from error
        try:
            yield
        finally:
            fcntl.flock(handle, fcntl.LOCK_UN)
