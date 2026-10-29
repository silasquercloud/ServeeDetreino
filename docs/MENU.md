# Menu de instalação e controle

[← Início](../README.md) · [Guia completo](GUIA.md) · [Playit](PLAYIT.md) · [Galeria](GALERIA.md)

Abra o terminal dentro do repositório e execute:

```bash
bash menu.sh
```

Se estiver com uma cópia antiga, primeiro use `git pull`. O comando antigo `bash scripts/setup.sh` também abre o mesmo menu.

## Dependências automáticas antes do menu

`menu.sh` começa em Bash, verifica o Python e usa o gerenciador de pacotes para instalar o que estiver faltando **antes** de chamar `menu.py`. Depois prepara o Java correto e baixa o Playit oficial. A entrada alternativa `scripts/setup.sh` usa exatamente o mesmo caminho.

Instalar, iniciar e atualizar também verificam/carregam esse ambiente. No Codespace existente, basta `git pull` e `bash menu.sh`; não é necessário reconstruir o container para corrigir o erro da falta de Python. Em um Codespace novo, o `postCreateCommand` já executa essa preparação.

| Dependência | Como é preparada |
| --- | --- |
| Python 3, venv, pip, Pillow | Pacotes da distribuição. |
| Git, curl, CA, tar/gzip, unzip/zip e bibliotecas/fontes | Pacotes da distribuição. |
| JDK 17 para compilação | Usa um JDK existente ou instala Temurin 17.0.21+9 com hash verificado. |
| JDK 8 para Spigot/BuildTools | Usa um JDK existente ou instala Temurin 8u504-b01 com hash verificado. |
| Playit agente e CLI | Release 1.1.0 oficial, com SHA-256 fixo. |
| Maven usado pelo BuildTools | Baixado pelo próprio BuildTools durante a compilação do servidor. |

Os JDKs baixados ficam em `.toolchains/`, ignorada pelo Git, e as variáveis de Java só são aplicadas aos processos do projeto. O bootstrap não vincula automaticamente uma conta Playit nem aceita a EULA; essas etapas continuam nas opções 1 e 3.

Para preparar/verificar apenas as dependências, sem abrir o menu:

```bash
bash scripts/bootstrap.sh
```

## As opções

| Opção | Como funciona |
| --- | --- |
| **1 — Configurar Playit** | Instala o agente oficial, apresenta o link para vincular sua conta e cadastra host/porta do túnel. |
| **2 — Instalar servidor completo** | Exige etapa 1 concluída; compila Spigot 1.8.8 e TreinoHub, instala o plugin completo e configura `server.properties`. |
| **3 — Ligar e mostrar IP/porta** | Exige as etapas anteriores; pede aceite da EULA na primeira vez, inicia Playit + Minecraft e mostra os dados da conexão. |
| **4 — Ver endereço salvo** | Exibe o host, a porta e o endereço cadastrado sem iniciar os processos. |
| **5 — Atualizar servidor** | Com o servidor parado, busca o código por fast-forward, compila/testa e substitui o plugin com backup; preserva configurações e Playit. |
| **6 — Parar com segurança** | Envia `stop` ao launcher atual, espera salvar e encerra Playit; pode ser usado em outro terminal. |
| **0 — Sair** | Encerra o menu. |

O estado de cada etapa aparece na tela. Uma falha não é marcada como conclusão; o erro é mostrado e você pode tentar de novo. Usar `stop` no console Minecraft encerra os dois processos e retorna ao menu.

## Exemplo de endereço exibido

Os valores abaixo são apenas ilustrativos:

```text
IP / host público: exemplo.ply.gg
Porta pública:    31123
Cole no Minecraft: exemplo.ply.gg:31123
Destino local:    127.0.0.1:25565
```

**A porta pública pode ser diferente da local.** Copie a do túnel Playit. O campo `server-port=25565` é o destino local e não informa a porta que seus amigos devem usar na internet.

Se você cadastrar um hostname automático como `exemplo.joinmc.link` sem informar porta, a tela mostra **automática via endereço/SRV (não informada)**. Nesse caso, copie apenas o hostname. O menu não resolve nem verifica registros DNS/SRV e não presume que a porta pública seja 25565.

