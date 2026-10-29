# Modos, mapas e desempenho • 1.4.0

[← Início](../README.md) · [Loja](LOJA.md) · [Bots](BOTS.md) · [Admin](ADMIN.md) · [Galeria](GALERIA.md)

**TreinoHub 1.4.0 / TreinoAntiLag 1.1.0**, com nomes de JAR estáveis para atualizar a instalação existente. Base: Spigot 1.8.8 / Java 8; cliente Java 1.8.9.

## Aplicar

Pare o Minecraft com `stop`, aguarde salvar e execute:

```bash
git pull --ff-only
bash scripts/update.sh
bash menu.sh
```

Escolha **3** para ligar. O atualizador valida os dois JARs, faz backup, preserva Playit/configurações e mostra as versões instaladas. A opção 6 ou `bash scripts/stop.sh` para a sessão iniciada pelo launcher novo. Não use `/reload` para trocar plugins durante partidas.

## BedWars

Solo ou duplas, loja com Villagers, geradores, defesa construída pelos jogadores e renascimento enquanto o time tiver cama. Duplas compartilham base, cama e melhorias; armaduras/ferramentas são individuais. Cada sala aceita 2–8 jogadores; duplas precisam de 4/6/8 vagas totais.

| Recurso | Intervalo | Acúmulo máximo por ponto |
| --- | --- | --- |
| Ferro | 1/s; 2/s com Forja II | 64 |
| Ouro | 5s; 3s com Forja II | 16 |
| Diamante | 30 → 20 → 15s | 4 |
| Esmeralda | 60 → 40 → 30s | 4 |

Diamantes e esmeraldas evoluem aos 4 e 8 minutos. Bases sem jogador ativo param de gerar ferro/ouro. O limite conta unidades, inclusive pilhas. Os geradores usam poços baixos de pedra com núcleo visível, preservando a circulação. [Itens, favoritos, hotbar e uso dos baús →](LOJA.md)

## BedFight

**Sem loja, Villagers ou geradores de recursos.** A partida já cria uma camada de madeira em volta de cada cama e entrega espada de madeira, picareta de madeira, machado de madeira, tesoura, couro e 128 lãs. O kit é reposto ao renascer. A defesa inicial é registrada como bloco removível, não como decoração invulnerável. Quebrar blocos de treino não gera drops.

Solo: 2/4/6/8 participantes, formando pares de 1x1. Duplas: 4 ou 8, formando 2v2. Dano e quebra de cama ficam limitados ao par adversário; aliados não se machucam. Esse kit/defesa é a regra deste projeto, sem alegar que cada item coincide com a configuração privada do Mush.

## SkyWars

2–12 participantes; duplas a partir de quatro e com número par. Baús nas bases e centro; reposição aos três minutos. Sem loja e sem cama. Ganha o último time vivo.

## Lobby e mapas

O lobby antigo dos quatro temas foi substituído por **uma praça neutra de 45 blocos de largura**, com jardins, caminhos, três seletores Villager e pavilhão de classificação. Bússola e NPCs abrem os modos; estrela abre ranking e etiqueta abre party. O lobby usa um mundo novo, sem alterar mapas pessoais do usuário.

Os temas das salas continuam Pokémon, Evangelion, Sonic/Green Hill e One Piece, sorteados ao criar. As bases BedWars/BedFight deixam livre a frente da cama, com marcos pequenos ao fundo; construções maiores ficam no centro. O Green Hill recebeu uma casinha quadriculada recuada e um arco gramado maior no centro. [Referências verificadas e limites da análise →](REFERENCIAS.md)

## Combate e encerramento

O clique esquerdo da cama segue para a quebra; o direito é bloqueado para impedir dormir/explodir. A cama do próprio time não pode ser quebrada. Destruir uma metade remove as duas e impede novos renascimentos. Não há plataforma de espera acima da arena: a espera usa o mundo separado do lobby.

Há proteção de dois segundos ao nascer, encerrada ao atacar. O intervalo nativo de dano da 1.8 é preservado. O plugin restaura Survival e atualiza o rastreamento/visibilidade após renascer. Espectadores ficam ocultos para ativos; outras salas são isoladas. Não há garantia de equivalência de knockback com o Mush.

Morte súbita começa por padrão aos dez minutos, remove camas e obriga aproximar do centro. Limites/tempo estão no config do TreinoHub. Partidas sem humanos são encerradas; a pasta do mundo temporário é removida ao terminar a sala.

## Party, ranking e treino

`/party NICK` envia convite com aceite clicável. A criação/entrada pelo líder reúne o grupo se todos couberem. O menu usa **modo → solo/duplas → vagas**. Em duplas, times seguem a ordem de entrada dois a dois.

```text
/treino criar bedwars 4 aleatorio duplas
/party jogar bedwars aleatorio duplas
/treino criar bedfight 2
/treino bots 1
/treino iniciar
```

O ranking `/ranking` é local, com vitórias, abates e camas. O menu mostra top diário de sequência. Estatísticas/favoritos/hotbar são salvos em `stats.yml`. Bots são zumbis controlados; veja [comportamentos e limites](BOTS.md). **Salabim_zz** permanece no cadastro de administradores; após reiniciar pode usar `/treinoadmin` e `/treino mapa sonic`.

## Desempenho e validação

TreinoAntiLag limpa somente itens/projéteis antigos dos mundos temporários; não limpa inventários ou todos os itens da partida de uma vez. `/treinolag` mostra amostra de TPS/heap/entidades para OP/console. Não corrige latência de internet ou falta de CPU/RAM.

Testes cobrem regras, salas, camas, defesa do BedFight, ausência de loja nesse modo, presença de Villagers no BedWars, decisões dos bots, pagamento ordenado sem desconto parcial, preferências de compra, admin, scripts e atualização com Git real. A interface e o comportamento completo de NPCs/bots ainda precisam ser conferidos numa partida real. As imagens são projeções/diagramas, não screenshots.
