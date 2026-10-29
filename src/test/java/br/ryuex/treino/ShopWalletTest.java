package br.ryuex.treino;
import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
final class ShopWalletTest {
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    static Inventory inventory(ItemStack[] stacks){return BedwarsEventsTest.proxy(Inventory.class,(m,a)->{
        switch(m.getName()){case "getContents":return stacks;case "getSize":return stacks.length;case "getItem":return stacks[(int)a[0]];case "setItem":stacks[(int)a[0]]=(ItemStack)a[1];return null;case "getMaxStackSize":return 64;default:return null;}
    });}
    public static void main(String[] args){
        ItemStack[] a={new ItemStack(Material.GOLD_INGOT,2),null},b={new ItemStack(Material.GOLD_INGOT,3),null};Inventory hand=inventory(a),chest=inventory(b);
        List<Inventory> sources=Arrays.asList(hand,chest,chest);
        check(ShopWallet.count(sources,Material.GOLD_INGOT)==5,"same chest is never counted twice");
        check(!ShopWallet.pay(sources,Material.GOLD_INGOT,6) && a[0].getAmount()==2 && b[0].getAmount()==3,"failed payment leaves all sources untouched");
        check(ShopWallet.pay(sources,Material.GOLD_INGOT,4) && a[0]==null && b[0].getAmount()==1,"payment uses inventory then chest and preserves change");
        ItemStack[] full={new ItemStack(Material.WOOL,64)};check(!ShopWallet.canFit(inventory(full),new ItemStack(Material.IRON_SWORD)),"full inventory refuses non-stackable purchase");
        check(ShopWallet.canFit(hand,new ItemStack(Material.WOOL,16)),"empty slot accepts purchase");
        System.out.println("ShopWalletTest: OK — ordered payments, no duplication, atomic shortage, inventory capacity");
    }
}
