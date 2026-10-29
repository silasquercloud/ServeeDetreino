# Bots de treino • 1.5.0

[← Início](../README.md) · [Modos e loja](BEDWARS.md)

Crie uma sala, preencha as vagas e inicie:

```text
/treino criar bedfight 2
/treino bots 1
/treino iniciar
```

Em duplas: `/treino criar bedwars 4 aleatorio duplas`, `/treino bots 3`, `/treino iniciar`. A ordem de entrada continua formando os times; nesse exemplo, você e o primeiro bot são aliados. Bots respeitam times e os pares de BedFight.

## Comportamentos

A decisão é atualizada a cada dois ticks. O bot prioriza combate próximo; longe de adversários, procura a cama inimiga ou caça o jogador sem cama. Alvos que o atacaram recebem prioridade, evitando mudanças aleatórias a cada atualização. Ao nascer em BedWars, pode passar alguns segundos coletando recursos da própria forja.

- **Pontes rápidas:** coloca lã abaixo/adiante do trajeto, limitada à área de construção. Durante a colocação, vira o corpo para trás e olha para baixo, produzindo uma aproximação visual de god bridge. Também tenta recuperar apoio abaixo da altura principal quando é derrubado.
- **Combate:** mantém distância, aproxima ou recua conforme o espaço, alterna o strafe e verifica alcance/linha de visão. Ataques programados têm intervalo de dez ticks e respeitam a invulnerabilidade nativa do Minecraft; não atravessam paredes nem removem os frames de dano do adversário. O objetivo é sustentar pressão/combos, não garantir hits.
- **Objetivo:** tenta chegar à cama e minerar a defesa. Lã, madeira e outros materiais têm tempos diferentes; não apaga toda a proteção instantaneamente. Só quebra blocos de defesa registrados, preservando a ilha e a loja.
- **Recursos:** em BedWars, recolhe itens de ferro/ouro próximos e usa uma carteira própria para melhorar a espada, pelos custos do catálogo. Não cria saldo grátis. A compra é uma decisão interna do bot, sem abrir uma GUI.
- **Duelos:** dano entre aliados e entre pares distintos de BedFight permanece bloqueado. Pode renascer enquanto a cama existir e é eliminado sem ela. Golems também podem atacar bots adversários.

## Aparência e limites

Os bots são exibidos como **jogadores com skin, armadura e animação de ataque**, identificados por nomes BOT. A entidade interna continua sendo um zumbi controlado: o adaptador troca os pacotes visuais e preserva o ID para dano e interação. Isso não cria contas reais. A colisão e alguns sons ainda vêm da entidade interna. O NoAI mantém os ataques programados; se o adaptador de IA falhar, permanece a IA nativa sem duplicar ataques. Se a camada visual falhar, registra aviso e mantém a aparência original.

Não foi treinado um modelo de IA, não há reprodução completa da técnica humana de god bridge e não se garante vencer combates. Não organiza inventário como jogador, não usa todos os itens da loja, não lança todas as utilidades e não saqueia baús no SkyWars. Pode ficar preso em cenários complexos; tenta saltar ou desviar quando detecta pouca progressão. Os mapas são gerados de forma simétrica para reduzir obstáculos no percurso.

A arquitetura suporta Spigot 1.8.8/Java 8; o adaptador foi conferido no código fonte dessa base. Os testes cobrem decisões, alcance, cooldown, tempo de mineração, limites de construção e retomada após respawn. **Movimento, animação, coleta e combate completos ainda precisam ser aferidos no servidor rodando com cliente 1.8.9.**

## Adaptação de combate em 1.5

Ao sofrer dano, o bot troca o lado do strafe com intervalo mínimo para não oscilar a cada hit. Com pouca vida e desvantagem, tenta abrir distância verificando apoio no chão. Inimigos próximos da própria base recebem prioridade enquanto sua cama existe. Essa adaptação é feita por regras; não é aprendizado de máquina e não reproduz todas as decisões humanas.
