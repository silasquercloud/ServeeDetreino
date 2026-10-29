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
        System.out.println("RulesTest: OK — vagas, início e isolamento de duelos");
    }
}
