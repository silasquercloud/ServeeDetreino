package br.ryuex.treino;
final class ClickMeterTest {
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    public static void main(String[] args){
        ClickMeter meter=new ClickMeter();meter.record(false,1000);meter.record(false,1100);meter.record(true,1150);
        check(meter.count(false,1500)==2 && meter.count(true,1500)==1,"separate left and right server actions");
        check(meter.count(false,2000)==1,"old actions expire on the window boundary");
        check(meter.count(true,2150)==0,"right window expires independently");
        for(int i=0;i<10000;i++)meter.record(false,3000);
        check(meter.count(false,3000)==256,"bounded storage under repeated packets");
        check(meter.count(false,4000)==0,"idle meter returns to zero");
        System.out.println("ClickMeterTest: OK — rolling time window, independent buttons, bounded storage");
    }
}
