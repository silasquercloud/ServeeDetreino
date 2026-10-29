package br.ryuex.treino;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.inventory.*;

/** Atomic preflight/payment over inventories on the server main thread. */
final class ShopWallet {
    static List<Inventory> unique(List<Inventory> sources){Set<Inventory> seen=Collections.newSetFromMap(new IdentityHashMap<Inventory,Boolean>());List<Inventory> result=new ArrayList<>();for(Inventory source:sources)if(source!=null && seen.add(source))result.add(source);return result;}
    static int count(List<Inventory> sources,Material currency){int total=0;for(Inventory inv:unique(sources))for(ItemStack item:inv.getContents())if(item!=null && item.getType()==currency)total+=item.getAmount();return total;}
    static boolean pay(List<Inventory> sources,Material currency,int cost){
        if(cost<=0 || count(sources,currency)<cost)return false;
        int remaining=cost;
        for(Inventory inv:unique(sources))for(int slot=0;slot<inv.getSize() && remaining>0;slot++){
            ItemStack stack=inv.getItem(slot);if(stack==null || stack.getType()!=currency)continue;int take=Math.min(remaining,stack.getAmount());ItemStack replacement=stack.clone();replacement.setAmount(stack.getAmount()-take);inv.setItem(slot,replacement.getAmount()==0?null:replacement);remaining-=take;
        }return remaining==0;
    }
    static boolean canFit(Inventory inventory,ItemStack item){int needed=item.getAmount();for(ItemStack existing:inventory.getContents()){if(existing==null || existing.getType()==Material.AIR)return true;if(existing.isSimilar(item))needed-=Math.max(0,Math.min(item.getMaxStackSize(),inventory.getMaxStackSize())-existing.getAmount());if(needed<=0)return true;}return false;}
    static void give(PlayerInventory inventory,ItemStack item,int preferred){
        if(preferred>=0 && preferred<9){ItemStack old=inventory.getItem(preferred);if(old==null || old.getType()==Material.AIR){inventory.setItem(preferred,item);return;}
            if(old.isSimilar(item) && old.getAmount()+item.getAmount()<=Math.min(old.getMaxStackSize(),inventory.getMaxStackSize())){ItemStack merged=old.clone();merged.setAmount(old.getAmount()+item.getAmount());inventory.setItem(preferred,merged);return;}}
        inventory.addItem(item);
    }
}
