package br.ryuex.treino;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

/** Calls real event handlers against an in-memory Bukkit world (no network/server). */
public final class BedwarsEventsTest {
    interface Call {Object call(Method method,Object[] args) throws Throwable;}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Call call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args)->{
        if(method.getName().equals("equals"))return object==args[0];
        if(method.getName().equals("hashCode"))return System.identityHashCode(object);
        if(method.getName().equals("toString"))return type.getSimpleName();
        Object result=call.call(method,args==null?new Object[0]:args);if(result!=null)return result;
        Class<?> r=method.getReturnType();if(r==boolean.class)return false;if(r==int.class)return 0;if(r==long.class)return 0L;if(r==double.class)return 0d;if(r==float.class)return 0f;if(r==byte.class)return (byte)0;return null;
    });}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static class MemoryWorld {
        final Map<String,Material> materials=new HashMap<>();final World world;
        MemoryWorld(String name){world=proxy(World.class,(method,args)->{
            if(method.getName().equals("getName"))return name;
            if(method.getName().equals("getBlockAt"))return block((int)args[0],(int)args[1],(int)args[2]);
            if(method.getName().equals("getEntities") || method.getName().equals("getNearbyEntities"))return Collections.emptyList();return null;
        });}
        Block block(int x,int y,int z){String key=x+":"+y+":"+z;return proxy(Block.class,(method,args)->{
            switch(method.getName()){
                case "getX":return x;case "getY":return y;case "getZ":return z;
                case "getWorld":return world;case "getLocation":return new Location(world,x,y,z);
                case "getType":return materials.getOrDefault(key,Material.AIR);
                case "setType":materials.put(key,(Material)args[0]);return null;
                default:return null;
            }
        });}
    }
    @SuppressWarnings({"unchecked","rawtypes"})public static void main(String[] args)throws Exception{
        MemoryWorld arena=new MemoryWorld("treino_room_test"),lobby=new MemoryWorld("lobby");
        Bukkit.setServer(proxy(Server.class,(method,values)->{
            switch(method.getName()){
                case "getLogger":return Logger.getLogger("test");case "getName":return "MemoryBukkit";case "getVersion":return "1.8.8";case "getBukkitVersion":return "1.8.8";
                case "createWorld":return arena.world;case "getOnlinePlayers":return Collections.emptyList();default:return null;
            }
        }));
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field unsafeField=unsafeType.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        Object unsafe=unsafeField.get(null);Treino plugin=(Treino)unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,Treino.class);
        set(plugin,"lobby",new Location(lobby.world,0,81,0));
        for(String name:new String[]{"membership","rooms","sidebarLines"})set(plugin,name,new LinkedHashMap<>());
        Path temp=Files.createTempDirectory("treino-stats-test");set(plugin,"stats",new TrainingStats(temp.toFile()));
        UUID me=UUID.randomUUID(),enemy=UUID.randomUUID();
        Class<?> roomType=Class.forName("br.ryuex.treino.Treino$Room");Constructor<?> constructor=roomType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object room=constructor.newInstance(plugin,1,Treino.Mode.BEDWARS,2,me,MapArt.Theme.SONIC);
        set(room,"phase",Enum.valueOf((Class<Enum>)Class.forName("br.ryuex.treino.Treino$Phase"),"PLAYING"));
        ((Set<UUID>)field(room,"players")).addAll(Arrays.asList(me,enemy));
        ((Map<UUID,Integer>)field(room,"teams")).put(me,0);((Map<UUID,Integer>)field(room,"teams")).put(enemy,1);
        ((Map<UUID,Object>)field(plugin,"membership")).put(me,room);((Map<Integer,Object>)field(plugin,"rooms")).put(1,room);
        Player player=proxy(Player.class,(method,values)->{
            switch(method.getName()){case "getUniqueId":return me;case "getWorld":return arena.world;case "getName":return "Tester";default:return null;}
        });
        List<Block> beds=(List<Block>)field(room,"bedBlocks");Block own=beds.get(0),target=beds.get(2);
        PlayerInteractEvent left=new PlayerInteractEvent(player,Action.LEFT_CLICK_BLOCK,new ItemStack(Material.WOOD_SWORD),target,BlockFace.UP);
        plugin.interact(left);check(!left.isCancelled(),"left click must reach block damage, or beds never break");
        PlayerInteractEvent right=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,new ItemStack(Material.WOOD_SWORD),target,BlockFace.UP);
        plugin.interact(right);check(right.isCancelled(),"right click must not sleep/explode beds");
        plugin.breakBlock(new BlockBreakEvent(own,player));check(own.getType()==Material.BED_BLOCK,"own bed remains");
        plugin.breakBlock(new BlockBreakEvent(target,player));check(target.getType()==Material.AIR && beds.get(3).getType()==Material.AIR,"both enemy bed halves removed");
        check(Boolean.FALSE.equals(((Map<Integer,Boolean>)field(room,"beds")).get(1)),"enemy respawn disabled");
        check(((TrainingStats)field(plugin,"stats")).get(me,"BEDWARS","beds")==1,"bed statistic counted once");
        plugin.breakBlock(new BlockBreakEvent(target,player));check(((TrainingStats)field(plugin,"stats")).get(me,"BEDWARS","beds")==1,"no repeated bed credit");
        for(String position:arena.materials.keySet())check(Integer.parseInt(position.split(":")[1])!=110,"no waiting platform above arena");
        Block decoration=arena.block(0,80,0),placed=arena.block(5,81,0),glass=arena.block(6,81,0);
        placed.setType(Material.WOOL);glass.setType(Material.STAINED_GLASS);
        ((Set<String>)field(room,"placed")).addAll(Arrays.asList("5:81:0","6:81:0"));
        Entity bomb=proxy(TNTPrimed.class,(method,values)->method.getName().equals("hasMetadata")?true:null);
        List<Block> blast=new ArrayList<>(Arrays.asList(decoration,placed,glass,own));
        plugin.explosion(new EntityExplodeEvent(bomb,new Location(arena.world,5,81,0),blast,1));
        check(blast.size()==1 && blast.get(0).getType()==Material.WOOL,"explosion affects only placed non-resistant blocks");
        check(own.getType()==Material.BED_BLOCK,"TNT cannot destroy a bed");
        Method waiting=roomType.getDeclaredMethod("waiting");waiting.setAccessible(true);
        set(room,"phase",Enum.valueOf((Class<Enum>)Class.forName("br.ryuex.treino.Treino$Phase"),"WAITING"));
        check(((Location)waiting.invoke(room)).getWorld()==lobby.world,"waiting room is in separate lobby world");
        TrainingStats stats=(TrainingStats)field(plugin,"stats");stats.name(me,"Tester");stats.add(me,"BEDWARS","wins");stats.save();
        TrainingStats loaded=new TrainingStats(temp.toFile());check(loaded.get(me,"BEDWARS","wins")==1 && loaded.get(me,"BEDWARS","beds")==1,"stats survive reload");
        stats.toggleFavorite(me,"item:FIREBALL");stats.save();check(!new TrainingStats(temp.toFile()).favorites(me).contains("item:FIREBALL"),"favorites survive reload");
        Files.deleteIfExists(temp.resolve("stats.yml"));Files.delete(temp);
        System.out.println("BedwarsEventsTest: OK — actual handlers: bed clicks/break, TNT protection, separate lobby, persistent stats");
    }
}
