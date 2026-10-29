#!/usr/bin/env python3
"""Provision verified Temurin JDKs and the pinned Playit tools before opening the UI."""
import glob
import hashlib
import json
import os
import pathlib
import platform
import re
import shlex
import shutil
import subprocess
import tarfile
import tempfile
import urllib.parse
import urllib.request
import playit

ROOT = pathlib.Path(__file__).resolve().parents[1]
TOOLCHAINS = ROOT/'.toolchains'
USER_AGENT = 'ServeeDetreino/1.0 (https://github.com/silasquercloud/ServeeDetreino)'


def java_major(executable):
    try:
        result = subprocess.run([str(executable), '-version'], capture_output=True, text=True, timeout=15)
        match = re.search(r'version "([0-9]+)(?:\.([0-9]+))?', result.stdout+result.stderr)
        if result.returncode or not match:
            return None
        return int(match[2]) if match[1] == '1' else int(match[1])
    except (OSError, subprocess.SubprocessError):
        return None


def find_java(major):
    candidates = [TOOLCHAINS/('java'+str(major))/'bin/java']
    if major == 8 and os.environ.get('JAVA8_BIN'):
        candidates.append(pathlib.Path(os.environ['JAVA8_BIN']))
    for name in (f'JAVA{major}_HOME', f'JAVA_HOME_{major}_X64', 'JAVA_HOME'):
        if os.environ.get(name):
            candidates.append(pathlib.Path(os.environ[name])/'bin/java')
    if shutil.which('java'):
        candidates.append(pathlib.Path(shutil.which('java')))
    for pattern in [f'/usr/local/sdkman/candidates/java/{major}*/bin/java', str(pathlib.Path.home()/f'.sdkman/candidates/java/{major}*/bin/java'), f'/usr/lib/jvm/*{major}*/bin/java']:
        candidates.extend(pathlib.Path(p) for p in glob.glob(pattern))
    for path in candidates:
        path = path.resolve()
        if path.is_file() and (path.parent/'javac').is_file() and java_major(path) == major:
            return path.parent.parent
    return None


def get(url):
    request = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
    return urllib.request.urlopen(request, timeout=180)


def within(path, root):
    try:
        path.relative_to(root)
        return True
    except ValueError:
        return False


def safe_extract(archive, destination):
    destination = destination.resolve()
    for entry in archive.getmembers():
        target = (destination/entry.name).resolve()
        if not within(target, destination) or entry.isdev() or entry.isfifo():
            raise RuntimeError('Arquivo Java contém uma entrada de extração inválida.')
        if entry.issym() or entry.islnk():
            link = ((target.parent if entry.issym() else destination)/entry.linkname).resolve()
            if not within(link, destination):
                raise RuntimeError('Arquivo Java contém um link fora da instalação.')
    if hasattr(tarfile, 'data_filter'):
        archive.extractall(destination, filter='data')
    else:
        archive.extractall(destination)


def install_java(major):
    architecture = {'x86_64': 'x64', 'amd64': 'x64', 'aarch64': 'aarch64', 'arm64': 'aarch64'}.get(platform.machine().lower())
    if platform.system() != 'Linux' or not architecture:
        raise RuntimeError('Java automático disponível em Linux x64/ARM64.')
    with (ROOT/'config/toolchains.json').open() as source:
        toolchains = json.load(source)
    asset = toolchains[str(major)]
    package = asset['packages'][architecture]
    url = urllib.parse.urlsplit(package['link'])
    if url.scheme != 'https' or url.hostname != 'github.com' or not url.path.startswith(f'/adoptium/temurin{major}-binaries/releases/download/'):
        raise RuntimeError('Origem inesperada para o download Java.')
    expected = package['checksum'].lower()
    if not re.fullmatch('[0-9a-f]{64}', expected):
        raise RuntimeError('Checksum Java inválido.')
    TOOLCHAINS.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='install-', dir=TOOLCHAINS) as temporary:
        staging = pathlib.Path(temporary)
        bundle = staging/'jdk.tar.gz'
        print(f'Instalando Temurin JDK {major} ({asset["release"]}) com verificação SHA-256...', flush=True)
        digest = hashlib.sha256()
        with get(package['link']) as response, bundle.open('wb') as output:
            while True:
                chunk = response.read(1024*1024)
                if not chunk:
                    break
                digest.update(chunk)
                output.write(chunk)
        if digest.hexdigest() != expected:
            raise RuntimeError('Checksum Java incorreto. Download não instalado.')
        extracted = staging/'unpacked'
        extracted.mkdir()
        with tarfile.open(bundle, 'r:gz') as archive:
            safe_extract(archive, extracted)
        roots = [p.parent.parent for p in extracted.glob('*/bin/java') if (p.parent/'javac').is_file()]
        if len(roots) != 1 or java_major(roots[0]/'bin/java') != major:
            raise RuntimeError('O arquivo não contém o JDK esperado.')
        destination = TOOLCHAINS/('java'+str(major))
        if destination.exists():
            backup = pathlib.Path(tempfile.mkdtemp(prefix=f'java{major}-previous-', dir=TOOLCHAINS))
            backup.rmdir()
            destination.rename(backup)
        roots[0].rename(destination)
        (TOOLCHAINS/f'java{major}-receipt.json').write_text(json.dumps({'release': asset['release'], 'url': package['link'], 'sha256': expected}, indent=2)+'\n')
    return destination


def provision():
    TOOLCHAINS.mkdir(exist_ok=True)
    homes = {}
    for major in (17, 8):
        homes[major] = find_java(major) or install_java(major)
    playit.ensure_tools()
    # Quote paths as shell data; do not touch global Java defaults or proxy settings.
    content = '# Generated by scripts/dependencies.py\n'
    content += 'export JAVA_HOME='+shlex.quote(str(homes[17]))+'\n'
    content += 'export JAVA8_BIN='+shlex.quote(str(homes[8]/'bin/java'))+'\n'
    content += 'export PATH="$JAVA_HOME/bin:$PATH"\n'
    path = TOOLCHAINS/'environment.sh'
    temporary = path.with_suffix('.tmp')
    temporary.write_text(content)
    temporary.replace(path)
    print('Dependências prontas: Python, Git, curl, JDK 17, JDK 8 e Playit.', flush=True)


if __name__ == '__main__':
    try:
        provision()
    except (OSError, RuntimeError, ValueError, KeyError, subprocess.SubprocessError, tarfile.TarError) as error:
        raise SystemExit('Falha ao preparar dependências: '+str(error))
