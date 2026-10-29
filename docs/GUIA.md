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

## 2. Abrir o menu e instalar

```bash
bash menu.sh
```

Escolha **1 — Configurar Playit**. O assistente baixa o agente/CLI oficiais com hashes fixos e mostra o link de vinculação da sua conta. Configure o túnel **Minecraft Java/TCP → 127.0.0.1:25565** e cadastre o endereço público. Se o endereço não incluir porta, o assistente permite informar a porta pública do painel separadamente ou marcar o endereço como automático/DNS-SRV. Veja [o guia Playit](PLAYIT.md).

Depois escolha **2 — Instalar servidor completo**. A etapa compila Spigot 1.8.8, compila e instala TreinoHub e prepara os arquivos de configuração. O lobby e os mapas temáticos são construídos pelo plugin quando o servidor e as salas forem iniciados. A primeira compilação do Spigot pode demorar vários minutos; aguarde a mensagem de conclusão.

As opções são separadas: configurar o Playit não instala nem liga o servidor automaticamente. Falhas são mostradas e o menu permite tentar novamente.

## 3. Ligar e consultar IP/porta

No menu, escolha **3**. Na primeira vez, leia a [EULA](https://aka.ms/MinecraftEULA) e, se concordar, digite `ACEITO`.

A tela mostra:

- IP/host público do Playit.
- Porta pública do túnel, quando informada.
- Endereço completo para colar no Minecraft.
- Destino local `127.0.0.1:25565`.

O launcher inicia o Playit, espera o agente ficar ativo e inicia o Minecraft no mesmo terminal. **Espere `Done`** no console antes de conectar. O endereço salvo, por si só, não comprova que o túnel público esteja acessível.

Digite `stop` no console para desligar os dois e voltar ao menu. A opção **4** consulta o endereço salvo sem ligar. Fechar a máquina ou o ambiente deixa o servidor offline.

Para ajustar a RAM antes de abrir o menu:

```bash
SERVER_RAM=2G bash menu.sh
```

## 4. Entrar com amigos • original e não original

A instalação já aplica:

```properties
online-mode=false
white-list=false
server-port=25565
server-ip=
```

O servidor aceita clientes Minecraft Java **1.8.9** originais e não autenticados na Microsoft. Seus amigos entram pelo endereço público mostrado pelo menu e não precisam instalar Playit. Se houver porta pública explícita, mantenha `endereco:porta`.

O modo offline não verifica a identidade do nick. Esta configuração não inclui login por senha nem skins restauradas automaticamente. Não dê OP a outros jogadores apenas para abrir o menu: os comandos de salas não exigem OP.

A whitelist vem desligada. Se você ativá-la depois, precisará cadastrar os jogadores permitidos; isso é independente de `online-mode`.

Ao atualizar uma instalação anterior, a opção **2** faz backup do arquivo existente e aplica o novo modo e destino local, preservando as outras propriedades. Se o arquivo ainda estiver com `online-mode=true`, a inicialização pede essa atualização antes de ligar.

## 5. Criar e jogar

Use a bússola ou `/treino`. Escolha modo → tema → vagas. Passe o ID da sala aos amigos.

| Comando | Função |
| --- | --- |
| `/treino` | Abre o menu. |
| `/treino criar skywars 6 onepiece` | SkyWars de até 6 jogadores com ilhas piratas. |
| `/treino criar skywars 6 sonic` | SkyWars Green Hill com 6 vagas. |
| `/treino criar bedwars 4 evangelion` | BedWars com 4 bases de Evangelion. |
| `/treino criar bedwars 4` | Cria BedWars solo com 4 bases. |
| `/treino criar bedfight 2` | Cria um duelo 1x1. |
| `/treino entrar 1` | Entra na sala de ID 1, se disponível. |
| `/treino iniciar` | Dono antecipa o início com pelo menos 2 jogadores. |
| `/treino salas` | Lista salas abertas. |
| `/treino loja` | Abre a loja durante BedWars. Também abre clicando na bancada. |
| `/treino sair` | Sai da sala e volta ao lobby. |

Temas: `pokemon`, `evangelion`, `sonic`, `onepiece`. O tema é opcional no comando; sem ele, usa Pokémon. O menu sempre oferece os quatro. Veja a [galeria](GALERIA.md).

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

Pare com `stop`, atualize os arquivos do repositório, abra `bash menu.sh`, escolha **2** para atualizar e **3** para ligar. Suas credenciais Playit ficam locais e não entram no GitHub. Não use `/reload` para atualizar o plugin.
