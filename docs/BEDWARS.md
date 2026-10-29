# BedWars, lobby, loja e desempenho

[← Início](../README.md) · [Menu de controle](MENU.md) · [Party](PARTY.md) · [Galeria](GALERIA.md)

TreinoHub **1.2.0**; TreinoAntiLag **1.1.0**. Os nomes dos arquivos JAR permanecem estáveis para substituir a instalação anterior sem carregar duas cópias.

## Aplicar em uma instalação existente

Nesta primeira atualização, pare o servidor atual digitando `stop` **no console onde ele está rodando**. Aguarde salvar e encerrar. Depois:

```bash
git pull
bash menu.sh
```

Escolha **5** para compilar, testar e instalar **TreinoHub e TreinoAntiLag**, depois **3** para ligar. O backup dos JARs anteriores fica em `server/backups/`. A atualização preserva Playit, propriedades, configurações locais e estatísticas. Não é necessário reinstalar Spigot.

Nas próximas sessões, você pode parar de outro terminal com **opção 6** ou:

```bash
bash scripts/stop.sh
```

O launcher encaminha `stop` ao console Minecraft, aguarda seu encerramento e fecha o Playit. A solicitação pertence à execução atual; pedidos antigos não param uma nova sessão. Não abre RCON na internet, não usa `kill -9` e não atualiza o servidor enquanto o launcher estiver ativo. Se o salvamento demorar mais de dois minutos, a ferramenta informa o atraso e deixa o processo terminar; confira o console. O script de parada não instala dependências nem faz downloads.

## Forjas, duplas, bots e administração • 1.2.0

O gerador da base agora é uma construção: piso de ferro/ouro iluminado, bancada de fornalha, colunas e cobertura temática. O holograma mostra ferro por segundo e contagem do ouro. O balcão tem um manequim vendedor clicável; é uma entidade ArmorStand estática, não um NPC humano. O BedFight usa a mesma loja e forja, com diamante a cada 30 segundos (limite 4) e esmeralda a cada 45 segundos (limite 2) na própria base. BedWars mantém os postos externos e o centro. Esses minérios são a moeda da loja; não existe saldo virtual em reais/cash.

Menu: **modo → solo/duplas → vagas**. O tema é sorteado por sala, entre os quatro temas. Jogadores comuns não forçam o tema por comando; administradores podem escolher um tema para testes.

**Duplas** usam uma base e cama para dois jogadores. Parceiros são definidos pela ordem de entrada, dois a dois; ao entrar pela party, os membros acompanham essa ordem. Melhorias são do time; armaduras/ferramentas são individuais. BedFight duplas é **2v2**, com 4 ou 8 vagas totais. BedWars duplas aceita 4/6/8; SkyWars duplas aceita 4/6/8/10/12. A sala só começa com times completos e pelo menos dois times.

```text
/treino criar bedwars 4 aleatorio duplas
/treino criar bedfight 4 aleatorio duplas
/party jogar bedwars aleatorio duplas
```

Para testar sozinho:

```text
/treino criar bedfight 2
/treino bots 1
/treino iniciar
```

Para testar duplas sozinho, crie 4 vagas com `aleatorio duplas` e adicione 3 bots. Bots ocupam vagas reais. São **zumbis controlados**, com IA básica: procuram adversários, constroem pontes de lã, tentam quebrar blocos de defesa/camas, atacam, renascem se tiverem cama e são eliminados sem ela. Não têm skin/combate idêntico a jogador, não compram na loja e podem ficar presos em obstáculos complexos. Em SkyWars, também não saqueiam baús. Não são jogadores falsos no TAB. Encerrar a sala remove o mundo e os bots; sair sendo o último humano encerra a sala. Bots não entram no ranking.

**Salabim_zz** foi incluído em `administrators`. Após instalar e reiniciar, recebe OP ao entrar. A lista é gravada no config do plugin; remova o nome da lista e use `deop` se quiser revogar. Como o servidor está em modo offline, a concessão depende do nick e não verifica a conta Microsoft.

Como administrador, explore sem partida:

```text
/treino mapa sonic
/treino mapa pokemon
/treino mapa evangelion
/treino mapa onepiece
/treino sair
```

A prévia abre em Creative, com voo e geometria das arenas/forjas. Os hologramas/vendedores são entidades criadas nas salas reais; não aparecem na prévia geométrica. O comando não altera o mundo do lobby.

