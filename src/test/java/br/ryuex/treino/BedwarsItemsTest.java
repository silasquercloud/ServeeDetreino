package br.ryuex.treino;
public final class BedwarsItemsTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        check(BedwarsItems.canBreakBed(true,true,false,0,1,false),"cama inimiga pode ser destruída");
        check(!BedwarsItems.canBreakBed(true,true,false,0,0,false),"própria cama protegida");
        check(!BedwarsItems.canBreakBed(false,true,false,0,1,false),"espera protegida");
        check(!BedwarsItems.canBreakBed(true,true,true,0,1,false),"renascendo não quebra cama");
        check(!BedwarsItems.canBreakBed(true,true,false,0,2,true),"BedFight não invade outro duelo");
        for(int category=0;category<8;category++)check(BedwarsItems.category(category).size()>0 && BedwarsItems.category(category).size()<=36,"categorias cabem na loja");
        for(BedwarsItems.Entry e:BedwarsItems.ALL)check(e.cost>0 && e.amount>0 && e.amount<=64,"quantidades válidas");
        check(BedwarsItems.interval("DIAMOND",480,false)<BedwarsItems.interval("DIAMOND",0,false),"diamantes evoluem");
        check(BedwarsItems.interval("IRON_INGOT",0,true)<BedwarsItems.interval("IRON_INGOT",0,false),"forja melhora ferro");
        System.out.println("BedwarsItemsTest: OK — camas, catálogo, evolução e forja");
    }
}
