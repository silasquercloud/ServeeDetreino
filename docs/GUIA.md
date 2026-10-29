# Guia de instalação e uso

[← Início](../README.md) · [Playit](PLAYIT.md) · [Imagens](GALERIA.md)

## 1. Preparar o ambiente

Opção mais simples para desenvolver/testar: [criar um Codespace](https://codespaces.new/silasquercloud/ServeeDetreino). O contêiner prepara Java 17 e Java 8. Ao abrir, ele mostra o comando inicial e não instala o servidor antes do Playit.

Em uma máquina Linux própria, instale Git, Python 3, curl, Java 17 para compilar o plugin e Java 8 para executar/compilar o Spigot. Clone o repositório e abra o terminal dentro dele.

Caso o Java 8 não seja encontrado automaticamente, indique seu executável:

```bash
export JAVA8_BIN=/caminho/real/do/jdk8/bin/java
```

Troque o caminho pelo da sua instalação. No Codespace isso normalmente é automático.

## 2. Fazer o setup completo

```bash
bash scripts/setup.sh
```

Primeiro você conclui o [setup do Playit](PLAYIT.md). O script baixa o agente e o CLI oficiais, valida os hashes, inicia o agente temporariamente e mostra o link de vinculação. Você aprova no navegador e configura o túnel.

Quando o agente estiver ativo, cadastre o endereço público solicitado. Só depois dessa etapa o assistente executa a instalação do servidor. Na primeira vez, o BuildTools precisa baixar dependências e compilar o Spigot; aguarde a mensagem de instalação concluída. Falhas interrompem o processo, sem apresentar a instalação como concluída.

Se o Playit já foi configurado e apenas a compilação falhou, corrija a causa e execute `bash scripts/install.sh` novamente.

## 3. Iniciar os dois juntos

Leia a [EULA](https://aka.ms/MinecraftEULA). Se concordar:

```bash
ACCEPT_EULA=true bash scripts/start.sh
```

Nas próximas vezes: `bash scripts/start.sh`.

O launcher verifica o setup, inicia o Playit, espera o estado `running` e inicia o Minecraft no mesmo terminal. O endereço mostrado é o que você cadastrou no setup; aguarde `Done` no console antes de entrar. A conectividade pública ainda deve ser testada no cliente.

Para limitar a RAM:

```bash
SERVER_RAM=2G bash scripts/start.sh
```

Para desligar, digite `stop` no console. O launcher também encerra seu agente Playit. Fechar/interromper esse processo ou desligar a máquina deixa o servidor offline; não há mecanismo para manter Codespaces artificialmente acordado.

## 4. Liberar seus amigos

O padrão usa autenticação Microsoft e whitelist. No console do servidor, **sem barra**:

```text
whitelist add SEU_NICK
whitelist add NICK_DO_AMIGO
```

No Minecraft Java **1.8.9**, adicione o endereço público exibido pelo Playit. Seus amigos não precisam instalar Playit. Se o painel fornecer uma porta, mantenha `endereco:porta`.

Não é necessário dar OP para abrir o menu ou criar salas.

## 5. Criar e jogar

Use a bússola ou `/treino`. Escolha modo → vagas. Passe o ID da sala aos amigos.

| Comando | Função |
| --- | --- |
| `/treino` | Abre o menu. |
| `/treino criar skywars 6` | Cria uma sala SkyWars de até 6 jogadores. |
| `/treino criar bedwars 4` | Cria BedWars solo com 4 bases. |
| `/treino criar bedfight 2` | Cria um duelo 1x1. |
| `/treino entrar 1` | Entra na sala de ID 1, se disponível. |
| `/treino iniciar` | Dono antecipa o início com pelo menos 2 jogadores. |
| `/treino salas` | Lista salas abertas. |
| `/treino loja` | Abre a loja durante BedWars. Também abre clicando na bancada. |
| `/treino sair` | Sai da sala e volta ao lobby. |

Máximo inicial: 4 salas simultâneas. Altere `server/plugins/TreinoHub/config.yml` com o servidor parado. Camas são removidas na morte súbita após 10 minutos; jogadores longe do centro recebem dano. Aos 15 minutos, empate. Desconectar elimina o jogador da partida.

## 6. Testar antes de chamar todo mundo

- Entre com dois clientes 1.8.9: lobby, bússola, criar sala, entrar pelo ID e iniciar.
- BedWars: recolher ferro/ouro, comprar, construir, destruir cama, morrer com/sem cama, conferir vitória.
- BedFight: testar 2 e 4 jogadores, dano permitido entre oponentes e bloqueado entre pares diferentes.
- SkyWars: testar seis vagas, lotação, baús, quedas, espectador e vencedor.
- Sair durante contagem e partida. Confirmar retorno ao lobby e arena limpa na próxima sala.
- Pedir a um amigo fora da sua rede que conecte pelo endereço Playit.

## Instalar em uma host Minecraft que já oferece IP

O fluxo automático deste projeto usa Playit primeiro. Em uma host Minecraft com IP próprio, basta escolher **Spigot 1.8.8 / Java 8**, enviar o JAR `TreinoHub-1.0.0.jar` para `plugins/` e reiniciar. Você pode baixar o plugin em [Actions → Compilar plugin](https://github.com/silasquercloud/ServeeDetreino/actions).

Preserve IP/porta definidos pela host. Não copie essas propriedades sem conferir. Nesse cenário você não precisa executar os scripts que gerenciam Playit. Compatibilidade com outra versão de servidor deve ser verificada antes da instalação.

## Atualizar

Pare com `stop`, atualize os arquivos do repositório, execute `bash scripts/install.sh` e reinicie. Suas credenciais Playit ficam locais e não entram no GitHub. Não use `/reload` para atualizar o plugin.
