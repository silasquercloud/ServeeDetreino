# Party • convites clicáveis e salas em grupo

[← Início](../README.md) · [Menu de instalação/atualização](MENU.md) · [Temas](TEMAS.md)

Implementação própria no TreinoHub para **Spigot 1.8.8, Java 8 e cliente Minecraft Java 1.8.9**. Não exige um plugin externo de parties.

## Chamar um amigo e entrar no x1

O líder usa:

```text
/party NICK_DO_AMIGO
```

O amigo recebe os botões **[ACEITAR]** e **[RECUSAR]** no chat. Basta clicar. Há texto de ajuda ao passar o mouse. O convite expira em **60 segundos**, só pode ser aceito pelo destinatário e tem intervalo de reenvio de 10 segundos.

Depois que o amigo aceitar, o líder pode usar:

```text
/party jogar bedfight onepiece
```

Com dois membros, isso cria uma sala BedFight 1x1 com duas vagas, entra com os dois e inicia a contagem. O tema pode ser `pokemon`, `evangelion`, `sonic` ou `onepiece`.

## Comandos

`/p` é um alias de `/party`.

| Comando | Ação |
| --- | --- |
| `/party` | Mostra a ajuda. |
| `/party NICK` | Cria a party se necessário e convida o jogador. |
| `/party convidar NICK` | Mesmo convite, de forma explícita. |
| `/party aceitar NICK_DO_LIDER` | Aceita manualmente um convite válido. O botão faz isso por UUID. |
| `/party recusar NICK_DO_LIDER` | Recusa o convite. |
| `/party lista` | Mostra membros e líder. |
| `/party chat MENSAGEM` | Envia uma mensagem somente à party. |
| `/party jogar bedfight TEMA` | Cria uma sala com o número atual de membros e leva todos. |
| `/party jogar skywars TEMA` | Mesmo fluxo para SkyWars. |
| `/party jogar bedwars TEMA` | Mesmo fluxo para BedWars. |
| `/party puxar` | Leva todos à sala em espera em que o líder está. |
| `/party puxar ID_DA_SALA` | Reúne todos numa sala em espera específica. |
| `/party sair` | Sai da party, mantendo a sala atual. |
| `/party expulsar NICK` | Líder remove um membro da party; não o expulsa da partida. |
| `/party lider NICK` | Transfere a liderança. |
| `/party dissolver` | Líder encerra a party. |

## Escolher pelo menu normal

O líder também pode usar a bússola ou `/treino`, escolher modo, tema e vagas. Ao criar ou entrar em uma sala em espera, a party toda acompanha. `/treino entrar ID` também acompanha esse comportamento.

Membros que não são líderes devem aguardar o líder para criar/entrar em grupo. A party tem até **12 membros**, mas o modo continua com seus limites:

- BedFight: 2/4/6/8. Com quatro pessoas, são dois duelos 1x1; não é 2x2.
- BedWars solo: 2–8.
- SkyWars solo: 2–12.

## Regras para puxar todos

- Todos precisam estar conectados.
- O destino precisa estar em espera ou na contagem inicial.
- Precisa haver espaço para o grupo inteiro. Quem já está no destino não conta duas vezes.
- Se alguém estiver em uma partida em andamento, o comando é bloqueado antes de mover o grupo.
- A entrada em grupo não altera as regras de PvP: no x1/solo, os membros podem ser adversários. Party não transforma jogadores em um time.

O líder fica primeiro na ordem de entrada, seguido dos outros membros. Nos duelos de uma sala criada pela party, os pares seguem essa ordem. Sair da party não retira ninguém da partida; `/treino sair` sai da sala individualmente.

Se o líder desconectar ou sair, um membro restante assume. Trocar líder ou dissolver a party invalida convites antigos; peça um novo convite. Parties e convites não persistem após reiniciar o servidor.

## Atualizar a instalação

Pare com `stop`, abra `bash menu.sh`, escolha **5 — Atualizar servidor** e, ao terminar, **3 — Ligar**. Para uma cópia antiga que ainda não tem a opção 5, execute `git pull` uma vez para obter o novo menu.

Testes automatizados cobrem conteúdo dos cliques, posse/expiração dos convites, permissão do líder, limite de membros e cálculo das vagas. **O clique e a transferência completa ainda precisam ser testados dentro de uma partida real com clientes 1.8.9.**

## Menu e barra lateral

No lobby, clique com a etiqueta **Minha party** para ver todos os membros e os botões de puxar/sair. A barra lateral do lobby e das partidas mostra líder, tamanho do grupo e até quatro membros com sua sala. Grupos maiores mostram o total adicional; `/party lista` exibe todos. A classificação local abre pela estrela ou `/ranking`.
