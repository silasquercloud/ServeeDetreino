# 46 arenas originais para BWFE 25.1.0

Mundos Anvil 1.8, gerados a partir das posições das configurações fornecidas pelo usuário. Não são os mapas originais da cxSTUDIOS/Hypixel/Mush: os nomes de pasta são preservados somente para compatibilidade com as arenas cadastradas.

Seis paletas: Jardins, Deserto, Glacial, Templo, Bosque e Quartzo. A geometria varia conforme as posições e quantidade de equipes do template, mas as arenas compartilham componentes de construção. Bases abertas, dois toldos por base, camas com suporte, ilhas de diamante e esmeralda e acessos internos entre os geradores centrais. Pontes entre bases e recursos devem ser construídas pelos jogadores.

Depois de importar o BWFE pela opção 7 do menu:

```bash
python3 scripts/template.py maps maps/arenas-bwfe-original.zip
python3 scripts/template.py check
```

Não repete a importação sobre mapas existentes. Nenhum plugin antigo é necessário para gerar ou carregar esses mundos. O ZIP contém exclusivamente mapas originais e manifesto, sem plugins de terceiros.

SHA-256 do ZIP: `8dfd4c618b45b60c2ffe3ff0f32c439953181a22b1b6c648564ab3da575a3df8`

Verificação: 46 level.dat, 2504 chunks, offsets e compressão de regiões; validação de suporte/headroom de spawns e NPCs e presença das camas. O relatório de importação passou a indicar zero mapas ausentes. Ainda falta validar luz, navegação, remoção da sala de espera e partidas no cliente. As regiões pedem recálculo de iluminação pelo servidor. Os mapas são uma primeira versão jogável estrutural, não uma garantia de acabamento ou balanceamento equivalente a uma rede comercial.

Para regenerar: Python 3 + PyYAML 6.0.3; `python3 scripts/generate-bwfe-arenas.py --server template-server --output generated-arenas`. O destino deve estar ausente para preservar mapas existentes. As configurações de origem não são redistribuídas; o manifesto registra seus hashes.
