# Citizens e arenas: atualização

Execute `git pull --ff-only` e `bash atualizar.sh` no Codespace. O script não inicia o Minecraft.

O pacote do repositório contém 46 mundos (181 arquivos de região), verificados por CRC do ZIP. O atualizador importa os mundos ausentes antes da ativação e também permite reparar mundos completamente ausentes de um BWFE já ativado. Não sobrescreve pastas de mundos existentes. Se houver pastas incompletas, o relatório impede a instalação silenciosa.

Citizens 2.0.35 build 3478: além do download direto e ZIP oficial, o atualizador usa um artefato do GitHub Actions. A execução https://github.com/silasquercloud/ServeeDetreino/actions/runs/41038941563 baixou o JAR oficial com sucesso e verificou versão, integridade ZIP e presença do adaptador v1_8_R3. Isso ainda não comprova NPCs funcionando em jogo.

O bootstrap instala GitHub CLI (`gh`) se ausente. No Codespace, ele usa a autenticação já disponível. Se faltar acesso ao artefato, a mensagem solicita verificar `gh auth status`; nenhuma credencial é armazenada pelo instalador. O artefato dura 90 dias; pode ser renovado na aba Actions, workflow “Obter Citizens 3478”, Run workflow.

O ZIP original BWFE ainda precisa estar em `imports/` se não foi importado. Ele não é transferido do chat para o Codespace. O lobby vem desse template. As 46 arenas do repositório são construções originais geradas, não os mapas oficiais Mush/Hypixel.
