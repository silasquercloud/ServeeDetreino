import importlib.util
import json
import pathlib
import tempfile
import unittest
import sys
from unittest.mock import Mock, patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'scripts'))
spec = importlib.util.spec_from_file_location('treino_playit', ROOT/'scripts/playit.py')
playit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(playit)


class SetupTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.directory = pathlib.Path(self.temporary.name)
        self.patcher = patch.multiple(playit, ROOT=self.directory, PRIVATE=self.directory, SECRET=self.directory/'secret', READY=self.directory/'ready.json', SOCKET=self.directory/'agent.sock')
        self.patcher.start()

    def tearDown(self):
        self.patcher.stop()
        self.temporary.cleanup()

    def ready(self):
        playit.SECRET.write_text('test-fixture-not-a-real-credential')
        playit.READY.write_text(json.dumps({'local_address': '127.0.0.1:25565', 'public_address': 'test.joinmc.link:12345'}))

    def test_no_install_before_claim(self):
        with self.assertRaisesRegex(RuntimeError, 'ANTES'):
            playit.check_ready()

    def test_ready_marker_alone_is_not_enough(self):
        self.ready()
        playit.SECRET.unlink()
        with self.assertRaisesRegex(RuntimeError, 'ANTES'):
            playit.check_ready()

    def test_local_target_must_match_minecraft(self):
        self.ready()
        data = json.loads(playit.READY.read_text())
        data['local_address'] = '127.0.0.1:25575'
        playit.READY.write_text(json.dumps(data))
        with self.assertRaisesRegex(RuntimeError, 'incompleto'):
            playit.check_ready()

    def test_ready_state_is_reused(self):
        self.ready()
        self.assertEqual(playit.check_ready()['public_address'], 'test.joinmc.link:12345')

    def test_public_address_is_never_a_command_or_http_url(self):
        for value in ['https://a.joinmc.link', 'localhost', 'a.joinmc.link;echo x', 'a.joinmc.link:0', 'a.joinmc.link:99999', 'a..b', '127.0.0.1:25565', '192.168.0.1', None]:
            self.assertFalse(playit.valid_address(value), value)
        self.assertTrue(playit.valid_address('one.joinmc.link:25565'))

    def test_phase_parser_does_not_trust_cli_exit_code(self):
        self.assertIsNone(playit.phase('The playit service is not running.'))
        self.assertEqual(playit.phase('  Phase: running\n  PID: 42'), 'running')

    def test_invalid_agent_fails_before_server_start(self):
        agent = Mock()
        agent.poll.return_value = None
        with patch.object(playit, 'status', return_value='invalid secret'):
            with self.assertRaisesRegex(RuntimeError, 'invalid secret'):
                playit.wait_running(agent)

    def test_start_failure_stops_agent(self):
        self.ready()
        agent = Mock()
        agent.poll.return_value = None
        with patch.object(playit, 'ensure_tools'), patch.object(playit, 'start_agent', return_value=agent), patch.object(playit, 'wait_running', side_effect=RuntimeError('network')), patch.object(playit, 'stop') as stop:
            with self.assertRaisesRegex(RuntimeError, 'network'):
                playit.run()
            self.assertIn(unittest.mock.call(agent), stop.call_args_list)

    def test_stop_request_reaches_child_console_before_agent_closes(self):
        self.ready()
        import io
        import subprocess
        import sys
        marker = self.directory/'saved.txt'
        real_popen = subprocess.Popen
        program = "import sys,pathlib; line=sys.stdin.readline(); assert line.strip()=='stop'; pathlib.Path(sys.argv[1]).write_text('saved')"
        child = real_popen([sys.executable, '-c', program, str(marker)], stdin=subprocess.PIPE, text=True)
        agent = Mock()
        agent.poll.return_value = None
        control = Mock()
        control.__enter__ = Mock(return_value=control)
        control.__exit__ = Mock(return_value=False)
        control.requested.return_value = True
        def close_agent(process):
            self.assertTrue(marker.exists(), 'Minecraft must save before agent stops')
        try:
            with patch.object(playit, 'ensure_tools'), patch.object(playit, 'start_agent', return_value=agent), patch.object(playit, 'wait_running'), patch.object(playit, 'print_connection'), patch.object(playit, 'Control', return_value=control), patch.object(playit.subprocess, 'Popen', return_value=child), patch.object(playit, 'stop', side_effect=close_agent), patch.object(playit.sys, 'stdin', io.StringIO('')):
                playit._run_locked()
            self.assertEqual(child.returncode, 0)
            self.assertEqual(marker.read_text(), 'saved')
        finally:
            if child.poll() is None:
                child.terminate()
                child.wait(timeout=5)
            child.stdin.close()

    def test_missing_setup_prevents_agent_launch(self):
        with patch.object(playit, 'start_agent') as start:
            with self.assertRaises(RuntimeError):
                playit.run()
            start.assert_not_called()

    def test_modified_download_is_rejected(self):
        tools = self.directory/'tools'
        tools.mkdir()
        (tools/'playit').write_bytes(b'wrong-binary')
        with patch.object(playit, 'TOOLS', tools), patch.object(playit, 'arch', return_value='amd64'):
            with self.assertRaisesRegex(RuntimeError, 'alterado'):
                playit.ensure_tools()


if __name__ == '__main__':
    unittest.main()
