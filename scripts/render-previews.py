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
COLORS={'GRASS':'#6ba64b','DIRT':'#87603f','STONE':'#858b98','QUARTZ_BLOCK':'#e2e2e9','SMOOTH_BRICK':'#879095','SEA_LANTERN':'#c0eee0','GOLD_BLOCK':'#f6ce4f','LOG':'#94704a','LEAVES':'#458448','HAY_BLOCK':'#d6b148','CHEST':'#d59938','ENDER_CHEST':'#29473e','WORKBENCH':'#b88651','STEP':'#8c929c','SANDSTONE':'#d9c794','WOOD':'#b88a53','FENCE':'#aa7e4d','BED_BLOCK':'#d84f67','IRON_BLOCK':'#d9dce1','DIAMOND_BLOCK':'#68dbdf','EMERALD_BLOCK':'#42c487','FURNACE':'#515a68','REDSTONE_BLOCK':'#bc303b','RED_ROSE':'#e34459','STAINED_GLASS':'#75c1cf'}
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
    d.text((78,59),'RYUEX / TREINO',font=font(16,True),fill=accent)
    d.text((62,108),title,font=font(43,True),fill='#f3f0fc')
    d.text((64,171),subtitle,font=font(21),fill='#b0bbcf')
    d.rounded_rectangle((62,1005,1538,1049),radius=9,fill='#232438')
    d.text((82,1017),'PRÉVIA TÉCNICA • blocos exportados de MapArt.java • não é captura do jogo',font=font(18),fill='#bfc4dd')
    return im,d

def render(name,filename,title,subtitle,accent,scale=None):
    cells=json.loads((SCENES/filename).read_text())
    if name in ('forge','shop-kiosk'):cells=[[-x,y,-z,material,data] for x,y,z,material,data in cells]  # look into the open entrance
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
        height=.5 if material=='STEP' else 1
        a=p(x,y+height,z);b=p(x+1,y+height,z);c=p(x+1,y+height,z+1);e=p(x,y+height,z+1)
        edge=shade(col,.56)
        if (x,y,z-1) not in blocks:d.polygon([a,b,p(x+1,y,z),p(x,y,z)],fill=shade(col,.78),outline=edge)
        if (x+1,y,z) not in blocks:d.polygon([b,c,p(x+1,y,z+1),p(x+1,y,z)],fill=shade(col,.58),outline=edge)
        if (x,y+1,z) not in blocks:d.polygon([a,b,c,e],fill=shade(col,1.08),outline=shade(col,.91))
    d.text((65,960),f'{len(cells):,} blocos no modelo • materiais nativos da 1.8 • sem resource pack obrigatório'.replace(',','.'),font=font(18),fill='#8998b4')
    im.quantize(colors=256).save(OUT/(name+'.png'),optimize=True)

def menu():
    im,d=card('SOLO OU DUPLAS','Modo → equipes → vagas → tema sorteado ao criar a sala','#c5a4ff')
    x0,y0,sz=250,290,123
    for row in range(3):
        for col in range(9):
            x=x0+col*sz;y=y0+row*sz
            d.rounded_rectangle((x,y,x+sz-10,y+sz-10),radius=9,fill='#222735',outline='#3a4358',width=2)
    for slot,label,accent in [(11,'SOLO','#f0c66f'),(15,'DUPLAS','#82d3ed')]:
        x=x0+slot%9*sz;y=y0+slot//9*sz
        d.rounded_rectangle((x+5,y+5,x+sz-15,y+sz-15),radius=8,fill='#2c2941',outline=accent,width=3)
        d.text((x+13,y+40),label,font=font(19,True),fill=accent)
    d.text((110,755),'CADA PARTIDA, UMA SURPRESA',font=font(32,True),fill='#f2ecff')
    d.text((110,820),'Pokémon • Evangelion • Sonic / Green Hill • One Piece',font=font(25),fill='#b9c5db')
    d.text((110,882),'Duplas compartilham cama e melhorias. Armaduras e ferramentas são individuais.',font=font(22),fill='#91aecb')
    d.text((110,940),'Prévia da disposição real dos slots; não é screenshot do Minecraft.',font=font(19),fill='#919ab3')
    im.quantize(colors=256).save(OUT/'menu.png',optimize=True)

