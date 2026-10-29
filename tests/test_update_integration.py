"""Exercise real Git pull -> new updater -> build -> both installed JARs."""
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


@unittest.skipUnless(shutil.which('git'), 'Git required')
class RealUpdateTests(unittest.TestCase):
    def test_real_pull_installs_remote_build_and_preserves_properties(self):
        with tempfile.TemporaryDirectory() as temporary:
            base = pathlib.Path(temporary)
            origin, seed, instance = [base/p for p in ('remote.git', 'seed', 'codespace')]
            def git(directory, *args):
                return subprocess.run(['git', *args], cwd=directory, check=True, capture_output=True, text=True)
            git(base, 'init', '--bare', str(origin))
            seed.mkdir()
            git(seed, 'init', '-b', 'main')
            git(seed, 'config', 'user.name', 'Update Test')
            git(seed, 'config', 'user.email', 'test@example.invalid')
            (seed/'scripts').mkdir()
            for name in ['update.py', 'runtime_lock.py', 'server_control.py']:
                shutil.copy2(ROOT/'scripts'/name, seed/'scripts'/name)
            (seed/'scripts/update.sh').write_text('ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"\nexec python3 "$ROOT/scripts/update.py" "$@"\n')
            (seed/'.gitignore').write_text('server/\n.private/\ntarget/\n__pycache__/\n')
            build = seed/'scripts/build.py'
            def write_build(version):
                build.write_text("import pathlib,zipfile\np=pathlib.Path('target');p.mkdir(exist_ok=True)\nfor name in ['TreinoHub','TreinoAntiLag']:\n with zipfile.ZipFile(p/(name+'-1.0.0.jar'),'w') as z:z.writestr('plugin.yml','name: '+name+'\\nversion: "+version+"\\n')\n")
            write_build('old')
            git(seed, 'add', '.')
            git(seed, 'commit', '-m', 'initial updater')
            git(seed, 'remote', 'add', 'origin', str(origin))
            git(seed, 'push', '-u', 'origin', 'main')
            git(base, 'clone', '-b', 'main', str(origin), str(instance))
            (instance/'server/plugins').mkdir(parents=True)
            (instance/'server/server.jar').write_bytes(b'pinned-server')
            (instance/'server/server.properties').write_text('level-name=my-world\nwhite-list=true\n')
            write_build('new-remote')
            git(seed, 'add', '.')
            git(seed, 'commit', '-m', 'new build')
            git(seed, 'push')
            result = subprocess.run([sys.executable, 'scripts/update.py'], cwd=instance, check=True, capture_output=True, text=True, timeout=30)
            for name in ['TreinoHub', 'TreinoAntiLag']:
                with zipfile.ZipFile(instance/'server/plugins'/(name+'-1.0.0.jar')) as archive:
                    self.assertIn('version: new-remote', archive.read('plugin.yml').decode())
            self.assertEqual((instance/'server/server.properties').read_text(), 'level-name=my-world\nwhite-list=true\n')
            self.assertIn('versão new-remote', result.stdout)
            self.assertTrue((instance/'.private/update-installed.json').is_file())
