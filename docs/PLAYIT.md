# Playit antes de instalar o servidor

[← Início](../README.md) · [Guia completo](GUIA.md)

O Playit cria um túnel entre um endereço público e o Minecraft na sua máquina. Ele **não é a máquina que hospeda o jogo**. Esta integração usa o agente **1.1.0** e seu CLI, com hashes fixos do release oficial, como processos locais sem sudo/systemd.

## Setup guiado

```bash
bash menu.sh
```

1. No menu, escolha **1 — Configurar Playit**. Aguarde o download e a verificação dos dois binários oficiais.
2. O terminal mostrará um link de `playit.gg/claim/...`. Abra-o, entre na sua conta Playit e vincule esse agente. Não precisa colar senha ou token no terminal.
3. Volte ao terminal. O assistente aguarda a aprovação e a ativação do agente.
4. No [painel de túneis](https://playit.gg/account/tunnels), crie um túnel com os dados abaixo.
5. Copie o endereço público e cole quando o assistente solicitar. Se ele não trouxer porta, informe a porta pública exibida no painel, ou pressione Enter para manter a indicação automática/DNS-SRV.
6. O agente temporário será encerrado e você volta ao menu. Escolha **2** para instalar o servidor e **3** para ligar.

| Campo | Valor |
| --- | --- |
| Agente | O que você acabou de vincular nesta máquina. |
| Tunnel Type | **Minecraft Java** (TCP). |
| Local Address / IP | `127.0.0.1` |
| Local Port | `25565` |
| Endereço compartilhado com os amigos | O hostname/endereço público fornecido pelo painel, com porta se indicada. |

Não use Minecraft Bedrock/UDP. As opções de plano/região dependem do painel da sua conta; não escolha uma opção paga sem querer. Não há contratação automática neste projeto.

**Antes de instalar/iniciar o Minecraft, é normal o destino local estar offline.** A etapa inicial confirma a vinculação/ativação do agente e registra o endereço informado por você. Ela não comprova que o túnel público já aceita uma conexão de jogo.

## Iniciar automaticamente nas próximas sessões

```bash
bash scripts/start.sh
```

O launcher inicia o agente, espera `Phase: running` e depois inicia o servidor. O host, a porta pública cadastrada e o endereço completo aparecem no terminal. A porta local 25565 é identificada separadamente. Ao digitar `stop` no console Minecraft, ele encerra os dois processos. Se o agente encerrar inesperadamente, o launcher para o servidor e informa o erro.

O agente tenta manter sua própria conexão com o Playit. A integração não promete uptime, IP disponível antes de criar o túnel ou funcionamento sem a máquina ligada.

## Refazer apenas a configuração

Pare o servidor e execute:

```bash
python3 scripts/playit.py setup
```

A credencial existente é reaproveitada. Você pode atualizar o endereço cadastrado se mudar o túnel. Para trocar de conta/agente, use o painel e as ferramentas oficiais; não há exclusão automática da credencial existente.

## Onde ficam os dados locais

| Local | Uso |
| --- | --- |
| `.tools/` | Binários do agente e CLI, validados por SHA-256. |
| `.private/playit.secret` | Credencial criada pelo Playit; nunca envie para o GitHub ou amigos. |
| `.private/playit-ready.json` | Endereço público informado e destino local esperado. |
| `.private/playit.log` | Log local para diagnóstico; pode conter dados de conta, não publique integralmente. |

As duas pastas são ignoradas pelo Git. A pasta privada usa permissão 700; os arquivos de credencial/configuração usam 600. Um novo Codespace/outra máquina pode exigir outra vinculação. Não há cópia automática de credenciais entre máquinas.

## Se algo falhar

| Sintoma | Verificação |
| --- | --- |
| “Configure o Playit ANTES…” | Abra `bash menu.sh`, escolha **1** e termine a vinculação e o cadastro do túnel. |
| “invalid secret” | Revise o agente no painel; não compartilhe a chave para tentar resolver. |
| “disabled over limit” | Confira o limite de agentes da sua conta no painel oficial. |
| Agente ativo, mas Minecraft não conecta | Aguarde `Done`, confirme TCP/Minecraft Java, agente correto e destino `127.0.0.1:25565`. |
| Endereço desconhecido | Copie exatamente o hostname e a porta do painel; não use `https://` nem URL `app.github.dev`. |
| “You are not whitelisted” | No console: `whitelist add NICK` se você tiver ativado a whitelist; o padrão atual é desligado. |
| Agente já em execução | Pare a execução anterior do launcher antes de iniciar outra. |
| Falha de download/conexão | Confira acesso ao GitHub, ao serviço Playit e a política de rede da máquina usada. Não desative TLS. |

Em Codespaces, use o projeto para desenvolvimento/testes; Playit não transforma o serviço em uma host permanente. A vinculação da sua conta e uma conexão pública real **ainda precisam ser verificadas por você**.

## Fontes oficiais usadas

- [Release 1.1.0 e hashes](https://github.com/playit-cloud/playit-agent/releases/tag/v1.1.0)
- [Agente e CLI oficiais](https://github.com/playit-cloud/playit-agent)
- [Instalação Linux](https://playit.gg/support/run-on-linux/)
