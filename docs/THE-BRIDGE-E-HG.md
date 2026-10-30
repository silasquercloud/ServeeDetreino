# The Bridge e HG — RyuexModes 1.0.0

Plugin separado do BedWars2023, compilado para a API Spigot 1.8.8 e instalado pelo `bash atualizar.sh`. Cliente alvo: Java 1.8.9. Não precisa de Citizens para estes dois modos.

```bash
git pull --ff-only
bash atualizar.sh
bash menu.sh
```

Depois da preparação, escolha 3 para iniciar. No lobby use `/jogos`. Se outro plugin ocupar um comando, use `/ryuexmodes:jogos`, `/ryuexmodes:bridge` ou `/ryuexmodes:hg`.

| Comando | Função |
| --- | --- |
| `/bridge solo` | Fila 1x1, começa com 2 jogadores |
| `/bridge dupla` | Fila 2x2, começa com 4 jogadores; equipes balanceadas automaticamente |
| `/hg` | Fila HG de 2 a 16 jogadores |
| `/hg kit` | Guerreiro, Arqueiro ou Explorador, selecionados antes da partida |
| `/jogos sair` | Sai da partida e restaura inventário e localização anteriores |

## The Bridge

Três variações visuais originais sorteadas: Jardins Suspensos, Templo de Quartzo e Fortaleza Glacial. Ponte central, bases, gols de 3x3 e limites de construção. Primeiro time a cinco gols vence. Espada, arco, picareta, blocos, maçãs e armadura de couro colorida. Uma flecha disponível a cada cinco segundos. Queda e dano letal retornam à base; um gol reposiciona os dois times. Limite de 15 minutos. Sem dano entre aliados.

## HG

Mundo natural com seed fixa, plataforma central, acessos ao terreno e borda de 384 blocos de diâmetro. Coleta, mineração, crafting, comida e sopa que cura 3,5 corações. A bússola acompanha o adversário vivo mais próximo. Proteção inicial de 60 segundos; Feast aos 8 minutos, fechamento da borda aos 10, deathmatch aos 15 e empate aos 20. Mortos viram espectadores; o último vivo vence. Sair durante a partida conta como eliminação.

## Mapas e inventário

Os mapas são criados pelo plugin ao abrir uma sala, em mundos separados do lobby e das 46 arenas BedWars. Não são downloads de mapas Mush. O HG usa geração natural Minecraft e uma área central construída pelo plugin; a primeira geração pode provocar uma pausa no servidor. Há uma sala solo, uma de duplas e uma de HG simultaneamente.

Inventários anteriores ficam em `plugins/RyuexModes/players/` até serem restaurados. Não apague essa pasta com jogadores em partida. Somente mundos temporários marcados pelo plugin são removidos depois de descarregados. Mundos deixados por encerramento abrupto podem permanecer no disco. Não há integração automática com o sistema de party do template nesta versão; entrada e balanceamento são individuais.

## Validação

Compilação real contra API 1.8.8 e testes de regras: limite de jogadores, reconhecimento dos gols e proteção de combate. Ainda é necessário testar uma partida com clientes 1.8.9 e os plugins do template em execução. Não foi confirmada a ausência de conflitos com proteção de mundo ou scoreboards do template.

Teste recomendado: dois jogadores no solo, pontuar nos dois gols, cair no vazio, sair e conferir o inventário; quatro jogadores em duplas e conferir fogo amigo; dois no HG, escolher kits, aguardar proteção, testar sopa, eliminação e retorno ao lobby. Verificar Feast/borda em uma partida longa. Os comandos exigem estar no mundo do lobby antes de entrar para evitar misturar inventários de partidas BedWars.
