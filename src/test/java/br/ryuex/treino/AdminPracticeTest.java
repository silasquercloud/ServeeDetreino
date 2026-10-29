package br.ryuex.treino;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;

final class AdminPracticeTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")public static void main(String[] args)throws Exception{
        BedwarsEventsTest.MemoryWorld map=new BedwarsEventsTest.MemoryWorld("treino_admin_practice_v13");
        Map<UUID,Player> online=new HashMap<>();
        PluginManager manager=BedwarsEventsTest.proxy(PluginManager.class,(m,a)->null);
        BukkitScheduler scheduler=BedwarsEventsTest.proxy(BukkitScheduler.class,(m,a)->BedwarsEventsTest.proxy(BukkitTask.class,(task,b)->null));
        ItemFactory factory=BedwarsEventsTest.proxy(ItemFactory.class,(m,a)->m.getName().equals("getItemMeta")?BedwarsEventsTest.metadata():m.getName().equals("asMetaFor")?a[0]:m.getName().equals("isApplicable")?true:null);
        Bukkit.setServer(BedwarsEventsTest.proxy(Server.class,(m,a)->{
            switch(m.getName()){case "getLogger":return Logger.getLogger("practice-test");case "getName":return "MemoryBukkit";case "getVersion":case "getBukkitVersion":return "1.8.8";case "getPluginManager":return manager;case "getScheduler":return scheduler;case "getItemFactory":return factory;case "getPlayer":return online.get(a[0]);default:return null;}
        }));
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field f=unsafeType.getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
        JavaPlugin plugin=(JavaPlugin)unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,Treino.class);
        AdminPractice practice=new AdminPractice(plugin,name->map.world,p->{});
        UUID id=UUID.randomUUID();boolean[] permission={false};Location[] location={new Location(map.world,0.5,81,-1.5)};
        PlayerInventory inventory=BedwarsEventsTest.proxy(PlayerInventory.class,(m,a)->null);
        Player p=BedwarsEventsTest.proxy(Player.class,(m,a)->{
            switch(m.getName()){case "getUniqueId":return id;case "hasPermission":return permission[0];case "getName":return "Salabim_zz";case "getWorld":return location[0].getWorld();case "getLocation":return location[0];case "getInventory":return inventory;case "getMaxHealth":return 20d;case "teleport":location[0]=(Location)a[0];return true;default:return null;}
        });online.put(id,p);
        try{practice.open(p,true);throw new AssertionError("non-admin admitted");}catch(IllegalArgumentException expected){}
        check(!practice.active(p),"permission checked before creating a session");
        permission[0]=true;practice.open(p,true);check(practice.active(p) && practice.cpsEnabled(p),"admin enters lane with meter");
        Map<UUID,AdminPractice.Session> sessions=(Map<UUID,AdminPractice.Session>)BedwarsEventsTest.field(practice,"sessions");AdminPractice.Session session=sessions.get(id);session.assistUntil=Long.MAX_VALUE;
        Method tick=AdminPractice.class.getDeclaredMethod("tick");tick.setAccessible(true);
        for(int i=0;i<40;i++)tick.invoke(practice);
        check(session.placed.size()==31 && session.cursor==32,"assistance stops at fixed lane end");
        check(map.block(0,80,32).getType()==Material.GOLD_BLOCK,"finish platform preserved");
        check(map.block(20,80,1).getType()==Material.AIR,"assistance never reaches a different lane");
        org.bukkit.block.Block trial=session.placed.iterator().next();org.bukkit.event.block.BlockBreakEvent broken=new org.bukkit.event.block.BlockBreakEvent(trial,p);practice.breakBlock(broken);
        check(broken.isCancelled() && trial.getType()==Material.AIR && session.placed.size()==30,"trial blocks removed without vanilla item drops");
        org.bukkit.event.block.BlockBreakEvent platform=new org.bukkit.event.block.BlockBreakEvent(map.block(0,80,0),p);practice.breakBlock(platform);
        check(platform.isCancelled() && map.block(0,80,0).getType()!=Material.AIR,"original platform protected");
        location[0]=new Location(map.world,0.5,81,32.5);session.started=AdminPractice.now()-2500;tick.invoke(practice);
        check(session.completed==1 && session.best>=2500 && session.placed.isEmpty(),"finish records best and clears only trial blocks");
        session.assistUntil=Long.MAX_VALUE;tick.invoke(practice);location[0]=new Location(map.world,0.5,30,5);tick.invoke(practice);
        check(location[0].getY()==81 && session.placed.isEmpty(),"fall resets player and generated bridge");
        practice.close(p,false);check(!practice.active(p),"closing releases session");
        System.out.println("AdminPracticeTest: OK — permissions, bounded assistance, timer, finish/fall reset, lane isolation");
    }
}
