# Galeria • Ryuex Multiverso

[← Início](../README.md) · [Temas e comandos](TEMAS.md) · [Instalar](GUIA.md)

**Prévias técnicas das construções implementadas; não são screenshots do Minecraft.** As posições e materiais são exportados diretamente de `MapArt.java`, o mesmo gerador usado pelo plugin. O render usa cores e faces simplificadas, sem reproduzir texturas, transparências, HUD ou formas especiais de itens/cercas.

## Lobby misto • quatro universos

Praça central, quatro alas decoradas, pontes iluminadas com bordas e arcos nas cores de cada tema. Explore tudo a pé e use a bússola para jogar.

![Lobby misto com quatro alas](images/lobby.png)

## Pokémon

Centro Pokémon com teto vermelho, cruz, porta e janelas; Pokébola em blocos, árvore e ilha de grama.

![Ilha Pokémon](images/pokemon.png)

<details><summary>Ver arena Pokémon para seis jogadores</summary>

![SkyWars Pokémon com seis bases](images/arena-pokemon.png)

</details>

## Evangelion

Cidade industrial inspirada em Tokyo-3, pirâmide e escultura de cabeça de EVA, com roxo e verde-lima.

![Ilha Evangelion](images/evangelion.png)

<details><summary>Ver arena Evangelion para seis jogadores</summary>

![SkyWars Evangelion com seis bases](images/arena-evangelion.png)

</details>

## Sonic • Green Hill

Loop, argolas de ouro, palmeiras e terreno quadriculado.

![Ilha Sonic Green Hill](images/sonic.png)

<details><summary>Ver arena Sonic para seis jogadores</summary>

![SkyWars Sonic com seis bases](images/arena-sonic.png)

</details>

## One Piece

Navio pirata, mastro, vela com caveira em blocos, monumento do chapéu de palha e palmeira.

![Ilha One Piece](images/onepiece.png)

<details><summary>Ver arena One Piece para seis jogadores</summary>

![SkyWars One Piece com seis bases](images/arena-onepiece.png)

</details>

## Menu de temas

A sequência agora é **modo → tema → vagas**. As siglas da prévia representam os itens usados no inventário real: redstone, olho do Ender, ouro e barco.

![Disposição do menu de temas](images/menu.png)

## BedWars e BedFight

Exemplos das bases com cama. O tema também pode ser trocado pelo menu.

![BedWars Pokémon com quatro bases](images/bedwars.png)

![BedFight One Piece com duas bases](images/bedfight.png)

## Screenshots reais e regeneração

Ainda não houve captura de uma partida real. Depois de iniciar o servidor, entre em Minecraft Java 1.8.9 e use **F2**. As capturas ficam na pasta `screenshots` da instância do seu launcher; podem ser adicionadas aqui com a identificação de screenshot real.

Para reconstruir as projeções a partir do código atual, com Python 3, Pillow e Java 17 instalados:

```bash
python3 scripts/render-previews.py
```

O script compila e testa o plugin, exporta as cenas do gerador Java e renderiza as imagens. As vistas de arena não mostram a plataforma de espera suspensa acima do centro.
