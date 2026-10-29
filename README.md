# Ryuex Treino • Minecraft Java 1.8.9

Servidor para jogar e treinar com amigos: **BedWars, BedFight e SkyWars**, solo/duplas, party, bots de treino e mapas próprios inspirados em Pokémon, Evangelion, Sonic/Green Hill e One Piece. O lobby é uma praça neutra de tamanho médio, separada das arenas.

[![Compilar plugin](https://github.com/silasquercloud/ServeeDetreino/actions/workflows/build.yml/badge.svg)](https://github.com/silasquercloud/ServeeDetreino/actions)

**[Galeria](docs/GALERIA.md) · [Modos](docs/BEDWARS.md) · [Loja](docs/LOJA.md) · [Bots](docs/BOTS.md) · [Party](docs/PARTY.md) · [Admin](docs/ADMIN.md) · [Instalação](docs/MENU.md) · [Playit](docs/PLAYIT.md) · [Referências](docs/REFERENCIAS.md)**

![Praça central do lobby](docs/images/lobby.png)

> As imagens são projeções dos blocos implementados e diagramas de interface; não são capturas do cliente Minecraft. Os Villagers e hologramas são entidades criadas no servidor.

## Atualização 1.4.0

- Lobby neutro de 45 blocos de largura, com três seletores Villager e ranking.
- **BedWars:** dois Villagers fixos no balcão, loja/melhorias, favoritos em slots, editor de hotbar e uso de recursos dos baús.
- **BedFight:** sem loja ou geradores; começa com kit e defesa de madeira na cama.
- Bots com decisões de combate, espaçamento/strafe, pontes rápidas, mineração e coleta de recursos. São zumbis controlados, com limites documentados.
- Green Hill com casinha recuada, frente aberta e arco quadriculado/gramado no centro. Geometria própria inspirada na imagem oficial do fórum, sem copiar arquivos do Mush.

## Instalar ou atualizar

Abra o terminal do Linux/Codespace dentro do repositório:

```bash
bash menu.sh
```

O script instala dependências ausentes antes de abrir o menu: Python, Git, curl, certificados, utilitários, Java 17 para compilar, Java 8 para executar e os binários oficiais do Playit, com versões/hashes fixos. A preparação de pacotes pode exigir sudo. [Detalhes →](docs/MENU.md)

| Opção | Ação |
| --- | --- |
| 1 | Configurar Playit: baixar ferramentas, vincular sua conta e salvar endereço/porta. |
| 2 | Instalar Spigot 1.8.8, TreinoHub, TreinoAntiLag e propriedades. |
| 3 | Iniciar Minecraft + Playit e mostrar endereço público. |
| 4 | Consultar endereço/configuração salva. |
| 5 | Atualizar código e plugins com testes, backup e preservação de dados. |
| 6 | Solicitar `stop`, aguardar salvar e fechar os processos. |
| 0 | Sair do menu. |

Primeira instalação: **1 → 2 → 3**. O vínculo da conta Playit é feito por você; a EULA só é aceita se você digitar `ACEITO`. Não são automatizados esses consentimentos.

Para uma instalação existente, pare com `stop` e aguarde encerrar. Depois:

```bash
git pull --ff-only
bash scripts/update.sh
bash menu.sh
```

Escolha **3**. O atualizador mostra versão/hash instalados e guarda os JARs anteriores em `server/backups/`. Playit, propriedades, mundos pessoais e configurações são preservados. Código modificado localmente não é descartado à força. Para parar de outro terminal nas sessões do launcher novo: `bash scripts/stop.sh`.

O instalador usa `online-mode=false` para clientes originais e não autenticados. Nesse modo, os nicks não têm identidade Microsoft verificada; não há plugin de senha instalado. A whitelist começa desligada. A porta pública do Playit pode ser diferente da porta local 25565: use o endereço mostrado pelo menu.

**Playit fornece o túnel; a máquina hospeda o servidor.** Ele não mantém Codespace suspenso ligado. Codespaces é ambiente de desenvolvimento/testes, não promessa de hospedagem 24h.

## Jogar

Bússola ou NPCs do lobby: **modo → solo/duplas → vagas**. O tema é sorteado por sala. Passe `/treino entrar ID` aos amigos ou use party.

| Modo | Vagas | Regras |
| --- | --- | --- |
| BedWars | Solo 2–8; duplas 4/6/8 | Cama, respawn, loja Villager, geradores e melhorias do time. |
| BedFight | Solo 2/4/6/8; duplas 4/8 | Pares 1x1 ou 2v2, kit pronto e cama protegida, sem economia. |
| SkyWars | Solo 2–12; duplas pares 4–12 | Baús nas bases/centro, reposição e último time vivo. |

```text
/party NICK
/party aceitar NICK
/party jogar bedwars aleatorio duplas
/treino criar skywars 6
/treino criar bedfight 2
/treino bots 1
/treino iniciar
```

A party tem aceite clicável e barra lateral. Duplas usam uma base e cama por time, com dano entre aliados bloqueado. A espera ocorre em outro mundo: não há plataforma de lobby sobre as arenas.

![Arena própria Green Hill](docs/images/bedwars-sonic.png)

## Loja e administração

No **BedWars**, clique nos Villagers **LOJA** ou **MELHORIAS**. Esquerdo compra; Shift configura favorito; direito configura hotbar do item. Há editor por categoria e `/importarcomprarapida NICK`. A carteira procura recursos nos inventários/baús na ordem documentada. [Loja completa e limites →](docs/LOJA.md)

**Salabim_zz** está em `administrators` e recebe OP ao entrar após reiniciar. Use `/treinoadmin`, `/cps`, `/fastplace`, `/bridge` ou `/treino mapa sonic`. CPS é observado pelo servidor; fastplace assistido funciona em pista separada, sem alterar o cliente. [Admin →](docs/ADMIN.md)

`/ranking` mostra estatísticas locais; o menu tem top diário de sequência de vitórias. Os dados ficam em `server/plugins/TreinoHub/stats.yml`, salvos periodicamente e ao desligar normalmente. TreinoAntiLag limpa somente itens/projéteis antigos das arenas; `/treinolag` exibe métricas para administrador.

## Compatibilidade e validação

**Alvo exato: Spigot 1.8.8, Java 8 e cliente Minecraft Java 1.8.9.** O plugin é compilado com a API 1.8.8 e bytecode Java 8. O adaptador NoAI/animação usa a base 1.8.8; não se declara suporte a outras versões. Não precisa de mods ou resource pack obrigatório no cliente.

Os testes cobrem regras, camas, kit/defesa, loja, carteira, preferências, bots, admin, instalação e atualização com Git real. Ainda falta validação multiplayer completa no cliente. Não se promete equivalência de loja, mapas, bots ou knockback com o Mush. [Fontes consultadas e análise do vídeo/fórum →](docs/REFERENCIAS.md)
