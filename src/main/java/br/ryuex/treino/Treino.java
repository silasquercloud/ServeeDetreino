package br.ryuex.treino;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.block.Action;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
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
    private final PartyBook parties = new PartyBook();
    private final Map<UUID, Room> membership = new HashMap<>();
    private final Map<UUID, Mode> choosing = new HashMap<>();
    private final Map<UUID, MapArt.Theme> themes = new HashMap<>();
    private final Map<UUID,Integer> shopPages=new HashMap<>();
    private final Map<UUID,Long> slingCooldown=new HashMap<>();
    private TrainingStats stats;
    private int seconds;
    private final Map<UUID,List<String>> sidebarLines=new HashMap<>();
    private Location lobby;
    private int nextId = 1;
    enum Mode { BEDWARS, BEDFIGHT, SKYWARS }
    enum Phase { WAITING, COUNTDOWN, PLAYING, ENDING }
    private static final String MENU = "§8Treino • modos", SIZE = "§8Treino • vagas", THEME = "§8Treino • tema", LIST = "§8Treino • salas", RANK = "§8Treino • classificação", PARTY = "§8Treino • party", SHOP = "§8Treino • loja";
    private static final byte[] COLORS = {14,11,5,4,1,10,9,6,3,13,2,0};
    public static class VoidGenerator extends ChunkGenerator {
        @Override public byte[] generate(World w, Random r, int x, int z) { return new byte[32768]; }
    }
    private World voidWorld(String name) {
        World w = Bukkit.createWorld(new WorldCreator(name).generator(new VoidGenerator()));
        w.setAutoSave(false); w.setKeepSpawnInMemory(false); w.setDifficulty(Difficulty.NORMAL); w.setTime(6000);
        w.setGameRuleValue("doMobSpawning", "false"); w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doFireTick", "false"); w.setStorm(false);
        return w;
    }
    @Override public void onEnable() {
        saveDefaultConfig();stats=new TrainingStats(getDataFolder());
        World w = voidWorld("treino_lobby_multiverso_v1");
        build(w,MapArt.lobby(),0,80,0);
        lobby = new Location(w,0.5,81,0.5); w.setSpawnLocation(0,81,0);
        Bukkit.getPluginManager().registerEvents(this,this);
        new BukkitRunnable() { public void run() { tick(); } }.runTaskTimer(this,20,20);
        for(Player p:Bukkit.getOnlinePlayers()) hub(p);
        getLogger().info("TreinoHub ativo: /treino. API alvo: Spigot 1.8.8.");
    }
    @Override public void onDisable() {
        for(Player p:Bukkit.getOnlinePlayers()) { p.teleport(lobby); p.getInventory().clear(); }
        for(Room r:rooms.values()) removeWorld(r.world);
        saveStats();
    }
    private void removeWorld(World world) {
        if(!world.getName().startsWith("treino_room_")) return;
        java.io.File folder=world.getWorldFolder();
        if(!Bukkit.unloadWorld(world,false)) { getLogger().warning("Não foi possível descarregar "+world.getName()); return; }
        try(java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.walk(folder.toPath())) {
            for(java.nio.file.Path path:(Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator) java.nio.file.Files.deleteIfExists(path);
        } catch(java.io.IOException ex) { getLogger().warning("Falha ao limpar arena: "+ex.getMessage()); }
    }
    private void build(World world,MapArt.Scene scene,int dx,int dy,int dz) {
        for(MapArt.Cell cell:scene.cells()) {
            Block block=world.getBlockAt(cell.x+dx,cell.y+dy,cell.z+dz);
            block.setType(Material.valueOf(cell.material),false);
            if(cell.data!=0) block.setData((byte)cell.data,false);
        }
    }
    private void themeMenu(Player p,Mode mode) {
        choosing.put(p.getUniqueId(),mode);
        Inventory inv=Bukkit.createInventory(null,27,THEME);
        inv.setItem(10,item(Material.REDSTONE,1,"§cPokémon §7• Centro Pokémon e Pokébola"));
        inv.setItem(12,item(Material.EYE_OF_ENDER,1,"§5Evangelion §7• Tokyo-3 e EVA"));
        inv.setItem(14,item(Material.GOLD_INGOT,1,"§9Sonic §7• Green Hill e argolas"));
        inv.setItem(16,item(Material.BOAT,1,"§6One Piece §7• navio pirata"));
        p.openInventory(inv);
    }
    private void msg(Player p,String s) { p.sendMessage("§5§lTREINO §8» §f"+s); }
    private ItemStack item(Material m,int count,String title) {
        ItemStack i=new ItemStack(m,count); ItemMeta meta=i.getItemMeta(); meta.setDisplayName(title); i.setItemMeta(meta); return i;
    }
    private void reset(Player p,GameMode mode) {
        p.closeInventory(); if(p.getGameMode()==GameMode.SPECTATOR) p.setSpectatorTarget(null); p.setGameMode(mode); p.getInventory().clear(); p.getInventory().setArmorContents(null);
        p.setHealth(p.getMaxHealth()); p.setFoodLevel(20); p.setFireTicks(0); p.setFallDistance(0);
        p.setAllowFlight(mode==GameMode.SPECTATOR); p.setFlying(false);
        for(org.bukkit.potion.PotionEffect e:p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());sidebarLines.remove(p.getUniqueId());
    }
    private void hub(Player p) {
        reset(p,GameMode.ADVENTURE); p.teleport(lobby);
        stats.name(p.getUniqueId(),p.getName());lobbyItems(p);
        refreshVisibility();
        msg(p,"Use a bússola ou §d/treino§f. Crie uma sala e passe o número aos amigos.");
    }
    private void lobbyItems(Player p) {
        p.getInventory().setItem(0,item(Material.COMPASS,1,"§dSelecionar modo / salas"));
        p.getInventory().setItem(4,item(Material.NETHER_STAR,1,"§eClassificação / ranking"));
        p.getInventory().setItem(8,item(Material.NAME_TAG,1,"§dMinha party"));
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
            inv.setItem(slot++,item(Material.PAPER,1,"§d#"+r.id+" §f"+r.mode+" / "+r.theme.title+" §7("+r.players.size()+"/"+r.capacity+")"));
        }
        p.openInventory(inv);
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(!(sender instanceof Player)) { sender.sendMessage("Use este comando dentro do jogo."); return true; }
        Player p=(Player)sender;
        try {
            if(cmd.getName().equalsIgnoreCase("party")) {partyCommand(p,args);return true;}
            if(cmd.getName().equalsIgnoreCase("ranking")){ranking(p,args.length>0?Mode.valueOf(args[0].toUpperCase(Locale.ROOT)):Mode.BEDWARS);return true;}
            if(args.length>0 && args[0].equalsIgnoreCase("ranking")){ranking(p,args.length>1?Mode.valueOf(args[1].toUpperCase(Locale.ROOT)):Mode.BEDWARS);return true;}
            if(args.length==0) menu(p);
            else if(args[0].equalsIgnoreCase("criar") && (args.length==3 || args.length==4)) {
                Mode m=Mode.valueOf(args[1].toUpperCase(Locale.ROOT)); int n=Integer.parseInt(args[2]); create(p,m,n,args.length==4?MapArt.Theme.parse(args[3]):MapArt.Theme.POKEMON);
            } else if(args[0].equalsIgnoreCase("entrar") && args.length==2) enter(p,rooms.get(Integer.parseInt(args[1])));
            else if(args[0].equalsIgnoreCase("sair")) { leave(p); hub(p); }
            else if(args[0].equalsIgnoreCase("salas")) list(p);
            else if(args[0].equalsIgnoreCase("loja")) shop(p);
            else if(args[0].equalsIgnoreCase("iniciar")) {
                Room r=membership.get(p.getUniqueId());
                if(r!=null && r.owner.equals(p.getUniqueId()) && r.phase==Phase.WAITING && Rules.canStart(r.mode.name(),r.players.size())) { r.phase=Phase.COUNTDOWN; r.countdown=5; }
                else msg(p,"Só o dono pode iniciar; precisa de 2 jogadores e número par no BedFight.");
            } else msg(p,"/treino criar <bedwars|bedfight|skywars> <vagas> [pokemon|evangelion|sonic|onepiece] | entrar <sala> | sair | salas | iniciar | loja");
        } catch(IllegalArgumentException e) { msg(p,e instanceof NumberFormatException?"Número inválido. Confira o ID da sala ou as vagas.":e.getMessage()!=null && e.getMessage().startsWith("No enum constant")?"Modo/tema inválido. Use bedwars, bedfight ou skywars; pokemon, evangelion, sonic ou onepiece.":e.getMessage()); }
        return true;
    }
    private String playerName(UUID id) { Player p=Bukkit.getPlayer(id);return p==null?"desconectado":p.getName(); }
    private void partySay(UUID member,String text) { for(UUID id:parties.members(member)) { Player p=Bukkit.getPlayer(id);if(p!=null)msg(p,"§d[PARTY] §f"+text); } }
    private Player partyTarget(String value) {
        Player target=Bukkit.getPlayerExact(value);
        if(target==null)try {target=Bukkit.getPlayer(UUID.fromString(value));}catch(IllegalArgumentException ignored) { }
        if(target==null)throw new IllegalArgumentException("Jogador não encontrado ou desconectado.");
        return target;
    }
    private void partyCommand(Player p,String[] args) {
        UUID me=p.getUniqueId();
        if(args.length==0) {
            msg(p,"§d/party <nick> §fconvida • §d/party aceitar <nick> §f• §d/party lista");
            msg(p,"§d/party puxar [sala] §f• §d/party jogar <modo> [tema] §f• §d/party sair");
            msg(p,"§d/party expulsar <nick> §f• §d/party lider <nick> §f• §d/party dissolver §f• §d/party chat <texto>");return;
        }
        String action=args[0].toLowerCase(Locale.ROOT);
        if((action.equals("aceitar") || action.equals("accept")) && args.length==2) {
            Player leader=partyTarget(args[1]);parties.accept(me,leader.getUniqueId(),System.currentTimeMillis());partySay(me,p.getName()+" entrou na party!");return;
        }
        if((action.equals("recusar") || action.equals("deny")) && args.length==2) { Player leader=partyTarget(args[1]);parties.decline(me,leader.getUniqueId());msg(p,"Convite recusado.");return; }
        if(action.equals("lista") || action.equals("list")) {
            if(parties.get(me)==null) {msg(p,"Você ainda não está em uma party. Use /party <nick>.");return;}
            msg(p,"§dParty ("+parties.members(me).size()+"/12) §f• líder: "+playerName(parties.get(me).leader));
            for(UUID id:parties.members(me))msg(p,"§7• §f"+playerName(id));return;
        }
        if(action.equals("sair") || action.equals("leave")) { partySay(me,p.getName()+" saiu da party.");parties.leave(me);return; }
        if(action.equals("dissolver") || action.equals("disband")) {parties.requireLeader(me);partySay(me,"A party foi encerrada.");parties.disband(me);return;}
        if((action.equals("expulsar") || action.equals("kick")) && args.length==2) {Player target=partyTarget(args[1]);parties.kick(me,target.getUniqueId());msg(target,"Você foi removido da party.");partySay(me,target.getName()+" foi removido.");return;}
        if((action.equals("lider") || action.equals("promover")) && args.length==2) {Player target=partyTarget(args[1]);parties.promote(me,target.getUniqueId());partySay(me,target.getName()+" é o novo líder.");return;}
        if(action.equals("chat") && args.length>=2) {if(parties.get(me)==null)throw new IllegalArgumentException("Você não está em uma party.");partySay(me,p.getName()+": "+String.join(" ",Arrays.copyOfRange(args,1,args.length)));return;}
        if(action.equals("puxar") || action.equals("warp")) {
            parties.requireLeader(me);Room target=args.length==2?rooms.get(Integer.parseInt(args[1])):membership.get(me);
            gather(p,target,parties.members(me));return;
        }
        if(action.equals("jogar") && (args.length==2 || args.length==3)) {
            parties.requireLeader(me);int n=parties.members(me).size();
            if(n<2)throw new IllegalArgumentException("Convide pelo menos um amigo e aguarde ele aceitar.");
            create(p,Mode.valueOf(args[1].toUpperCase(Locale.ROOT)),n,args.length==3?MapArt.Theme.parse(args[2]):MapArt.Theme.POKEMON);return;
        }
        String nick=(action.equals("convidar") || action.equals("invite")) && args.length==2?args[1]:args.length==1?args[0]:null;
        if(nick==null)throw new IllegalArgumentException("Use /party para ver os comandos.");
        Player target=partyTarget(nick);parties.invite(me,target.getUniqueId(),System.currentTimeMillis());
        msg(p,"Convite enviado para §d"+target.getName()+"§f. Expira em 60 segundos.");
        msg(target,"§d"+p.getName()+"§f convidou você para uma party. Clique para responder:");
        target.spigot().sendMessage(PartyChat.invitation(me,p.getName()));
    }
    private List<UUID> travelling(Player leader) {
        PartyBook.Party party=parties.get(leader.getUniqueId());
        if(party==null)return Collections.singletonList(leader.getUniqueId());
        parties.requireLeader(leader.getUniqueId());return parties.members(leader.getUniqueId());
    }
    private void canGather(Room target,List<UUID> group,int capacity) {
        if(target!=null && target.phase!=Phase.WAITING && target.phase!=Phase.COUNTDOWN)throw new IllegalArgumentException("A sala já iniciou. Escolha uma sala em espera.");
        int present=0;
        for(UUID id:group) {
            if(Bukkit.getPlayer(id)==null)throw new IllegalArgumentException("Todos da party precisam estar conectados.");
            Room source=membership.get(id);
            if(source!=null && source!=target && source.phase!=Phase.WAITING && source.phase!=Phase.COUNTDOWN)throw new IllegalArgumentException(playerName(id)+" ainda está em uma partida. Aguarde o término.");
            if(source!=null && source==target)present++;
        }
        if(!PartyBook.fits(target==null?0:target.players.size(),present,group.size(),capacity))throw new IllegalArgumentException("Não há vagas para a party inteira. Ninguém foi movido.");
    }
    private void gather(Player leader,Room target,List<UUID> group) {
        if(target==null)throw new IllegalArgumentException("Entre numa sala em espera ou use /party puxar <sala>.");
        canGather(target,group,target.capacity);
        for(UUID id:group) {
            if(membership.get(id)==target)continue;
            Player member=Bukkit.getPlayer(id);leave(member);join(member,target);
        }
        if(parties.get(leader.getUniqueId())!=null)partySay(leader.getUniqueId(),"Grupo reunido na sala #"+target.id+". §7"+target.mode+" / "+target.theme.title);
    }
    private void enter(Player p,Room target) { gather(p,target,travelling(p)); }
    private void create(Player p,Mode mode,int n,MapArt.Theme theme) {
        List<UUID> group=travelling(p);
        canGather(null,group,n);
        if(!Rules.validCapacity(mode.name(),n)) { msg(p,"Vagas: SkyWars 2–12, BedWars 2–8, BedFight 2/4/6/8."); return; }
        if(rooms.size()>=getConfig().getInt("max-rooms",4)) { msg(p,"Limite de salas atingido. Aguarde uma terminar."); return; }
        int id=nextId++; Room r=new Room(id,mode,n,p.getUniqueId(),theme); rooms.put(id,r); gather(p,r,group);
        msg(p,"Sala §d#"+id+"§f criada. Amigos: §d/treino entrar "+id);
    }
    private void join(Player p,Room r) {
        if(membership.containsKey(p.getUniqueId())) { msg(p,"Use /treino sair antes de trocar de sala."); return; }
        if(r==null || (r.phase!=Phase.WAITING && r.phase!=Phase.COUNTDOWN) || r.players.size()>=r.capacity) { msg(p,"Sala indisponível."); return; }
        p.closeInventory(); r.players.add(p.getUniqueId()); membership.put(p.getUniqueId(),r);
        reset(p,GameMode.ADVENTURE); p.teleport(r.waiting());lobbyItems(p); refreshVisibility();
        r.say("§f"+p.getName()+" entrou §7("+r.players.size()+"/"+r.capacity+")");
        if(r.players.size()==r.capacity) { r.phase=Phase.COUNTDOWN; r.countdown=10; }
    }
    private void leave(Player p) {
        choosing.remove(p.getUniqueId()); themes.remove(p.getUniqueId()); Room r=membership.remove(p.getUniqueId()); if(r==null) return;
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
        parties.prune(System.currentTimeMillis());seconds++;
        if(seconds%30==0)saveStats();
        if(seconds%2==0)for(Player p:Bukkit.getOnlinePlayers()) {
            Room room=membership.get(p.getUniqueId());if(room==null || room.phase==Phase.WAITING || room.phase==Phase.COUNTDOWN)lobbyBoard(p,room);
        }
        for(Room r:new ArrayList<>(rooms.values())) {
            if(r.phase==Phase.COUNTDOWN) { if(r.countdown<=0) r.start(); else { r.say("§eIniciando em "+r.countdown+"s"); r.countdown--; } }
            if(r.phase!=Phase.PLAYING) continue;
            r.elapsed++;
            for(UUID u:new ArrayList<>(r.players)) { Player p=Bukkit.getPlayer(u); if(p!=null && !r.eliminated.contains(u) && !r.respawning.contains(u) && p.getLocation().getY()<40) knockOut(p,r); }
            if(r.mode==Mode.BEDWARS) {
                for(int t=0;t<r.spawns.size();t++) if(r.activeBase(t)) {
                    Location l=r.spawns.get(t).clone().add(0,0.2,-2);
                    if(r.elapsed%BedwarsItems.interval("IRON_INGOT",r.elapsed,r.forge.contains(t))==0) generate(l,Material.IRON_INGOT,48);
                    if(r.elapsed%BedwarsItems.interval("GOLD_INGOT",r.elapsed,r.forge.contains(t))==0) generate(l,Material.GOLD_INGOT,12);
                }
                int di=BedwarsItems.interval("DIAMOND",r.elapsed,false),em=BedwarsItems.interval("EMERALD",r.elapsed,false);
                if(r.elapsed%di==0) for(Location l:r.diamonds)generate(l,Material.DIAMOND,4);
                if(r.elapsed%em==0)generate(new Location(r.world,0.5,81,0.5),Material.EMERALD,4);
                if(r.elapsed==240 || r.elapsed==480)r.say("§bGeradores evoluíram para nível "+(r.elapsed==240?"II":"III")+"!");
                r.sentries();
            }
            if(r.mode==Mode.SKYWARS && r.elapsed==180) { r.fillChests(); r.say("§eBaús reabastecidos!"); }
            if(r.elapsed==getConfig().getInt("sudden-death-seconds",600)) {
                r.beds.replaceAll((k,v)->false); for(Block b:r.bedBlocks) b.setType(Material.AIR);
                r.say("§cMorte súbita! Camas destruídas e centro obrigatório.");
            }
            int limit=getConfig().getInt("sudden-death-seconds",600);
            if(r.elapsed>=limit) for(UUID u:new ArrayList<>(r.players)) { Player p=Bukkit.getPlayer(u); if(p!=null && !r.eliminated.contains(u) && p.getLocation().distanceSquared(new Location(r.world,0,81,0))>625) p.damage(2); }
            if(r.elapsed>=limit+300) finish(r,"Tempo esgotado: empate.");
            if(r.elapsed%2==0) {r.board();refreshVisibility();}
        }
    }
    private void saveStats() {try{stats.save();}catch(java.io.IOException error){getLogger().warning("Não foi possível salvar classificação: "+error.getMessage());}}
    private void sidebar(Player p,List<String> lines) {
        if(lines.equals(sidebarLines.get(p.getUniqueId())))return;
        org.bukkit.scoreboard.Scoreboard board=p.getScoreboard();org.bukkit.scoreboard.Objective objective=board.getObjective("treino");
        if(objective==null){board=Bukkit.getScoreboardManager().getNewScoreboard();objective=board.registerNewObjective("treino","dummy");objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);objective.setDisplayName("§5§lRYUEX TREINO");p.setScoreboard(board);}
        List<String> old=sidebarLines.get(p.getUniqueId());if(old!=null)for(String line:old)board.resetScores(line);
        List<String> actual=new ArrayList<>();int score=Math.min(15,lines.size());for(String line:lines){if(score<=0)break;String text=line.length()>38?line.substring(0,38):line;actual.add(text);objective.getScore(text).setScore(score--);}
        sidebarLines.put(p.getUniqueId(),actual);
    }
    private void partyLines(Player p,List<String> lines) {
        PartyBook.Party party=parties.get(p.getUniqueId());
        if(party==null){lines.add("§dParty: §7/party <nick>");return;}
        List<UUID> group=parties.members(p.getUniqueId());lines.add("§dParty §f"+group.size()+"/12");
        lines.add("§eLíder: §f"+playerName(party.leader));
        int maximum=Math.min(4,group.size());for(int i=0;i<maximum;i++) {UUID id=group.get(i);Room r=membership.get(id);lines.add("§7"+(i+1)+". §f"+playerName(id)+(r==null?" §8Lobby":" §8#"+r.id));}
        if(group.size()>maximum)lines.add("§7+"+(group.size()-maximum)+" • /party lista");
    }
    private void lobbyBoard(Player p,Room room) {
        List<String> lines=new ArrayList<>();lines.add("§fLobby Multiverso");
        lines.add("§7Online: §f"+Bukkit.getOnlinePlayers().size());
        lines.add("§7Vitórias BW: §a"+stats.get(p.getUniqueId(),"BEDWARS","wins"));
        lines.add("§7Camas BW: §c"+stats.get(p.getUniqueId(),"BEDWARS","beds"));
        if(room!=null)lines.add("§eSala #"+room.id+" "+room.players.size()+"/"+room.capacity);
        partyLines(p,lines);sidebar(p,lines);
    }
    private ItemStack description(ItemStack item,String... lines) {ItemMeta meta=item.getItemMeta();meta.setLore(Arrays.asList(lines));item.setItemMeta(meta);return item;}
    private void ranking(Player p,Mode mode) {
        Inventory inv=Bukkit.createInventory(null,54,RANK);int tab=0;for(Mode m:Mode.values())inv.setItem(tab++,item(m==Mode.SKYWARS?Material.ENDER_PEARL:Material.BED,1,(m==mode?"§a":"§e")+m.name()));
        List<UUID> top=stats.top(mode.name());int slot=9,place=1;for(UUID id:top) {if(slot>=45)break;if(stats.get(id,mode.name(),"games")==0)continue;
            inv.setItem(slot++,description(item(Material.PAPER,1,"§e#"+place+++" §f"+stats.name(id)),"§7Vitórias: §a"+stats.get(id,mode.name(),"wins"),"§7Abates: §f"+stats.get(id,mode.name(),"kills"),"§7Finais: §c"+stats.get(id,mode.name(),"final-kills"),"§7Camas: §d"+stats.get(id,mode.name(),"beds")));}
        UUID me=p.getUniqueId();inv.setItem(49,description(item(Material.BOOK,1,"§dSuas estatísticas • "+mode.name()),"§7Partidas: "+stats.get(me,mode.name(),"games"),"§7Vitórias: "+stats.get(me,mode.name(),"wins"),"§7Abates: "+stats.get(me,mode.name(),"kills"),"§7Camas: "+stats.get(me,mode.name(),"beds")));p.openInventory(inv);
    }
    private void partyMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,PARTY);int slot=0;
        for(UUID id:parties.members(p.getUniqueId())){Room r=membership.get(id);inv.setItem(slot++,description(item(Material.SKULL_ITEM,1,"§f"+playerName(id)),r==null?"§7No lobby":"§7Sala #"+r.id,"§7Convide com /party <nick>"));}
        inv.setItem(22,item(Material.ENDER_PEARL,1,"§dPuxar todos para sua sala • líder"));inv.setItem(26,item(Material.BARRIER,1,"§cSair da party"));p.openInventory(inv);
    }
    private void generate(Location location,Material material,int cap) {
        int count=0;for(Entity entity:location.getWorld().getNearbyEntities(location,2,2,2))if(entity instanceof Item) {
            ItemStack stack=((Item)entity).getItemStack();if(stack.getType()==material)count+=stack.getAmount();
        }
        if(count<cap)location.getWorld().dropItem(location,new ItemStack(material)).setVelocity(new Vector(0,0.05,0));
    }
    private void refreshVisibility() {
        for(Player viewer:Bukkit.getOnlinePlayers())for(Player target:Bukkit.getOnlinePlayers())if(viewer!=target) {
            Room a=membership.get(viewer.getUniqueId()),b=membership.get(target.getUniqueId());
            boolean viewSpectator=a!=null && (!a.alive(viewer.getUniqueId()) || a.respawning.contains(viewer.getUniqueId()));
            boolean targetSpectator=b!=null && (!b.alive(target.getUniqueId()) || b.respawning.contains(target.getUniqueId()));
            boolean same=viewer.getWorld().equals(target.getWorld()) && (a==b || viewer.getWorld().equals(lobby.getWorld()));
            if(same && (!targetSpectator || viewSpectator || b.phase!=Phase.PLAYING))viewer.showPlayer(target);else viewer.hidePlayer(target);
        }
    }
    private void repairVisibility(final Player player) {
        refreshVisibility();
        new BukkitRunnable(){public void run(){
            if(!player.isOnline())return;
            for(Player viewer:Bukkit.getOnlinePlayers())if(viewer!=player)viewer.hidePlayer(player);
            refreshVisibility();
        }}.runTaskLater(this,2);
        new BukkitRunnable(){public void run(){if(player.isOnline())refreshVisibility();}}.runTaskLater(this,20);
    }
    @EventHandler public void visibility(PlayerChangedWorldEvent e) { refreshVisibility(); }
    private boolean playing(Player p,Room r) {return r!=null && r.phase==Phase.PLAYING && r.alive(p.getUniqueId()) && !r.respawning.contains(p.getUniqueId()) && p.getWorld()==r.world;}
    private final class Room {
        final int id,capacity; final Mode mode; final MapArt.Theme theme; UUID owner; final World world;
        Phase phase=Phase.WAITING; int countdown,elapsed;
        final Set<UUID> players=new LinkedHashSet<>(),eliminated=new HashSet<>(),respawning=new HashSet<>();
        final Map<UUID,Integer> teams=new HashMap<>(); final Map<Integer,Boolean> beds=new HashMap<>();
        final List<Location> spawns=new ArrayList<>(); final List<Block> bedBlocks=new ArrayList<>();
        final List<Location> diamonds=new ArrayList<>();
        final Map<Integer,String> armor=new HashMap<>(),pickaxes=new HashMap<>(),axes=new HashMap<>();
        final Set<Integer> shears=new HashSet<>(),sharpness=new HashSet<>(),protection=new HashSet<>(),forge=new HashSet<>();
        final Map<UUID,UUID> lastAttack=new HashMap<>();final Map<UUID,Integer> lastHit=new HashMap<>();
        final Map<UUID,UUID> summoned=new HashMap<>(); final Map<UUID,Integer> expires=new HashMap<>();
        final Map<String,Integer> bedOwners=new HashMap<>(); final Set<String> placed=new HashSet<>(); final List<Block> chests=new ArrayList<>();
        Room(int id,Mode mode,int cap,UUID owner,MapArt.Theme theme) {
            this.id=id; this.mode=mode; this.capacity=cap; this.owner=owner; this.theme=theme;
            world=voidWorld("treino_room_"+System.currentTimeMillis()+"_"+id);

            build(world,MapArt.center(theme),0,80,0);
            for(int i=0;i<cap;i++) {
                double angle=2*Math.PI*i/cap; int x=(int)Math.round(Math.cos(angle)*42), z=(int)Math.round(Math.sin(angle)*42);
                build(world,MapArt.island(theme,8),x,80,z);
                spawns.add(new Location(world,x+0.5,81,z+0.5,(float)(angle*180/Math.PI+90),0)); beds.put(i,true);
                if(mode!=Mode.SKYWARS) {
                    Block foot=world.getBlockAt(x+2,81,z),head=world.getBlockAt(x+2,81,z+1);
                    foot.setType(Material.BED_BLOCK,false); head.setType(Material.BED_BLOCK,false); foot.setData((byte)0,false); head.setData((byte)8,false);
                    bedBlocks.add(foot); bedBlocks.add(head); bedOwners.put(key(foot),i); bedOwners.put(key(head),i);
                    Block shop=world.getBlockAt(x-2,81,z); shop.setType(Material.WORKBENCH);
                    for(int dx=-1;dx<=1;dx++) { Block b=world.getBlockAt(x+dx,80,z); b.setType(Material.WOOL); b.setData(COLORS[i]); }
                } else { chest(x+2,81,z); chest(x-2,81,z); }
            }
            if(mode==Mode.BEDWARS) for(int i=0;i<4;i++) {
                int[] pos=MapArt.diamondPosition(i); build(world,MapArt.diamondOutpost(theme),pos[0],80,pos[1]);
                diamonds.add(new Location(world,pos[0]+0.5,81,pos[1]+0.5));
            }
            if(mode==Mode.SKYWARS) { chest(0,81,0); chest(3,81,3); chest(-3,81,-3); fillChests(); }
        }
        Location waiting() { return phase==Phase.WAITING || phase==Phase.COUNTDOWN?lobby.clone():new Location(world,0.5,95,0.5); }
        void say(String s) { for(UUID u:players) { Player p=Bukkit.getPlayer(u); if(p!=null) msg(p,s); } }
        void chest(int x,int y,int z) { Block b=world.getBlockAt(x,y,z); b.setType(Material.CHEST); chests.add(b); }
        void fillChests() {
            for(Block b:chests) { Inventory inv=((Chest)b.getState()).getBlockInventory(); inv.clear();
                inv.addItem(new ItemStack(Material.STONE_SWORD),new ItemStack(Material.BOW),new ItemStack(Material.ARROW,12),new ItemStack(Material.COOKED_BEEF,8),new ItemStack(Material.WOOD,48),new ItemStack(Material.SNOW_BALL,8),new ItemStack(Material.IRON_CHESTPLATE),new ItemStack(Material.IRON_HELMET));
                if(b.getX()*b.getX()+b.getZ()*b.getZ()<100) inv.addItem(new ItemStack(Material.DIAMOND_SWORD),new ItemStack(Material.GOLDEN_APPLE,2),new ItemStack(Material.ENDER_PEARL));
            }
        }
        void start() {
            if(!Rules.canStart(mode.name(),players.size())) { phase=Phase.WAITING; return; }
            phase=Phase.PLAYING; int i=0;
            for(UUID id:players)stats.add(id,mode.name(),"games");
            for(UUID u:players) { teams.put(u,i++); Player p=Bukkit.getPlayer(u); if(p!=null) spawn(p); }
            say("§aValendo! §f"+(mode==Mode.BEDFIGHT?"Duelo contra a ilha oposta do seu par. Sem loja, kit pronto.":mode==Mode.BEDWARS?"Proteja sua cama. Clique na bancada para abrir a loja.":"Equipe-se nos baús e seja o último vivo."));
            refreshVisibility(); board();
        }
        void spawn(Player p) {
            reset(p,GameMode.SURVIVAL); int t=teams.get(p.getUniqueId()); p.teleport(spawns.get(t)); p.setNoDamageTicks(60);
            if(mode!=Mode.SKYWARS) {
                p.getInventory().addItem(new ItemStack(Material.WOOD_SWORD),new ItemStack(Material.WOOD_PICKAXE),new ItemStack(Material.COOKED_BEEF,16));
                p.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                p.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                p.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                if(mode==Mode.BEDWARS) {
                    p.getInventory().remove(Material.WOOD_PICKAXE);
                    if(shears.contains(t))p.getInventory().addItem(new ItemStack(Material.SHEARS));
                    if(pickaxes.containsKey(t))p.getInventory().addItem(new ItemStack(Material.valueOf(pickaxes.get(t))));
                    if(axes.containsKey(t))p.getInventory().addItem(new ItemStack(Material.valueOf(axes.get(t))));
                }
                equipment(p);
                if(mode==Mode.BEDFIGHT) p.getInventory().addItem(new ItemStack(Material.WOOL,64,(short)COLORS[t]),new ItemStack(Material.WOOL,64,(short)COLORS[t]),new ItemStack(Material.SHEARS));
            }
            repairVisibility(p);
        }
        boolean activeBase(int team) { for(UUID id:players)if(teams.get(id)==team && alive(id))return true;return false; }
        void sentries() {
            for(UUID id:new ArrayList<>(summoned.keySet())) {
                Entity entity=null;for(Entity candidate:world.getEntities())if(candidate.getUniqueId().equals(id)){entity=candidate;break;}
                if(entity==null || entity.isDead()) {summoned.remove(id);expires.remove(id);continue;}
                UUID ownerId=summoned.get(id);
                if(elapsed>=expires.get(id) || !alive(ownerId)) {entity.remove();summoned.remove(id);expires.remove(id);continue;}
                if(entity instanceof IronGolem) {
                    Player target=null;double distance=256;
                    for(UUID playerId:players)if(!playerId.equals(ownerId) && alive(playerId) && !respawning.contains(playerId)) {
                        Player candidate=Bukkit.getPlayer(playerId);if(candidate==null || candidate.getWorld()!=world)continue;
                        double d=candidate.getLocation().distanceSquared(entity.getLocation());if(d<distance){distance=d;target=candidate;}
                    }
                    ((IronGolem)entity).setTarget(target);
                }
            }
        }
        void equipment(Player p) {
            int t=teams.get(p.getUniqueId());
            String material=armor.get(t);
            if(material!=null) {p.getInventory().setLeggings(new ItemStack(Material.valueOf(material+"_LEGGINGS")));p.getInventory().setBoots(new ItemStack(Material.valueOf(material+"_BOOTS")));}
            if(protection.contains(t)){ItemStack[] pieces=p.getInventory().getArmorContents();for(ItemStack piece:pieces)if(piece!=null)piece.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,1);p.getInventory().setArmorContents(pieces);}
            if(sharpness.contains(t))for(ItemStack weapon:p.getInventory().getContents())if(weapon!=null && weapon.getType().name().endsWith("_SWORD"))weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,1);
        }
        boolean alive(UUID u) { return players.contains(u) && !eliminated.contains(u); }
        void resolveDuel(int t) {
            int opponent=Rules.opponent(t);
            boolean remaining=false; for(UUID u:players) if(teams.get(u)==t && alive(u)) remaining=true;
            if(!remaining) for(UUID u:players) if(teams.get(u)==opponent && alive(u)) {
                eliminated.add(u);stats.add(u,mode.name(),"wins"); Player p=Bukkit.getPlayer(u); if(p!=null) { msg(p,"§aVocê venceu seu duelo!"); reset(p,GameMode.SPECTATOR); p.teleport(waiting()); }
            }
        }
        void checkWinner() {
            List<UUID> live=new ArrayList<>(); for(UUID u:players) if(alive(u)) live.add(u);
            if(mode==Mode.BEDFIGHT) { if(live.isEmpty()) finish(this,"Todos os duelos terminaram!"); }
            else if(live.size()<=1 && phase==Phase.PLAYING) {if(!live.isEmpty())stats.add(live.get(0),mode.name(),"wins"); Player win=live.isEmpty()?null:Bukkit.getPlayer(live.get(0)); finish(this,win==null?"Empate!":win.getName()+" venceu!"); }
        }
        void board() {
            int live=0;for(UUID u:players)if(alive(u))live++;
            for(UUID u:players){Player p=Bukkit.getPlayer(u);if(p==null)continue;
                List<String> lines=new ArrayList<>();lines.add("§f"+mode+" §7#"+id);lines.add("§7"+theme.title);lines.add("§7Vivos: §f"+live+" §8| §7"+elapsed+"s");
                Integer t=teams.get(u);if(mode!=Mode.SKYWARS)lines.add("§7Sua cama: "+(Boolean.TRUE.equals(beds.get(t))?"§a✔":"§c✘"));
                if(mode==Mode.BEDWARS) {int next=elapsed<240?240:elapsed<480?480:Math.max(elapsed,getConfig().getInt("sudden-death-seconds",600));lines.add("§bEvento em "+Math.max(0,next-elapsed)+"s");}
                partyLines(p,lines);sidebar(p,lines);
            }
        }

    }
    private static String key(Block b) { return b.getX()+":"+b.getY()+":"+b.getZ(); }
    private void knockOut(final Player p,final Room r) {
        UUID u=p.getUniqueId(); if(r.phase!=Phase.PLAYING || !r.alive(u) || r.respawning.contains(u)) return;
        int team=r.teams.get(u); boolean respawn=r.mode!=Mode.SKYWARS && Boolean.TRUE.equals(r.beds.get(team));
        r.respawning.add(u);
        UUID killer=r.lastAttack.remove(u);Integer hit=r.lastHit.remove(u);
        stats.add(u,r.mode.name(),"deaths");
        if(killer!=null && !killer.equals(u) && hit!=null && r.elapsed-hit<=15) {stats.add(killer,r.mode.name(),"kills");if(!respawn)stats.add(killer,r.mode.name(),"final-kills");}
        for(ItemStack i:p.getInventory().getContents()) if(i!=null && (i.getType()==Material.IRON_INGOT || i.getType()==Material.GOLD_INGOT || i.getType()==Material.EMERALD || i.getType()==Material.DIAMOND)) r.world.dropItemNaturally(p.getLocation(),i);
        reset(p,GameMode.SPECTATOR); p.teleport(r.waiting()); refreshVisibility();
        if(respawn) {
            msg(p,"§eRenascendo em 3 segundos...");
            new BukkitRunnable(){ public void run(){ r.respawning.remove(p.getUniqueId()); if(membership.get(p.getUniqueId())==r && r.phase==Phase.PLAYING && r.alive(p.getUniqueId())) r.spawn(p); }}.runTaskLater(this,60);
        } else { r.respawning.remove(u); r.eliminated.add(u); r.say("§c"+p.getName()+" foi eliminado!"); if(r.mode==Mode.BEDFIGHT) r.resolveDuel(team); r.checkWinner(); }
    }
    private void shop(Player p) {shop(p,8);}
    private void shop(Player p,int category) {
        Room r=membership.get(p.getUniqueId());if(!playing(p,r) || r.mode!=Mode.BEDWARS)return;
        shopPages.put(p.getUniqueId(),category);Inventory inv=Bukkit.createInventory(null,54,SHOP);
        for(int n=0;n<BedwarsItems.CATEGORIES.length;n++)inv.setItem(n,item(Material.valueOf(BedwarsItems.ICONS[n]),1,(n==category?"§a":"§e")+BedwarsItems.CATEGORIES[n]));
        inv.setItem(8,item(Material.NETHER_STAR,1,category==8?"§aCompra rápida":"§eCompra rápida"));
        for(int filler=9;filler<18;filler++) {ItemStack pane=item(Material.STAINED_GLASS_PANE,1,"§8•");pane.setDurability((short)7);inv.setItem(filler,pane);}
        int slot=18;for(BedwarsItems.Entry entry:shopEntries(p,category)) {
            ItemStack icon=description(item(Material.valueOf(entry.material),entry.amount,"§e"+entry.name),"§7Custo: §f"+entry.cost+" "+BedwarsItems.currencyName(entry.currency),"§aClique para comprar","§dShift + clique: favoritar/remover");
            icon.setDurability((short)entry.data);inv.setItem(slot++,icon);
        }
        p.openInventory(inv);
    }
    private List<BedwarsItems.Entry> shopEntries(Player p,int category) {
        if(category!=8)return BedwarsItems.category(category);
        List<String> favorites=stats.favorites(p.getUniqueId());List<BedwarsItems.Entry> result=new ArrayList<>();
        for(BedwarsItems.Entry entry:BedwarsItems.ALL)if(favorites.contains(BedwarsItems.key(entry)))result.add(entry);return result;
    }
    private void purchase(Player p,Room r,int slot,boolean shift) {
        if(slot>=0 && slot<9){shop(p,slot);return;}
        List<BedwarsItems.Entry> entries=shopEntries(p,shopPages.getOrDefault(p.getUniqueId(),8));
        int index=slot-18;if(index<0 || index>=entries.size())return;
        BedwarsItems.Entry entry=entries.get(index);int team=r.teams.get(p.getUniqueId());String action=entry.action;
        if(shift){stats.toggleFavorite(p.getUniqueId(),BedwarsItems.key(entry));shop(p,shopPages.getOrDefault(p.getUniqueId(),8));return;}
        Set<Integer> upgrade=action.equals("sharpness")?r.sharpness:action.equals("protection")?r.protection:action.equals("forge")?r.forge:action.equals("shears")?r.shears:null;
        Map<Integer,String> tools=action.equals("pickaxe")?r.pickaxes:action.equals("axe")?r.axes:null;
        if(upgrade!=null && upgrade.contains(team)){msg(p,"Você já possui essa melhoria.");return;}
        if(action.equals("armor") && r.armor.containsKey(team) && BedwarsItems.tier(r.armor.get(team))>=BedwarsItems.tier(entry.material)) {msg(p,"Você já possui armadura igual ou melhor.");return;}
        if(tools!=null && tools.containsKey(team) && BedwarsItems.tier(tools.get(team))>=BedwarsItems.tier(entry.material)){msg(p,"Você já possui ferramenta igual ou melhor.");return;}
        boolean inventory=upgrade==null && !action.equals("armor") || action.equals("shears");
        if(inventory && p.getInventory().firstEmpty()==-1){msg(p,"Libere um espaço no inventário.");return;}
        Material currency=Material.valueOf(entry.currency);if(!p.getInventory().contains(currency,entry.cost)){msg(p,"Recursos insuficientes.");return;}
        p.getInventory().removeItem(new ItemStack(currency,entry.cost));
        if(upgrade!=null)upgrade.add(team);
        if(action.equals("armor"))r.armor.put(team,entry.material.substring(0,entry.material.indexOf('_')));
        else if(inventory) {
            ItemStack bought=new ItemStack(Material.valueOf(entry.material),entry.amount,(short)entry.data);
            if(bought.getType()==Material.WOOL || bought.getType()==Material.STAINED_GLASS)bought.setDurability(COLORS[team]);
            if(action.equals("knockback"))bought.addUnsafeEnchantment(Enchantment.KNOCKBACK,1);
            if(tools!=null){String old=tools.put(team,entry.material);if(old!=null)p.getInventory().remove(Material.valueOf(old));}
            p.getInventory().addItem(bought);
        }
        r.equipment(p);msg(p,"§aCompra concluída: "+entry.name);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        try {handleClick(e);}catch(IllegalArgumentException error) {
            if(e.getWhoClicked() instanceof Player)msg((Player)e.getWhoClicked(),error.getMessage());
        }
    }
    private void handleClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p=(Player)e.getWhoClicked(); String title=e.getView().getTitle();
        if(!Arrays.asList(MENU,THEME,SIZE,LIST,SHOP,RANK,PARTY).contains(title)) return;
        e.setCancelled(true); if(e.getRawSlot()<0 || e.getRawSlot()>=e.getView().getTopInventory().getSize()) return;
        ItemStack i=e.getCurrentItem(); if(i==null || i.getType()==Material.AIR) return;
        if(title.equals(RANK)){if(e.getRawSlot()<3)ranking(p,Mode.values()[e.getRawSlot()]);return;}
        if(title.equals(PARTY)){if(e.getRawSlot()==22){p.closeInventory();partyCommand(p,new String[]{"puxar"});}else if(e.getRawSlot()==26){p.closeInventory();partyCommand(p,new String[]{"sair"});}return;}
        if(title.equals(MENU)) { if(e.getRawSlot()==22) list(p); else if(e.getRawSlot()==10) themeMenu(p,Mode.BEDWARS); else if(e.getRawSlot()==13) themeMenu(p,Mode.BEDFIGHT); else if(e.getRawSlot()==16) themeMenu(p,Mode.SKYWARS); }
        else if(title.equals(THEME)) {
            Mode m=choosing.get(p.getUniqueId()); if(m==null) return;
            int slot=e.getRawSlot(); MapArt.Theme t=slot==10?MapArt.Theme.POKEMON:slot==12?MapArt.Theme.EVANGELION:slot==14?MapArt.Theme.SONIC:slot==16?MapArt.Theme.ONEPIECE:null;
            if(t!=null) { themes.put(p.getUniqueId(),t); sizeMenu(p,m); }
        }
        else if(title.equals(SIZE)) { Mode m=choosing.remove(p.getUniqueId()); MapArt.Theme t=themes.remove(p.getUniqueId()); if(m!=null && t!=null) { p.closeInventory(); create(p,m,i.getAmount(),t); } }
        else if(title.equals(LIST)) enter(p,rooms.get(Integer.parseInt(ChatColor.stripColor(i.getItemMeta().getDisplayName()).split(" ")[0].substring(1))));
        else {
            Room r=membership.get(p.getUniqueId()); if(r==null || r.phase!=Phase.PLAYING || r.mode!=Mode.BEDWARS || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) return;
            purchase(p,r,e.getRawSlot(),e.isShiftClick());
        }
    }
    @EventHandler public void drag(InventoryDragEvent e) { if(Arrays.asList(MENU,THEME,SIZE,LIST,SHOP,RANK,PARTY).contains(e.getView().getTitle())) e.setCancelled(true); }
    @EventHandler public void interact(PlayerInteractEvent e) {
        Player p=e.getPlayer(); Room r=membership.get(p.getUniqueId());
        if(p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(e.hasItem() && (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK)) {
            if(e.getItem().getType()==Material.COMPASS)menu(p);else if(e.getItem().getType()==Material.NETHER_STAR)ranking(p,Mode.BEDWARS);else if(e.getItem().getType()==Material.NAME_TAG)partyMenu(p);
        } return; }
        if(r!=null && !playing(p,r)) { e.setCancelled(true); return; }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.WORKBENCH) { if(e.getAction()==Action.RIGHT_CLICK_BLOCK){e.setCancelled(true);shop(p);}return; }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.BED_BLOCK && e.getAction()==Action.RIGHT_CLICK_BLOCK) {e.setCancelled(true);return;}
        if(p.getOpenInventory()!=null && SHOP.equals(p.getOpenInventory().getTitle())){e.setCancelled(true);return;}
        if(playing(p,r) && r.mode==Mode.BEDWARS && (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) && e.hasItem()) {
            if(e.hasBlock() && (e.getClickedBlock().getState() instanceof InventoryHolder))return;
            Material type=e.getItem().getType();
            if(type==Material.FIREBALL) {
                e.setCancelled(true);consume(p);Fireball ball=p.launchProjectile(Fireball.class);ball.setIsIncendiary(false);ball.setYield(2.5f);ball.setVelocity(p.getLocation().getDirection().multiply(1.2));
                ball.setMetadata("treino-owner",new FixedMetadataValue(this,p.getUniqueId().toString()));
            } else if(type==Material.MAGMA_CREAM) {
                e.setCancelled(true);long now=System.currentTimeMillis();if(slingCooldown.getOrDefault(p.getUniqueId(),0L)>now){msg(p,"Aguarde o estilingue recarregar.");return;}
                consume(p);slingCooldown.put(p.getUniqueId(),now+3000);Vector boost=p.getLocation().getDirection().multiply(1.7);boost.setY(Math.max(0.65,Math.min(1.2,boost.getY())));p.setVelocity(boost);p.setFallDistance(0);
            } else if(type==Material.MONSTER_EGG) {
                e.setCancelled(true);if(!e.hasBlock() || e.getBlockFace()!=org.bukkit.block.BlockFace.UP){msg(p,"Use em cima de um bloco sólido.");return;}
                int count=0;for(UUID owner:r.summoned.values())if(owner.equals(p.getUniqueId()))count++;
                if(count>=2){msg(p,"Limite: 2 golems por jogador.");return;}
                Location at=e.getClickedBlock().getLocation().add(0.5,1,0.5);if(Math.abs(at.getX())>65 || Math.abs(at.getZ())>65 || at.getY()>100)return;
                if(at.getBlock().getType()!=Material.AIR || at.clone().add(0,1,0).getBlock().getType()!=Material.AIR){msg(p,"Libere espaço para o golem.");return;}
                IronGolem golem=(IronGolem)r.world.spawnEntity(at,EntityType.IRON_GOLEM);golem.setPlayerCreated(true);golem.setCustomName("§aDefensor de "+p.getName());
                r.summoned.put(golem.getUniqueId(),p.getUniqueId());r.expires.put(golem.getUniqueId(),r.elapsed+45);consume(p);
            }
        }
    }
    @EventHandler public void place(BlockPlaceEvent e) {
        Room r=membership.get(e.getPlayer().getUniqueId());
        if(r==null) { if(e.getBlock().getWorld()==lobby.getWorld()) e.setCancelled(true); return; }
        if(!playing(e.getPlayer(),r) || e.getBlock().getY()<60 || e.getBlock().getY()>100 || Math.abs(e.getBlock().getX())>65 || Math.abs(e.getBlock().getZ())>65) { e.setCancelled(true); return; }
        if(r.mode==Mode.BEDWARS) {
            List<Location> protectedGenerators=new ArrayList<>(r.diamonds);protectedGenerators.add(new Location(r.world,0.5,81,0.5));
            for(Location l:r.spawns)protectedGenerators.add(l.clone().add(0,0,-2));
            for(Location l:protectedGenerators)if(e.getBlock().getLocation().add(0.5,0,0.5).distanceSquared(l)<2.5){e.setCancelled(true);msg(e.getPlayer(),"Não bloqueie o gerador.");return;}
        }
        if(r.mode==Mode.BEDWARS && e.getBlock().getType()==Material.TNT) {
            e.getBlock().setType(Material.AIR);TNTPrimed tnt=(TNTPrimed)r.world.spawnEntity(e.getBlock().getLocation().add(0.5,0,0.5),EntityType.PRIMED_TNT);
            tnt.setFuseTicks(60);tnt.setMetadata("treino-owner",new FixedMetadataValue(this,e.getPlayer().getUniqueId().toString()));return;
        }
        r.placed.add(key(e.getBlock()));
    }
    @EventHandler public void breakBlock(BlockBreakEvent e) {
        Player p=e.getPlayer(); Room r=membership.get(p.getUniqueId()); Block b=e.getBlock();
        if(r==null) { if(b.getWorld()==lobby.getWorld()) e.setCancelled(true); return; }
        e.setCancelled(true); if(!playing(p,r)) return;
        Integer target=r.bedOwners.get(key(b));
        if(target!=null && Boolean.TRUE.equals(r.beds.get(target))) {
            int own=r.teams.get(p.getUniqueId()); if(own==target) { msg(p,"Essa é sua cama!"); return; }
            if(!BedwarsItems.canBreakBed(true,true,false,own,target,r.mode==Mode.BEDFIGHT)) { msg(p,"Ataque apenas o oponente do seu par."); return; }
            stats.add(p.getUniqueId(),r.mode.name(),"beds");r.beds.put(target,false); for(Block bed:r.bedBlocks) if(r.bedOwners.get(key(bed)).equals(target)) bed.setType(Material.AIR);
            r.say("§c"+p.getName()+" destruiu a cama da base "+(target+1)+"!"); return;
        }
        if(r.placed.remove(key(b))) e.setCancelled(false);
    }
    @EventHandler(ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player)) return; Player p=(Player)e.getEntity(); Room r=membership.get(p.getUniqueId());
        if(r==null) { if(p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(p.getLocation().getY()<50) p.teleport(lobby); } return; }
        if(!playing(p,r)) { e.setCancelled(true); if(e.getCause()==EntityDamageEvent.DamageCause.VOID) p.teleport(r.waiting()); return; }
        if(e instanceof EntityDamageByEntityEvent) {
            Entity attacker=((EntityDamageByEntityEvent)e).getDamager();
            UUID defender=r.summoned.get(attacker.getUniqueId());if(defender!=null && defender.equals(p.getUniqueId())){e.setCancelled(true);return;}
            Player source=attacker instanceof Player?(Player)attacker:attacker instanceof Projectile && ((Projectile)attacker).getShooter() instanceof Player?(Player)((Projectile)attacker).getShooter():null;
            if(source!=null && (membership.get(source.getUniqueId())!=r || !r.alive(source.getUniqueId()) || r.respawning.contains(source.getUniqueId()) || (r.mode==Mode.BEDFIGHT && !Rules.sameDuel(r.teams.get(source.getUniqueId()),r.teams.get(p.getUniqueId()))))) { e.setCancelled(true); return; }
            UUID credited=source!=null?source.getUniqueId():defender;
            if(credited==null && attacker.hasMetadata("treino-owner"))try {credited=UUID.fromString(attacker.getMetadata("treino-owner").get(0).asString());}catch(IllegalArgumentException ignored){}
            if(credited!=null && !credited.equals(p.getUniqueId()) && r.players.contains(credited) && e.getFinalDamage()>0) {r.lastAttack.put(p.getUniqueId(),credited);r.lastHit.put(p.getUniqueId(),r.elapsed);}
        }
        if(e.getFinalDamage()>=p.getHealth() || e.getCause()==EntityDamageEvent.DamageCause.VOID) { e.setCancelled(true); knockOut(p,r); }
    }
    @EventHandler public void mobDeath(EntityDeathEvent e) {if(roomWorld(e.getEntity().getWorld())!=null && !(e.getEntity() instanceof Player)){e.getDrops().clear();e.setDroppedExp(0);}}
    @EventHandler public void death(PlayerDeathEvent e) { Room r=membership.get(e.getEntity().getUniqueId()); if(r!=null) { e.getDrops().clear(); e.setDroppedExp(0); } }
    @EventHandler public void respawn(PlayerRespawnEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); e.setRespawnLocation(r==null?lobby:r.waiting()); if(r!=null) new BukkitRunnable(){public void run(){knockOut(e.getPlayer(),r);}}.runTaskLater(this,1); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        UUID id=e.getPlayer().getUniqueId();
        if(parties.get(id)!=null)partySay(id,e.getPlayer().getName()+" desconectou e saiu da party.");
        parties.leave(id);leave(e.getPlayer());sidebarLines.remove(id);shopPages.remove(id);slingCooldown.remove(id);
    }
    @EventHandler public void join(PlayerJoinEvent e) { hub(e.getPlayer());repairVisibility(e.getPlayer()); }
    @EventHandler public void food(FoodLevelChangeEvent e) { if(e.getEntity() instanceof Player && e.getEntity().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    private void consume(Player p) {ItemStack item=p.getItemInHand();if(item.getAmount()<=1)p.setItemInHand(null);else {item.setAmount(item.getAmount()-1);p.setItemInHand(item);}}
    private Room roomWorld(World world) {for(Room r:rooms.values())if(r.world==world)return r;return null;}
    @EventHandler public void target(EntityTargetEvent e) {
        Room r=roomWorld(e.getEntity().getWorld());if(r==null || !r.summoned.containsKey(e.getEntity().getUniqueId()))return;
        if(!(e.getTarget() instanceof Player)){if(e.getTarget()!=null)e.setCancelled(true);return;}
        Player p=(Player)e.getTarget();if(!playing(p,r) || p.getUniqueId().equals(r.summoned.get(e.getEntity().getUniqueId())))e.setCancelled(true);
    }
    @EventHandler public void golemDamage(EntityDamageByEntityEvent e) {
        Room r=roomWorld(e.getDamager().getWorld());if(r==null)return;
        UUID owner=r.summoned.get(e.getDamager().getUniqueId());
        if(owner!=null && (!(e.getEntity() instanceof Player) || owner.equals(e.getEntity().getUniqueId()) || !playing((Player)e.getEntity(),r)))e.setCancelled(true);
        if(r.summoned.containsKey(e.getEntity().getUniqueId())) {
            if(!(e.getDamager() instanceof Player) || !playing((Player)e.getDamager(),r) || r.summoned.get(e.getEntity().getUniqueId()).equals(e.getDamager().getUniqueId()))e.setCancelled(true);
        }
    }
    @EventHandler public void explosion(EntityExplodeEvent e) {
        if(!e.getLocation().getWorld().getName().startsWith("treino_"))return;
        Room r=roomWorld(e.getLocation().getWorld());
        if(r==null || r.phase!=Phase.PLAYING || r.mode!=Mode.BEDWARS || !e.getEntity().hasMetadata("treino-owner")){e.setCancelled(true);return;}
        e.setYield(0);Iterator<Block> blocks=e.blockList().iterator();while(blocks.hasNext()) {
            Block b=blocks.next();Material m=b.getType();
            boolean resistant=m==Material.OBSIDIAN || m==Material.STAINED_GLASS || m==Material.GLASS || (e.getEntity() instanceof Fireball && m==Material.ENDER_STONE);
            if(!r.placed.contains(key(b)) || resistant)blocks.remove();else r.placed.remove(key(b));
        }
    }
    @EventHandler public void fluid(BlockFromToEvent e) {
        Room r=roomWorld(e.getBlock().getWorld());if(r!=null && (e.getToBlock().getY()<60 || Math.abs(e.getToBlock().getX())>65 || Math.abs(e.getToBlock().getZ())>65))e.setCancelled(true);
    }
    @EventHandler public void bucket(PlayerBucketEmptyEvent e) {Room r=membership.get(e.getPlayer().getUniqueId());if(!playing(e.getPlayer(),r) || r.mode!=Mode.BEDWARS)e.setCancelled(true);}
    @EventHandler public void bucketFill(PlayerBucketFillEvent e) {Room r=membership.get(e.getPlayer().getUniqueId());if(!playing(e.getPlayer(),r) || r.mode!=Mode.BEDWARS)e.setCancelled(true);}
    @EventHandler public void burn(BlockBurnEvent e) { if(e.getBlock().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    @EventHandler public void drop(PlayerDropItemEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); if(e.getPlayer().getWorld()==lobby.getWorld() || (r!=null && (r.phase!=Phase.PLAYING || !r.alive(e.getPlayer().getUniqueId()) || r.respawning.contains(e.getPlayer().getUniqueId())))) e.setCancelled(true); }
}
