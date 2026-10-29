#!/usr/bin/env python3
"""Compilação reproduzível, inclusive sem Maven/JDK completo: Java 17+ e Python 3."""
import hashlib
import pathlib
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
CACHE = ROOT / '.cache'
TARGET = ROOT / 'target'
DEPS = {
    'ecj.jar': ('https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.33.0/ecj-3.33.0.jar', 'f7686c4960cf70c2ebc5c500a73a8cfc04541b730c18f1c5c21329889b137f45'),
    'spigot-api.jar': ('https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/spigot-api-1.8.8-R0.1-20160221.082514-43-shaded.jar', '2e192832b9ddb70d89b376559282969371d9b062110d5b93aa698ae9784f8608'),
}

def main():
    CACHE.mkdir(exist_ok=True)
    for name, (url, expected) in DEPS.items():
        dest = CACHE / name
        if not dest.exists():
            req = urllib.request.Request(url, headers={'User-Agent': 'ServeeDetreino/1.0 (https://github.com/silasquercloud/ServeeDetreino)'})
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if hashlib.sha256(data).hexdigest() != expected:
                raise SystemExit('Checksum incorreto: ' + name)
            dest.write_bytes(data)
        if hashlib.sha256(dest.read_bytes()).hexdigest() != expected:
            raise SystemExit('Cache modificado: apague ' + str(dest))
    classes = TARGET / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    sources = sorted((ROOT / 'src/main/java').rglob('*.java'))
    subprocess.run(['java', '-jar', str(CACHE/'ecj.jar'), '-1.8', '-proc:none', '-encoding', 'UTF-8', '-cp', str(CACHE/'spigot-api.jar'), '-d', str(classes), *map(str, sources)], check=True)
    tests = sorted((ROOT / 'src/test/java').rglob('*.java'))
    subprocess.run(['java', '-jar', str(CACHE/'ecj.jar'), '-1.8', '-proc:none', '-encoding', 'UTF-8', '-cp', str(classes), '-d', str(TARGET/'test-classes'), *map(str, tests)], check=True)
    import os
    subprocess.run(['java', '-cp', os.pathsep.join(map(str, [classes, TARGET/'test-classes'])), 'br.ryuex.treino.RulesTest'], check=True)
    artifact = TARGET/'TreinoHub-1.0.0.jar' 
    with zipfile.ZipFile(artifact, 'w', zipfile.ZIP_DEFLATED) as out:
        for base in (classes, ROOT/'src/main/resources'):
            for path in sorted(base.rglob('*')):
                if path.is_file():
                    out.write(path, path.relative_to(base))
    print('Plugin compilado: ' + str(artifact))

if __name__ == '__main__':
    main()
