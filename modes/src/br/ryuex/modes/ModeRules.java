package br.ryuex.modes;
public final class ModeRules {
    private ModeRules() {}
    public static int minimum(boolean hg,int teamSize){return hg?2:teamSize*2;}
    public static int goalOwner(double x,double y,double z){
        if(y>=64||y<=58||z< -1||z>=2)return -1;
        if(x>=-37&&x< -34)return 0;
        if(x>=35&&x<38)return 1;
        return -1;
    }
    public static boolean canFight(boolean running,boolean hg,int seconds,boolean alive){return running&&(!hg||(alive&&seconds>=60));}
}
