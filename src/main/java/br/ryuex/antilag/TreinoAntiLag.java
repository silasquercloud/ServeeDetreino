package br.ryuex.antilag;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Bounded cleanup only in ephemeral arena worlds. Never clears inventories or mobs. */
public final class TreinoAntiLag extends JavaPlugin {
    private long last=System.nanoTime(); private double tps=20;
    public void onEnable() {
        getConfig().addDefault("loose-item-lifetime-seconds",120);
        getConfig().options().copyDefaults(true);saveConfig();
        new BukkitRunnable(){public void run(){
            long now=System.nanoTime();tps=Math.min(20,1000000000.0/(now-last));last=now;
        }}.runTaskTimer(this,1,1);
        new BukkitRunnable(){public void run(){
            int age=Math.max(30,getConfig().getInt("loose-item-lifetime-seconds",120))*20;
            for(World world:Bukkit.getWorlds())if(world.getName().startsWith("treino_room_"))for(Entity entity:world.getEntities()) {
                if(entity instanceof Item && (entity.getLocation().getY()<40 || entity.getTicksLived()>age))entity.remove();
                else if(entity instanceof Projectile && (entity.getLocation().getY()<40 || entity.getTicksLived()>600))entity.remove();
            }
        }}.runTaskTimer(this,100,100);
        getLogger().info("Proteção ativa: limpeza restrita a itens/projéteis antigos nas arenas, sem remoção global.");
    }
    public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        Runtime runtime=Runtime.getRuntime();int entities=0;
        for(World w:Bukkit.getWorlds())if(w.getName().startsWith("treino_"))entities+=w.getEntities().size();
        sender.sendMessage(String.format(java.util.Locale.ROOT,"§aTreinoAntiLag §7• TPS amostra: %.1f • RAM: %d/%d MB • entidades de treino: %d",tps,(runtime.totalMemory()-runtime.freeMemory())/1048576,runtime.maxMemory()/1048576,entities));
        return true;
    }
}
