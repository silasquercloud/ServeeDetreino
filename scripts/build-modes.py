#!/usr/bin/env python3
"""Compile the isolated Bridge/HG plugin against pinned Spigot 1.8.8."""
import hashlib, pathlib, subprocess, urllib.request, zipfile, shutil
from build import DEPS, CACHE, ROOT, TARGET

def main():
    CACHE.mkdir(exist_ok=True);TARGET.mkdir(exist_ok=True)
    for name,(url,digest) in DEPS.items():
        path=CACHE/name
        if not path.exists():
            with urllib.request.urlopen(url,timeout=120) as r:data=r.read()
            if hashlib.sha256(data).hexdigest()!=digest:raise ValueError('Checksum inválido: '+name)
            path.write_bytes(data)
        if hashlib.sha256(path.read_bytes()).hexdigest()!=digest:raise ValueError('Cache inválido: '+name)
    out=TARGET/'modes-classes'
    if out.exists():shutil.rmtree(out)
    out.mkdir()
    subprocess.run(['java','-jar',str(CACHE/'ecj.jar'),'-1.8','-proc:none','-encoding','UTF-8','-cp',str(CACHE/'spigot-api.jar'),'-d',str(out),*map(str,(ROOT/'modes/src').rglob('*.java'))],check=True)
    tests=TARGET/'modes-tests';tests.mkdir(exist_ok=True)
    subprocess.run(['java','-jar',str(CACHE/'ecj.jar'),'-1.8','-proc:none','-cp',str(out),'-d',str(tests),str(ROOT/'modes/tests/ModeRulesTest.java')],check=True)
    subprocess.run(['java','-cp',str(out)+':'+str(tests),'ModeRulesTest'],check=True)
    jar=TARGET/'RyuexModes-1.0.0.jar'
    with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
        z.write(ROOT/'modes/plugin.yml','plugin.yml')
        for p in out.rglob('*.class'):z.write(p,p.relative_to(out))
    print(jar)
if __name__=='__main__':main()