**PvP:** proteção explícita de 2 segundos ao nascer, encerrada ao atacar; dano entre aliados e entre pares distintos de BedFight é bloqueado, inclusive de projéteis/golems. O intervalo nativo de dano da 1.8 é mantido; não foi instalado um motor de knockback nem alegada equivalência ao Mush.

## Camas e visibilidade

O clique esquerdo na cama agora segue para o evento de quebra; apenas o direito é bloqueado para impedir dormir/explodir. A própria cama permanece protegida. Destruir qualquer metade da cama inimiga remove as duas e impede novos renascimentos. BedFight só permite destruir a cama do oponente do par.

A espera acontece no mundo do lobby. **A plataforma de vidro acima da arena foi removida**, e a câmera de espectador não tem uma estrutura suspensa. A volta ao jogo limpa o alvo de espectador e efeitos, restaura Survival e atualiza a visibilidade entre os jogadores da mesma sala. Há uma atualização do rastreamento após o teleporte e uma segunda conferência um segundo depois. Espectadores ficam ocultos para os jogadores ativos; salas diferentes ficam isoladas. A distância de renderização do cliente ainda afeta quais jogadores distantes ele mostra.

## Loja

Disponível em **BedWars e BedFight**. Abra pelo vendedor manequim sobre a bancada, pela própria bancada ou `/treino loja`. A loja tem **Blocos, Espadas, Armaduras, Ferramentas, Arcos, Poções, Utilidades, Melhorias e Compra rápida**. As compras usam os recursos do inventário. `Shift + clique` adiciona/remove favoritos da compra rápida, salvos por jogador.

| Categoria | Itens / comportamento |
| --- | --- |
| Blocos | Lã da sua cor, madeira, pedra do End, vidro resistente a explosões, obsidiana e escada. |
| Espadas | Pedra, ferro, diamante e graveto com Repulsão I. Espadas compradas são perdidas ao morrer. |
| Armaduras | Calças/botas de malha, ferro ou diamante; permanecem após a morte. Capacete e peitoral continuam de couro. |
| Ferramentas | Picaretas, machados e tesoura permanecem após a morte. Compras de nível igual/inferior são recusadas. |
| Arcos | Arco e flechas. |
| Poções | Velocidade e salto. Não há invisibilidade nesta versão. |
| Bola de fogo | 40 ferros; botão direito lança na mira. Não dispara ao abrir bancada ou baú. Não incendeia o mapa. |
| Estilingue | 8 ouros; creme de magma, consumido ao clicar com o botão direito. Impulso na direção da mira, com mínimo vertical e recarga de 3 segundos. É uma implementação própria; força/recarga não foram aferidas no Mush. |
| Golem | 120 ferros; use o ovo sobre bloco com espaço livre. Defende contra adversários da sala, não ataca o dono, dura 45 segundos, limite de 2 por jogador. |
| TNT | 4 ouros; acende ao colocar, pavio de 3 segundos. |
| Outras utilidades | Pérola, maçã dourada e balde de água com fluxo limitado à arena. |
| Melhorias | Afiação I por 4 diamantes, Proteção I por 2 diamantes e Forja II por 4 diamantes. São compartilhadas pela dupla e permanecem na partida. |

TNT e bola de fogo só destroem blocos colocados por jogadores. Ilhas originais, camas, vidro e obsidiana são preservados; pedra do End resiste à bola de fogo. A cama precisa ser quebrada à mão. O vidro protege o próprio bloco; não há simulação de escudo de explosão para os blocos atrás dele nesta versão.

## Geradores

| Recurso | Intervalo inicial | Limite acumulado por gerador |
| --- | --- | --- |
| Ferro | 1 unidade por segundo; 2 com Forja II | 64 |
| Ouro | 5 segundos; 3 segundos com Forja II | 16 |
| Diamante | Quatro ilhas intermediárias: 30 → 20 → 15 segundos | 4 |
| Esmeralda | Centro: 60 → 40 → 30 segundos | 4 |

Diamantes/esmeraldas evoluem aos 4 e 8 minutos. A morte súbita padrão começa aos 10 minutos. Bases sem jogador ativo param de gerar ferro/ouro. O limite conta unidades próximas, inclusive pilhas; não cria uma entidade nova indefinidamente. Os pontos de geração ficam livres de construção.

## Lobby, classificação e party

