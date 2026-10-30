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

**A ativação definitiva e a migração do launcher ainda estão pendentes.** As opções 2, 3 e 5 do menu continuam operando a instalação antiga. Não copie os JARs de TreinoHub para o template. Depois de receber mapas/dependências, validar inicialização, NPCs, lojas e uma partida antes de substituir `server/`.
