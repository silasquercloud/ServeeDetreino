import contextlib
import importlib
import io
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'scripts'))
deps = importlib.import_module('dependencies')


class BootstrapTests(unittest.TestCase):
    def fixture(self, directory, python=False, fail=False):
        binary = directory/'bin'
        binary.mkdir()
        for name in ('dirname', 'env', 'chmod'):
            (binary/name).symlink_to(shutil.which(name))
        for name in ('git', 'curl', 'tar', 'gzip', 'unzip', 'zip'):
            path = binary/name
            path.write_text('#!/bin/bash\nexit 0\n')
            path.chmod(0o755)
        identity = binary/'id'
        identity.write_text('#!/bin/bash\nprintf "0\\n"\n')
        identity.chmod(0o755)
        apt = binary/'apt-get'
        apt.write_text('''#!/bin/bash
printf '%s\\n' "$*" >> "$FAKE_LOG"
if [[ "$1" == install ]]; then
  if [[ "${FAKE_FAIL:-}" == 1 ]]; then exit 42; fi
  printf '#!/bin/bash\\nexit 0\\n' > "$FAKE_BIN/python3"
  chmod +x "$FAKE_BIN/python3"
fi
''')
        apt.chmod(0o755)
        if python:
            p = binary/'python3'
            p.write_text('#!/bin/bash\nexit 0\n')
            p.chmod(0o755)
        return dict(os.environ, PATH=str(binary), FAKE_BIN=str(binary), FAKE_LOG=str(directory/'apt.log'), FAKE_FAIL='1' if fail else '0')

    def test_missing_python_is_installed_before_python_is_called(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = pathlib.Path(temp)
            environment = self.fixture(folder)
            result = subprocess.run(['/bin/bash', str(ROOT/'scripts/bootstrap.sh'), '--base-only'], env=environment, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertTrue((folder/'bin/python3').is_file())
            log = (folder/'apt.log').read_text()
            self.assertIn('update', log)
            self.assertIn('python3-venv', log)
            self.assertIn('ca-certificates', log)

    def test_failed_system_install_stops_before_menu(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = pathlib.Path(temp)
            result = subprocess.run(['/bin/bash', str(ROOT/'scripts/bootstrap.sh'), '--base-only'], env=self.fixture(folder, fail=True), capture_output=True, text=True)
            self.assertEqual(result.returncode, 42)
            self.assertFalse((folder/'bin/python3').exists())

    def test_existing_dependencies_do_not_rerun_apt(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = pathlib.Path(temp)
            result = subprocess.run(['/bin/bash', str(ROOT/'scripts/bootstrap.sh'), '--base-only'], env=self.fixture(folder, python=True), capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertFalse((folder/'apt.log').exists())


class RuntimeTests(unittest.TestCase):
    def test_jre_without_compiler_is_not_mistaken_for_jdk(self):
        with tempfile.TemporaryDirectory() as temp:
            toolchains = pathlib.Path(temp)
            home = toolchains/'java8'
            (home/'bin').mkdir(parents=True)
            (home/'bin/java').write_text('fixture')
            with patch.object(deps, 'TOOLCHAINS', toolchains), patch.object(deps, 'java_major', return_value=8), patch.object(deps.shutil, 'which', return_value=None), patch.object(deps.glob, 'glob', return_value=[]), patch.dict(os.environ, {}, clear=True):
                self.assertIsNone(deps.find_java(8))
                (home/'bin/javac').write_text('fixture')
                self.assertEqual(deps.find_java(8), home)

    def test_archive_cannot_extract_outside_installation(self):
        with tempfile.TemporaryDirectory() as temp:
            destination = pathlib.Path(temp)/'jdk'
            destination.mkdir()
            payload = io.BytesIO()
            with tarfile.open(fileobj=payload, mode='w') as out:
                entry = tarfile.TarInfo('../outside')
                entry.size = 1
                out.addfile(entry, io.BytesIO(b'x'))
            payload.seek(0)
            with tarfile.open(fileobj=payload) as archive:
                with self.assertRaises(RuntimeError):
                    deps.safe_extract(archive, destination)
            self.assertFalse((destination.parent/'outside').exists())

    def test_download_checksum_failure_does_not_install_jdk(self):
        with tempfile.TemporaryDirectory() as temp:
            toolchains = pathlib.Path(temp)
            metadata = {'8': {'release': 'test', 'packages': {'x64': {'link': 'https://github.com/adoptium/temurin8-binaries/releases/download/test/jdk.tar.gz', 'checksum': '0'*64}}}}
            (toolchains/'config').mkdir()
            (toolchains/'config/toolchains.json').write_text(json.dumps(metadata))
            with patch.object(deps, 'TOOLCHAINS', toolchains/'tools'), patch.object(deps, 'ROOT', toolchains), patch.object(deps.platform, 'machine', return_value='x86_64'), patch.object(deps, 'get', return_value=io.BytesIO(b'wrong-download')), contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaisesRegex(RuntimeError, 'Checksum'):
                    deps.install_java(8)
            self.assertFalse((toolchains/'tools/java8').exists())

    def test_generated_environment_preserves_paths_with_spaces(self):
        with tempfile.TemporaryDirectory(prefix='treino java ') as temp:
            toolchains = pathlib.Path(temp)/'toolchains'
            homes = {17: pathlib.Path(temp)/'jdk 17', 8: pathlib.Path(temp)/'jdk 8'}
            with patch.object(deps, 'TOOLCHAINS', toolchains), patch.object(deps, 'find_java', side_effect=lambda major: homes[major]), patch.object(deps.playit, 'ensure_tools') as tools, contextlib.redirect_stdout(io.StringIO()):
                deps.provision()
                tools.assert_called_once()
            output = subprocess.check_output(['/bin/bash', '-c', 'source "$1"; printf "%s\\n%s\\n" "$JAVA_HOME" "$JAVA8_BIN"', 'bash', str(toolchains/'environment.sh')], text=True)
            self.assertEqual(output.splitlines(), [str(homes[17]), str(homes[8]/'bin/java')])


if __name__ == '__main__':
    unittest.main()
