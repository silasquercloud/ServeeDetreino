# Ryuex Treino

Servidor Minecraft **Java 1.8.8 / clientes 1.8.9**, com um plugin próprio: **TreinoHub 1.0.0**.
Menus em português, lobby, salas independentes e mapas de ilhas gerados automaticamente. Não exige baixar um mapa nem configurar coordenadas.

## Modos disponíveis

| Modo | Vagas | Regras desta versão |
| --- | --- | --- |
| BedWars | 2–8 | Solo, uma base por jogador, cama, respawn, loja na bancada, ferro/ouro nas bases e esmeralda no centro. |
| BedFight | 2/4/6/8 | Pares de duelos **1x1 independentes**, kit com blocos, espada, picareta e tesoura. Cada jogador tem uma cama. Vencedores assistem até os outros duelos acabarem. |
| SkyWars | 2–12 | Solo, baús com equipamentos, ilhas e centro. Último jogador vivo vence. Reposição de baús aos 3 minutos. |

BedWars é uma implementação de treino enxuta: não inclui todos os upgrades, cosméticos, TNT/fireballs ou recursos de grandes redes. BedFight com 6 vagas cria três duelos; não é 3x3. Os mapas são arenas simples próprias, não cópias de mapas famosos. Esta versão não inclui bots.

## Compatibilidade e validação

- Plugin compilado contra **Spigot API 1.8.8-R0.1-20160221.082514-43**, bytecode Java 8. Dependências de compilação têm SHA-256 fixo.
- Alvo do servidor: **Spigot 1.8.8 com Java 8 atualizado**. Minecraft Java 1.8.9 usa o mesmo protocolo da 1.8.8.
- Compilação realizada neste projeto. **Uma partida multiplayer completa ainda precisa ser validada com jogadores.** Não confundir compilação com teste em jogo.
- Não há BedWars1058, SkyWarsReloaded ou plugin BedFight de terceiros instalado: os três modos pertencem ao TreinoHub, evitando conflito entre plugins de minigames.
- Clientes modernos e Bedrock não foram configurados nem validados.

## Abrir no Codespaces

[**Criar Codespace deste repositório**](https://codespaces.new/silasquercloud/ServeeDetreino)

O contêiner instala Java 17 para compilar o plugin e Java 8 para o servidor. O plugin compila automaticamente ao criar o ambiente.

No terminal:

```bash
bash scripts/install.sh
```

A primeira instalação compila o Spigot original com BuildTools oficial e pode demorar vários minutos. Sem downloads de servidores de procedência desconhecida. Leia a [EULA do Minecraft](https://aka.ms/MinecraftEULA). Se concordar:

```bash
ACCEPT_EULA=true bash scripts/start.sh
```

Próximas inicializações: `bash scripts/start.sh`. Desligue pelo comando `stop` no console. Para alterar RAM: `SERVER_RAM=3G bash scripts/start.sh`.

**Codespaces é ambiente de desenvolvimento/teste, não hospedagem permanente.** Os termos do GitHub limitam atividades fora do desenvolvimento/teste do projeto. O encaminhamento público no navegador usa HTTP/HTTPS: uma URL `app.github.dev` **não é IP Minecraft**. A configuração não cria um endereço público jogável para seus amigos.

Para testar localmente com GitHub CLI, em **seu computador** autenticado:

```bash
gh codespace ports forward 25565:25565 --codespace NOME_DO_CODESPACE
```

Mantenha o comando aberto e conecte em `localhost:25565`. Isso é um teste local autenticado, não acesso público para amigos. Para jogar regularmente, instale numa host Minecraft com porta TCP ou em seu PC com conectividade adequada.

## Liberar acesso e jogar

O servidor inicia com autenticação Microsoft e whitelist ligadas. No **console do servidor**, sem `/`:

```text
whitelist add SEU_NICK
whitelist add NICK_DO_AMIGO
```

Os menus não exigem OP. Não é necessário dar OP a todos.

No jogo, use a bússola ou `/treino`. Selecione o modo, depois a quantidade de vagas. A sala começa ao encher, com contagem de 10 segundos. O dono pode antecipar com pelo menos 2 jogadores; BedFight exige quantidade par.

```text
/treino criar skywars 6
/treino entrar 1
/treino iniciar
/treino salas
/treino loja
/treino sair
```

O exemplo cria uma sala SkyWars com **6 vagas máximas**. Passe aos amigos o número mostrado no chat. Há até 4 salas simultâneas, ajustável em `server/plugins/TreinoHub/config.yml` depois da primeira inicialização. Partidas não persistem após desligar.

Aos 10 minutos começa a morte súbita: camas são destruídas e quem estiver a mais de 25 blocos do centro recebe dano. Após mais 5 minutos, empate. Sair/desconectar elimina o jogador e pode encerrar uma partida. Há proteção de blocos do mapa, lobby e salas em espera.

## Instalar numa host Minecraft

1. Escolha **Spigot 1.8.8, Java 8**. Se a host só oferecer outra versão, a compatibilidade deve ser conferida antes de instalar.
2. Compile com `python3 scripts/build.py` ou baixe o artefato na aba **Actions → Compilar plugin** do GitHub.
3. Envie `TreinoHub-1.0.0.jar` para a pasta `plugins/`.
4. Com o servidor parado, ajuste as propriedades usando `config/server.properties` como referência. Preserve a porta e o IP exigidos pela host.
5. Inicie, adicione seus amigos à whitelist e use `/treino`.

Não instale junto de outros gerenciadores de lobby/minigames antes de verificar conflitos. Para atualizar este plugin, pare o servidor, substitua o JAR e inicie novamente; não use `/reload`.

## Roteiro do primeiro teste em jogo

- Dois jogadores: entrar no lobby, abrir menu, criar sala, entrar pelo ID e iniciar.
- BedWars: coletar ferro/ouro, comprar, construir, quebrar cama inimiga, morrer com e sem cama, conferir vencedor.
- BedFight: 2 e 4 jogadores; confirmar isolamento entre pares, respawn e vencedor de cada duelo.
- SkyWars: 6 jogadores; baús, quedas, dano, espectador, vitória e saída para lobby.
- Sair durante contagem e durante partida; confirmar que não fica uma sala presa.
- Jogar outra partida: arena deve estar limpa, sem blocos da anterior.

## Referências oficiais

- [BuildTools e versões Spigot](https://www.spigotmc.org/wiki/buildtools/)
- [Portas no Codespaces](https://docs.github.com/en/codespaces/developing-in-a-codespace/forwarding-ports-in-your-codespace)
- [Termos do Codespaces](https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features#codespaces)