def ui_panels():
    im,d=card('MENU / INFORMAÇÕES DA PARTIDA','Estrutura do tooltip implementado • dados locais do servidor','#8fdbaf')
    d.rounded_rectangle((235,270,1365,930),radius=20,fill='#140b1b',outline='#633b82',width=5)
    rows=[('Bed Wars (Solo)','#75e65d',38),('0 participantes em partida','#aab0be',27),('','#aaa',24),('TOP WINSTREAK DIÁRIO:','#f0b842',32),('Nenhum resultado hoje.','#aeb5c7',28),('','#aaa',24),('Clique para jogar.','#eae867',35)]
    y=320
    for text,color,size in rows:
        d.text((285,y),text,font=font(size,True),fill=color);y+=78
    d.text((260,955),'Estado vazio real: o ranking só mostra nomes após vitórias registradas.',font=font(20),fill='#9dacc1')
    im.quantize(colors=256).save(OUT/'menu-lobby.png',optimize=True)
    im,d=card('ADMIN / PAINEL DE TESTES','Permissão treino.admin • Salabim_zz configurado como administrador','#bf9bfa')
    cards=[('CPS OBSERVADO','Esquerdo / direito','Janela de 1 segundo','#e78aaa'),('FASTPLACE ASSISTIDO','Pista própria • 10 blocos/s','Botão direito com a vara','#e6c779'),('BRIDGE MANUAL','Ponte de 32 blocos','Tempo • reinício • recorde','#7ecdd7')]
    for i,(title,a,b,color) in enumerate(cards):
        x=85+i*490;d.rounded_rectangle((x,310,x+450,740),radius=18,fill='#1e2231',outline=color,width=3)
        d.text((x+24,365),title,font=font(23,True),fill=color);d.text((x+24,470),a,font=font(21),fill='#e1e6f1');d.text((x+24,535),b,font=font(20),fill='#aeb9d0')
        d.text((x+24,645),['/cps','/fastplace','/bridge'][i],font=font(29,True),fill='#e8e3ff')
    d.text((100,855),'Slots reais do menu: CPS 10 • fastplace 13 • bridge 16. Layout acima é explicativo.',font=font(23),fill='#aeb9d0')
    d.text((100,925),'O CPS representa ações recebidas no servidor; não mede cada clique físico.',font=font(22),fill='#aeb9d0')
    im.quantize(colors=256).save(OUT/'admin-tools.png',optimize=True)

