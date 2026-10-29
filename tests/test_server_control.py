import importlib
import json
import os
import pathlib
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]/'scripts'))
control = importlib.import_module('server_control')


class ServerControlTests(unittest.TestCase):
    def test_stop_request_only_matches_current_generation(self):
        with tempfile.TemporaryDirectory() as temp:
            root = pathlib.Path(temp)
            with control.Control(root) as current:
                current.request.write_text(json.dumps({'generation': 'previous-start'}))
                self.assertFalse(current.requested())
                current.request.write_text(json.dumps({'generation': current.token}))
                self.assertTrue(current.requested())
            self.assertFalse(current.state.exists())
            self.assertFalse(current.request.exists())

    def test_old_launcher_does_not_kill_processes(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(os, 'kill') as kill:
            with self.assertRaisesRegex(RuntimeError, 'console original'):
                control.request_stop(pathlib.Path(temp), timeout=0)
            kill.assert_not_called()

    def test_sends_stop_and_waits_for_acknowledgement(self):
        with tempfile.TemporaryDirectory() as temp:
            root = pathlib.Path(temp)
            with control.Control(root) as current:
                def saved(_):
                    self.assertTrue(current.requested())
                    current.state.unlink()
                with patch.object(control.time, 'sleep', side_effect=saved):
                    control.request_stop(root, timeout=1)

    def test_timeout_does_not_force_kill_server(self):
        with tempfile.TemporaryDirectory() as temp:
            root = pathlib.Path(temp)
            with control.Control(root), patch.object(os, 'kill') as kill:
                with self.assertRaisesRegex(RuntimeError, 'nenhum processo'):
                    control.request_stop(root, timeout=0)
                kill.assert_called_once_with(os.getpid(), 0)
