package br.ryuex.modes;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

/** Independent 1.8.8 modes; no dependency on the legacy TreinoHub. */
public final class ExtraModes extends JavaPlugin implements Listener {
    private final Map<UUID,Room> members=new HashMap<>();
    private final Map<String,Room> rooms=new LinkedHashMap<>();
    private final Map<UUID,String> kits=new HashMap<>();
    private final Set<UUID> moving=new HashSet<>();
    private final Random random=new Random();
    private int task;
    private static final String MENU="§8Escolha seu modo";
    private static final String KITS="§8Kits de HG";
    private static class Room {
        String id; boolean hg, running, ending; int size, clock, countdown=-1, red, blue;
        World world; String theme;
        final LinkedHashMap<UUID,Integer> teams=new LinkedHashMap<>();
        final Set<UUID> alive=new HashSet<>();
        final Map<Location,BlockState> edits=new LinkedHashMap<>();
        Room(String id,boolean hg,int size){this.id=id;this.hg=hg;this.size=size;}
    }
    public static final class VoidGen extends ChunkGenerator {
        @Override public byte[][] generateBlockSections(World w,Random r,int x,int z,BiomeGrid b){return new byte[16][];}
    }
    @Override public void onEnable(){
        getDataFolder().mkdirs();
        Bukkit.getPluginManager().registerEvents(this,this);
        task=Bukkit.getScheduler().scheduleSyncRepeatingTask(this,()->tick(),20,20);
        getLogger().info("The Bridge + HG disponíveis: /jogos, /bridge, /hg. Mapas gerados ao abrir cada sala.");
        for(Player p:Bukkit.getOnlinePlayers())restore(p);
    }
    @Override public void onDisable(){
        Bukkit.getScheduler().cancelTask(task);
        for(Room r:new ArrayList<>(rooms.values()))finish(r,"Servidor encerrando.");
    }
    private File snapshot(UUID id){return new File(getDataFolder(),"players/"+id+".yml");}
    private void save(Player p) throws IOException {
        File file=snapshot(p.getUniqueId());if(file.exists())throw new IOException("Existe inventário pendente; entre novamente antes de jogar.");
        file.getParentFile().mkdirs();YamlConfiguration y=new YamlConfiguration();
        y.set("inventory",Arrays.asList(p.getInventory().getContents()));y.set("armor",Arrays.asList(p.getInventory().getArmorContents()));
        y.set("location",p.getLocation());y.set("mode",p.getGameMode().name());y.set("level",p.getLevel());y.set("exp",p.getExp());y.set("food",p.getFoodLevel());y.set("health",p.getHealth());y.save(file);
    }
    @SuppressWarnings("unchecked") private void restore(Player p){
        File file=snapshot(p.getUniqueId());if(!file.exists())return;
        YamlConfiguration y=YamlConfiguration.loadConfiguration(file);
        try {
            p.getInventory().clear();p.getInventory().setArmorContents(null);
            List<ItemStack> inv=(List<ItemStack>)y.getList("inventory");List<ItemStack> armor=(List<ItemStack>)y.getList("armor");
            if(inv!=null)p.getInventory().setContents(inv.toArray(new ItemStack[0]));
            if(armor!=null)p.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
            p.setGameMode(GameMode.valueOf(y.getString("mode","SURVIVAL")));p.setLevel(y.getInt("level"));p.setExp((float)y.getDouble("exp"));p.setFoodLevel(y.getInt("food",20));
            p.setHealth(Math.max(1,Math.min(p.getMaxHealth(),y.getDouble("health",20))));p.setFireTicks(0);p.setFallDistance(0);
            Location old=(Location)y.get("location");if(old==null||old.getWorld()==null)old=lobby();
            teleport(p,old);p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            if(!file.delete())getLogger().warning("Não foi possível remover snapshot "+file);
        }catch(Exception e){getLogger().severe("Inventário preservado para recuperação: "+file+" / "+e);}
    }
    private Location lobby(){World w=Bukkit.getWorld("spawn");return (w==null?Bukkit.getWorlds().get(0):w).getSpawnLocation();}
    private void teleport(Player p,Location loc){moving.add(p.getUniqueId());try{p.teleport(loc);}finally{moving.remove(p.getUniqueId());}}
    private void clean(Player p){
        p.closeInventory();p.getInventory().clear();p.getInventory().setArmorContents(null);
        for(org.bukkit.potion.PotionEffect e:p.getActivePotionEffects())p.removePotionEffect(e.getType());
        p.setGameMode(GameMode.SURVIVAL);p.setHealth(p.getMaxHealth());p.setFoodLevel(20);p.setFireTicks(0);p.setFallDistance(0);p.setLevel(0);p.setExp(0);p.setVelocity(new Vector());p.setNoDamageTicks(20);
    }
    @Override public boolean onCommand(CommandSender sender,Command c,String label,String[] args){
        if(!(sender instanceof Player)){sender.sendMessage("Use dentro do jogo.");return true;}Player p=(Player)sender;
        String arg=args.length==0?"":args[0].toLowerCase(Locale.ROOT);
        if(arg.equals("sair")||arg.equals("leave")){leave(p);return true;}
        if(c.getName().equals("jogos")){menu(p,false);return true;}
        if(c.getName().equals("hg")&&arg.equals("kit")){menu(p,true);return true;}
        if(!arg.isEmpty()&&!arg.equals("solo")&&!arg.equals("dupla")&&!arg.equals("entrar")){p.sendMessage("§e/bridge [solo|dupla|sair] • /hg [entrar|kit|sair]");return true;}
        join(p,c.getName().equals("hg"),arg.equals("dupla")?2:1);return true;
    }
    private ItemStack item(Material m,String name){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);i.setItemMeta(meta);return i;}
    private void menu(Player p,boolean kit){
        Inventory inv=Bukkit.createInventory(null,27,kit?KITS:MENU);
        if(kit){inv.setItem(11,item(Material.STONE_SWORD,"§aGuerreiro"));inv.setItem(13,item(Material.BOW,"§aArqueiro"));inv.setItem(15,item(Material.COOKED_BEEF,"§aExplorador"));}
        else {inv.setItem(11,item(Material.STAINED_CLAY,"§cThe Bridge • Solo"));inv.setItem(13,item(Material.DIAMOND_PICKAXE,"§9The Bridge • Duplas"));inv.setItem(15,item(Material.MUSHROOM_SOUP,"§aHG • Kits e sobrevivência"));}
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        String title=e.getView().getTitle();if(!title.equals(MENU)&&!title.equals(KITS))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player)||e.getRawSlot()<0||e.getRawSlot()>=27)return;
        Player p=(Player)e.getWhoClicked();int slot=e.getRawSlot();
        if(title.equals(KITS)){
            Room r=members.get(p.getUniqueId());if(r!=null&&r.running){p.sendMessage("§cEscolha o kit antes de começar.");return;}
            String kit=slot==11?"Guerreiro":slot==13?"Arqueiro":slot==15?"Explorador":null;
            if(kit!=null){kits.put(p.getUniqueId(),kit);p.closeInventory();p.sendMessage("§aKit escolhido: "+kit);}
        }else if(slot==11||slot==13||slot==15){p.closeInventory();join(p,slot==15,slot==13?2:1);}
    }
    private void join(Player p,boolean hg,int size){
        if(members.containsKey(p.getUniqueId())){p.sendMessage("§eUse /bridge sair ou /hg sair primeiro.");return;}
        if(p.getWorld()!=lobby().getWorld()){p.sendMessage("§eVolte ao lobby antes de entrar em outro modo.");return;}
        if(p.isDead()||p.getVehicle()!=null){p.sendMessage("§cSaia do veículo e esteja vivo para entrar.");return;}
        String id=hg?"hg":"bridge"+size;
        Room r=rooms.get(id);
        if(r!=null&&(r.running||r.ending||r.teams.size()>=(hg?16:size*2))){p.sendMessage("§eSala ocupada. Aguarde a partida terminar.");return;}
        try{
            if(r==null){r=new Room(id,hg,size);makeWorld(r);rooms.put(id,r);}
            save(p);int red=0,blue=0;for(int t:r.teams.values())if(t==0)red++;else blue++;
            r.teams.put(p.getUniqueId(),red<=blue?0:1);members.put(p.getUniqueId(),r);clean(p);
            teleport(p,hg?hgSpawn(r,r.teams.size()-1):bridgeSpawn(r,r.teams.get(p.getUniqueId())));
            p.sendMessage("§aEntrou em "+(hg?"HG. Escolha /hg kit":"The Bridge "+(size==1?"solo":"duplas"))+". §e/jogos sair para voltar.");
            announce(r,"§e"+p.getName()+" entrou ("+r.teams.size()+"/"+(hg?16:size*2)+").");
        }catch(Exception e){p.sendMessage("§cFalha ao preparar sala: "+e.getMessage());getLogger().severe(e.toString());if(members.containsKey(p.getUniqueId()))leave(p);else if(r!=null&&r.teams.isEmpty())finish(r,"Falha na preparação.");}
    }
    private void makeWorld(Room r){
        String name="ryuex_"+r.id+"_"+UUID.randomUUID().toString().substring(0,8);
        WorldCreator creator=new WorldCreator(name);creator.generateStructures(false);
        if(!r.hg)creator.generator(new VoidGen());else creator.seed(8723419L).type(WorldType.NORMAL);
        r.world=Bukkit.createWorld(creator);r.world.setAutoSave(false);r.world.setDifficulty(r.hg?Difficulty.NORMAL:Difficulty.PEACEFUL);
        r.world.setGameRuleValue("doMobSpawning","false");r.world.setGameRuleValue("doDaylightCycle","false");r.world.setTime(6000);r.world.setStorm(false);
        if(r.hg){
            r.theme="Vale dos Sobreviventes";r.world.getWorldBorder().setCenter(0,0);r.world.getWorldBorder().setSize(384);
            int y=r.world.getHighestBlockYAt(0,0)+3;
            for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)if(x*x+z*z<=324){r.world.getBlockAt(x,y-1,z).setType(Material.SMOOTH_BRICK);for(int h=0;h<5;h++)r.world.getBlockAt(x,y+h,z).setType(Material.AIR);}
            r.world.setSpawnLocation(0,y,0);
            // Radial stairways link the starting platform to natural terrain.
            for(int side=0;side<4;side++){int dx=side==0?1:side==1?-1:0,dz=side==2?1:side==3?-1:0;
                for(int step=18;step<55;step++){int ground=r.world.getHighestBlockYAt(dx*step,dz*step)-1;int sy=Math.max(ground,y-1-(step-18));
                    for(int width=-1;width<=1;width++)r.world.getBlockAt(dx*step+(dz!=0?width:0),sy,dz*step+(dx!=0?width:0)).setType(Material.COBBLESTONE);}}
        }else{
            int theme=random.nextInt(3);r.theme=new String[]{"Jardins Suspensos","Templo de Quartzo","Fortaleza Glacial"}[theme];Material stone=new Material[]{Material.SMOOTH_BRICK,Material.QUARTZ_BLOCK,Material.PACKED_ICE}[theme];
            for(int side=-1;side<=1;side+=2){
                for(int x=side*32-8;x<=side*32+8;x++)for(int z=-9;z<=9;z++){
                    r.world.getBlockAt(x,64,z).setType(stone);if(Math.abs(z)==9)r.world.getBlockAt(x,65,z).setType(Material.FENCE);}
                // Goal is a 3x3 open hole; colored floor five blocks below catches the scorer.
                for(int x=side*36-1;x<=side*36+1;x++)for(int z=-1;z<=1;z++){
                    r.world.getBlockAt(x,64,z).setType(Material.AIR);r.world.getBlockAt(x,59,z).setType(Material.WOOL);r.world.getBlockAt(x,59,z).setData((byte)(side<0?14:11));}
                for(int z:new int[]{-5,5})for(int h=1;h<=5;h++)r.world.getBlockAt(side*39,64+h,z).setType(stone);
                for(int z=-5;z<=5;z++)r.world.getBlockAt(side*39,70,z).setType(stone);
            }
            for(int x=-24;x<=24;x++)for(int z=-1;z<=1;z++)r.world.getBlockAt(x,64,z).setType(Material.STAINED_CLAY);
            r.world.setSpawnLocation(-30,65,0);
        }
        try{new File(r.world.getWorldFolder(),"ryuex-modes-owned").createNewFile();}catch(IOException e){getLogger().warning(e.toString());}
    }
    private Location bridgeSpawn(Room r,int team){return new Location(r.world,team==0?-30.5:30.5,65,0.5,team==0?-90:90,0);}
    private Location hgSpawn(Room r,int index){Location s=r.world.getSpawnLocation();double a=index*Math.PI*2/16;return new Location(r.world,Math.cos(a)*12+.5,s.getY(),Math.sin(a)*12+.5,(float)(index*22.5+90),0);}
    private ItemStack leather(Material m,boolean red){ItemStack i=new ItemStack(m);LeatherArmorMeta meta=(LeatherArmorMeta)i.getItemMeta();meta.setColor(red?Color.RED:Color.BLUE);i.setItemMeta(meta);return i;}
    private void bridgeKit(Player p,Room r){clean(p);int team=r.teams.get(p.getUniqueId());
        p.getInventory().addItem(new ItemStack(Material.IRON_SWORD),new ItemStack(Material.BOW),new ItemStack(Material.DIAMOND_PICKAXE),new ItemStack(Material.STAINED_CLAY,64,(short)(team==0?14:11)),new ItemStack(Material.STAINED_CLAY,64,(short)(team==0?14:11)),new ItemStack(Material.GOLDEN_APPLE,8),new ItemStack(Material.ARROW));
        p.getInventory().setChestplate(leather(Material.LEATHER_CHESTPLATE,team==0));p.getInventory().setLeggings(leather(Material.LEATHER_LEGGINGS,team==0));p.getInventory().setBoots(leather(Material.LEATHER_BOOTS,team==0));teleport(p,bridgeSpawn(r,team));}
    private void hgKit(Player p){String k=kits.getOrDefault(p.getUniqueId(),"Guerreiro");p.getInventory().addItem(new ItemStack(Material.COMPASS),new ItemStack(Material.COOKED_BEEF,8));
        if(k.equals("Guerreiro"))p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        else if(k.equals("Arqueiro"))p.getInventory().addItem(new ItemStack(Material.BOW),new ItemStack(Material.ARROW,16));
        else p.getInventory().addItem(new ItemStack(Material.STONE_AXE),new ItemStack(Material.COOKED_BEEF,16));
        for(int i=0;i<4;i++)p.getInventory().addItem(new ItemStack(Material.MUSHROOM_SOUP));}
    private void start(Room r){r.running=true;r.clock=0;r.alive.addAll(r.teams.keySet());int i=0;
        for(UUID id:r.teams.keySet()){Player p=Bukkit.getPlayer(id);if(p==null)continue;if(r.hg){clean(p);teleport(p,hgSpawn(r,i++));hgKit(p);}else bridgeKit(p,r);}
        announce(r,r.hg?"§aHG começou! Proteção inicial por 60 segundos. Feast em 8 minutos.":"§aThe Bridge começou! Primeiro a 5 gols vence.");}
    private void tick(){for(Room r:new ArrayList<>(rooms.values())){
        if(r.ending)continue;
        if(!r.running){int minimum=ModeRules.minimum(r.hg,r.size);
            if(r.teams.size()<minimum){r.countdown=-1;}else {if(r.countdown<0)r.countdown=r.hg?20:10;if(r.countdown==0)start(r);else {if(r.countdown<=5||r.countdown%10==0)announce(r,"§eComeça em "+r.countdown+"s");r.countdown--;}}}
        else {r.clock++;if(r.hg){
            if(r.clock==60)announce(r,"§cPvP liberado!");
            if(r.clock==480)feast(r);
            if(r.clock==600){r.world.getWorldBorder().setSize(40,300);announce(r,"§cA borda está fechando em direção ao spawn!");}
            if(r.clock==900){int i=0;for(UUID id:r.alive){Player p=Bukkit.getPlayer(id);if(p!=null)teleport(p,hgSpawn(r,i++*4));}r.world.getWorldBorder().setSize(32);announce(r,"§cDeathmatch!");}
            if(r.clock>=1200){finish(r,"§eTempo esgotado: empate.");continue;}
        }else if(r.clock>=900){finish(r,"§eFim do tempo: "+(r.red==r.blue?"empate":r.red>r.blue?"vermelho venceu":"azul venceu"));continue;}}
        for(UUID id:r.teams.keySet()){Player p=Bukkit.getPlayer(id);if(p==null)continue;
            if(r.running&&!r.hg&&r.clock%5==0&&!p.getInventory().contains(Material.ARROW))p.getInventory().addItem(new ItemStack(Material.ARROW));
            if(r.hg&&r.running&&r.alive.contains(id)){Player nearest=null;double best=Double.MAX_VALUE;for(UUID other:r.alive){Player q=Bukkit.getPlayer(other);if(q!=null&&!other.equals(id)&&q.getWorld()==p.getWorld()){double dist=q.getLocation().distanceSquared(p.getLocation());if(dist<best){best=dist;nearest=q;}}}if(nearest!=null)p.setCompassTarget(nearest.getLocation());}
            board(p,r);
        }
    }}
    private void feast(Room r){Location s=r.world.getSpawnLocation();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){r.world.getBlockAt(x,s.getBlockY()-1,z).setType(Material.SMOOTH_BRICK);for(int y=0;y<3;y++)r.world.getBlockAt(x,s.getBlockY()+y,z).setType(Material.AIR);}
        Block b=r.world.getBlockAt(0,s.getBlockY(),0);b.setType(Material.CHEST);org.bukkit.block.Chest c=(org.bukkit.block.Chest)b.getState();
        c.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD),new ItemStack(Material.IRON_CHESTPLATE),new ItemStack(Material.GOLDEN_APPLE,4),new ItemStack(Material.COOKED_BEEF,32),new ItemStack(Material.ARROW,32));announce(r,"§6Feast liberado no centro: X 0 / Z 0!");}
    private void board(Player p,Room r){Scoreboard b=Bukkit.getScoreboardManager().getNewScoreboard();Objective o=b.registerNewObjective("modes","dummy");o.setDisplaySlot(DisplaySlot.SIDEBAR);o.setDisplayName(r.hg?"§a§lHG":"§c§lTHE BRIDGE");
        o.getScore("§f"+r.theme).setScore(6);o.getScore("§7Jogadores: §f"+r.teams.size()).setScore(5);
        if(!r.running)o.getScore("§eAguardando: "+(r.countdown<0?"jogadores":r.countdown+"s")).setScore(4);
        else {o.getScore("§7Tempo: "+r.clock/60+":"+String.format("%02d",r.clock%60)).setScore(4);if(r.hg){o.getScore("§aVivos: "+r.alive.size()).setScore(3);o.getScore(r.clock<60?"§eProteção inicial":"§cPvP ativo").setScore(2);}else{o.getScore("§cVermelho: "+r.red+" / 5").setScore(3);o.getScore("§9Azul: "+r.blue+" / 5").setScore(2);}}
        o.getScore("§7/jogos sair").setScore(1);p.setScoreboard(b);}
    private void announce(Room r,String msg){for(UUID id:r.teams.keySet()){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage(msg);}}
    private void winner(Room r){if(!r.running||r.ending)return;if(r.hg){if(r.alive.size()<=1){String name=r.alive.isEmpty()?"ninguém":Bukkit.getOfflinePlayer(r.alive.iterator().next()).getName();finish(r,"§6Vencedor do HG: "+name);}}else{boolean red=r.teams.containsValue(0),blue=r.teams.containsValue(1);if(!red||!blue)finish(r,"§6Vitória por desistência: "+(red?"vermelho":"azul"));}}
    private void leave(Player p){Room r=members.remove(p.getUniqueId());if(r==null){restore(p);return;}r.teams.remove(p.getUniqueId());r.alive.remove(p.getUniqueId());
        if(!p.isDead())restore(p);winner(r);if(r.teams.isEmpty()&&!r.ending)finish(r,"Sala encerrada.");}
    private void finish(Room r,String message){if(r.ending)return;r.ending=true;announce(r,message);
        for(UUID id:new ArrayList<>(r.teams.keySet())){members.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null&&!p.isDead())restore(p);}
        r.teams.clear();rooms.remove(r.id);World w=r.world;if(w==null)return;
        for(Player p:new ArrayList<>(w.getPlayers()))if(!p.isDead())teleport(p,lobby());
        // Delete only the ephemeral world created by this plugin, after a successful unload.
        if(w.getPlayers().isEmpty()&&Bukkit.unloadWorld(w,false)){
            File dir=w.getWorldFolder();if(new File(dir,"ryuex-modes-owned").isFile()){if(isEnabled())Bukkit.getScheduler().runTaskAsynchronously(this,()->delete(dir));else delete(dir);}
        }
    }
    private void delete(File f){if(java.nio.file.Files.isSymbolicLink(f.toPath()))return;File[] list=f.listFiles();if(list!=null)for(File child:list)delete(child);if(!f.delete())getLogger().warning("Não removido: "+f);}
    @EventHandler public void quit(PlayerQuitEvent e){leave(e.getPlayer());}
    @EventHandler public void login(PlayerJoinEvent e){Bukkit.getScheduler().runTask(this,()->restore(e.getPlayer()));}
    @EventHandler(priority=EventPriority.HIGH) public void travel(PlayerTeleportEvent e){if(members.containsKey(e.getPlayer().getUniqueId())&&!moving.contains(e.getPlayer().getUniqueId())){e.setCancelled(true);e.getPlayer().sendMessage("§eUse /jogos sair antes de teleportar.");}}
    @EventHandler(priority=EventPriority.HIGH) public void command(PlayerCommandPreprocessEvent e){if(!members.containsKey(e.getPlayer().getUniqueId()))return;String cmd=e.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);if(!Arrays.asList("/bridge","/hg","/jogos","/ryuexmodes:bridge","/ryuexmodes:hg","/ryuexmodes:jogos").contains(cmd)){e.setCancelled(true);e.getPlayer().sendMessage("§eUse /jogos sair antes de outros comandos.");}}
    @EventHandler(priority=EventPriority.HIGH) public void move(PlayerMoveEvent e){Player p=e.getPlayer();Room r=members.get(p.getUniqueId());if(r==null||e.getTo()==null||p.isDead())return;
        if(!r.running){if(e.getTo().distanceSquared(e.getFrom())>0&&(e.getTo().getY()<50||Math.abs(e.getTo().getX())>45||Math.abs(e.getTo().getZ())>18))teleport(p,r.hg?hgSpawn(r,0):bridgeSpawn(r,r.teams.get(p.getUniqueId())));return;}
        if(r.hg)return;Location l=e.getTo();int team=r.teams.get(p.getUniqueId());
        if(ModeRules.goalOwner(l.getX(),l.getY(),l.getZ())==1-team){
            if(team==0)r.red++;else r.blue++;announce(r,"§6GOL de "+p.getName()+"! §c"+r.red+" §f× §9"+r.blue);
            if(r.red==5||r.blue==5){finish(r,"§6Time "+(team==0?"vermelho":"azul")+" venceu!");return;}
            for(UUID id:r.teams.keySet()){Player q=Bukkit.getPlayer(id);if(q!=null)bridgeKit(q,r);}return;
        }if(l.getY()<45||Math.abs(l.getX())>48||Math.abs(l.getZ())>16||(l.getY()<64&&Math.abs(l.getX()-(team==0?-35.5:36.5))<2&&Math.abs(l.getZ()-.5)<2))bridgeKit(p,r);
    }
    private Player attacker(Entity entity){if(entity instanceof Player)return (Player)entity;if(entity instanceof Projectile&&((Projectile)entity).getShooter() instanceof Player)return (Player)((Projectile)entity).getShooter();return null;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(!(e.getEntity() instanceof Player))return;Player p=(Player)e.getEntity();Room r=members.get(p.getUniqueId());if(r==null)return;
        if(!ModeRules.canFight(r.running,r.hg,r.clock,r.alive.contains(p.getUniqueId()))){e.setCancelled(true);return;}
        if(e instanceof EntityDamageByEntityEvent){Player a=attacker(((EntityDamageByEntityEvent)e).getDamager());if(a!=null&&(members.get(a.getUniqueId())!=r||(r.hg&&!r.alive.contains(a.getUniqueId()))||(!r.hg&&Objects.equals(r.teams.get(a.getUniqueId()),r.teams.get(p.getUniqueId()))))){e.setCancelled(true);return;}}
        if(!r.hg&&(e.getCause()==EntityDamageEvent.DamageCause.FALL||e.getFinalDamage()>=p.getHealth())){e.setCancelled(true);if(e.getCause()!=EntityDamageEvent.DamageCause.FALL)bridgeKit(p,r);}
    }
    @EventHandler public void death(PlayerDeathEvent e){Player p=e.getEntity();Room r=members.get(p.getUniqueId());if(r==null)return;e.setDeathMessage(null);e.setDroppedExp(0);
        if(!r.hg){e.getDrops().clear();}else{r.alive.remove(p.getUniqueId());announce(r,"§c"+p.getName()+" foi eliminado. Restam "+r.alive.size());}
        Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline())p.spigot().respawn();},2);Bukkit.getScheduler().runTaskLater(this,()->winner(r),4);
    }
    @EventHandler public void respawn(PlayerRespawnEvent e){Player p=e.getPlayer();Room r=members.get(p.getUniqueId());
        if(r==null){if(snapshot(p.getUniqueId()).exists()){e.setRespawnLocation(lobby());Bukkit.getScheduler().runTask(this,()->restore(p));}return;}
        e.setRespawnLocation(r.hg?r.world.getSpawnLocation():bridgeSpawn(r,r.teams.get(p.getUniqueId())));
        Bukkit.getScheduler().runTask(this,()->{if(members.get(p.getUniqueId())!=r){restore(p);return;}if(r.hg){clean(p);p.setGameMode(GameMode.SPECTATOR);}else bridgeKit(p,r);});}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void place(BlockPlaceEvent e){Room r=members.get(e.getPlayer().getUniqueId());if(r==null)return;
        if(!r.running||(r.hg&&!r.alive.contains(e.getPlayer().getUniqueId()))){e.setCancelled(true);return;}
        if(!r.hg){Location l=e.getBlock().getLocation();if(Math.abs(l.getX())>=27||Math.abs(l.getZ())>8||l.getY()<60||l.getY()>76){e.setCancelled(true);return;}r.edits.put(l,e.getBlockReplacedState());}}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){Room r=members.get(e.getPlayer().getUniqueId());if(r==null)return;
        if(!r.running||(r.hg&&!r.alive.contains(e.getPlayer().getUniqueId()))){e.setCancelled(true);return;}
        if(!r.hg){if(!r.edits.containsKey(e.getBlock().getLocation()))e.setCancelled(true);else{e.setCancelled(true);e.getBlock().setType(Material.AIR);e.getPlayer().getInventory().addItem(new ItemStack(Material.STAINED_CLAY,1,(short)(r.teams.get(e.getPlayer().getUniqueId())==0?14:11)));}}}
    @EventHandler public void hunger(FoodLevelChangeEvent e){Room r=members.get(e.getEntity().getUniqueId());if(r!=null&&(!r.hg||!r.running))e.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent e){Room r=members.get(e.getPlayer().getUniqueId());if(r!=null&&(!r.hg||!r.running))e.setCancelled(true);}
    @EventHandler public void interact(PlayerInteractEvent e){Player p=e.getPlayer();Room r=members.get(p.getUniqueId());if(r==null)return;
        if(!r.running){e.setCancelled(true);return;}
        if(r.hg&&r.alive.contains(p.getUniqueId())&&e.hasItem()&&e.getItem().getType()==Material.MUSHROOM_SOUP&&e.getAction().name().startsWith("RIGHT_CLICK")&&p.getHealth()<p.getMaxHealth()){
            e.setCancelled(true);p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+7));p.setItemInHand(new ItemStack(Material.BOWL));}
    }
}
