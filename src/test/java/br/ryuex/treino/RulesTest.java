package br.ryuex.treino;
public final class RulesTest {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(Rules.validCapacity("SKYWARS",6),"SkyWars com seis amigos deve ser permitido");
        check(Rules.validCapacity("SKYWARS",12),"SkyWars limite superior");
        check(!Rules.validCapacity("SKYWARS",13),"SkyWars não pode exceder 12");
        check(!Rules.validCapacity("BEDWARS",9),"BedWars não pode exceder 8 bases");
        check(!Rules.validCapacity("BEDFIGHT",3),"BedFight não pode deixar um jogador sem par");
        check(!Rules.canStart("BEDFIGHT",3),"Saída de jogador não deve iniciar duelo com número ímpar");
        check(!Rules.canStart("SKYWARS",1),"Não iniciar sala vazia ou com um jogador");
        check(Rules.canStart("SKYWARS",2),"Dono pode antecipar início com dois jogadores");
        for(int team=0;team<8;team++) {
            check(Rules.opponent(Rules.opponent(team))==team,"Oponente deve ser recíproco");
            check(Rules.sameDuel(team,Rules.opponent(team)),"Par deve compartilhar duelo");
            check(!Rules.sameDuel(team,(team+2)%8),"Não permitir dano entre duelos diferentes");
        }
        check(!Rules.validCapacity("OUTRO",2),"Modo desconhecido rejeitado");
        check(Rules.validCapacity("BEDWARS",4,2),"duplas de BedWars com 4 vagas");
        check(!Rules.validCapacity("BEDWARS",2,2),"duplas precisam de dois times completos");
        check(!Rules.validCapacity("BEDFIGHT",6,2),"BedFight 2v2 não deixa um time sem adversário");
        check(Rules.canStart("BEDFIGHT",8,2),"dois duelos 2v2 completos");
        check(Rules.team(0,2)==Rules.team(1,2) && Rules.team(1,2)!=Rules.team(2,2),"parceiros na mesma base");
        System.out.println("RulesTest: OK — vagas, início e isolamento de duelos");
    }
}
