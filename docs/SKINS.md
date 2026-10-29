# Skins e NPCs • 1.5.0

Compatibilidade alvo: **Spigot 1.8.8, pacote v1_8_R3, cliente Java 1.8.9**. Sem plugin de NPC externo. Não habilitar este adaptador em outras versões do servidor.

Bots, vendedores e seletores do lobby são mostrados como jogadores. Os IDs das entidades são preservados para clique e dano. A camada visual intercepta nascimento e metadados, mantém movimento/equipamento e envia o perfil antes do personagem. Entradas de TAB ficam identificadas como NPC e são removidas ao descarregar a entidade. Não representam jogadores conectados.

Configure em `plugins/TreinoHub/config.yml` e reinicie:

```yaml
skins:
  bot: Technoblade
  shop: Notch
  upgrades: jeb_
```

São nomes de perfis públicos usados apenas como referência de textura; não há vínculo com seus titulares. A consulta oficial Mojang é assíncrona, com timeout e uma consulta por nome em cada execução. A textura assinada é compartilhada entre personagens. Enquanto carrega, ou se a API estiver indisponível, aparece o modelo padrão Steve/Alex. Clientes também precisam conseguir baixar as texturas da Mojang. Offline-mode não desativa esse mecanismo.

Ao chegar uma textura, a entidade é reapresentada com o equipamento. A implementação preserva entidades internas de mob para física e eventos; sons e dimensões de colisão ainda podem diferir de jogadores. Bots não são contas reais nem têm toda a lógica de um jogador humano.

## Verificação

Compilação com API 1.8.8 e bytecode Java 8. Teste adicional `VisualPacketSmoke` executado com o JAR real Spigot 1.8.8: serialização de ADD/REMOVE de perfil, metadados e nascimento humano; canal Netty EmbeddedChannel valida que o perfil chega antes do nascimento e que o pacote de mob é substituído. Esse teste não substitui a inspeção visual no cliente e não confirma disponibilidade da API Mojang no seu host.

Para repetir após compilar, usando um JAR Spigot 1.8.8 local:

```bash
java -cp 'target/classes:target/test-classes:.cache/spigot-api.jar:server/spigot.jar' br.ryuex.treino.VisualPacketSmoke
```

Ajuste apenas o caminho do JAR do servidor se sua instalação usar outro nome. No jogo, confira NPCs depois de reconectar, trocar de mundo e entrar em partida; clique em LOJA/MELHORIAS, confira armadura e ataque dos bots.
