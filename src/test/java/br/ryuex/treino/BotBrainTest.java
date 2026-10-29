package br.ryuex.treino;
final class BotBrainTest {
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    public static final class Handle {boolean frozen;public void k(boolean value){frozen=value;}public boolean ce(){return frozen;}}
    public static void main(String[] args)throws Exception {
        check(BotBrain.state(16,true,true)==BotBrain.State.COMBAT,"nearby combat interrupts shopping/bed rush");
        check(BotBrain.state(100,true,true)==BotBrain.State.RESUPPLY,"safe resupply before rushing");
        check(BotBrain.state(100,true,false)==BotBrain.State.RUSH_BED,"bed objective when safe");
        check(BotBrain.state(100,false,false)==BotBrain.State.HUNT,"hunt after bed falls");
        check(!BotBrain.canHit(9.1,0,true,20,0) && !BotBrain.canHit(4,0,false,20,0),"reach and line of sight are enforced");
        check(!BotBrain.canHit(4,0,true,19,20) && BotBrain.canHit(4,0,true,20,20),"attack cooldown enforced");
        double[] near=BotBrain.movement(1,0,2,0,0,true),far=BotBrain.movement(1,0,25,0,0,true);
        check(near[0]<0 && far[0]>0,"spacing: retreat when too close, approach when far");
        check(BotBrain.mineTicks("WOOD",false,true,false)>1,"bed defenses take time to break");
        check(!BotBrain.validBridgeCell(66,80,0) && !BotBrain.validBridgeCell(0,59,0) && BotBrain.validBridgeCell(0,80,0),"bridge placements stay inside arena bounds");
        BotBrain.Memory memory=new BotBrain.Memory();memory.observe(9,18,20);check(memory.side==1,"changes strafe after taking damage");memory.observe(9,16,22);check(memory.side==1,"does not oscillate every hit");check(memory.retreat(4,18) && !memory.retreat(12,18),"health disadvantage triggers spacing");
        Handle handle=new Handle();check(Native18.freezeHandle(handle) && handle.frozen,"NoAI adapter verifies the state it set");
        System.out.println("BotBrainTest: OK — combat priority, spacing, reach/cooldown, mining, bridge bounds, NoAI verification");
    }
}
