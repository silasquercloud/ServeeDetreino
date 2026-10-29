# Mapas temáticos • Multiverso

[← Início](../README.md) · [Ver galeria](GALERIA.md) · [Instalação](GUIA.md)

Os quatro temas podem ser usados em **BedWars, BedFight e SkyWars**. Escolha pelo menu: **modo → tema → vagas**. Pelo comando, informe o tema depois das vagas.

| Tema | Identificador | Construções |
| --- | --- | --- |
| **Pokémon** | `pokemon` | Centro Pokémon com teto vermelho e cruz, entrada e janelas; escultura de Pokébola, árvores e ilhas de grama. |
| **Evangelion** | `evangelion` | Torres de uma cidade inspirada em Tokyo-3, pirâmide, cabeça de EVA em roxo/verde e piso industrial. |
| **Sonic / Green Hill** | `sonic` | Loop de blocos, argolas de ouro, palmeiras, piso verde e camadas de terreno quadriculado. |
| **One Piece** | `onepiece` | Navio pirata, mastro e vela com caveira em blocos, chapéu de palha, palmeira e borda azul inspirada no mar. |

São construções próprias inspiradas nesses universos, feitas com materiais nativos disponíveis na 1.8.8. Não dependem de mods do cliente ou resource pack obrigatório. Blocos decorativos são protegidos nas partidas, preservando a arena.

## Exemplos

```text
/treino criar skywars 6 onepiece
/treino criar bedwars 4 pokemon
/treino criar bedfight 2 evangelion
/treino criar skywars 8 sonic
```

Vagas continuam obedecendo às regras: SkyWars 2–12; BedWars 2–8; BedFight 2/4/6/8. As ilhas ficam com centros a 42 blocos do centro da arena. A base normal tem raio de 8 blocos, com construções e camadas inferiores; o centro tem raio de 13. Não há pontes prontas nas partidas: construí-las faz parte do treino.

## Lobby misto

Uma praça central de quartzo e iluminação recebe os jogadores. Quatro caminhos com bordas de vidro e arcos coloridos conectam as alas:

- Vermelho: Pokémon.
- Roxo: Evangelion.
- Azul: Sonic.
- Laranja: One Piece.

As quatro alas fazem parte do mesmo lobby e podem ser exploradas a pé. Os arcos são construções decorativas; a escolha da partida é feita pela bússola ou `/treino`.

## Como as imagens correspondem ao mapa

O código [MapArt.java](../src/main/java/br/ryuex/treino/MapArt.java) fornece as posições e materiais tanto para o construtor Bukkit quanto para o exportador da galeria. O renderizador usa esses blocos e os representa com cores simplificadas. Assim, as construções da prévia têm implementação no plugin, em vez de serem apenas artes conceituais.

As projeções ainda não são screenshots. Texturas, transparências, formas de cercas/itens, iluminação e HUD serão os do cliente Minecraft. A mobília de partida (camas, lojas, baús e marcação das equipes) é aplicada ao criar cada sala; as imagens isoladas de tema focam a construção decorativa. As vistas de arena também incluem os locais dos baús/camas.

## Validação

Os testes verificam os materiais contra a API real da 1.8.8, espaço livre no spawn e nos baús/camas, caminho dos geradores, configuração para 12 jogadores e continuidade das pontes do lobby. A compilação do plugin usa bytecode Java 8 para o servidor Spigot 1.8.8 e cliente Minecraft Java 1.8.9.

**Ainda falta testar estas novas construções dentro de uma partida multiplayer real.** As prévias não certificam desempenho ou jogabilidade no cliente.

Ao atualizar, pare com `stop`, execute `git pull`, instale novamente com `bash scripts/install.sh` e inicie. O setup Playit anterior é reaproveitado. O lobby novo usa um mundo próprio, sem modificar mapas pessoais da host.