def shop_panels():
    catalog=json.loads((SCENES/'shop-catalog.json').read_text())
    categories=['Blocos','Espadas','Armadura','Ferramenta','Arcos','Poções','Utilidades','Melhorias','Rápida']
    def grid(name,title,subtitle,kind):
        im,d=card(title,subtitle,'#d7b485');x0,y0,k=135,285,146
        rows=6 if kind=='shop' else 3
        if rows==3:y0=350
        for row in range(rows):
            for col in range(9):
                x=x0+col*k;y=y0+row*99
                d.rounded_rectangle((x,y,x+137,y+91),radius=6,fill='#252b39',outline='#687386',width=2)
        if kind=='shop':
            for c,label in enumerate(categories):d.text((x0+c*k+7,y0+32),label,font=font(15,True),fill='#d9d1f1')
            for index,entry in enumerate([e for e in catalog if e[0]==0]):
                cat,material,amount,cost,currency,item_name,key=entry;x=x0+index*k;y=y0+2*99
                labels=__import__("textwrap").wrap(item_name,width=14)
                for line,label in enumerate(labels[:2]):d.text((x+8,y+10+line*18),label,font=font(13,True),fill='#f4dda4')
                d.text((x+8,y+62),str(cost)+' '+currency,font=font(16),fill='#a3c9c0')
            for slot,label in [(48,'Hotbar'),(49,'Ajuda'),(51,'Baú pessoal'),(53,'Fechar')]:d.text((x0+slot%9*k+8,y0+slot//9*99+30),label,font=font(15,True),fill='#b4cded')
            d.text((140,935),'Esquerdo compra • Shift escolhe slot do favorito • Direito configura hotbar do item.',font=font(21),fill='#bbc6d9')
        else:
            for c,label in enumerate(categories[:8]):d.text((x0+c*k+6,y0+32),label,font=font(15,True),fill='#d9d1f1')
            for slot in range(9):d.text((x0+slot*k+33,y0+2*99+25),str(slot+1),font=font(28,True),fill='#8bd2ea')
            d.text((140,820),'Escolha categoria ou clique direito em um item na loja; depois selecione um slot.',font=font(21),fill='#bbc6d9')
        d.text((140,970),'Diagrama dos slots e textos implementados • a interface no cliente usa o inventário Minecraft.',font=font(18),fill='#8f9db4')
        im.quantize(colors=256).save(OUT/(name+'.png'),optimize=True)
    grid('shop-bedwars','BEDWARS / LOJA','Catálogo exportado do plugin • categorias, compra rápida e recursos dos baús','shop')
    grid('shop-hotbar','BEDWARS / EDITOR DE HOTBAR','Preferência por categoria e por item • posições 1 a 9 • salvamento individual','hotbar')

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    subprocess.run(['python3',str(ROOT/'scripts/build.py')],check=True,cwd=ROOT)
    subprocess.run(['java','-cp',str(ROOT/'target/classes'),'br.ryuex.treino.MapArt',str(SCENES)],check=True,cwd=ROOT)
    subprocess.run(['java','-cp',str(ROOT/'target/classes'),'br.ryuex.treino.BedwarsItems',str(SCENES/'shop-catalog.json')],check=True,cwd=ROOT)
    for key,(title,sub,accent) in THEMES.items():
        render('base-'+key,'base-'+key+'.json',title+' / BASE DE COMBATE','Cama e área frontal livres • gerador baixo • vendedor na lateral',accent)
        render(key,key+'.json',title+' / ILHA TEMÁTICA',sub,accent)
        render('arena-'+key,'arena-'+key+'.json',title+' / SKYWARS PARA 6','Seis bases temáticas • baús nas bases e centro • pontes construídas na partida',accent)
    render('forge','forge.json','FORJA / MODELO BAIXO','Pedra e lajes • núcleo de ferro • detalhe de ouro • sem teto e sem colunas','#79b9ff')
    render('emerald-generator','emerald-generator.json','GERADOR / ESMERALDA','Borda de pedra baixa • lajes de meio bloco • núcleo verde visível','#72dfb3')
    render('admin-practice','admin-practice.json','ADMIN / PISTA DE BRIDGE','Partida vazia • 32 blocos de trajeto • começo e chegada iluminados','#bc9cf3')
    render('admin-fastplace','admin-fastplace.json','ADMIN / FASTPLACE ASSISTIDO','Estado após a assistência construir os 31 blocos do vão • não é o estado inicial','#bc9cf3')
    render('bedwars-sonic','bedwars-sonic.json','BEDWARS / GREEN HILL','Bases livres • poços de recursos • postos de diamantes • centro de esmeraldas','#79b9ff')
    render('lobby','lobby.json','LOBBY / PRAÇA CENTRAL','Praça de 45 blocos • três seletores de modo • jardim e pavilhão de ranking','#c6afff')
    render('bedwars','bedwars.json','BEDWARS / POKÉMON','Quatro bases abertas com cama • duas lojas Villager por base • geradores',THEMES['pokemon'][2])
    render('bedfight-base','bedfight-base.json','BEDFIGHT / BASE COM KIT','Defesa inicial de madeira • base de combate livre • sem loja e sem economia','#dfb575')
    render('shop-kiosk','shop-kiosk.json','BEDWARS / BALCÃO DOS VILLAGERS','Estrutura real do balcão • dois Villagers adicionados em tempo de jogo • baú e baú pessoal','#e5c287')
    render('bedfight','bedfight.json','BEDFIGHT / ONE PIECE','Kit inicial • cama protegida com madeira • sem loja e sem gerador',THEMES['onepiece'][2])
    render('skywars','arena-sonic.json','SKYWARS / SONIC PARA 6','Seis ilhas Green Hill com loop, argolas, palmeiras e terreno quadriculado',THEMES['sonic'][2])
    menu();ui_panels();shop_panels()
    print('Galeria atualizada com a geometria real do gerador; prévias técnicas, não screenshots.')
