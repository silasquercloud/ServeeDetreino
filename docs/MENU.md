# Menu de instalação e controle

[← Início](../README.md) · [Guia completo](GUIA.md) · [Playit](PLAYIT.md) · [Galeria](GALERIA.md)

Abra o terminal dentro do repositório e execute:

```bash
bash menu.sh
```

Se estiver com uma cópia antiga, primeiro use `git pull`. O comando antigo `bash scripts/setup.sh` também abre o mesmo menu.

## As opções

| Opção | Como funciona |
| --- | --- |
| **1 — Configurar Playit** | Instala o agente oficial, apresenta o link para vincular sua conta e cadastra host/porta do túnel. |
| **2 — Instalar / atualizar servidor** | Exige etapa 1 concluída; compila Spigot 1.8.8 e TreinoHub, instala o plugin completo e configura `server.properties`. |
| **3 — Ligar e mostrar IP/porta** | Exige as etapas anteriores; pede aceite da EULA na primeira vez, inicia Playit + Minecraft e mostra os dados da conexão. |
| **4 — Ver endereço salvo** | Exibe o host, a porta e o endereço cadastrado sem iniciar os processos. |
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
