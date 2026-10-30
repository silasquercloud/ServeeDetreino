#!/usr/bin/env python3
"""Original 1.8 Anvil arenas fitted to BWFE coordinates. Never downloads map assets."""
import argparse, collections, gzip, hashlib, json, math, pathlib, struct, time, zlib
import yaml
P=struct.pack

def tag(t,n,v):
    n=n.encode();return bytes([t])+P('>H',len(n))+n+payload(t,v)
def payload(t,v):
    if t==1:return P('>b',v)
    if t==3:return P('>i',v)
    if t==4:return P('>q',v)
    if t==8:
        b=v.encode();return P('>H',len(b))+b
    if t==7:return P('>i',len(v))+bytes(v)
    if t==11:return P('>i',len(v))+b''.join(P('>i',x) for x in v)
    if t==9:return bytes([v[0]])+P('>i',len(v[1]))+b''.join(payload(v[0],x) for x in v[1])
    if t==10:return b''.join(tag(a,b,c) for a,b,c in v)+b'\0'
    raise ValueError(t)
def loc(s):return tuple(float(x) for x in str(s).split(',')[:5])
def xyz(s):return tuple(math.floor(x) for x in loc(s)[:3])
PALETTES=[('Jardins',2,1,5),('Deserto',24,24,1),('Glacial',80,1,3),('Templo',98,1,14),('Bosque',2,17,13),('Quartzo',155,1,10)]
COLORS={'RED':14,'BLUE':11,'GREEN':5,'YELLOW':4,'AQUA':9,'WHITE':0,'PINK':6,'GRAY':7}
class Map:
    def __init__(self):self.blocks={}
    def set(self,x,y,z,b,d=0):
        if 0<=y<256:
            if b:self.blocks[x,y,z]=(b,d)
            else:self.blocks.pop((x,y,z),None)
    def disc(self,x,y,z,rx,rz,top,rock):
        for dx in range(-rx,rx+1):
            for dz in range(-rz,rz+1):
                r=(dx/rx)**2+(dz/rz)**2
                if r<=1:
                    for h in range(max(2,int((1-r)*5)+2)):
                        self.set(x+dx,y-h,z+dz,top if h==0 else rock)
    def path(self,a,b,material=98,width=1):
        steps=max(abs(b[0]-a[0]),abs(b[1]-a[1]),abs(b[2]-a[2]),1)
        for i in range(steps+1):
            x,y,z=[round(a[k]+(b[k]-a[k])*i/steps) for k in range(3)]
            for dx in range(-width,width+1):
                for dz in range(-width,width+1):self.set(x+dx,y,z+dz,material)
            for dy in (1,2,3):self.set(x,y+dy,z,0)
    def pad(self,s,block=98,r=2):
        x,y,z=xyz(s);y-=1;self.disc(x,y,z,r,r,block,1)
        for dx in range(-r,r+1):
            for dz in range(-r,r+1):
                for dy in range(1,4):self.set(x+dx,y+dy,z+dz,0)
        return x,y,z
    def export(self,root,name,spawn):
        root.mkdir(parents=True);chunks=collections.defaultdict(dict)
        for (x,y,z),v in self.blocks.items():chunks[x//16,z//16][x%16,y,z%16]=v
        regions=collections.defaultdict(dict)
        for (cx,cz),blocks in chunks.items():
            sections=[];height=[0]*256
            for sy in sorted({y//16 for x,y,z in blocks}):
                ids=bytearray(4096);data=bytearray(2048)
                for (x,y,z),(b,d) in blocks.items():
                    if y//16!=sy:continue
                    idx=(y%16)*256+z*16+x;ids[idx]=b;data[idx//2]|=(d&15)<<(4*(idx%2));height[z*16+x]=max(height[z*16+x],y+1)
                sections.append([(1,'Y',sy),(7,'Blocks',ids),(7,'Data',data),(7,'SkyLight',b'\xff'*2048),(7,'BlockLight',b'\0'*2048)])
            level=[(3,'xPos',cx),(3,'zPos',cz),(4,'LastUpdate',0),(1,'TerrainPopulated',1),(1,'LightPopulated',0),(4,'InhabitedTime',0),(11,'HeightMap',height),(7,'Biomes',b'\x01'*256),(9,'Sections',(10,sections)),(9,'Entities',(10,[])),(9,'TileEntities',(10,[]))]
            blob=zlib.compress(tag(10,'',[(10,'Level',level)]));record=P('>I',len(blob)+1)+b'\x02'+blob
            regions[cx//32,cz//32][(cz%32)*32+cx%32]=record
        (root/'region').mkdir()
        for (rx,rz),entries in regions.items():
            header=bytearray(8192);data=bytearray();offset=2
            for idx,record in sorted(entries.items()):
                sectors=math.ceil(len(record)/4096);header[idx*4:idx*4+4]=P('>I',(offset<<8)|sectors);data.extend(record+b'\0'*(sectors*4096-len(record)));offset+=sectors
            (root/'region'/f'r.{rx}.{rz}.mca').write_bytes(header+data)
        x,y,z=spawn
        data=[(3,'version',19133),(8,'LevelName',name),(8,'generatorName','flat'),(8,'generatorOptions','3;minecraft:air;1;'),(3,'generatorVersion',0),(4,'RandomSeed',0),(3,'GameType',0),(1,'MapFeatures',0),(1,'initialized',1),(1,'allowCommands',1),(3,'SpawnX',x),(3,'SpawnY',y),(3,'SpawnZ',z),(4,'Time',6000),(4,'DayTime',6000),(4,'LastPlayed',int(time.time()*1000)),(1,'raining',0),(1,'thundering',0)]
        with gzip.open(root/'level.dat','wb') as f:f.write(tag(10,'',[(10,'Data',data)]))
        return len(chunks)

def generate(config,dest,index):
    cfg=yaml.safe_load(config.read_text());m=Map();theme,top,rock,accent=PALETTES[index%len(PALETTES)]
    teams=cfg['Team'];points=[]
    for team in teams.values():
        sp=xyz(team['Spawn']);points.append(sp)
        coords=[xyz(team[k]) for k in ('Spawn','Bed','Shop','Upgrade')]
        coords += [xyz(s) for k in ('Iron','Gold','Emerald') for s in team.get(k,[])]
        lo=[min(c[k] for c in coords) for k in (0,2)];hi=[max(c[k] for c in coords) for k in (0,2)]
        cx,cz=(lo[0]+hi[0])//2,(lo[1]+hi[1])//2
        rx,rz=(hi[0]-lo[0])//2+5,(hi[1]-lo[1])//2+5
        m.disc(cx,sp[1]-1,cz,rx,rz,top,rock)
        for k in ('Spawn','Bed','Shop','Upgrade'):
            pad=m.pad(team[k]);m.path((sp[0],sp[1]-1,sp[2]),pad)
        color=COLORS.get(team.get('Color'),accent)
        # Two open-sided canopies: roof above heads, no counter blocking interaction.
        for key in ('Shop','Upgrade'):
            x,y,z=xyz(team[key]);yaw=loc(team[key])[3];dx=round(-math.sin(math.radians(yaw)));dz=round(math.cos(math.radians(yaw)))
            for side in (-2,2):
                px,pz=x-dx*2-dz*side,z-dz*2+dx*side
                for h in range(3):m.set(px,y+h,pz,85)
            for xx in range(-2,3):
                for zz in range(-2,3):m.set(x+xx,y+3,z+zz,35,color)
        for k,block in [('Iron',42),('Gold',41),('Emerald',133)]:
            for s in team.get(k,[]):
                q=m.pad(s,block,1);m.path((sp[0],sp[1]-1,sp[2]),q)
        x,y,z=xyz(team['Bed']);m.disc(x,y-1,z,3,3,98,rock);m.set(x,y,z,26,0);m.set(x,y,z+1,26,8)
        # Spawn remains clear even where template coordinates are tightly grouped.
        m.pad(team['Spawn'],35,1)
    gens=cfg.get('generator',{});emeralds=[xyz(s) for s in gens.get('Emerald',[])]
    cx=round(sum(p[0] for p in points)/len(points));cz=round(sum(p[2] for p in points)/len(points));cy=min((p[1]-1 for p in emeralds),default=min(p[1]-1 for p in points))
    radius=14+index%4;m.disc(cx,cy,cz,radius,radius,top,rock)
    # Monument kept off the generator paths: open arch framing the central island.
    for dx in (-7,7):
        for h in range(1,9):
            for dz in (4,5):m.set(cx+dx,cy+h,cz+dz,155 if index%2 else 98)
    for dx in range(-7,8):m.set(cx+dx,cy+9,cz+4,35,accent)
    for kind,block in [('Diamond',57),('Emerald',133)]:
        for s in gens.get(kind,[]):
            x,y,z=xyz(s);m.disc(x,y-1,z,5 if kind=='Diamond' else 4,5 if kind=='Diamond' else 4,top,rock)
            if kind=='Emerald':
                # Stairway via a long enough approach for high generators.
                anchor=(cx+max(0,y-1-cy)+3,cy,cz)
                m.path((cx,cy,cz),anchor,width=2);m.path(anchor,(x,y-1,z),width=2)
            m.pad(s,block,1)
    wait=xyz(cfg['waiting']['Loc']);m.disc(wait[0],wait[1]-1,wait[2],6,6,20,20)
    for x in range(-6,7):
        for z in range(-6,7):
            if 25<=x*x+z*z<=36:m.set(wait[0]+x,wait[1],wait[2]+z,20)
    # Check all configured interactive points for ground and headroom.
    for team in teams.values():
        for key in ('Spawn','Shop','Upgrade'):
            x,y,z=xyz(team[key]);assert m.blocks.get((x,y-1,z)),(config.name,key,'no ground')
            assert (x,y,z) not in m.blocks and (x,y+1,z) not in m.blocks,(config.name,key,'blocked')
        x,y,z=xyz(team['Bed']);assert m.blocks.get((x,y,z),(0,))[0]==26,(config.name,'bed')
    chunks=m.export(dest/config.stem,config.stem,wait)
    return {'arena':config.stem,'theme':theme,'teams':len(teams),'blocks':len(m.blocks),'chunks':chunks,'sha256_config':hashlib.sha256(config.read_bytes()).hexdigest()}

def main():
    a=argparse.ArgumentParser();a.add_argument('--server',type=pathlib.Path,default=pathlib.Path('template-server'));a.add_argument('--output',type=pathlib.Path,default=pathlib.Path('generated-arenas'));args=a.parse_args()
    files=sorted((args.server/'plugins/BedWars2023/Arenas').glob('*.yml'))
    if not files:raise SystemExit('Nenhuma configuração de arena encontrada')
    if args.output.exists():raise SystemExit('Destino já existe; mapas preservados')
    args.output.mkdir(parents=True);report=[]
    for i,f in enumerate(files):
        report.append(generate(f,args.output,i));print(f.stem,report[-1]['blocks'],flush=True)
    (args.output/'manifest.json').write_text(json.dumps(report,indent=2));print('Geradas',len(report),'arenas originais; validar em jogo antes de ativar.')
if __name__=='__main__':main()
