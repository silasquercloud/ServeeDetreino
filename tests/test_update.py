import contextlib
import importlib
import io
import pathlib
import subprocess
import sys
import tempfile
import unittest
import zipfile
from types import SimpleNamespace
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'scripts'))
updater = importlib.import_module('update')
from runtime_lock import server_lock


class UpdateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temp.name)
        self.destination = self.root/'server/plugins/TreinoHub-1.0.0.jar'
        self.destination.parent.mkdir(parents=True)
        self.destination.write_bytes(b'previous-plugin')
        (self.root/'server/server.jar').write_bytes(b'pinned-spigot')
        (self.root/'server/server.properties').write_text('online-mode=false\nwhite-list=true\nlevel-name=custom-world\n')
        (self.root/'.private').mkdir()
        (self.root/'.private/playit.secret').write_text('fake-test-fixture')
        (self.root/'server/plugins/TreinoHub').mkdir()
        (self.root/'server/plugins/TreinoHub/config.yml').write_text('max-rooms: 8\n')
        self.root_patch = patch.object(updater, 'ROOT', self.root)
        self.root_patch.start()
        self.output = contextlib.redirect_stdout(io.StringIO())
        self.output.__enter__()
        self.commands = []

    def tearDown(self):
        self.output.__exit__(None, None, None)
        self.root_patch.stop()
        self.temp.cleanup()

    def fake_execute(self, args, capture=False):
        self.commands.append(args)
        if args[-1] == 'scripts/build.py':
            directory = self.root/'target'
            directory.mkdir()
            for name in ['TreinoHub-1.0.0.jar', 'TreinoAntiLag-1.0.0.jar']:
                with zipfile.ZipFile(directory/name, 'w') as archive:
                    archive.writestr('plugin.yml', 'name: '+name+'\n')
        return SimpleNamespace(stdout='')

    def test_success_backs_up_plugin_and_preserves_local_data(self):
        props = (self.root/'server/server.properties').read_bytes()
        with patch.object(updater, 'execute', side_effect=self.fake_execute):
            updater.update()
        self.assertIn(['git', 'pull', '--ff-only'], self.commands)
        self.assertEqual((self.root/'server/server.properties').read_bytes(), props)
        self.assertEqual((self.root/'.private/playit.secret').read_text(), 'fake-test-fixture')
        self.assertEqual((self.root/'server/plugins/TreinoHub/config.yml').read_text(), 'max-rooms: 8\n')
        self.assertEqual((self.root/'server/server.jar').read_bytes(), b'pinned-spigot')
        self.assertEqual(next((self.root/'server/backups').glob('*.jar')).read_bytes(), b'previous-plugin')
        self.assertTrue((self.destination.parent/'TreinoAntiLag-1.0.0.jar').exists())
        with zipfile.ZipFile(self.destination) as archive:
            self.assertIn('plugin.yml', archive.namelist())

    def test_invalid_second_plugin_does_not_replace_first(self):
        def invalid(args, capture=False):
            result = self.fake_execute(args, capture)
            if args[-1] == 'scripts/build.py':
                (self.root/'target/TreinoAntiLag-1.0.0.jar').write_bytes(b'bad-archive')
            return result
        with patch.object(updater, 'execute', side_effect=invalid):
            with self.assertRaises(zipfile.BadZipFile):
                updater.update()
        self.assertEqual(self.destination.read_bytes(), b'previous-plugin')
        self.assertFalse((self.destination.parent/'TreinoAntiLag-1.0.0.jar').exists())

    def test_build_failure_keeps_installed_plugin(self):
        def fail(args, capture=False):
            if args[-1] == 'scripts/build.py':
                raise subprocess.CalledProcessError(1, args)
            return SimpleNamespace(stdout='')
        with patch.object(updater, 'execute', side_effect=fail):
            with self.assertRaises(subprocess.CalledProcessError):
                updater.update()
        self.assertEqual(self.destination.read_bytes(), b'previous-plugin')

    def test_dirty_source_is_not_reset_or_pulled(self):
        def dirty(args, capture=False):
            self.commands.append(args)
            return SimpleNamespace(stdout=' M README.md' if 'status' in args else '')
        with patch.object(updater, 'execute', side_effect=dirty):
            with self.assertRaisesRegex(RuntimeError, 'modificados'):
                updater.update()
        self.assertNotIn(['git', 'pull', '--ff-only'], self.commands)
        self.assertEqual(self.destination.read_bytes(), b'previous-plugin')

    def test_running_server_lock_prevents_update(self):
        with server_lock(self.root), patch.object(updater, 'execute') as execute:
            with self.assertRaisesRegex(RuntimeError, 'stop'):
                updater.update()
            execute.assert_not_called()


if __name__ == '__main__':
    unittest.main()
