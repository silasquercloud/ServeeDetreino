# Painel do administrador • 1.4.0

[← Início](../README.md) · [Galeria](GALERIA.md) · [BedWars e BedFight](BEDWARS.md)

**Salabim_zz** continua configurado em `administrators`, recebendo OP ao entrar após instalar e reiniciar. Os comandos abaixo exigem `treino.admin` (padrão: OP). O plugin continua usando **Spigot 1.8.8, Java 8 e cliente Java 1.8.9**, sem mod adicional.

Abra `/treinoadmin` (alias `/tadmin`), `/treino admin` ou o bloco de comando no canto do menu principal.

![Painel do administrador — disposição explicativa](images/admin-tools.png)

## CPS observado

`/cps` ativa/desativa a linha **CPS servidor E / D** na barra lateral do lobby, das salas ou da pista. São contados os movimentos de braço recebidos do cliente e as ações de botão direito, separadamente, numa janela móvel de um segundo. A contagem volta a zero quando você para. O armazenamento é limitado mesmo com muitos eventos.

**Esse número não mede todos os cliques físicos do mouse.** A rede, o cliente e o protocolo podem agrupar ou limitar ações. Segurar o botão direito pode gerar ações repetidas. O comando não aumenta o CPS, não instala autoclick e não altera o cliente.

## Fastplace assistido

`/fastplace` abre uma pista administrativa separada. Com a **vara de blaze**, segure o botão direito para a assistência construir o trajeto central a **10 blocos por segundo de servidor**. Soltar interrompe após o curto prazo de renovação dos eventos; o trajeto termina no bloco 31. A assistência só escreve na faixa reservada dessa sessão, sem alcançar a pista de outro administrador.

O caminho gerado é explícito e controlado pelo servidor. Isso não altera o atraso de colocação do Minecraft do usuário. A ferramenta existe no mundo de teste; ela não é aplicada às partidas BedWars/BedFight/SkyWars.

![Pista após construir com assistência](images/admin-fastplace.png)

## Bridge manual

`/bridge` abre uma pista vazia para construir sua ponte normalmente. Você recebe lã, relógio de reinício e bússola de saída. A lã é reposta no slot 0 após colocar um bloco válido. São aceitos blocos na altura da pista e em até três colunas de largura; o vão tem 31 blocos, entre a saída Z=0 e a chegada Z=32.

O cronômetro começa na primeira colocação. Ao chegar ao outro lado, registra o melhor tempo da sessão e reinicia a pista. Cair no vazio também limpa a ponte e devolve você ao início. O relógio reinicia a tentativa; a bússola ou `/treino admin sair` volta ao lobby. O tempo recorde fica apenas na sessão atual, sem ranking competitivo.

O plugin protege as plataformas originais. Quebrar um bloco que você colocou remove o bloco sem gerar drop. As pistas são reservadas individualmente, com até oito sessões administrativas simultâneas. Ao sair/desconectar, os blocos da tentativa são removidos. O modo não exige outro jogador ou bot.

![Pista de bridge no estado inicial](images/admin-practice.png)

## Menu inspirado na referência enviada

Os cartões de modo e solo/duplas mostram participantes em partida, **top 3 de maior sequência de vitórias do dia** e a instrução de clique. O menu usa os dados locais; nenhum nome ou ranking de outro servidor foi copiado.

Vitórias aumentam a sequência; perder o time ou abandonar uma partida em andamento zera a sequência atual. O maior valor do dia é preservado. Empates não alteram a sequência. Em duplas, a eliminação individual não zera a sequência enquanto o parceiro ainda puder vencer; sair voluntariamente conta como abandono. O dia usa `America/Sao_Paulo`; as estatísticas e recordes diários são persistidos em `stats.yml`. Os rankings anteriores de vitórias totais continuam em `/ranking`. Sequências diárias começam a ser registradas nesta versão; o histórico antigo de vitórias não permite reconstruir sequências passadas.

![Estrutura do tooltip com dados vazios reais](images/menu-lobby.png)

## Geradores e bases

A forja alta foi substituída por um **poço baixo de pedra**, com lajes ao redor e o minério visível no centro, seguindo a referência enviada. Os geradores de diamante/esmeralda usam o mesmo vocabulário visual. Hologramas e recursos são adicionados durante a partida.

As bases BedWars/BedFight têm uma área frontal livre de construções altas; cama, marcação do time e acessos. BedWars tem Villagers na lateral; BedFight começa com defesa baixa de madeira e kit, sem vendedor. A identidade temática aparece no relevo, no piso e num pequeno detalhe ao fundo. As construções temáticas maiores permanecem no centro das arenas; SkyWars mantém sua própria decoração.

![Gerador de esmeralda baixo](images/emerald-generator.png)

## Validação

Testes verificam: borda baixa e núcleo dos geradores; espaço livre ao redor da cama nos quatro temas; permissões do treino; colocação assistida limitada ao trajeto; isolamento entre pistas; reinício ao cair/terminar; contagem em janela de um segundo; persistência do recorde diário. A compilação usa a API real 1.8.8 e bytecode Java 8.

As imagens são projeções técnicas/diagramas de interface. Ainda é necessário conferir no cliente o tooltip, a coleta no gerador, o CPS observado e a construção manual/assistida. Não se alega medição perfeita de cliques nem equivalência ao Mush.
