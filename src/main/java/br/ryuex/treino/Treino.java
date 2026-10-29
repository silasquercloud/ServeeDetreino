package br.ryuex.treino;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.generator.ChunkGenerator;

/** One world per room. All state changes run on Bukkit's main thread. */
public final class Treino extends JavaPlugin implements Listener {
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private final Map<UUID, Room> membership = new HashMap<>();
    private final Map<UUID, Mode> choosing = new HashMap<>();
    private Location lobby;
    private int nextId = 1;
    enum Mode { BEDWARS, BEDFIGHT, SKYWARS }
    enum Phase { WAITING, COUNTDOWN, PLAYING, ENDING }
    private static final String MENU = "§8Treino • modos", SIZE = "§8Treino • vagas", LIST = "§8Treino • salas", SHOP = "§8Treino • loja";
    private static final byte[] COLORS = {14,11,5,4,1,10,9,6,3,13,2,0};
    public static class VoidGenerator extends ChunkGenerator {
        @Override public byte[] generate(World w, Random r, int x, int z) { return new byte[32768]; }
    }
    private World voidWorld(String name) {
        World w = Bukkit.createWorld(new WorldCreator(name).generator(new VoidGenerator()));
        w.setAutoSave(false); w.setDifficulty(Difficulty.NORMAL); w.setTime(6000);
        w.setGameRuleValue("doMobSpawning", "false"); w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doFireTick", "false"); w.setStorm(false);
        return w;
    }
    @Override public void onEnable() {
        saveDefaultConfig();
        World w = voidWorld("treino_lobby");
        for (int x=-12;x<=12;x++) for(int z=-12;z<=12;z++) {
            w.getBlockAt(x,79,z).setType(Material.BEDROCK);
            w.getBlockAt(x,80,z).setType((Math.abs(x)==12 || Math.abs(z)==12)?Material.SEA_LANTERN:Material.QUARTZ_BLOCK);
        }
        lobby = new Location(w,0.5,81,0.5); w.setSpawnLocation(0,81,0);
        Bukkit.getPluginManager().registerEvents(this,this);
        new BukkitRunnable() { public void run() { tick(); } }.runTaskTimer(this,20,20);
        for(Player p:Bukkit.getOnlinePlayers()) hub(p);
        getLogger().info("TreinoHub ativo: /treino. API alvo: Spigot 1.8.8.");
    }
    @Override public void onDisable() {
        for(Player p:Bukkit.getOnlinePlayers()) { p.teleport(lobby); p.getInventory().clear(); }
        for(Room r:rooms.values()) removeWorld(r.world);
    }
    private void removeWorld(World world) {
        if(!world.getName().startsWith("treino_room_")) return;
        java.io.File folder=world.getWorldFolder();
        if(!Bukkit.unloadWorld(world,false)) { getLogger().warning("Não foi possível descarregar "+world.getName()); return; }
        try(java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.walk(folder.toPath())) {
            for(java.nio.file.Path path:(Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator) java.nio.file.Files.deleteIfExists(path);
        } catch(java.io.IOException ex) { getLogger().warning("Falha ao limpar arena: "+ex.getMessage()); }
    }
    private void msg(Player p,String s) { p.sendMessage("§5§lTREINO §8» §f"+s); }
    private ItemStack item(Material m,int count,String title) {
        ItemStack i=new ItemStack(m,count); ItemMeta meta=i.getItemMeta(); meta.setDisplayName(title); i.setItemMeta(meta); return i;
    }
    private void reset(Player p,GameMode mode) {
        p.closeInventory(); p.setGameMode(mode); p.getInventory().clear(); p.getInventory().setArmorContents(null);
        p.setHealth(p.getMaxHealth()); p.setFoodLevel(20); p.setFireTicks(0); p.setFallDistance(0);
        for(org.bukkit.potion.PotionEffect e:p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
    private void hub(Player p) {
        reset(p,GameMode.ADVENTURE); p.teleport(lobby);
        p.getInventory().setItem(0,item(Material.COMPASS,1,"§dSelecionar modo / salas"));
        msg(p,"Use a bússola ou §d/treino§f. Crie uma sala e passe o número aos amigos.");
    }
    private void menu(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,MENU);
        inv.setItem(10,item(Material.BED,1,"§cBedWars §7• solo, geradores e loja"));
        inv.setItem(13,item(Material.IRON_SWORD,1,"§bBedFight §7• duelos 1x1 simultâneos"));
        inv.setItem(16,item(Material.ENDER_PEARL,1,"§aSkyWars §7• 2 a 12 jogadores"));
        inv.setItem(22,item(Material.CHEST,1,"§eEntrar em uma sala")); p.openInventory(inv);
    }
    private void sizeMenu(Player p,Mode mode) {
        choosing.put(p.getUniqueId(),mode); Inventory inv=Bukkit.createInventory(null,27,SIZE);
        int slot=0, max=mode==Mode.SKYWARS?12:8;
        for(int n=2;n<=max;n++) if(mode!=Mode.BEDFIGHT || n%2==0) inv.setItem(slot++,item(Material.PAPER,n,"§d"+n+" jogadores"));
        p.openInventory(inv);
    }
    private void list(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,LIST); int slot=0;
        for(Room r:rooms.values()) if(r.phase==Phase.WAITING || r.phase==Phase.COUNTDOWN) {
            if(slot>=54) break;
            inv.setItem(slot++,item(Material.PAPER,1,"§d#"+r.id+" §f"+r.mode+" §7("+r.players.size()+"/"+r.capacity+")"));
        }
        p.openInventory(inv);
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(!(sender instanceof Player)) { sender.sendMessage("Use este comando dentro do jogo."); return true; }
        Player p=(Player)sender;
        try {
            if(args.length==0) menu(p);
            else if(args[0].equalsIgnoreCase("criar") && args.length==3) {
                Mode m=Mode.valueOf(args[1].toUpperCase(Locale.ROOT)); int n=Integer.parseInt(args[2]); create(p,m,n);
            } else if(args[0].equalsIgnoreCase("entrar") && args.length==2) join(p,rooms.get(Integer.parseInt(args[1])));
            else if(args[0].equalsIgnoreCase("sair")) { leave(p); hub(p); }
            else if(args[0].equalsIgnoreCase("salas")) list(p);
            else if(args[0].equalsIgnoreCase("loja")) shop(p);
            else if(args[0].equalsIgnoreCase("iniciar")) {
                Room r=membership.get(p.getUniqueId());
                if(r!=null && r.owner.equals(p.getUniqueId()) && r.phase==Phase.WAITING && r.players.size()>=2 && (r.mode!=Mode.BEDFIGHT || r.players.size()%2==0)) { r.phase=Phase.COUNTDOWN; r.countdown=5; }
                else msg(p,"Só o dono pode iniciar; precisa de 2 jogadores e número par no BedFight.");
            } else msg(p,"/treino criar <bedwars|bedfight|skywars> <vagas> | entrar <sala> | sair | salas | iniciar | loja");
        } catch(IllegalArgumentException e) { msg(p,"Confira o modo, número de sala e vagas. Ex.: /treino criar skywars 6"); }
        return true;
    }
    private void create(Player p,Mode mode,int n) {
        if(membership.containsKey(p.getUniqueId())) { msg(p,"Saia da sala atual primeiro: /treino sair"); return; }
        if(n<2 || n>(mode==Mode.SKYWARS?12:8) || (mode==Mode.BEDFIGHT && n%2!=0)) { msg(p,"Vagas: SkyWars 2–12, BedWars 2–8, BedFight 2/4/6/8."); return; }
        if(rooms.size()>=getConfig().getInt("max-rooms",4)) { msg(p,"Limite de salas atingido. Aguarde uma terminar."); return; }
        int id=nextId++; Room r=new Room(id,mode,n,p.getUniqueId()); rooms.put(id,r); join(p,r);
        msg(p,"Sala §d#"+id+"§f criada. Amigos: §d/treino entrar "+id);
    }
    private void join(Player p,Room r) {
        if(membership.containsKey(p.getUniqueId())) { msg(p,"Use /treino sair antes de trocar de sala."); return; }
        if(r==null || (r.phase!=Phase.WAITING && r.phase!=Phase.COUNTDOWN) || r.players.size()>=r.capacity) { msg(p,"Sala indisponível."); return; }
        p.closeInventory(); r.players.add(p.getUniqueId()); membership.put(p.getUniqueId(),r);
        reset(p,GameMode.ADVENTURE); p.teleport(r.waiting());
        r.say("§f"+p.getName()+" entrou §7("+r.players.size()+"/"+r.capacity+")");
        if(r.players.size()==r.capacity) { r.phase=Phase.COUNTDOWN; r.countdown=10; }
    }
    private void leave(Player p) {
        choosing.remove(p.getUniqueId()); Room r=membership.remove(p.getUniqueId()); if(r==null) return;
        r.players.remove(p.getUniqueId()); Integer team=r.teams.remove(p.getUniqueId());
        r.eliminated.remove(p.getUniqueId()); r.respawning.remove(p.getUniqueId());
        if(r.owner.equals(p.getUniqueId()) && !r.players.isEmpty()) r.owner=r.players.iterator().next();
        if(r.players.isEmpty()) finish(r,"Sala encerrada.");
        else if(r.phase==Phase.PLAYING) {
            if(team!=null && r.mode==Mode.BEDFIGHT) r.resolveDuel(team);
            r.checkWinner();
        } else if(r.phase==Phase.COUNTDOWN) { r.phase=Phase.WAITING; r.say("§eContagem cancelada: jogador saiu. O dono pode usar /treino iniciar."); }
    }
    private void finish(final Room r,String message) {
        if(r.phase==Phase.ENDING) return;
        r.phase=Phase.ENDING; r.say("§a§l"+message);
        new BukkitRunnable() { public void run() {
            for(UUID u:new ArrayList<>(r.players)) { membership.remove(u); Player p=Bukkit.getPlayer(u); if(p!=null) hub(p); }
            r.players.clear(); rooms.remove(r.id); removeWorld(r.world);
        }}.runTaskLater(this,80);
    }
    private void tick() {
        for(Room r:new ArrayList<>(rooms.values())) {
            if(r.phase==Phase.COUNTDOWN) { if(r.countdown<=0) r.start(); else { r.say("§eIniciando em "+r.countdown+"s"); r.countdown--; } }
            if(r.phase!=Phase.PLAYING) continue;
            r.elapsed++;
            for(UUID u:new ArrayList<>(r.players)) { Player p=Bukkit.getPlayer(u); if(p!=null && !r.eliminated.contains(u) && !r.respawning.contains(u) && p.getLocation().getY()<40) knockOut(p,r); }
            if(r.mode==Mode.BEDWARS) {
                if(r.elapsed%2==0) for(int t=0;t<r.spawns.size();t++) r.world.dropItem(r.spawns.get(t).clone().add(0,0.2,2),new ItemStack(Material.IRON_INGOT));
                if(r.elapsed%8==0) for(Location l:r.spawns) r.world.dropItem(l.clone().add(0,0.2,2),new ItemStack(Material.GOLD_INGOT));
                if(r.elapsed%20==0) r.world.dropItem(new Location(r.world,0.5,81,0.5),new ItemStack(Material.EMERALD));
            }
            if(r.mode==Mode.SKYWARS && r.elapsed==180) { r.fillChests(); r.say("§eBaús reabastecidos!"); }
            if(r.elapsed==getConfig().getInt("sudden-death-seconds",600)) {
                r.beds.replaceAll((k,v)->false); for(Block b:r.bedBlocks) b.setType(Material.AIR);
                r.say("§cMorte súbita! Camas destruídas e centro obrigatório.");
            }
            int limit=getConfig().getInt("sudden-death-seconds",600);
            if(r.elapsed>=limit) for(UUID u:new ArrayList<>(r.players)) { Player p=Bukkit.getPlayer(u); if(p!=null && !r.eliminated.contains(u) && p.getLocation().distanceSquared(new Location(r.world,0,81,0))>625) p.damage(2); }
            if(r.elapsed>=limit+300) finish(r,"Tempo esgotado: empate.");
            if(r.elapsed%2==0) r.board();
        }
    }
    private final class Room {
        final int id,capacity; final Mode mode; UUID owner; final World world;
        Phase phase=Phase.WAITING; int countdown,elapsed;
        final Set<UUID> players=new LinkedHashSet<>(),eliminated=new HashSet<>(),respawning=new HashSet<>();
        final Map<UUID,Integer> teams=new HashMap<>(); final Map<Integer,Boolean> beds=new HashMap<>();
        final List<Location> spawns=new ArrayList<>(); final List<Block> bedBlocks=new ArrayList<>();
        final Map<String,Integer> bedOwners=new HashMap<>(); final Set<String> placed=new HashSet<>(); final List<Block> chests=new ArrayList<>();
        Room(int id,Mode mode,int cap,UUID owner) {
            this.id=id; this.mode=mode; this.capacity=cap; this.owner=owner;
            world=voidWorld("treino_room_"+System.currentTimeMillis()+"_"+id);
            platform(0,110,0,5,Material.GLASS);
            platform(0,79,0,9,Material.STONE); platform(0,80,0,8,Material.GRASS);
            for(int i=0;i<cap;i++) {
                double angle=2*Math.PI*i/cap; int x=(int)Math.round(Math.cos(angle)*42), z=(int)Math.round(Math.sin(angle)*42);
                platform(x,79,z,5,Material.STONE); platform(x,80,z,4,Material.GRASS);
                spawns.add(new Location(world,x+0.5,81,z+0.5,(float)(angle*180/Math.PI+90),0)); beds.put(i,true);
                if(mode!=Mode.SKYWARS) {
                    Block foot=world.getBlockAt(x+2,81,z),head=world.getBlockAt(x+2,81,z+1);
                    foot.setType(Material.BED_BLOCK,false); head.setType(Material.BED_BLOCK,false); foot.setData((byte)0,false); head.setData((byte)8,false);
                    bedBlocks.add(foot); bedBlocks.add(head); bedOwners.put(key(foot),i); bedOwners.put(key(head),i);
                    Block shop=world.getBlockAt(x-2,81,z); shop.setType(Material.WORKBENCH);
                    for(int dx=-1;dx<=1;dx++) { Block b=world.getBlockAt(x+dx,80,z); b.setType(Material.WOOL); b.setData(COLORS[i]); }
                } else { chest(x+2,81,z); chest(x-2,81,z); }
            }
            if(mode==Mode.SKYWARS) { chest(0,81,0); chest(3,81,3); chest(-3,81,-3); fillChests(); }
        }
        void platform(int x,int y,int z,int radius,Material mat) {
            for(int a=-radius;a<=radius;a++) for(int b=-radius;b<=radius;b++) if(a*a+b*b<=radius*radius) world.getBlockAt(x+a,y,z+b).setType(mat);
        }
        Location waiting() { return new Location(world,0.5,111,0.5); }
        void say(String s) { for(UUID u:players) { Player p=Bukkit.getPlayer(u); if(p!=null) msg(p,s); } }
        void chest(int x,int y,int z) { Block b=world.getBlockAt(x,y,z); b.setType(Material.CHEST); chests.add(b); }
        void fillChests() {
            for(Block b:chests) { Inventory inv=((Chest)b.getState()).getBlockInventory(); inv.clear();
                inv.addItem(new ItemStack(Material.STONE_SWORD),new ItemStack(Material.BOW),new ItemStack(Material.ARROW,12),new ItemStack(Material.COOKED_BEEF,8),new ItemStack(Material.WOOD,48),new ItemStack(Material.SNOW_BALL,8),new ItemStack(Material.IRON_CHESTPLATE),new ItemStack(Material.IRON_HELMET));
                if(b.getX()*b.getX()+b.getZ()*b.getZ()<100) inv.addItem(new ItemStack(Material.DIAMOND_SWORD),new ItemStack(Material.GOLDEN_APPLE,2),new ItemStack(Material.ENDER_PEARL));
            }
        }
        void start() {
            if(players.size()<2 || (mode==Mode.BEDFIGHT && players.size()%2!=0)) { phase=Phase.WAITING; return; }
            phase=Phase.PLAYING; int i=0;
            for(UUID u:players) { teams.put(u,i++); Player p=Bukkit.getPlayer(u); if(p!=null) spawn(p); }
            say("§aValendo! §f"+(mode==Mode.BEDFIGHT?"Duelo contra a ilha oposta do seu par. Sem loja, kit pronto.":mode==Mode.BEDWARS?"Proteja sua cama. Clique na bancada para abrir a loja.":"Equipe-se nos baús e seja o último vivo."));
            board();
        }
        void spawn(Player p) {
            reset(p,GameMode.SURVIVAL); int t=teams.get(p.getUniqueId()); p.teleport(spawns.get(t)); p.setNoDamageTicks(60);
            if(mode!=Mode.SKYWARS) {
                p.getInventory().addItem(new ItemStack(Material.WOOD_SWORD),new ItemStack(Material.WOOD_PICKAXE),new ItemStack(Material.COOKED_BEEF,16));
                p.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                if(mode==Mode.BEDFIGHT) p.getInventory().addItem(new ItemStack(Material.WOOL,64,(short)COLORS[t]),new ItemStack(Material.WOOL,64,(short)COLORS[t]),new ItemStack(Material.SHEARS));
            }
        }
        boolean alive(UUID u) { return players.contains(u) && !eliminated.contains(u); }
        void resolveDuel(int t) {
            int opponent=t%2==0?t+1:t-1;
            boolean remaining=false; for(UUID u:players) if(teams.get(u)==t && alive(u)) remaining=true;
            if(!remaining) for(UUID u:players) if(teams.get(u)==opponent && alive(u)) {
                eliminated.add(u); Player p=Bukkit.getPlayer(u); if(p!=null) { msg(p,"§aVocê venceu seu duelo!"); reset(p,GameMode.SPECTATOR); p.teleport(waiting()); }
            }
        }
        void checkWinner() {
            List<UUID> live=new ArrayList<>(); for(UUID u:players) if(alive(u)) live.add(u);
            if(mode==Mode.BEDFIGHT) { if(live.isEmpty()) finish(this,"Todos os duelos terminaram!"); }
            else if(live.size()<=1) { Player win=live.isEmpty()?null:Bukkit.getPlayer(live.get(0)); finish(this,win==null?"Empate!":win.getName()+" venceu!"); }
        }
        void board() {
            int live=0; for(UUID u:players) if(alive(u)) live++;
            for(UUID u:players) { Player p=Bukkit.getPlayer(u); if(p==null) continue;
                org.bukkit.scoreboard.Scoreboard s=Bukkit.getScoreboardManager().getNewScoreboard();
                org.bukkit.scoreboard.Objective o=s.registerNewObjective("treino","dummy"); o.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR); o.setDisplayName("§5§lRYUEX TREINO");
                o.getScore("§f"+mode).setScore(5); o.getScore("§7Sala: §d#"+id).setScore(4); o.getScore("§7Vivos: §f"+live).setScore(3); o.getScore("§7Tempo: §f"+elapsed+"s").setScore(2);
                Integer t=teams.get(u); o.getScore("§7Cama: "+(mode==Mode.SKYWARS?"§8—":Boolean.TRUE.equals(beds.get(t))?"§aOK":"§cX")).setScore(1); p.setScoreboard(s);
            }
        }
    }
    private static String key(Block b) { return b.getX()+":"+b.getY()+":"+b.getZ(); }
    private void knockOut(final Player p,final Room r) {
        UUID u=p.getUniqueId(); if(r.phase!=Phase.PLAYING || !r.alive(u) || r.respawning.contains(u)) return;
        int team=r.teams.get(u); boolean respawn=r.mode!=Mode.SKYWARS && Boolean.TRUE.equals(r.beds.get(team));
        r.respawning.add(u);
        for(ItemStack i:p.getInventory().getContents()) if(i!=null && (i.getType()==Material.IRON_INGOT || i.getType()==Material.GOLD_INGOT || i.getType()==Material.EMERALD)) r.world.dropItemNaturally(p.getLocation(),i);
        reset(p,GameMode.SPECTATOR); p.teleport(r.waiting());
        if(respawn) {
            msg(p,"§eRenascendo em 3 segundos...");
            new BukkitRunnable(){ public void run(){ r.respawning.remove(p.getUniqueId()); if(membership.get(p.getUniqueId())==r && r.phase==Phase.PLAYING && r.alive(p.getUniqueId())) r.spawn(p); }}.runTaskLater(this,60);
        } else { r.respawning.remove(u); r.eliminated.add(u); r.say("§c"+p.getName()+" foi eliminado!"); if(r.mode==Mode.BEDFIGHT) r.resolveDuel(team); r.checkWinner(); }
    }
    private void shop(Player p) {
        Room r=membership.get(p.getUniqueId()); if(r==null || r.mode!=Mode.BEDWARS || r.phase!=Phase.PLAYING || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) return;
        Inventory inv=Bukkit.createInventory(null,27,SHOP);
        inv.setItem(10,item(Material.WOOL,16,"§f16 lãs §7• 4 ferros")); inv.setItem(11,item(Material.STONE_SWORD,1,"§fEspada de pedra §7• 10 ferros"));
        inv.setItem(12,item(Material.IRON_SWORD,1,"§fEspada de ferro §7• 7 ouros")); inv.setItem(13,item(Material.IRON_CHESTPLATE,1,"§fPeitoral de ferro §7• 12 ouros"));
        inv.setItem(14,item(Material.WOOD,16,"§f16 madeiras §7• 4 ouros")); inv.setItem(15,item(Material.GOLDEN_APPLE,1,"§fMaçã dourada §7• 3 ouros"));
        inv.setItem(16,item(Material.DIAMOND_SWORD,1,"§fEspada de diamante §7• 4 esmeraldas"));
        inv.setItem(21,item(Material.SHEARS,1,"§fTesoura §7• 20 ferros")); inv.setItem(22,item(Material.IRON_PICKAXE,1,"§fPicareta §7• 20 ferros")); p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p=(Player)e.getWhoClicked(); String title=e.getView().getTitle();
        if(!Arrays.asList(MENU,SIZE,LIST,SHOP).contains(title)) return;
        e.setCancelled(true); if(e.getRawSlot()<0 || e.getRawSlot()>=e.getView().getTopInventory().getSize()) return;
        ItemStack i=e.getCurrentItem(); if(i==null || i.getType()==Material.AIR) return;
        if(title.equals(MENU)) { if(e.getRawSlot()==22) list(p); else if(e.getRawSlot()==10) sizeMenu(p,Mode.BEDWARS); else if(e.getRawSlot()==13) sizeMenu(p,Mode.BEDFIGHT); else if(e.getRawSlot()==16) sizeMenu(p,Mode.SKYWARS); }
        else if(title.equals(SIZE)) { Mode m=choosing.remove(p.getUniqueId()); if(m!=null) { p.closeInventory(); create(p,m,i.getAmount()); } }
        else if(title.equals(LIST)) join(p,rooms.get(Integer.parseInt(ChatColor.stripColor(i.getItemMeta().getDisplayName()).split(" ")[0].substring(1))));
        else {
            Room r=membership.get(p.getUniqueId()); if(r==null || r.phase!=Phase.PLAYING || r.mode!=Mode.BEDWARS || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) return;
            int slot=e.getRawSlot(),cost; Material currency;
            if(slot==10) { cost=4; currency=Material.IRON_INGOT; } else if(slot==11) { cost=10; currency=Material.IRON_INGOT; }
            else if(slot==12) { cost=7; currency=Material.GOLD_INGOT; } else if(slot==13) { cost=12; currency=Material.GOLD_INGOT; }
            else if(slot==14) { cost=4; currency=Material.GOLD_INGOT; } else if(slot==15) { cost=3; currency=Material.GOLD_INGOT; }
            else if(slot==16) { cost=4; currency=Material.EMERALD; } else if(slot==21 || slot==22) { cost=20; currency=Material.IRON_INGOT; } else return;
            if(p.getInventory().firstEmpty()==-1) { msg(p,"Libere um espaço no inventário."); return; }
            if(!p.getInventory().contains(currency,cost)) { msg(p,"Recursos insuficientes."); return; }
            p.getInventory().removeItem(new ItemStack(currency,cost)); ItemStack bought=new ItemStack(i.getType(),i.getAmount());
            if(bought.getType()==Material.WOOL) bought.setDurability(COLORS[r.teams.get(p.getUniqueId())]);
            p.getInventory().addItem(bought); msg(p,"§aCompra concluída.");
        }
    }
    @EventHandler public void drag(InventoryDragEvent e) { if(Arrays.asList(MENU,SIZE,LIST,SHOP).contains(e.getView().getTitle())) e.setCancelled(true); }
    @EventHandler public void interact(PlayerInteractEvent e) {
        Player p=e.getPlayer(); Room r=membership.get(p.getUniqueId());
        if(r==null && p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(e.hasItem() && e.getItem().getType()==Material.COMPASS) menu(p); return; }
        if(r!=null && (r.phase!=Phase.PLAYING || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId()))) { e.setCancelled(true); return; }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.WORKBENCH) { e.setCancelled(true); shop(p); }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.BED_BLOCK) e.setCancelled(true);
    }
    @EventHandler public void place(BlockPlaceEvent e) {
        Room r=membership.get(e.getPlayer().getUniqueId());
        if(r==null) { if(e.getBlock().getWorld()==lobby.getWorld()) e.setCancelled(true); return; }
        if(r.phase!=Phase.PLAYING || !r.alive(e.getPlayer().getUniqueId()) || r.respawning.contains(e.getPlayer().getUniqueId()) || e.getBlock().getY()<60 || e.getBlock().getY()>100 || Math.abs(e.getBlock().getX())>65 || Math.abs(e.getBlock().getZ())>65) { e.setCancelled(true); return; }
        r.placed.add(key(e.getBlock()));
    }
    @EventHandler public void breakBlock(BlockBreakEvent e) {
        Player p=e.getPlayer(); Room r=membership.get(p.getUniqueId()); Block b=e.getBlock();
        if(r==null) { if(b.getWorld()==lobby.getWorld()) e.setCancelled(true); return; }
        e.setCancelled(true); if(r.phase!=Phase.PLAYING || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) return;
        Integer target=r.bedOwners.get(key(b));
        if(target!=null && Boolean.TRUE.equals(r.beds.get(target))) {
            int own=r.teams.get(p.getUniqueId()); if(own==target) { msg(p,"Essa é sua cama!"); return; }
            if(r.mode==Mode.BEDFIGHT && own/2!=target/2) { msg(p,"Ataque apenas o oponente do seu par."); return; }
            r.beds.put(target,false); for(Block bed:r.bedBlocks) if(r.bedOwners.get(key(bed)).equals(target)) bed.setType(Material.AIR);
            r.say("§c"+p.getName()+" destruiu a cama da base "+(target+1)+"!"); return;
        }
        if(r.placed.remove(key(b))) e.setCancelled(false);
    }
    @EventHandler public void damage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player)) return; Player p=(Player)e.getEntity(); Room r=membership.get(p.getUniqueId());
        if(r==null) { if(p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(p.getLocation().getY()<50) p.teleport(lobby); } return; }
        if(r.phase!=Phase.PLAYING || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) { e.setCancelled(true); if(e.getCause()==EntityDamageEvent.DamageCause.VOID) p.teleport(r.waiting()); return; }
        if(e instanceof EntityDamageByEntityEvent) {
            Entity attacker=((EntityDamageByEntityEvent)e).getDamager(); Player source=attacker instanceof Player?(Player)attacker:attacker instanceof Projectile && ((Projectile)attacker).getShooter() instanceof Player?(Player)((Projectile)attacker).getShooter():null;
            if(source!=null && (membership.get(source.getUniqueId())!=r || !r.alive(source.getUniqueId()) || r.respawning.contains(source.getUniqueId()) || (r.mode==Mode.BEDFIGHT && r.teams.get(source.getUniqueId())/2!=r.teams.get(p.getUniqueId())/2))) { e.setCancelled(true); return; }
        }
        if(e.getFinalDamage()>=p.getHealth() || e.getCause()==EntityDamageEvent.DamageCause.VOID) { e.setCancelled(true); knockOut(p,r); }
    }
    @EventHandler public void death(PlayerDeathEvent e) { Room r=membership.get(e.getEntity().getUniqueId()); if(r!=null) { e.getDrops().clear(); e.setDroppedExp(0); } }
    @EventHandler public void respawn(PlayerRespawnEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); e.setRespawnLocation(r==null?lobby:r.waiting()); if(r!=null) new BukkitRunnable(){public void run(){knockOut(e.getPlayer(),r);}}.runTaskLater(this,1); }
    @EventHandler public void quit(PlayerQuitEvent e) { leave(e.getPlayer()); }
    @EventHandler public void join(PlayerJoinEvent e) { hub(e.getPlayer()); }
    @EventHandler public void food(FoodLevelChangeEvent e) { if(e.getEntity() instanceof Player && e.getEntity().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    @EventHandler public void explosion(EntityExplodeEvent e) { if(e.getLocation().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    @EventHandler public void burn(BlockBurnEvent e) { if(e.getBlock().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    @EventHandler public void drop(PlayerDropItemEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); if(e.getPlayer().getWorld()==lobby.getWorld() || (r!=null && (r.phase!=Phase.PLAYING || !r.alive(e.getPlayer().getUniqueId()) || r.respawning.contains(e.getPlayer().getUniqueId())))) e.setCancelled(true); }
}