No lobby misto: bússola abre os modos, estrela abre a classificação, etiqueta abre sua party. A barra lateral mostra vitórias/camas do BedWars, líder, quantidade de membros e as primeiras quatro pessoas da party com sua sala. Grupos maiores mostram quantos nomes faltam; `/party lista` ou o menu da etiqueta mostram o grupo completo. Na partida, a barra lateral combina dados do jogo com a party.

`/ranking [bedwars|bedfight|skywars]` (alias `/top`) abre a classificação **local deste servidor**, ordenada por vitórias. As abas separam os modos. Mostra abates, finais e camas; seu perfil mostra também as partidas. Não importa estatísticas do Mush. Os dados e favoritos ficam em `server/plugins/TreinoHub/stats.yml`, salvos a cada 30 segundos e ao desligar normalmente. Ataques têm janela de 15 segundos para creditar abates por queda. Em modo offline o UUID depende do nick, então trocar de nick muda o perfil.

## Plugin contra lag

**TreinoAntiLag** é um segundo plugin próprio, compilado para Java 8 / Spigot 1.8.8 e incluído automaticamente na instalação e na atualização. Não depende de ClearLagg nem baixa JARs de terceiros.

A cada 5 segundos verifica somente mundos temporários `treino_room_`: remove itens largados há mais de 120 segundos e projéteis há mais de 30 segundos, ou entidades desses tipos que já caíram no vazio. Não limpa inventários, baús, golems ou todos os itens de uma partida ao mesmo tempo. O tempo dos itens pode ser ajustado em `server/plugins/TreinoAntiLag/config.yml`; mínimo de 30 segundos. `/treinolag` (administrador/console) mostra amostra de TPS, uso de heap e número de entidades. Não garante corrigir latência de internet, lentidão do túnel, falta de CPU/RAM ou suspensão da máquina.

## Referência pública do Mush e diferenças

Foi consultado o [anúncio oficial Bed Wars v1.9.0](https://forum.mush.com.br/topic/298527/atualiza%C3%A7%C3%A3o-bed-wars-v1.9.0), que documenta compra rápida, editor de hotbar, missões, estatísticas de camas, melhorias e mudanças em bola de fogo/golem. A [documentação do autor de um addon de estilingue](https://www.spigotmc.org/resources/estilingue-plugin.122942/field?field=documentation) descreve a mecânica pública de impulso com creme de magma; o código deste projeto é próprio e não usa o addon.

Esta análise é de fontes públicas, **não uma inspeção completa do servidor em jogo**. Não há acesso ao código/configuração/mapas privados do Mush. A loja e os preços acima são os deste projeto; não são uma cópia certificada dos valores atuais de outra rede.

| Recurso citado na referência | Situação neste projeto |
| --- | --- |
| Loja por categorias, compra rápida, favoritos | Implementados; favoritos individuais por Shift + clique. |
| Bola de fogo, estilingue, golem, TNT, melhorias | Implementados com regras documentadas acima. |
| Classificação e estatísticas | Ranking local total por modo; sem temporadas, XP ou recompensas. |
| Party e convites | Aceite/recusa clicáveis, menu e barra lateral. Solo e duplas com mesma base/cama, sem dano entre aliados. |
| Editor de hotbar, importar favoritos de outro jogador, missões e cosméticos | Não implementados. |
| Todas as armadilhas, percevejos, ovo de ponte, torre compacta, compra usando baús | Não implementados. |
| Mapa Green Hill do Mush / mapas de terceiros | Não incluídos. O tema Sonic/Green Hill é a construção própria do projeto. Não foi encontrado um mapa autorizado do Mush para redistribuir. |

## Validação e limites

A compilação usa a API real Spigot 1.8.8 e gera bytecode Java 8. Testes invocam os handlers reais com um mundo Bukkit em memória: cliques da cama, destruição das duas metades, proteção da própria cama e da decoração contra explosões, espera no mundo separado e persistência de estatísticas/favoritos. Há testes de catálogo, geradores, party, bootstrap, atualização e parada.

Esses testes **não equivalem a uma partida multiplayer no cliente**. Após atualizar, confira com dois jogadores: visibilidade ao renascer, cama inimiga, permanência da armadura, impulso do estilingue, direção da bola de fogo, alvo/expiração do golem, placar e parada com salvamento. Não foi medido PvP/knockback contra o Mush e não há garantia de equivalência total ou ausência de bugs.
