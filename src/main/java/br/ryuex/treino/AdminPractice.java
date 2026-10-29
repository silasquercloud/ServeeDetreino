package br.ryuex.treino;

import java.util.*;
import java.util.function.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Admin-only training lanes. Assistance is explicit and limited to a private lane. */
final class AdminPractice implements Listener {
    private final JavaPlugin plugin;private final Function<String,World> worlds;private final Consumer<Player> returnHub;
    private final Map<UUID,Session> sessions=new LinkedHashMap<>();private final Map<UUID,ClickMeter> meters=new HashMap<>();
    private final Set<UUID> cps=new HashSet<>();private World world;
    static final class Session {
        final int x;final boolean assisted;final Set<Block> placed=new HashSet<>();int cursor=1,attempts,completed,placedCount;long started,best,assistUntil;
        Session(int x,boolean assisted){this.x=x;this.assisted=assisted;}
    }
    AdminPractice(JavaPlugin plugin,Function<String,World> worlds,Consumer<Player> returnHub){
        this.plugin=plugin;this.worlds=worlds;this.returnHub=returnHub;
        Bukkit.getPluginManager().registerEvents(this,plugin);
        new BukkitRunnable(){public void run(){tick();}}.runTaskTimer(plugin,2,2);
    }
    static long now(){return System.nanoTime()/1000000;}
    boolean active(Player p){return sessions.containsKey(p.getUniqueId());}
    boolean cpsEnabled(Player p){return cps.contains(p.getUniqueId());}
    String cpsLine(Player p){ClickMeter meter=meters.computeIfAbsent(p.getUniqueId(),id->new ClickMeter());long time=now();return "§dCPS servidor §f"+meter.count(false,time)+" §7/ §f"+meter.count(true,time);}
    void toggleCps(Player p){require(p);if(!cps.remove(p.getUniqueId()))cps.add(p.getUniqueId());p.sendMessage("§dCPS observado: "+(cpsEnabled(p)?"ativado (esquerdo / direito).":"desativado."));}
    private void require(Player p){if(!p.hasPermission("treino.admin"))throw new IllegalArgumentException("Ferramentas exclusivas para administradores.");}
    private ItemStack tool(Material type,String name){ItemStack stack=new ItemStack(type);ItemMeta meta=stack.getItemMeta();meta.setDisplayName(name);stack.setItemMeta(meta);return stack;}
    void open(Player p,boolean assisted){
        require(p);close(p,false);
        if(world==null)world=worlds.apply("treino_admin_practice_v13");
        int slot=0;while(slot<8){final int origin=slot*20;if(sessions.values().stream().noneMatch(s->s.x==origin))break;slot++;}
        if(slot==8)throw new IllegalArgumentException("Todas as 8 pistas de teste estão ocupadas.");
        Session session=new Session(slot*20,assisted);
        for(MapArt.Cell cell:MapArt.practiceArena().cells()){Block b=world.getBlockAt(cell.x+session.x,cell.y+80,cell.z);b.setType(Material.valueOf(cell.material),false);b.setData((byte)cell.data,false);}
        sessions.put(p.getUniqueId(),session);cps.add(p.getUniqueId());p.closeInventory();p.setGameMode(GameMode.SURVIVAL);p.setAllowFlight(false);p.getInventory().clear();p.getInventory().setArmorContents(null);p.setHealth(p.getMaxHealth());p.setFoodLevel(20);p.setFireTicks(0);
        p.getInventory().setItem(0,new ItemStack(Material.WOOL,64,(short)10));p.getInventory().setItem(6,tool(Material.BLAZE_ROD,"§dFastplace assistido • segure o botão direito"));p.getInventory().setItem(7,tool(Material.WATCH,"§eReiniciar tentativa"));p.getInventory().setItem(8,tool(Material.COMPASS,"§cVoltar ao lobby"));
        spawn(p,session);p.sendMessage("§dPista privada: "+(assisted?"FASTPLACE ASSISTIDO":"BRIDGE MANUAL")+". Cruze 32 blocos. Relógio reinicia; bússola sai.");
        if(assisted)p.sendMessage("§7A vara coloca blocos no trajeto fixo: 10 blocos/s. O cliente e seus cliques não são acelerados.");
    }
    private void spawn(Player p,Session s){p.setFallDistance(0);p.setVelocity(new org.bukkit.util.Vector(0,0,0));p.teleport(new Location(world,s.x+0.5,81,-1.5,0,0));}
    private void reset(Player p,Session s){for(Block b:s.placed)b.setType(Material.AIR,false);s.placed.clear();s.cursor=1;s.placedCount=0;s.started=0;s.assistUntil=0;s.attempts++;spawn(p,s);p.getInventory().setItem(0,new ItemStack(Material.WOOL,64,(short)10));}
    void close(Player p,boolean hub){Session s=sessions.remove(p.getUniqueId());if(s!=null)for(Block b:s.placed)b.setType(Material.AIR,false);if(hub)returnHub.accept(p);}
    void shutdown(){for(UUID id:new ArrayList<>(sessions.keySet())){Player p=Bukkit.getPlayer(id);if(p!=null)close(p,false);}sessions.clear();meters.clear();cps.clear();}
    List<String> lines(Player p){Session s=sessions.get(p.getUniqueId());List<String> lines=new ArrayList<>();if(s==null)return lines;
        lines.add("§f"+(s.assisted?"Fastplace assistido":"Bridge manual"));lines.add(cpsLine(p));lines.add("§7Blocos: §f"+s.placedCount+"/31");lines.add("§7Concluídas: §a"+s.completed);lines.add("§7Melhor: §e"+(s.best==0?"—":String.format(Locale.ROOT,"%.2fs",s.best/1000d)));lines.add("§7Relógio: reiniciar");lines.add("§7Bússola: sair");return lines;
    }
    private void tick(){
        for(UUID id:new ArrayList<>(sessions.keySet())){
            Player p=Bukkit.getPlayer(id);Session s=sessions.get(id);if(p==null){sessions.remove(id);for(Block b:s.placed)b.setType(Material.AIR,false);continue;}
            if(p.getWorld()!=world){close(p,false);continue;}if(!p.hasPermission("treino.admin")){close(p,true);continue;}
            Location at=p.getLocation();if(at.getY()<60 || Math.abs(at.getX()-s.x)>7 || at.getZ()< -8 || at.getZ()>42){reset(p,s);continue;}
            if(s.started>0 && at.getZ()>=32 && at.getZ()<=38){long time=now()-s.started;s.completed++;if(s.best==0 || time<s.best)s.best=time;p.sendMessage("§aPista concluída em "+String.format(Locale.ROOT,"%.2fs",time/1000d)+"!");reset(p,s);continue;}
            if(s.assisted && s.assistUntil>=now() && s.cursor<32){Block b=world.getBlockAt(s.x,80,s.cursor++);if(b.getType()==Material.AIR){b.setType(Material.WOOL,false);b.setData((byte)10,false);s.placed.add(b);s.placedCount++;if(s.started==0)s.started=now();}}
        }
    }
    @EventHandler public void swing(PlayerAnimationEvent e){Player p=e.getPlayer();if(cpsEnabled(p))meters.computeIfAbsent(p.getUniqueId(),id->new ClickMeter()).record(false,now());}
    @EventHandler public void interact(PlayerInteractEvent e){
        Player p=e.getPlayer();boolean right=e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK;
        if(right && cpsEnabled(p))meters.computeIfAbsent(p.getUniqueId(),id->new ClickMeter()).record(true,now());
        Session s=sessions.get(p.getUniqueId());if(s==null || !right || !e.hasItem())return;
        switch(e.getItem().getType()){
        case WATCH:e.setCancelled(true);reset(p,s);break;
        case COMPASS:e.setCancelled(true);close(p,true);break;
        case BLAZE_ROD:e.setCancelled(true);if(s.assisted)s.assistUntil=now()+400;else p.sendMessage("§7Esta pista é manual. Use /fastplace para assistência.");break;
        default:break;
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e){
        Session s=sessions.get(e.getPlayer().getUniqueId());if(s==null){if(e.getBlock().getWorld()==world)e.setCancelled(true);return;}
        Block b=e.getBlock();if(b.getWorld()!=world || b.getY()!=80 || Math.abs(b.getX()-s.x)>1 || b.getZ()<1 || b.getZ()>31 || b.getType()!=Material.WOOL){e.setCancelled(true);return;}
        s.placed.add(b);s.placedCount=s.placed.size();if(s.started==0)s.started=now();new BukkitRunnable(){public void run(){if(active(e.getPlayer()))e.getPlayer().getInventory().setItem(0,new ItemStack(Material.WOOL,64,(short)10));}}.runTaskLater(plugin,1);
    }
    @EventHandler public void breakBlock(BlockBreakEvent e){if(e.getBlock().getWorld()!=world)return;Session s=sessions.get(e.getPlayer().getUniqueId());e.setCancelled(true);if(s!=null && s.placed.remove(e.getBlock())){e.getBlock().setType(Material.AIR,false);s.placedCount=s.placed.size();}}
    @EventHandler public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player && active((Player)e.getEntity()))e.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent e){if(active(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){close(e.getPlayer(),false);cps.remove(e.getPlayer().getUniqueId());meters.remove(e.getPlayer().getUniqueId());}
}
