#!/usr/bin/env python3
"""Technical projections of the geometry and menu implemented in Treino.java.
These are diagrams, not game screenshots. Requires Pillow only to regenerate.
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/images'
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def font(n,bold=False): return ImageFont.truetype(BOLD if bold else FONT,n)
def base(title,subtitle):
    im=Image.new('RGB',(1440,900),'#0c0d12');d=ImageDraw.Draw(im)
    d.rounded_rectangle((30,28,1410,872),radius=28,fill='#12141d',outline='#2b2e40',width=2)
    d.text((65,52),'RYUEX / TREINO',font=font(18,True),fill='#bd9bff')
    d.text((65,87),title,font=font(38,True),fill='#f4f2ff')
    d.text((65,145),subtitle,font=font(19),fill='#aab0c4')
    d.rounded_rectangle((65,813,1375,851),radius=9,fill='#272035')
    d.text((85,821),'PRÉVIA TÉCNICA • geometria do plugin • não é captura real do Minecraft',font=font(17),fill='#c7b7e7')
    return im,d
COL={'grass':('#639c48','#416b39','#315034'),'stone':('#838796','#555967','#3f4250'),'wood':('#b68b52','#88603c','#63472e'),'chest':('#d79b37','#946426','#67461f'),'bed':('#e04b65','#b42d4a','#702139'),'quartz':('#e3e2eb','#a6a6be','#797f9b'),'light':('#bdf2e9','#76c6cf','#4f96aa')}
def arena(mode,n):
    im,d=base(mode.upper()+f' / {n} jogadores','Projeção isométrica das ilhas geradas • raio de 42 blocos • centro compartilhado')
    blocks={}
    def put(x,y,z,m):blocks[(x,y,z)]=m
    def plat(x,y,z,r,m):
        for a in range(-r,r+1):
            for b in range(-r,r+1):
                if a*a+b*b<=r*r:put(x+a,y,z+b,m)
    plat(0,79,0,9,'stone');plat(0,80,0,8,'grass')
    spawns=[]
    for i in range(n):
        a=2*math.pi*i/n;x=math.floor(math.cos(a)*42+0.5);z=math.floor(math.sin(a)*42+0.5)
        spawns.append((x,z))
        plat(x,79,z,5,'stone');plat(x,80,z,4,'grass')
        if mode=='SkyWars':put(x+2,81,z,'chest');put(x-2,81,z,'chest')
        else:put(x+2,81,z,'bed');put(x+2,81,z+1,'bed');put(x-2,81,z,'wood')
    if mode=='SkyWars':
        for x,z in [(0,0),(3,3),(-3,-3)]:put(x,81,z,'chest')
    def iso(x,y,z): return (720+(x-z)*6,460+(x+z)*3-(y-79)*9)
    for (x,y,z),m in sorted(blocks.items(),key=lambda a:(sum((a[0][0],a[0][2])),a[0][1])):
        top,left,right=COL[m];a=iso(x,y+1,z);b=iso(x+1,y+1,z);c=iso(x+1,y+1,z+1);e=iso(x,y+1,z+1)
        d.polygon([e,c,iso(x+1,y,z+1),iso(x,y,z+1)],fill=left)
        d.polygon([b,c,iso(x+1,y,z+1),iso(x+1,y,z)],fill=right)
        d.polygon([a,b,c,e],fill=top)
    for i,(x,z) in enumerate(spawns):
        px,py=iso(x,83,z);label=f'BASE {i+1}'
        d.rounded_rectangle((px-40,py-21,px+43,py+2),radius=5,fill='#282438')
        d.text((px-33,py-19),label,font=font(13,True),fill='#e6dcff')
    note={'SkyWars':'Baús nas bases e no centro. Você constrói as pontes durante a partida.','BedWars':'Uma cama por jogador. Bancada abre a loja; recursos aparecem nas bases.','BedFight':'Duelos por pares: bases 1 × 2. Kit pronto, respawn enquanto a cama existir.'}[mode]
    d.text((65,763),note,font=font(18),fill='#c0c7dd')
    im.save(OUT/(mode.lower()+'.png'))
def lobby():
    im,d=base('LOBBY / entrada do servidor','Plataforma gerada no mundo treino_lobby • quartzo e iluminação na borda')
    blocks=[]
    for x in range(-12,13):
        for z in range(-12,13):blocks.append((x,z,'light' if abs(x)==12 or abs(z)==12 else 'quartz'))
    def p(x,z,h=0):return(720+(x-z)*13,430+(x+z)*6.5-h*14)
    for x,z,m in sorted(blocks,key=lambda v:v[0]+v[1]):
        top,left,right=COL[m];a=p(x,z,1);b=p(x+1,z,1);c=p(x+1,z+1,1);e=p(x,z+1,1)
        d.polygon([e,c,p(x+1,z+1),p(x,z+1)],fill=left);d.polygon([b,c,p(x+1,z+1),p(x+1,z)],fill=right);d.polygon([a,b,c,e],fill=top)
    d.text((65,737),'Bússola no primeiro slot → selecionar modo → escolher vagas → chamar os amigos',font=font(22),fill='#d2c5ed')
    im.save(OUT/'lobby.png')
def menu():
    im,d=base('MENU / escolha sua partida','Reconstrução da disposição dos slots do inventário implementado no plugin')
    x0,y0,sz=260,280,101
    for row in range(3):
        for col in range(9):
            x=x0+col*sz;y=y0+row*sz
            d.rounded_rectangle((x,y,x+sz-8,y+sz-8),radius=9,fill='#232632',outline='#3d4256',width=2)
    for slot,name,desc,color in [(10,'BED','BedWars','#f27497'),(13,'PVP','BedFight','#86d8ff'),(16,'SKY','SkyWars','#a4dd88'),(22,'SALAS','Entrar','#d6b2ff')]:
        x=x0+(slot%9)*sz;y=y0+(slot//9)*sz
        d.rounded_rectangle((x+5,y+5,x+sz-13,y+sz-13),radius=8,fill='#302941',outline=color,width=2)
        d.text((x+12,y+30),name,font=font(18,True),fill=color)
        # legends live outside inventory so labels never overlap actual slots
    labels=[('BEDWARS','2–8 • solo, loja e geradores','#f27497'),('BEDFIGHT','2/4/6/8 • pares de duelos 1x1','#86d8ff'),('SKYWARS','2–12 • escolha uma sala com 6 vagas','#a4dd88')]
    for j,(title,sub,color) in enumerate(labels):
        x=110+j*445;d.text((x,660),title,font=font(23,True),fill=color);d.text((x,703),sub,font=font(17),fill='#b0b8cf')
    im.save(OUT/'menu.png')
if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    lobby();menu();arena('SkyWars',6);arena('BedWars',4);arena('BedFight',2)
    print('5 prévias técnicas criadas em docs/images; não são screenshots do jogo.')
