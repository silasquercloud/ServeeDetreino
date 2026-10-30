import br.ryuex.modes.ModeRules;
public class ModeRulesTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check(ModeRules.minimum(false,2)==4);check(ModeRules.minimum(false,1)==2);check(ModeRules.minimum(true,1)==2);
        check(ModeRules.goalOwner(-36,63,0)==0);check(ModeRules.goalOwner(36,60,0)==1);
        check(ModeRules.goalOwner(36,65,0)==-1);check(ModeRules.goalOwner(36,63,2)==-1);check(ModeRules.goalOwner(35,63,-1)==1);check(ModeRules.goalOwner(38,63,0)==-1);
        check(!ModeRules.canFight(false,false,90,true));check(!ModeRules.canFight(true,true,59,true));check(ModeRules.canFight(true,true,60,true));check(!ModeRules.canFight(true,true,60,false));check(ModeRules.canFight(true,false,0,true));
        System.out.println("14 mode-rule checks passed");
    }
}
