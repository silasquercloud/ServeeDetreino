#!/usr/bin/env python3
"""Render the SAME voxel scenes used by the plugin. Requires Java 17 and Pillow.
Block faces are schematic; these are not screenshots or Minecraft textures.
"""
import json
import math
from pathlib import Path
import subprocess
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/images'
SCENES=ROOT/'.cache/scenes'
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
WOOL=['#eee9df','#e5752b','#bc58c4','#74bbd6','#e1c638','#75b832','#dd88a7','#3b4145','#8b9192','#238c9a','#773fa8','#3258a6','#765238','#4e743a','#bf3544','#222834']
CLAY=['#c7a98e','#c87944','#a3647a','#8c9eaa','#c3a14b','#8e9955','#bb7e7f','#615449','#9b8072','#668a8b','#805a8d','#515e7c','#76523c','#65714a','#a95340','#36353c']
COLORS={'GRASS':'#6ba64b','DIRT':'#87603f','STONE':'#858b98','QUARTZ_BLOCK':'#e2e2e9','SMOOTH_BRICK':'#879095','SEA_LANTERN':'#c0eee0','GOLD_BLOCK':'#f6ce4f','LOG':'#94704a','LEAVES':'#458448','HAY_BLOCK':'#d6b148','CHEST':'#d59938','WORKBENCH':'#b88651','SANDSTONE':'#d9c794','WOOD':'#b88a53','FENCE':'#aa7e4d','BED_BLOCK':'#d84f67','STAINED_GLASS':'#75c1cf'}
THEMES={'pokemon':('POKÉMON','Centro Pokémon • Pokébola • árvores e ilhas de grama','#f28595'),'evangelion':('EVANGELION','Tokyo-3 • pirâmide • cabeça de EVA e detalhes verde-lima','#b6a0ff'),'sonic':('SONIC / GREEN HILL','Loop • argolas douradas • palmeiras e falésias quadriculadas','#7eb9ff'),'onepiece':('ONE PIECE','Navio pirata • vela e caveira • monumento do chapéu de palha','#f4c66f')}
def font(n,bold=False):
    try:return ImageFont.truetype(BOLD if bold else FONT,n)
    except OSError:return ImageFont.load_default(size=n)
def shade(hexcolor,factor):
    rgb=tuple(int(hexcolor[i:i+2],16) for i in (1,3,5))
    return tuple(max(0,min(255,int(c*factor))) for c in rgb)
def color(material,data):
    if material=='WOOL':return WOOL[data%16]
    if material=='STAINED_CLAY':return CLAY[data%16]
    if material=='STAINED_GLASS':return WOOL[data%16]
    if material=='WOOD' and data==1:return '#715437'
    return COLORS.get(material,'#a3a8b1')
def card(title,subtitle,accent):
    im=Image.new('RGB',(1600,1100),'#0a0c12');d=ImageDraw.Draw(im)
    for y in range(1100):
        t=y/1100;d.line((0,y,1600,y),fill=(int(15-4*t),int(18-4*t),int(29-9*t)))
    d.rounded_rectangle((28,26,1572,1074),radius=30,outline='#303548',width=2)
    d.rounded_rectangle((62,54,300,86),radius=8,fill='#25213a')
    d.text((78,59),'RYUEX / MULTIVERSO',font=font(16,True),fill=accent)
    d.text((62,108),title,font=font(43,True),fill='#f3f0fc')
    d.text((64,171),subtitle,font=font(21),fill='#b0bbcf')
    d.rounded_rectangle((62,1005,1538,1049),radius=9,fill='#232438')
    d.text((82,1017),'PRÉVIA TÉCNICA • blocos exportados de MapArt.java • não é captura do jogo',font=font(18),fill='#bfc4dd')
    return im,d

