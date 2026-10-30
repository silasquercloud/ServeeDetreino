# Atualização automática para o template

Com o ZIP BWFE original dentro de `imports/` no Codespace, execute na raiz:

```bash
git pull --ff-only
bash atualizar.sh
```

O anexo enviado ao chat não é transferido automaticamente para o Codespace. Só é necessário enviar o ZIP uma vez; uma importação anterior em `template-server/` é reutilizada.

O comando busca alterações sem reset, prepara dependências do ambiente, encerra com segurança o launcher gerenciado, verifica Java 17, importa mapas ausentes, tenta baixar Citizens 2.0.35 build 3478, Parties 3.2.14 e LastLoginAPI 1.7.4 dos distribuidores oficiais. São versões fixadas para esta migração, não necessariamente as mais recentes. Valida conteúdo dos JARs, versão de bytecode, adaptador Citizens 1.8_R3, plugins duplicados, dependências obrigatórias, mapas, lobby, NMS do servidor (ou hash do launcher PandaSpigot 1.8.8 original do template) e configuração Playit. Não inicia o Minecraft.

Se um download falhar, informa a pendência. Alternativamente coloque os três JARs nas versões indicadas em `imports/plugins/` e repita. Não existe bypass de autenticação ou download de fontes não oficiais.

Quando os requisitos passam, move `server/` para um backup datado dentro de `.private/` e promove `template-server/` para `server/`. Preserva credenciais Playit e não mistura TreinoHub/TreinoAntiLag com BWFE. O launcher seleciona Java 17 para BWFE. Depois use `bash menu.sh`, opção 3; a EULA é tratada no início como antes. A configuração de arquivos não substitui um teste real dos plugins e partidas.

A opção 5 usa o novo comando. O instalador legado recusa sobrescrever um BWFE já ativo.

---

# Reconstrução com BWFE 25.1.0

O arquivo fornecido pelo usuário foi inspecionado. Contém `Server Files.zip`, lobby `spawn`, servidor e plugins/configurações. O núcleo é **BedWars2023 1.3.0**, não o TreinoHub anterior. O README do autor exige servidor **1.8.8 com NMS** e **Java 17 ou superior**. O funcionamento de todas as dependências ainda não foi validado em execução.

## Mapas originais disponíveis

Foram gerados 46 mundos originais compatíveis com as posições cadastradas. Use `python3 scripts/template.py maps maps/arenas-bwfe-original.zip`. Consulte [mapas e validação](../maps/README.md). Na instalação de preparação, o relatório agora mostra **zero mapas ausentes**. Os nomes correspondem às configurações, não a cópias das construções originais.

## Auditoria do ZIP original e bloqueios restantes

- 46 arquivos de configuração de arenas, mas nenhum dos 46 mundos de partida acompanha o ZIP.
- O autor indica os mundos em https://cxstudios.in/download/bwfe-arenas — a tentativa de acesso retornou HTTP 503.
- JARs ausentes: Citizens, Parties e LastLoginAPI. BetterSocial já acompanha o pacote, embora também seja listado como dependência no README.
- Citizens indicado pelo autor: https://ci.citizensnpcs.co/job/citizens2/3478/ . Não substituir automaticamente pela versão mais recente, pois a base é 1.8.8.
- BedFight, SkyWars e bots não foram encontrados como modos prontos nesse template. Não foram misturados com os plugins antigos.

## Importar no Codespace

Envie o ZIP original para a pasta `imports/` do Codespace. Não faça commit desse pacote: contém assets e plugins de terceiros, cuja redistribuição pública não foi estabelecida.

```bash
bash menu.sh
```

Escolha **7** e informe o caminho do ZIP. Também funciona diretamente:

```bash
python3 scripts/template.py import 'imports/BWFE (25.1.0).zip'
python3 scripts/template.py check
```

A importação extrai em `template-server/`, preserva `server/` e suas configurações Playit, valida caminhos e recusa sobrescrever uma importação existente. Ajusta a porta para 25565, desliga transporte nativo, define 32 vagas e distância de visão 6. Mantém a EULA pendente e não inicia o servidor automaticamente. O template já usa `online-mode=false`.

Quando o ZIP oficial das arenas estiver disponível:

```bash
python3 scripts/template.py maps 'imports/arenas.zip'
python3 scripts/template.py check
```

O importador procura mundos com nomes correspondentes às configurações e exige dados de região. Ele recusa colisões de arquivos. O relatório lista mapas e dependências ausentes, sem dizer que o servidor está pronto por apenas existir um JAR.

**A ativação é feita pelo novo `atualizar.sh` somente após as verificações.** Não copie os JARs de TreinoHub para o template. Depois de receber mapas/dependências, validar inicialização, NPCs, lojas e uma partida antes de substituir `server/`.

O server.jar fornecido no ZIP é **PandaSpigot Paperclip 1.8.8**. Seu hash foi conferido; ele prepara o servidor final no primeiro início e pode precisar baixar o JAR original da Mojang. Não foi trocado silenciosamente por Spigot.

### Download alternativo do Citizens

O atualizador tenta o JAR individual e, se falhar, o ZIP oficial dos artefatos do mesmo build 3478. Extrai apenas `Citizens-2.0.35-b3478.jar` e valida o plugin e o adaptador `v1_8_R3` antes de instalar. Se o acesso ao site oficial estiver bloqueado por completo, ambos podem falhar: nesse caso coloque o JAR em `imports/plugins/` e execute novamente. O ZIP é uma alternativa de transporte, não outra versão do plugin.
