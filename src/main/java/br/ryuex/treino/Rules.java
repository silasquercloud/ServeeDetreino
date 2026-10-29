package br.ryuex.treino;

/** Shared validation used by commands and round countdowns. */
final class Rules {
    static boolean validCapacity(String mode,int capacity) {
        if(capacity<2) return false;
        if("SKYWARS".equals(mode)) return capacity<=12;
        if("BEDWARS".equals(mode)) return capacity<=8;
        return "BEDFIGHT".equals(mode) && capacity<=8 && capacity%2==0;
    }
    static boolean canStart(String mode,int players) {
        return players>=2 && (!"BEDFIGHT".equals(mode) || players%2==0);
    }
    static boolean validCapacity(String mode,int players,int teamSize) {
        return (teamSize==1 || teamSize==2) && validCapacity(mode,players) && players>=2*teamSize && players%teamSize==0 && (!"BEDFIGHT".equals(mode) || players%(2*teamSize)==0);
    }
    static boolean canStart(String mode,int players,int teamSize) {return players>=2*teamSize && players%teamSize==0 && (!"BEDFIGHT".equals(mode) || players%(2*teamSize)==0);}
    static int team(int playerIndex,int teamSize) {return playerIndex/teamSize;}
    static boolean sameDuel(int first,int second) { return first>=0 && second>=0 && first/2==second/2; }
    static int opponent(int team) { return team%2==0?team+1:team-1; }
}
