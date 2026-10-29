# Revisão de jogabilidade • 1.5.1

Referências consultadas: [Bed Wars 1.9.0](https://forum.mush.com.br/topic/298527/atualiza%C3%A7%C3%A3o-bed-wars-v1.9.0) e [anúncios oficiais](https://mush.com.br/). Não há confirmação pública de todas as fórmulas internas de knockback, preços e tempos. Os valores do projeto são próprios; não devem ser apresentados como os valores exatos do Mush.

| Área | Implementado | Diferenças / pendências |
|---|---|---|
| Loja | NPC com skin, categorias, favoritos, editor de hotbar, importação local, carteira de baús | Catálogo e preços próprios; faltam itens e melhorias |
| Interface | Bloqueio de uso de itens com loja/editor aberto | Personalização de favoritos no lobby ainda pendente |
| Partidas | Salas privadas, solo/duplas, times, camas, respawn, party | Não há todos os modos, matchmaking ou escala de uma rede |
| Mapas | Geometria própria, base aberta, Green Hill inspirado em referências | Não são arquivos originais do Mush |
| Bots | Skin, combate por regras, defesa de base, ponte e mineração | Física de mob, navegação limitada, sem todas as utilidades |
| Progressão | Estatísticas e classificação local | Missões, XP e cosméticos completos pendentes |
| Combate | Regras de alcance, proteção, aliados e duelos | Fórmula exata de knockback do Mush não confirmada |

## Correções desta revisão

- Bloqueado uso de itens com a loja ou seus editores abertos.
- Bots de BedWars pagam pelos blocos com ferro coletado e consomem o estoque ao construir.
- Removidas ferramentas gratuitas de mineração dos bots no BedWars.
- Corrigido dano-base de espada dos bots para 4/5/6 (madeira/pedra/ferro).

Compilação e testes automatizados não certificam perfeição em jogo. A validação final inclui duas pessoas em duplas, compra e respawn, destruição de cama, troca de mundo e combate com bots no cliente 1.8.9. Nenhuma fórmula privada foi inventada ou atribuída ao Mush.
