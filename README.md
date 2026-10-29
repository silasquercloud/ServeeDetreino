# Ryuex Treino • Minecraft Java 1.8.9

BedWars, BedFight e SkyWars para jogar com seus amigos. **Ilhas inspiradas em Pokémon, Evangelion, Sonic e One Piece**, com lobby misturando os quatro universos. Plugin próprio, seleção de tema/vagas e construções geradas automaticamente.

[![Compilar plugin](https://github.com/silasquercloud/ServeeDetreino/actions/workflows/build.yml/badge.svg)](https://github.com/silasquercloud/ServeeDetreino/actions)

**[Guia completo](docs/GUIA.md) · [Configurar Playit](docs/PLAYIT.md) · [Ver imagens](docs/GALERIA.md) · [Criar Codespace](https://codespaces.new/silasquercloud/ServeeDetreino)**

![Lobby misto com quatro universos](docs/images/lobby.png)

> As imagens são projeções técnicas do mapa e do menu implementados, não screenshots de uma partida. [Galeria e limites das prévias →](docs/GALERIA.md)

## Comece por aqui

No Linux ou no terminal do Codespace:

```bash
bash scripts/setup.sh
```

O assistente segue esta ordem:

1. Baixa **Playit 1.1.0 oficial**, com SHA-256 verificado, sem exigir root/systemd.
2. Mostra o link para você vincular o agente à sua conta.
3. Orienta a criação de um túnel **Minecraft Java → 127.0.0.1:25565** e solicita o endereço público.
4. Só então compila o plugin e instala o **Spigot 1.8.8**.

A instalação direta do servidor é bloqueada enquanto o setup do Playit estiver incompleto. Não coloque tokens ou chaves no GitHub.

Depois, leia a [EULA do Minecraft](https://aka.ms/MinecraftEULA). Se concordar:

```bash
ACCEPT_EULA=true bash scripts/start.sh
```

Nas próximas vezes:

```bash
bash scripts/start.sh
```

Esse comando inicia **Playit + Minecraft juntos**, mostra o endereço cadastrado e encerra o agente quando o servidor para. Não é necessário deixar dois terminais abertos. No console, use `stop` para desligar.

**O Playit fornece o túnel/endereço; a máquina onde você executa o projeto hospeda o servidor.** Ele não mantém uma máquina desligada online. Codespaces deve ser usado para desenvolvimento/testes do projeto, conforme os [termos do GitHub](https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features#codespaces).

## Escolha o modo, o tema e as vagas

| Modo | Vagas | O que está implementado |
| --- | --- | --- |
| **BedWars** | 2–8 | Solo, camas, respawn, loja na bancada, geradores de ferro/ouro e esmeralda no centro. |
| **BedFight** | 2/4/6/8 | Pares de duelos 1x1, kit pronto, blocos, cama e respawn. Seis vagas = três duelos. |
| **SkyWars** | 2–12 | Uma ilha por jogador, baús nas bases e no centro, espectador, vitória e reposição aos 3 minutos. |

Use a bússola ou `/treino`: **modo → tema → vagas**. Todos os quatro temas estão disponíveis nos três modos. [Veja os mapas e comandos de cada tema](docs/TEMAS.md). Exemplo para seis amigos em One Piece:

```text
/treino criar skywars 6 onepiece
/treino entrar 1
```

O número da sala aparece no chat. Ela inicia ao encher; o dono pode antecipar com `/treino iniciar` se houver pelo menos dois jogadores. BedFight exige número par.

## Imagens do projeto

![Disposição do menu implementado](docs/images/menu.png)

Veja os quatro temas em detalhe e arenas para seis jogadores na [galeria completa](docs/GALERIA.md).

## Compatibilidade e estado dos testes

- **Cliente principal: Minecraft Java 1.8.9.** Spigot 1.8.8 e cliente 1.8.9 usam o protocolo 47; não precisa ViaVersion para essa combinação.
- **Servidor: Spigot 1.8.8, Java 8 atualizado.** BuildTools oficial compilado com sucesso neste projeto.
- Plugin: **TreinoHub 1.0.0**, compilado contra a API 1.8.8 com bytecode Java 8 e dependências verificadas por hash.
- Compilação do plugin e testes de vagas/início/pares aprovados. Os scripts Playit têm testes de pré-requisitos e encerramento de processos.
- **Ainda não foi validada uma partida multiplayer completa.** Vincular sua conta, criar o túnel e testar uma conexão pública são etapas que dependem do seu setup.
- Playit funciona como processo externo: não depende da versão da API Bukkit. O setup portátil suporta Linux x64 e ARM64; Java/Spigot em ARM64 não foram testados neste projeto.
- Não há plugins BedWars1058/SkyWarsReloaded instalados: os três modos estão no TreinoHub. Clientes modernos e Bedrock não foram configurados.

BedWars é uma versão enxuta de treino. Não inclui todos os upgrades, cosméticos, TNT/fireballs ou recursos de grandes redes. Não inclui bots. Os mapas são construções próprias em blocos, inspiradas nos quatro universos. Não são cópias de mapas de outros servidores, e não exigem resource pack. As áreas de spawn, camas, baús e pontes do lobby têm testes de geometria e materiais da 1.8.8.

## Onde está cada coisa

| Pasta/arquivo | Conteúdo |
| --- | --- |
| [docs/GUIA.md](docs/GUIA.md) | Instalar, iniciar, liberar amigos, comandos e teste em jogo. |
| [docs/PLAYIT.md](docs/PLAYIT.md) | Conta, túnel, endereço público e diagnóstico. |
| [docs/GALERIA.md](docs/GALERIA.md) | Prévias visuais e instruções para screenshots reais. |
| [docs/TEMAS.md](docs/TEMAS.md) | Temas, construções, seleção e compatibilidade dos mapas. |
| [scripts/setup.sh](scripts/setup.sh) | Playit primeiro, instalação depois. |
| [scripts/start.sh](scripts/start.sh) | Inicia os dois processos juntos. |
| [config/server.properties](config/server.properties) | Configuração padrão do servidor. |
| [src/main/java](src/main/java) | Código do plugin. |

## Referências

[Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/) · [Protocolos 1.8.8/1.8.9](https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/common/protocolVersions.json) · [Playit oficial](https://playit.gg/) · [Código e release fixada do Playit](https://github.com/playit-cloud/playit-agent/releases/tag/v1.1.0)
