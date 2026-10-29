# Galeria • Ryuex Multiverso

[← Início](../README.md) · [Temas e comandos](TEMAS.md) · [Instalar](GUIA.md)

**Prévias técnicas das construções implementadas; não são screenshots do Minecraft.** As posições e materiais são exportados diretamente de `MapArt.java`, o mesmo gerador usado pelo plugin. O render usa cores e faces simplificadas, sem reproduzir texturas, transparências, HUD ou formas especiais de itens/cercas.

## Geradores e bases • versão 1.3

Poços baixos de pedra, com núcleo de minério visível, presentes em BedWars e BedFight. Os contadores flutuantes e o vendedor são entidades adicionadas no jogo; as projeções abaixo mostram a geometria em blocos. A área da cama fica livre e os detalhes maiores ficam no centro/lobby.

![Forja baixa implementada](images/forge.png)

![Gerador baixo de esmeralda](images/emerald-generator.png)

![BedWars Green Hill com forjas](images/bedwars-sonic.png)

## Lobby misto • quatro universos

Praça central, quatro alas decoradas, pontes iluminadas com bordas e arcos nas cores de cada tema. Explore tudo a pé e use a bússola para jogar.

![Lobby misto com quatro alas](images/lobby.png)

## Pokémon

Centro Pokémon com teto vermelho, cruz, porta e janelas; Pokébola em blocos, árvore e ilha de grama.

![Ilha Pokémon](images/pokemon.png)

**Base de combate com cama e área frontal livre:**

![Base pokemon](images/base-pokemon.png)

<details><summary>Ver arena Pokémon para seis jogadores</summary>

![SkyWars Pokémon com seis bases](images/arena-pokemon.png)

</details>

## Evangelion

Cidade industrial inspirada em Tokyo-3, pirâmide e escultura de cabeça de EVA, com roxo e verde-lima.

![Ilha Evangelion](images/evangelion.png)

**Base de combate com cama e área frontal livre:**

![Base evangelion](images/base-evangelion.png)

<details><summary>Ver arena Evangelion para seis jogadores</summary>

![SkyWars Evangelion com seis bases](images/arena-evangelion.png)

</details>

## Sonic • Green Hill

Loop, argolas de ouro, palmeiras e terreno quadriculado.

![Ilha Sonic Green Hill](images/sonic.png)

**Base de combate com cama e área frontal livre:**

![Base sonic](images/base-sonic.png)

<details><summary>Ver arena Sonic para seis jogadores</summary>

![SkyWars Sonic com seis bases](images/arena-sonic.png)

</details>

## One Piece

Navio pirata, mastro, vela com caveira em blocos, monumento do chapéu de palha e palmeira.

![Ilha One Piece](images/onepiece.png)

**Base de combate com cama e área frontal livre:**

![Base onepiece](images/base-onepiece.png)

<details><summary>Ver arena One Piece para seis jogadores</summary>

![SkyWars One Piece com seis bases](images/arena-onepiece.png)

</details>

## Menu de equipes

A sequência agora é **modo → solo/duplas → vagas**. O tema é sorteado por sala. Na prévia, as etiquetas representam os itens reais de espada/cama.

![Disposição do menu de temas](images/menu.png)

## BedWars e BedFight

Exemplos das bases com cama. O tema também pode ser trocado pelo menu.

![BedWars Pokémon com quatro bases](images/bedwars.png)

![BedFight One Piece com duas bases](images/bedfight.png)

## Menu do lobby e ferramentas do administrador

Estas duas imagens são diagramas dos itens/textos implementados; não são capturas do cliente.

![Tooltip do modo com ranking diário](images/menu-lobby.png)

![Painel CPS, fastplace e bridge](images/admin-tools.png)

![Pista manual de bridge](images/admin-practice.png)

![Pista após fastplace assistido](images/admin-fastplace.png)

[Comandos e limites das ferramentas →](ADMIN.md)

## Screenshots reais e regeneração

Ainda não houve captura de uma partida real. Depois de iniciar o servidor, entre em Minecraft Java 1.8.9 e use **F2**. As capturas ficam na pasta `screenshots` da instância do seu launcher; podem ser adicionadas aqui com a identificação de screenshot real.

Para reconstruir as projeções a partir do código atual, com Python 3, Pillow e Java 17 instalados:

```bash
python3 scripts/render-previews.py
```

O script compila e testa o plugin, exporta as cenas do gerador Java e renderiza as imagens. A espera agora ocorre no mundo separado do lobby; não há plataforma suspensa acima das arenas. Os postos de diamantes aparecem na prévia BedWars.