A opção 4 exibe dados salvos; ela não testa se o servidor está online. Na opção 3, espere `Done` no console para tentar conectar e confirme o túnel com um amigo de outra rede.

## Original e não original

O arquivo padrão e a etapa de instalação já usam:

```properties
online-mode=false
white-list=false
server-port=25565
server-ip=
```

Isso desliga a verificação de conta nos servidores Microsoft/Mojang e permite conectar clientes Java 1.8.9 originais e não autenticados. A whitelist fica desligada para entrada direta.

**Os nicks não são autenticados nesse modo.** Não há plugin de senha ou autenticação adicional instalado, e skins de contas originais não são garantidas. Evite dar permissões de administrador a um nick que não esteja protegido por autenticação.

Ao instalar/atualizar, o menu faz backup de um `server.properties` existente antes de aplicar essas quatro propriedades. As demais configurações são preservadas. A etapa de início verifica `online-mode`, IP de bind e porta para evitar iniciar com uma configuração antiga diferente da solicitada.

Se quiser ativar whitelist depois, você pode fazê-lo; a inicialização preserva essa alteração. A opção 2 volta a aplicar o padrão de whitelist desligada quando você reinstala/atualiza.

## EULA e compatibilidade

O menu não aceita a EULA sozinho. Na primeira inicialização, exibe o link oficial e só passa o aceite ao launcher se você digitar **ACEITO**. Recusar mantém o servidor parado.

Alvo: **Linux x64, Spigot 1.8.8, Java 8 e clientes Minecraft Java 1.8.9**. Java 17 é usado para compilar o plugin. O Playit portátil também tem binário ARM64, mas o conjunto Java/Spigot ARM64 não foi validado.

Testes automatizados cobrem ordem das etapas, recusa da EULA, migração com backup, preservação de propriedades e distinção entre porta pública/local. A vinculação da sua conta e uma partida real com jogadores continuam dependendo do teste no seu ambiente.

## Atualizar pela opção 5

1. Se o servidor estiver ligado, digite `stop` no console e aguarde voltar ao menu.
2. Escolha **5 — Atualizar servidor**.
3. Aguarde a atualização do GitHub, os testes e a compilação.
4. Escolha **3** para iniciar o plugin novo.

Também funciona por `bash scripts/update.sh`. O atualizador mantém **Spigot 1.8.8**, troca os JARs TreinoHub e TreinoAntiLag e inclui as mudanças de mapas e comandos. Não muda automaticamente a versão do Minecraft.

Playit, `server.properties`, EULA, mundos e configurações dos plugins são preservados. A versão anterior do JAR fica em `server/backups/`. Esta opção difere da instalação da opção 2, que reaplica as propriedades padrão do projeto.

O atualizador bloqueia atualização enquanto o launcher estiver ativo e detecta o agente das versões anteriores. Faz `git pull --ff-only`; se houver código modificado localmente ou histórico divergente, informa o problema sem `reset`, sem apagar alterações e sem fazer stash automático. Uma compilação que falha mantém o plugin instalado; o código fonte pode já ter recebido o fast-forward.

Parties e convites são temporários e precisam ser recriados depois que o servidor reiniciar.

A parada de outro terminal funciona nas sessões iniciadas após esta atualização. Se o servidor já estava ligado com a versão antiga, use `stop` no console original nesta primeira vez. [BedWars, loja, classificação e detalhes da parada →](BEDWARS.md)

## Recuperar uma atualização que falhou

Se o menu ainda estiver aberto na versão antiga, feche-o com **0**. Nesta primeira recuperação, digite `stop` no console onde o Minecraft está rodando e aguarde encerrar. Depois execute, dentro do repositório:

```bash
git pull --ff-only
bash scripts/update.sh
```

Ao concluir, abra `bash menu.sh` e escolha **3**. A atualização agora recarrega o atualizador e o ambiente após buscar o código, repara JARs ausentes, tolera um socket Playit antigo sem serviço respondendo e mostra a versão e o SHA-256 dos plugins instalados. Nas sessões com o launcher novo, solicita parada segura antes de atualizar; servidores antigos ainda exigem `stop` no console original. Código local modificado ou histórico divergente continua protegido, sem reset automático. Se houver erro, envie a mensagem completa do terminal.
