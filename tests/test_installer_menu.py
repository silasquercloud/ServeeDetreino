import contextlib
import importlib
import io
import pathlib
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'scripts'))
menu = importlib.import_module('menu')
config = importlib.import_module('server_config')
playit = importlib.import_module('playit')


class PropertiesTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temp.name)
        self.path = self.root/'server.properties'
        self.defaults = self.root/'defaults.properties'
        self.defaults.write_text('online-mode=true\nwhite-list=true\nlevel-name=world\n')
        self.patch = patch.multiple(config, PROPERTIES=self.path, DEFAULTS=self.defaults)
        self.patch.start()
        self.output = contextlib.redirect_stdout(io.StringIO())
        self.output.__enter__()

    def tearDown(self):
        self.output.__exit__(None, None, None)
        self.patch.stop()
        self.temp.cleanup()

    def test_first_install_allows_original_and_offline_clients(self):
        config.apply()
        values = config.check()
        self.assertEqual(values['online-mode'], 'false')
        self.assertEqual(values['white-list'], 'false')
        self.assertEqual(values['server-port'], '25565')
        self.assertEqual(values['server-ip'], '')

    def test_migration_preserves_world_settings_and_creates_exact_backup(self):
        original = '# keep this comment\nonline-mode=true\nlevel-name=my-custom-world\nview-distance=4\nonline-mode=true\nwhite-list=true\n'
        self.path.write_text(original)
        config.apply()
        current = self.path.read_text()
        self.assertIn('level-name=my-custom-world', current)
        self.assertIn('view-distance=4', current)
        self.assertIn('# keep this comment', current)
        self.assertEqual(current.count('online-mode='), 1)
        backups = list(self.root.glob('server.properties.backup-*'))
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0].read_text(), original)
        config.apply()
        self.assertEqual(len(list(self.root.glob('server.properties.backup-*'))), 1)

    def test_start_rejects_old_authenticated_configuration(self):
        self.path.write_text('online-mode=true\nserver-ip=\nserver-port=25565\n')
        with self.assertRaisesRegex(RuntimeError, 'opção 2'):
            config.check()

    def test_whitelist_can_be_reenabled_after_install(self):
        config.apply()
        self.path.write_text(self.path.read_text().replace('white-list=false', 'white-list=true'))
        self.assertEqual(config.check()['white-list'], 'true')


class AddressTests(unittest.TestCase):
    def test_explicit_public_port_is_not_replaced_with_local_port(self):
        info = playit.connection_info({'public_address': 'example.ply.gg:31123'})
        self.assertEqual(info, {'host': 'example.ply.gg', 'port': 31123, 'connect': 'example.ply.gg:31123'})

    def test_unknown_srv_port_is_not_invented(self):
        info = playit.connection_info({'public_address': 'example.joinmc.link'})
        self.assertIsNone(info['port'])
        self.assertEqual(info['connect'], 'example.joinmc.link')

    def test_separate_public_port_is_used(self):
        info = playit.connection_info({'public_address': 'example.ply.gg', 'public_port': 39999})
        self.assertEqual(info['connect'], 'example.ply.gg:39999')

    def test_invalid_port_fails_before_start(self):
        with self.assertRaises(ValueError):
            playit.connection_info({'public_address': 'example.ply.gg', 'public_port': 99999})


class MenuTests(unittest.TestCase):
    def test_menu_blocks_install_and_start_before_playit_setup(self):
        with patch.object(menu, 'playit_state', return_value=None), patch.object(menu, 'installed', return_value=False), patch.object(menu, 'call') as call, patch('builtins.input', side_effect=['2', '3', 'bad', '0']), contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(menu.main(), 0)
            call.assert_not_called()
            self.assertIn('opção 1', output.getvalue())

    def test_refusing_eula_does_not_launch_server(self):
        data = {'public_address': 'example.ply.gg:31123'}
        with patch.object(menu, 'playit_state', return_value=data), patch.object(menu, 'installed', return_value=True), patch.object(config, 'check', return_value={'online-mode': 'false'}), patch.object(menu, 'ensure_eula', return_value=False), patch.object(menu, 'call') as call:
            menu.start()
            call.assert_not_called()

    def test_explicit_eula_acceptance_is_passed_to_launcher(self):
        data = {'public_address': 'example.ply.gg:31123'}
        with patch.object(menu, 'playit_state', return_value=data), patch.object(menu, 'installed', return_value=True), patch.object(config, 'check', return_value={'online-mode': 'false'}), patch.object(menu, 'ensure_eula', return_value=True), patch.object(menu, 'call', return_value=True) as call, contextlib.redirect_stdout(io.StringIO()) as output:
            menu.start()
            self.assertEqual(call.call_args.args[1]['ACCEPT_EULA'], 'true')
            self.assertIn('31123', output.getvalue())

    def test_eula_prompt_does_not_write_acceptance_on_refusal(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            path = root/'server/eula.txt'
            path.parent.mkdir()
            path.write_text('eula=false\n')
            with patch.object(menu, 'ROOT', root), patch('builtins.input', return_value='não'), contextlib.redirect_stdout(io.StringIO()):
                self.assertFalse(menu.ensure_eula())
            self.assertEqual(path.read_text(), 'eula=false\n')


if __name__ == '__main__':
    unittest.main()