def render(name,filename,title,subtitle,accent,scale=None):
    cells=json.loads((SCENES/filename).read_text())
    blocks={(x,y,z):(material,data) for x,y,z,material,data in cells}
    # Camera faces the front (-Z) and the right side (+X), so façades/skull are visible.
    raw=[((x+z),(x-z)*.5-y) for x,y,z,_,_ in cells]
    minx=min(v[0] for v in raw)-1;maxx=max(v[0] for v in raw)+1
    miny=min(v[1] for v in raw)-2;maxy=max(v[1] for v in raw)+2
    k=scale or min(1340/(maxx-minx),730/(maxy-miny))
    centerx=800-(minx+maxx)*.5*k;centery=598-(miny+maxy)*.5*k
    def p(x,y,z):return(centerx+(x+z)*k,centery+(x-z)*k*.5-y*k)
    im,d=card(title,subtitle,accent)
    for (x,y,z),(material,data) in sorted(blocks.items(),key=lambda b:(b[0][0]-b[0][2]+b[0][1],b[0][1])):
        col=color(material,data)
        a=p(x,y+1,z);b=p(x+1,y+1,z);c=p(x+1,y+1,z+1);e=p(x,y+1,z+1)
        edge=shade(col,.56)
        if (x,y,z-1) not in blocks:d.polygon([a,b,p(x+1,y,z),p(x,y,z)],fill=shade(col,.78),outline=edge)
        if (x+1,y,z) not in blocks:d.polygon([b,c,p(x+1,y,z+1),p(x+1,y,z)],fill=shade(col,.58),outline=edge)
        if (x,y+1,z) not in blocks:d.polygon([a,b,c,e],fill=shade(col,1.08),outline=shade(col,.91))
    d.text((65,960),f'{len(cells):,} blocos no modelo • materiais nativos da 1.8 • sem resource pack obrigatório'.replace(',','.'),font=font(18),fill='#8998b4')
    im.quantize(colors=256).save(OUT/(name+'.png'),optimize=True)

def menu():
    im,d=card('ESCOLHA O TEMA DA SUA SALA','Modo → tema → vagas → jogar com os amigos','#c5a4ff')
    x0,y0,sz=250,290,123
    for row in range(3):
        for col in range(9):
            x=x0+col*sz;y=y0+row*sz
            d.rounded_rectangle((x,y,x+sz-10,y+sz-10),radius=9,fill='#222735',outline='#3a4358',width=2)
    for slot,label,accent in [(10,'POKÉ',THEMES['pokemon'][2]),(12,'EVA',THEMES['evangelion'][2]),(14,'SONIC',THEMES['sonic'][2]),(16,'PIRATA',THEMES['onepiece'][2])]:
        x=x0+slot%9*sz;y=y0+slot//9*sz
        d.rounded_rectangle((x+5,y+5,x+sz-15,y+sz-15),radius=8,fill='#2c2941',outline=accent,width=3)
        d.text((x+13,y+40),label,font=font(20,True),fill=accent)
    for i,(key,(title,sub,accent)) in enumerate(THEMES.items()):
        x=110+i*380;d.text((x,755),title.split(' /')[0],font=font(23,True),fill=accent)
        d.text((x,800),'Escolha no inventário',font=font(17),fill='#b0bbcf')
    d.text((110,899),'Dentro do jogo aparecem os itens Minecraft. Esta imagem mostra a disposição dos slots.',font=font(20),fill='#a4b3c9')
    im.quantize(colors=256).save(OUT/'menu.png',optimize=True)

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    subprocess.run(['python3',str(ROOT/'scripts/build.py')],check=True,cwd=ROOT)
    subprocess.run(['java','-cp',str(ROOT/'target/classes'),'br.ryuex.treino.MapArt',str(SCENES)],check=True,cwd=ROOT)
    for key,(title,sub,accent) in THEMES.items():
        render(key,key+'.json',title+' / ILHA TEMÁTICA',sub,accent)
        render('arena-'+key,'arena-'+key+'.json',title+' / SKYWARS PARA 6','Seis bases temáticas • baús nas bases e centro • pontes construídas na partida',accent)
    render('lobby','lobby.json','LOBBY / QUATRO UNIVERSOS','Praça central • quatro alas temáticas • pontes iluminadas e portais coloridos','#c6afff')
    render('bedwars','bedwars.json','BEDWARS / POKÉMON','Quatro bases com cama • Centro Pokémon em cada ilha • loja e geradores',THEMES['pokemon'][2])
    render('bedfight','bedfight.json','BEDFIGHT / ONE PIECE','Duelo 1x1 entre ilhas piratas • kit pronto e camas • cenário de treino',THEMES['onepiece'][2])
    render('skywars','arena-sonic.json','SKYWARS / SONIC PARA 6','Seis ilhas Green Hill com loop, argolas, palmeiras e terreno quadriculado',THEMES['sonic'][2])
    menu()
    print('Galeria atualizada com a geometria real do gerador; prévias técnicas, não screenshots.')
