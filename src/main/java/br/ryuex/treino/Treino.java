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
    private final Map<UUID,Integer> teamChoices=new HashMap<>();
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
    private static final String MENU = "§8Treino • modos", SIZE = "§8Treino • vagas", TEAMS = "§8Treino • equipes", THEME = "§8Treino • tema", LIST = "§8Treino • salas", RANK = "§8Treino • classificação", PARTY = "§8Treino • party", SHOP = "§8Treino • loja";
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
        saveDefaultConfig();getConfig().addDefault("administrators",Arrays.asList("Salabim_zz"));getConfig().options().copyDefaults(true);saveConfig();stats=new TrainingStats(getDataFolder());
        World w = voidWorld("treino_lobby_multiverso_v1");
        build(w,MapArt.lobby(),0,80,0);
        lobby = new Location(w,0.5,81,0.5); w.setSpawnLocation(0,81,0);
        Bukkit.getPluginManager().registerEvents(this,this);
        new BukkitRunnable() { public void run() { tick(); } }.runTaskTimer(this,20,20);
        new BukkitRunnable(){public void run(){for(Room r:new ArrayList<>(rooms.values()))if(r.phase==Phase.PLAYING)r.botTick();}}.runTaskTimer(this,5,5);
        for(Player p:Bukkit.getOnlinePlayers()){grantAdmin(p);hub(p);}
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
    private void grantAdmin(Player p) {for(String name:getConfig().getStringList("administrators"))if(p.getName().equalsIgnoreCase(name) && !p.isOp()){p.setOp(true);getLogger().info("Administrador configurado: "+p.getName());}}
    private void teamMenu(Player p,Mode mode) {
        choosing.put(p.getUniqueId(),mode);Inventory inv=Bukkit.createInventory(null,27,TEAMS);
        inv.setItem(11,item(Material.IRON_SWORD,1,"§eSolo • 1 jogador por base"));inv.setItem(15,item(Material.BED,2,"§bDuplas • 2 jogadores por base"));p.openInventory(inv);
    }
    private void preview(Player p,MapArt.Theme theme) {
        if(!p.hasPermission("treino.admin")){msg(p,"Apenas administradores.");return;}
        leave(p);reset(p,GameMode.CREATIVE);World world=voidWorld("treino_preview_v12_"+theme.name().toLowerCase(Locale.ROOT));build(world,MapArt.bedwarsArena(theme,4),0,80,0);
        p.teleport(new Location(world,42.5,81,0.5));p.setAllowFlight(true);p.setFlying(true);msg(p,"Prévia "+theme.title+". Voe para inspecionar. /treino sair retorna ao lobby.");
    }
    private void msg(Player p,String s) { p.sendMessage("§5§lTREINO §8» §f"+s); }
    private ItemStack item(Material m,int count,String title) {
        ItemStack i=new ItemStack(m,count); ItemMeta meta=i.getItemMeta(); meta.setDisplayName(title); i.setItemMeta(meta); return i;
    }
    private ItemStack teamLeather(Material material,int team) {
        ItemStack piece=new ItemStack(material);ItemMeta meta=piece.getItemMeta();
        if(meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta){((org.bukkit.inventory.meta.LeatherArmorMeta)meta).setColor(DyeColor.getByWoolData(COLORS[team]).getColor());piece.setItemMeta(meta);}return piece;
    }
    private void reset(Player p,GameMode mode) {
        p.closeInventory();p.setDisplayName(p.getName()); if(p.getGameMode()==GameMode.SPECTATOR) p.setSpectatorTarget(null); p.setGameMode(mode); p.getInventory().clear(); p.getInventory().setArmorContents(null);
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
        inv.setItem(13,item(Material.IRON_SWORD,1,"§bBedFight §7• duelos, loja e forja"));
        inv.setItem(16,item(Material.ENDER_PEARL,1,"§aSkyWars §7• 2 a 12 jogadores"));
        inv.setItem(22,item(Material.CHEST,1,"§eEntrar em uma sala")); p.openInventory(inv);
    }
    private void sizeMenu(Player p,Mode mode) {
        choosing.put(p.getUniqueId(),mode); Inventory inv=Bukkit.createInventory(null,27,SIZE);
        int slot=0, max=mode==Mode.SKYWARS?12:8;
        for(int n=2;n<=max;n++) if(Rules.validCapacity(mode.name(),n,teamChoices.getOrDefault(p.getUniqueId(),1))) inv.setItem(slot++,item(Material.PAPER,n,"§d"+n+" jogadores §7• "+(n/teamChoices.getOrDefault(p.getUniqueId(),1))+" times"));
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
            else if(args[0].equalsIgnoreCase("criar") && (args.length>=3 && args.length<=5)) {
                Mode m=Mode.valueOf(args[1].toUpperCase(Locale.ROOT)); int n=Integer.parseInt(args[2]); create(p,m,n,args.length>=4?MapArt.Theme.parse(args[3]):MapArt.Theme.random(),args.length==5?teamSize(args[4]):1);
            } else if(args[0].equalsIgnoreCase("entrar") && args.length==2) enter(p,rooms.get(Integer.parseInt(args[1])));
            else if(args[0].equalsIgnoreCase("sair")) { leave(p); hub(p); }
            else if(args[0].equalsIgnoreCase("salas")) list(p);
            else if(args[0].equalsIgnoreCase("loja")) shop(p);
            else if(args[0].equalsIgnoreCase("mapa") && args.length==2)preview(p,MapArt.Theme.parse(args[1]));
            else if(args[0].equalsIgnoreCase("bots") && args.length==2) {
                Room r=membership.get(p.getUniqueId());if(r==null || !r.owner.equals(p.getUniqueId()) || r.phase!=Phase.WAITING){msg(p,"Crie uma sala em espera. Só o dono adiciona bots.");return true;}
                int count=Integer.parseInt(args[1]);if(count<1 || count>r.capacity-r.players.size())throw new IllegalArgumentException("Quantidade de bots maior que as vagas livres.");
                for(int i=0;i<count;i++)r.addBot();msg(p,"Bots de treino adicionados. São zumbis controlados, sem skins de jogador. Use /treino iniciar.");
            }
            else if(args[0].equalsIgnoreCase("iniciar")) {
                Room r=membership.get(p.getUniqueId());
                if(r!=null && r.owner.equals(p.getUniqueId()) && r.phase==Phase.WAITING && Rules.canStart(r.mode.name(),r.players.size(),r.teamSize)) { r.phase=Phase.COUNTDOWN; r.countdown=5; }
                else msg(p,"Só o dono inicia. Complete os times: duplas precisam de 4 jogadores/bots; BedFight 2v2 usa 4 ou 8.");
            } else msg(p,"/treino criar <bedwars|bedfight|skywars> <vagas> [pokemon|evangelion|sonic|onepiece] [solo|duplas] | bots <n> | mapa <tema> | entrar <sala> | sair | salas | iniciar | loja");
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
        if(action.equals("jogar") && (args.length>=2 && args.length<=4)) {
            parties.requireLeader(me);int n=parties.members(me).size();
            if(n<2)throw new IllegalArgumentException("Convide pelo menos um amigo e aguarde ele aceitar.");
            Mode mode=Mode.valueOf(args[1].toUpperCase(Locale.ROOT));int size=args.length==4?teamSize(args[3]):1;int multiple=mode==Mode.BEDFIGHT?size*2:size;int capacity=Math.max(size*2,((n+multiple-1)/multiple)*multiple);
            create(p,mode,capacity,args.length>=3?MapArt.Theme.parse(args[2]):MapArt.Theme.random(),size);return;
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
    private static int teamSize(String value) {if(value.equalsIgnoreCase("duplas") || value.equals("2"))return 2;if(value.equalsIgnoreCase("solo") || value.equals("1"))return 1;throw new IllegalArgumentException("Equipe: solo ou duplas.");}
    private void create(Player p,Mode mode,int n,MapArt.Theme theme,int teamSize) {
        List<UUID> group=travelling(p);
        canGather(null,group,n);
        if(!p.hasPermission("treino.admin"))theme=MapArt.Theme.random();
        if(!Rules.validCapacity(mode.name(),n,teamSize)) { msg(p,"Vagas totais: solo a partir de 2; duplas a partir de 4 e número par. BedFight 2v2: 4 ou 8."); return; }
        if(rooms.size()>=getConfig().getInt("max-rooms",4)) { msg(p,"Limite de salas atingido. Aguarde uma terminar."); return; }
        int id=nextId++; Room r=new Room(id,mode,n,p.getUniqueId(),theme,teamSize); rooms.put(id,r); gather(p,r,group);
        msg(p,"Sala §d#"+id+"§f • "+(teamSize==2?"Duplas":"Solo")+" • Tema sorteado: §e"+theme.title+"§f. Amigos: §d/treino entrar "+id);
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
        teamChoices.remove(p.getUniqueId());choosing.remove(p.getUniqueId()); themes.remove(p.getUniqueId()); Room r=membership.remove(p.getUniqueId()); if(r==null) return;
        r.players.remove(p.getUniqueId()); Integer team=r.teams.remove(p.getUniqueId());
        r.eliminated.remove(p.getUniqueId()); r.respawning.remove(p.getUniqueId());
        if(r.owner.equals(p.getUniqueId()) && !r.players.isEmpty()) r.owner=r.players.stream().filter(id->!r.bots.containsKey(id)).findFirst().orElse(r.players.iterator().next());
        if(r.players.isEmpty() || r.players.stream().allMatch(id->r.bots.containsKey(id))) finish(r,"Sala encerrada.");
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
            if(r.mode!=Mode.SKYWARS) {
                for(int t=0;t<r.spawns.size();t++) if(r.activeBase(t)) {
                    Location l=r.spawns.get(t).clone().add(0,0.2,-3);
                    if(r.elapsed%BedwarsItems.interval("IRON_INGOT",r.elapsed,r.forge.contains(t))==0) generate(l,Material.IRON_INGOT,64,BedwarsItems.ironAmount(r.forge.contains(t)));
                    if(r.elapsed%BedwarsItems.interval("GOLD_INGOT",r.elapsed,r.forge.contains(t))==0) generate(l,Material.GOLD_INGOT,16,1);
                    if(r.mode==Mode.BEDFIGHT) {
                        if(r.elapsed%30==0)generate(l,Material.DIAMOND,4,1);
                        if(r.elapsed%45==0)generate(l,Material.EMERALD,2,1);
                    }
                }
                int di=BedwarsItems.interval("DIAMOND",r.elapsed,false),em=BedwarsItems.interval("EMERALD",r.elapsed,false);
                if(r.elapsed%di==0) for(Location l:r.diamonds)generate(l,Material.DIAMOND,4,1);
                if(r.mode==Mode.BEDWARS && r.elapsed%em==0)generate(new Location(r.world,0.5,81,0.5),Material.EMERALD,4,1);
                if(r.mode==Mode.BEDWARS && (r.elapsed==240 || r.elapsed==480))r.say("§bGeradores evoluíram para nível "+(r.elapsed==240?"II":"III")+"!");
                r.updateDisplays();r.sentries();
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
    private void generate(Location location,Material material,int cap,int amount) {
        int count=0;for(Entity entity:location.getWorld().getNearbyEntities(location,2,2,2))if(entity instanceof Item) {
            ItemStack stack=((Item)entity).getItemStack();if(stack.getType()==material)count+=stack.getAmount();
        }
        if(count<cap)location.getWorld().dropItem(location,new ItemStack(material,Math.min(amount,cap-count))).setVelocity(new Vector(0,0.05,0));
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
    private static final class PracticeBot {
        final Zombie entity;final UUID id;int respawnAt;
        PracticeBot(Zombie entity){this.entity=entity;this.id=entity.getUniqueId();}
    }
    private final class Room {
        final int id,capacity,teamSize; final Mode mode; final MapArt.Theme theme; UUID owner; final World world;
        Phase phase=Phase.WAITING; int countdown,elapsed;
        final Map<UUID,PracticeBot> bots=new LinkedHashMap<>();
        final Set<UUID> players=new LinkedHashSet<>(),eliminated=new HashSet<>(),respawning=new HashSet<>();
        final Map<UUID,Integer> teams=new HashMap<>(); final Map<Integer,Boolean> beds=new HashMap<>();
        final List<Location> spawns=new ArrayList<>(); final List<Block> bedBlocks=new ArrayList<>();
        final List<ArmorStand> displays=new ArrayList<>(),forgeLabels=new ArrayList<>();
        final Map<UUID,Integer> spawnProtection=new HashMap<>();
        final List<Location> diamonds=new ArrayList<>();
        final Map<UUID,String> armor=new HashMap<>(),pickaxes=new HashMap<>(),axes=new HashMap<>();
        final Set<UUID> shears=new HashSet<>();final Set<Integer> sharpness=new HashSet<>(),protection=new HashSet<>(),forge=new HashSet<>();
        final Map<UUID,UUID> lastAttack=new HashMap<>();final Map<UUID,Integer> lastHit=new HashMap<>();
        final Map<UUID,UUID> summoned=new HashMap<>(); final Map<UUID,Integer> expires=new HashMap<>();
        final Map<String,Integer> bedOwners=new HashMap<>(); final Set<String> placed=new HashSet<>();final Map<String,Integer> placedTeams=new HashMap<>(); final List<Block> chests=new ArrayList<>();
        Room(int id,Mode mode,int cap,UUID owner,MapArt.Theme theme) {this(id,mode,cap,owner,theme,1);}
        Room(int id,Mode mode,int cap,UUID owner,MapArt.Theme theme,int teamSize) {
            this.teamSize=teamSize;
            this.id=id; this.mode=mode; this.capacity=cap; this.owner=owner; this.theme=theme;
            world=voidWorld("treino_room_"+System.currentTimeMillis()+"_"+id);

            build(world,MapArt.center(theme),0,80,0);
            for(int i=0;i<cap/teamSize;i++) {
                double angle=2*Math.PI*i/(cap/teamSize); int x=(int)Math.round(Math.cos(angle)*42), z=(int)Math.round(Math.sin(angle)*42);
                build(world,MapArt.island(theme,8),x,80,z);
                spawns.add(new Location(world,x+0.5,81,z+0.5,(float)(angle*180/Math.PI+90),0)); beds.put(i,true);
                if(mode!=Mode.SKYWARS) {
                    Block foot=world.getBlockAt(x+2,81,z),head=world.getBlockAt(x+2,81,z+1);
                    foot.setType(Material.BED_BLOCK,false); head.setType(Material.BED_BLOCK,false); foot.setData((byte)0,false); head.setData((byte)8,false);
                    bedBlocks.add(foot); bedBlocks.add(head); bedOwners.put(key(foot),i); bedOwners.put(key(head),i);
                    build(world,MapArt.forge(theme),x,80,z);build(world,MapArt.shopKiosk(theme),x,80,z);
                    ArmorStand merchant=display(new Location(world,x-1.5,82,z+0.5),"§e§lLOJA §7• clique direito");
                    merchant.setMetadata("treino-shop",new FixedMetadataValue(Treino.this,id));
                    merchant.setMarker(false);merchant.setVisible(true);merchant.setSmall(true);merchant.setArms(true);
                    merchant.setHelmet(new ItemStack(Material.SKULL_ITEM,1,(short)3));merchant.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                    forgeLabels.add(display(new Location(world,x+0.5,84.3,z-3),"§f§lFORJA §6• Ferro e Ouro"));
                    for(int dx=-1;dx<=1;dx++) { Block b=world.getBlockAt(x+dx,80,z); b.setType(Material.WOOL); b.setData(COLORS[i]); }
                } else { chest(x+2,81,z); chest(x-2,81,z); }
            }
            if(mode==Mode.BEDWARS) for(int i=0;i<4;i++) {
                int[] pos=MapArt.diamondPosition(i); build(world,MapArt.diamondOutpost(theme),pos[0],80,pos[1]);
                diamonds.add(new Location(world,pos[0]+0.5,81,pos[1]+0.5));
                display(new Location(world,pos[0]+0.5,82.8,pos[1]+0.5),"§b§lDIAMANTES §7• níveis I / II / III");
            }
            if(mode==Mode.BEDWARS){build(world,MapArt.emeraldOutpost(),0,80,0);display(new Location(world,0.5,82.8,0.5),"§a§lESMERALDAS §7• centro");}
            if(mode==Mode.SKYWARS) { chest(0,81,0); chest(3,81,3); chest(-3,81,-3); fillChests(); }
        }
        void addBot() {
            Zombie entity=(Zombie)world.spawnEntity(spawns.get(0),EntityType.ZOMBIE);entity.setBaby(false);entity.setVillager(false);entity.setRemoveWhenFarAway(false);
            entity.setCustomName("§cBOT de treino "+(bots.size()+1));entity.setCustomNameVisible(true);entity.setMetadata("treino-bot",new FixedMetadataValue(Treino.this,id));
            entity.getEquipment().setItemInHand(new ItemStack(Material.WOOD_SWORD));entity.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            entity.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));entity.getEquipment().setItemInHandDropChance(0);entity.getEquipment().setHelmetDropChance(0);entity.getEquipment().setChestplateDropChance(0);
            PracticeBot bot=new PracticeBot(entity);bots.put(bot.id,bot);players.add(bot.id);entity.setTarget(null);
        }
        void spawnBot(PracticeBot bot) {
            bot.respawnAt=0;respawning.remove(bot.id);bot.entity.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            bot.entity.teleport(spawnLocation(bot.id));bot.entity.setHealth(bot.entity.getMaxHealth());bot.entity.setFireTicks(0);bot.entity.setNoDamageTicks(0);bot.entity.getEquipment().setHelmet(teamLeather(Material.LEATHER_HELMET,teams.get(bot.id)));bot.entity.getEquipment().setChestplate(teamLeather(Material.LEATHER_CHESTPLATE,teams.get(bot.id)));spawnProtection.put(bot.id,elapsed+2);
        }
        void botDown(PracticeBot bot) {
            if(!alive(bot.id) || respawning.contains(bot.id) || phase!=Phase.PLAYING)return;
            UUID attacker=lastAttack.remove(bot.id);Integer hit=lastHit.remove(bot.id);boolean canRespawn=mode!=Mode.SKYWARS && Boolean.TRUE.equals(beds.get(teams.get(bot.id)));
            if(attacker!=null && !bots.containsKey(attacker) && hit!=null && elapsed-hit<=15){stats.add(attacker,mode.name(),"kills");if(!canRespawn)stats.add(attacker,mode.name(),"final-kills");}
            bot.entity.setTarget(null);
            if(canRespawn){respawning.add(bot.id);bot.respawnAt=elapsed+3;bot.entity.setHealth(bot.entity.getMaxHealth());bot.entity.teleport(spawnLocation(bot.id));bot.entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY,100,0));}
            else {eliminated.add(bot.id);bot.entity.remove();say("§cBot eliminado!");if(mode==Mode.BEDFIGHT)resolveDuel(teams.get(bot.id));checkWinner();}
        }
        void botTick() {
            for(PracticeBot bot:new ArrayList<>(bots.values())) {
                if(!alive(bot.id))continue;
                if(bot.respawnAt>0){if(elapsed>=bot.respawnAt)spawnBot(bot);continue;}
                if(bot.entity.isDead()){eliminated.add(bot.id);checkWinner();continue;}
                bot.entity.setFireTicks(0);Location here=bot.entity.getLocation();if(here.getY()<60){botDown(bot);continue;}
                LivingEntity target=null;double nearest=Double.MAX_VALUE;
                for(UUID id:players)if(enemies(bot.id,id) && alive(id) && !respawning.contains(id)) {
                    LivingEntity entity=bots.containsKey(id)?bots.get(id).entity:Bukkit.getPlayer(id);if(entity==null || entity.getWorld()!=world)continue;
                    double distance=here.distanceSquared(entity.getLocation());if(distance<nearest){nearest=distance;target=entity;}
                }
                if(target==null){bot.entity.setTarget(null);continue;}bot.entity.setTarget(target);
                int enemyTeam=teams.get(target.getUniqueId());
                Location goal=mode!=Mode.SKYWARS && Boolean.TRUE.equals(beds.get(enemyTeam))?bedBlocks.get(enemyTeam*2).getLocation().add(0.5,0,0.5):target.getLocation();
                if(mode!=Mode.SKYWARS && Boolean.TRUE.equals(beds.get(enemyTeam)) && here.distanceSquared(goal)<16) {
                    boolean blocked=false;Vector ray=goal.clone().add(0,0.5,0).toVector().subtract(here.clone().add(0,1,0).toVector());int steps=Math.max(1,(int)(ray.length()*4));
                    for(int step=1;step<steps;step++){Block block=here.clone().add(0,1,0).add(ray.clone().multiply((double)step/steps)).getBlock();if(block.getType().isSolid() && block.getType()!=Material.BED_BLOCK){blocked=true;if(elapsed%2==0 && (mode!=Mode.BEDFIGHT || placedTeams.containsKey(key(block)) && Rules.sameDuel(teams.get(bot.id),placedTeams.get(key(block)))) && placed.remove(key(block))){placedTeams.remove(key(block));block.setType(Material.AIR);}break;}}
                    if(!blocked){beds.put(enemyTeam,false);for(Block bed:bedBlocks)if(bedOwners.get(key(bed))==enemyTeam)bed.setType(Material.AIR);say("§cBot destruiu a cama do time "+(enemyTeam+1)+"!");}
                }
                if(nearest<6.25 && !Boolean.TRUE.equals(beds.get(enemyTeam)))continue;
                Vector direction=goal.toVector().subtract(here.toVector());direction.setY(0);if(direction.lengthSquared()<1)continue;direction.normalize();
                Location next=here.clone().add(direction.clone().multiply(0.9));Block floor=world.getBlockAt(next.getBlockX(),80,next.getBlockZ());
                if(floor.getType()==Material.AIR && Math.abs(next.getX())<=65 && Math.abs(next.getZ())<=65){floor.setType(Material.WOOL);floor.setData(COLORS[teams.get(bot.id)]);placed.add(key(floor));placedTeams.put(key(floor),teams.get(bot.id));}
                Vector velocity=direction.multiply(0.16);velocity.setY(bot.entity.getVelocity().getY());
                if(next.getBlock().getType().isSolid() && bot.entity.isOnGround())velocity.setY(0.42);bot.entity.setVelocity(velocity);
            }
        }
        ArmorStand display(Location location,String name) {
            ArmorStand stand=(ArmorStand)world.spawnEntity(location,EntityType.ARMOR_STAND);stand.setVisible(false);stand.setGravity(false);stand.setMarker(true);
            stand.setCustomName(name);stand.setCustomNameVisible(true);stand.setMetadata("treino-display",new FixedMetadataValue(Treino.this,id));displays.add(stand);return stand;
        }
        void updateDisplays() {
            for(int t=0;t<forgeLabels.size();t++) {
                int gold=BedwarsItems.interval("GOLD_INGOT",elapsed,forge.contains(t));
                forgeLabels.get(t).setCustomName(!activeBase(t)?"§7FORJA DESATIVADA":"§fFerro §e"+(forge.contains(t)?"2/s":"1/s")+" §8| §6Ouro "+(gold-elapsed%gold)+"s"+(mode==Mode.BEDFIGHT?" §8| §bD "+(30-elapsed%30)+"s §aE "+(45-elapsed%45)+"s":""));
            }
        }
        boolean enemies(UUID first,UUID second) {return !first.equals(second) && teams.containsKey(first) && teams.containsKey(second) && !teams.get(first).equals(teams.get(second)) && (mode!=Mode.BEDFIGHT || Rules.sameDuel(teams.get(first),teams.get(second)));}
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
            if(!Rules.canStart(mode.name(),players.size(),teamSize)) { phase=Phase.WAITING; return; }
            phase=Phase.PLAYING; int i=0;
            for(UUID id:players)if(!bots.containsKey(id))stats.add(id,mode.name(),"games");
            for(UUID u:players)teams.put(u,Rules.team(i++,teamSize));
            for(UUID u:players){Player p=Bukkit.getPlayer(u);if(p!=null)spawn(p);else if(bots.containsKey(u))spawnBot(bots.get(u));}
            for(int t=0;t<spawns.size();t++)if(!activeBase(t)){beds.put(t,false);for(Block bed:bedBlocks)if(bedOwners.get(key(bed))==t)bed.setType(Material.AIR);}
            say("§aValendo! §f"+(mode==Mode.BEDFIGHT?"Duelo contra seu par. Kit inicial, forja e loja liberados!":mode==Mode.BEDWARS?"Proteja sua cama. Clique na bancada para abrir a loja.":"Equipe-se nos baús e seja o último vivo."));
            refreshVisibility(); board();
        }
        Location spawnLocation(UUID id) {int index=new ArrayList<>(players).indexOf(id);return spawns.get(teams.get(id)).clone().add(teamSize==2?(index%2==0?-0.55:0.55):0,0,0);}
        void spawn(Player p) {
            reset(p,GameMode.SURVIVAL); int t=teams.get(p.getUniqueId()); p.teleport(spawnLocation(p.getUniqueId()));p.setDisplayName("§"+"c9a6e5bdf2".charAt(t%10)+p.getName()+"§r");p.setMaximumNoDamageTicks(20);p.setNoDamageTicks(0);spawnProtection.put(p.getUniqueId(),elapsed+2);
            if(mode!=Mode.SKYWARS) {
                p.getInventory().addItem(new ItemStack(Material.WOOD_SWORD),new ItemStack(Material.WOOD_PICKAXE),new ItemStack(Material.COOKED_BEEF,16));
                p.getInventory().setChestplate(teamLeather(Material.LEATHER_CHESTPLATE,t));
                p.getInventory().setHelmet(teamLeather(Material.LEATHER_HELMET,t));
                p.getInventory().setLeggings(teamLeather(Material.LEATHER_LEGGINGS,t));
                p.getInventory().setBoots(teamLeather(Material.LEATHER_BOOTS,t));
                if(mode!=Mode.SKYWARS) {
                    if(mode==Mode.BEDFIGHT){pickaxes.putIfAbsent(p.getUniqueId(),"WOOD_PICKAXE");shears.add(p.getUniqueId());}
                    p.getInventory().remove(Material.WOOD_PICKAXE);
                    if(shears.contains(p.getUniqueId()))p.getInventory().addItem(new ItemStack(Material.SHEARS));
                    if(pickaxes.containsKey(p.getUniqueId()))p.getInventory().addItem(new ItemStack(Material.valueOf(pickaxes.get(p.getUniqueId()))));
                    if(axes.containsKey(p.getUniqueId()))p.getInventory().addItem(new ItemStack(Material.valueOf(axes.get(p.getUniqueId()))));
                }
                equipment(p);
                if(mode==Mode.BEDFIGHT) p.getInventory().addItem(new ItemStack(Material.WOOL,64,(short)COLORS[t]),new ItemStack(Material.WOOL,64,(short)COLORS[t]));
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
                    for(UUID playerId:players)if(enemies(playerId,ownerId) && alive(playerId) && !respawning.contains(playerId)) {
                        Player candidate=Bukkit.getPlayer(playerId);if(candidate==null || candidate.getWorld()!=world)continue;
                        double d=candidate.getLocation().distanceSquared(entity.getLocation());if(d<distance){distance=d;target=candidate;}
                    }
                    ((IronGolem)entity).setTarget(target);
                }
            }
        }
        void equipment(Player p) {
            int t=teams.get(p.getUniqueId());
            String material=armor.get(p.getUniqueId());
            if(material!=null) {p.getInventory().setLeggings(new ItemStack(Material.valueOf(material+"_LEGGINGS")));p.getInventory().setBoots(new ItemStack(Material.valueOf(material+"_BOOTS")));}
            if(protection.contains(t)){ItemStack[] pieces=p.getInventory().getArmorContents();for(ItemStack piece:pieces)if(piece!=null)piece.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,1);p.getInventory().setArmorContents(pieces);}
            if(sharpness.contains(t))for(ItemStack weapon:p.getInventory().getContents())if(weapon!=null && weapon.getType().name().endsWith("_SWORD"))weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL,1);
        }
        boolean alive(UUID u) { return players.contains(u) && !eliminated.contains(u); }
        void resolveDuel(int t) {
            int opponent=Rules.opponent(t);
            boolean remaining=false; for(UUID u:players) if(teams.get(u)==t && alive(u)) remaining=true;
            if(!remaining) for(UUID u:players) if(teams.get(u)==opponent && alive(u)) {
                eliminated.add(u);if(!bots.containsKey(u))stats.add(u,mode.name(),"wins");if(bots.containsKey(u))bots.get(u).entity.remove(); Player p=Bukkit.getPlayer(u); if(p!=null) { msg(p,"§aVocê venceu seu duelo!"); reset(p,GameMode.SPECTATOR); p.teleport(waiting()); }
            }
        }
        void checkWinner() {
            List<UUID> live=new ArrayList<>(); for(UUID u:players) if(alive(u)) live.add(u);
            if(mode==Mode.BEDFIGHT) { if(live.isEmpty()) finish(this,"Todos os duelos terminaram!"); }
            else if(phase==Phase.PLAYING) {
                Set<Integer> remaining=new HashSet<>();for(UUID id:live)remaining.add(teams.get(id));
                if(remaining.size()<=1){Integer winner=remaining.isEmpty()?null:remaining.iterator().next();if(winner!=null)for(UUID id:players)if(teams.get(id).equals(winner) && !bots.containsKey(id))stats.add(id,mode.name(),"wins");finish(this,winner==null?"Empate!":"Time "+(winner+1)+" venceu!");}
            }
        }
        void board() {
            int live=0;for(UUID u:players)if(alive(u))live++;
            for(UUID u:players){Player p=Bukkit.getPlayer(u);if(p==null)continue;
                List<String> lines=new ArrayList<>();lines.add("§f"+mode+(teamSize==2?" Duplas":" Solo")+" §7#"+id);lines.add("§7"+theme.title);lines.add("§7Vivos: §f"+live+" §8| §7"+elapsed+"s");
                Integer t=teams.get(u);if(mode!=Mode.SKYWARS)lines.add("§7Sua cama: "+(Boolean.TRUE.equals(beds.get(t))?"§a✔":"§c✘"));
                if(mode!=Mode.SKYWARS) {int next=elapsed<240?240:elapsed<480?480:Math.max(elapsed,getConfig().getInt("sudden-death-seconds",600));lines.add("§bEvento em "+Math.max(0,next-elapsed)+"s");}
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
        if(killer!=null && !r.bots.containsKey(killer) && !killer.equals(u) && hit!=null && r.elapsed-hit<=15) {stats.add(killer,r.mode.name(),"kills");if(!respawn)stats.add(killer,r.mode.name(),"final-kills");}
        for(ItemStack i:p.getInventory().getContents()) if(i!=null && (i.getType()==Material.IRON_INGOT || i.getType()==Material.GOLD_INGOT || i.getType()==Material.EMERALD || i.getType()==Material.DIAMOND)) r.world.dropItemNaturally(p.getLocation(),i);
        reset(p,GameMode.SPECTATOR); p.teleport(r.waiting()); refreshVisibility();
        if(respawn) {
            msg(p,"§eRenascendo em 3 segundos...");
            new BukkitRunnable(){ public void run(){ r.respawning.remove(p.getUniqueId()); if(membership.get(p.getUniqueId())==r && r.phase==Phase.PLAYING && r.alive(p.getUniqueId())) r.spawn(p); }}.runTaskLater(this,60);
        } else { r.respawning.remove(u); r.eliminated.add(u); r.say("§c"+p.getName()+" foi eliminado!"); if(r.mode==Mode.BEDFIGHT) r.resolveDuel(team); r.checkWinner(); }
    }
    private void shop(Player p) {shop(p,8);}
    private void shop(Player p,int category) {
        Room r=membership.get(p.getUniqueId());if(!playing(p,r) || r.mode==Mode.SKYWARS)return;
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
        Set<Integer> upgrade=action.equals("sharpness")?r.sharpness:action.equals("protection")?r.protection:action.equals("forge")?r.forge:null;
        Map<UUID,String> tools=action.equals("pickaxe")?r.pickaxes:action.equals("axe")?r.axes:null;
        if(action.equals("shears") && r.shears.contains(p.getUniqueId())){msg(p,"Você já possui tesoura.");return;}
        if(upgrade!=null && upgrade.contains(team)){msg(p,"Você já possui essa melhoria.");return;}
        if(action.equals("armor") && r.armor.containsKey(p.getUniqueId()) && BedwarsItems.tier(r.armor.get(p.getUniqueId()))>=BedwarsItems.tier(entry.material)) {msg(p,"Você já possui armadura igual ou melhor.");return;}
        if(tools!=null && tools.containsKey(p.getUniqueId()) && BedwarsItems.tier(tools.get(p.getUniqueId()))>=BedwarsItems.tier(entry.material)){msg(p,"Você já possui ferramenta igual ou melhor.");return;}
        boolean inventory=upgrade==null && !action.equals("armor") || action.equals("shears");
        if(inventory && p.getInventory().firstEmpty()==-1){msg(p,"Libere um espaço no inventário.");return;}
        Material currency=Material.valueOf(entry.currency);if(!p.getInventory().contains(currency,entry.cost)){msg(p,"Recursos insuficientes.");return;}
        p.getInventory().removeItem(new ItemStack(currency,entry.cost));
        if(upgrade!=null)upgrade.add(team);if(action.equals("shears"))r.shears.add(p.getUniqueId());
        if(action.equals("armor"))r.armor.put(p.getUniqueId(),entry.material.substring(0,entry.material.indexOf('_')));
        else if(inventory) {
            ItemStack bought=new ItemStack(Material.valueOf(entry.material),entry.amount,(short)entry.data);
            if(bought.getType()==Material.WOOL || bought.getType()==Material.STAINED_GLASS)bought.setDurability(COLORS[team]);
            if(action.equals("knockback"))bought.addUnsafeEnchantment(Enchantment.KNOCKBACK,1);
            if(tools!=null){String old=tools.put(p.getUniqueId(),entry.material);if(old!=null)p.getInventory().remove(Material.valueOf(old));}
            p.getInventory().addItem(bought);
        }
        for(UUID member:r.players)if(r.teams.get(member)==team){Player ally=Bukkit.getPlayer(member);if(ally!=null)r.equipment(ally);}msg(p,"§aCompra concluída: "+entry.name);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        try {handleClick(e);}catch(IllegalArgumentException error) {
            if(e.getWhoClicked() instanceof Player)msg((Player)e.getWhoClicked(),error.getMessage());
        }
    }
    private void handleClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p=(Player)e.getWhoClicked(); String title=e.getView().getTitle();
        if(!Arrays.asList(MENU,TEAMS,THEME,SIZE,LIST,SHOP,RANK,PARTY).contains(title)) return;
        e.setCancelled(true); if(e.getRawSlot()<0 || e.getRawSlot()>=e.getView().getTopInventory().getSize()) return;
        ItemStack i=e.getCurrentItem(); if(i==null || i.getType()==Material.AIR) return;
        if(title.equals(RANK)){if(e.getRawSlot()<3)ranking(p,Mode.values()[e.getRawSlot()]);return;}
        if(title.equals(PARTY)){if(e.getRawSlot()==22){p.closeInventory();partyCommand(p,new String[]{"puxar"});}else if(e.getRawSlot()==26){p.closeInventory();partyCommand(p,new String[]{"sair"});}return;}
        if(title.equals(MENU)) { if(e.getRawSlot()==22) list(p); else if(e.getRawSlot()==10) teamMenu(p,Mode.BEDWARS); else if(e.getRawSlot()==13) teamMenu(p,Mode.BEDFIGHT); else if(e.getRawSlot()==16) teamMenu(p,Mode.SKYWARS); }
        else if(title.equals(TEAMS)){if(e.getRawSlot()==11 || e.getRawSlot()==15){Mode mode=choosing.get(p.getUniqueId());teamChoices.put(p.getUniqueId(),e.getRawSlot()==15?2:1);if(mode!=null){themes.put(p.getUniqueId(),MapArt.Theme.random());sizeMenu(p,mode);}}}
        else if(title.equals(THEME)) {
            Mode m=choosing.get(p.getUniqueId()); if(m==null) return;
            int slot=e.getRawSlot(); MapArt.Theme t=slot==10?MapArt.Theme.POKEMON:slot==12?MapArt.Theme.EVANGELION:slot==14?MapArt.Theme.SONIC:slot==16?MapArt.Theme.ONEPIECE:null;
            if(t!=null) { themes.put(p.getUniqueId(),t); sizeMenu(p,m); }
        }
        else if(title.equals(SIZE)) { Mode m=choosing.remove(p.getUniqueId()); MapArt.Theme t=themes.remove(p.getUniqueId()); if(m!=null && t!=null) { p.closeInventory(); create(p,m,i.getAmount(),t,teamChoices.getOrDefault(p.getUniqueId(),1)); } }
        else if(title.equals(LIST)) enter(p,rooms.get(Integer.parseInt(ChatColor.stripColor(i.getItemMeta().getDisplayName()).split(" ")[0].substring(1))));
        else {
            Room r=membership.get(p.getUniqueId()); if(r==null || r.phase!=Phase.PLAYING || r.mode==Mode.SKYWARS || !r.alive(p.getUniqueId()) || r.respawning.contains(p.getUniqueId())) return;
            purchase(p,r,e.getRawSlot(),e.isShiftClick());
        }
    }
    @EventHandler public void drag(InventoryDragEvent e) { if(Arrays.asList(MENU,TEAMS,THEME,SIZE,LIST,SHOP,RANK,PARTY).contains(e.getView().getTitle())) e.setCancelled(true); }
    @EventHandler public void interact(PlayerInteractEvent e) {
        Player p=e.getPlayer(); Room r=membership.get(p.getUniqueId());
        if(p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(e.hasItem() && (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK)) {
            if(e.getItem().getType()==Material.COMPASS)menu(p);else if(e.getItem().getType()==Material.NETHER_STAR)ranking(p,Mode.BEDWARS);else if(e.getItem().getType()==Material.NAME_TAG)partyMenu(p);
        } return; }
        if(r!=null && !playing(p,r)) { e.setCancelled(true); return; }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.WORKBENCH) { if(e.getAction()==Action.RIGHT_CLICK_BLOCK){e.setCancelled(true);shop(p);}return; }
        if(r!=null && e.hasBlock() && e.getClickedBlock().getType()==Material.BED_BLOCK && e.getAction()==Action.RIGHT_CLICK_BLOCK) {e.setCancelled(true);return;}
        if(p.getOpenInventory()!=null && SHOP.equals(p.getOpenInventory().getTitle())){e.setCancelled(true);return;}
        if(playing(p,r) && r.mode!=Mode.SKYWARS && (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) && e.hasItem()) {
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
        if(r.mode!=Mode.SKYWARS) {
            List<Location> protectedGenerators=new ArrayList<>(r.diamonds);protectedGenerators.add(new Location(r.world,0.5,81,0.5));
            for(Location l:r.spawns)protectedGenerators.add(l.clone().add(0,0,-3));
            for(Location l:protectedGenerators)if(e.getBlock().getLocation().add(0.5,0,0.5).distanceSquared(l)<2.5){e.setCancelled(true);msg(e.getPlayer(),"Não bloqueie o gerador.");return;}
        }
        if(r.mode!=Mode.SKYWARS && e.getBlock().getType()==Material.TNT) {
            e.getBlock().setType(Material.AIR);TNTPrimed tnt=(TNTPrimed)r.world.spawnEntity(e.getBlock().getLocation().add(0.5,0,0.5),EntityType.PRIMED_TNT);
            tnt.setFuseTicks(60);tnt.setMetadata("treino-owner",new FixedMetadataValue(this,e.getPlayer().getUniqueId().toString()));return;
        }
        r.placed.add(key(e.getBlock()));r.placedTeams.put(key(e.getBlock()),r.teams.get(e.getPlayer().getUniqueId()));
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
        Integer placedTeam=r.placedTeams.get(key(b));
        if(r.mode==Mode.BEDFIGHT && placedTeam!=null && !Rules.sameDuel(placedTeam,r.teams.get(p.getUniqueId())))return;
        if(r.placed.remove(key(b))){r.placedTeams.remove(key(b));e.setCancelled(false);}
    }
    @EventHandler(ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player)) return; Player p=(Player)e.getEntity(); Room r=membership.get(p.getUniqueId());
        if(r==null) { if(p.getWorld()==lobby.getWorld()) { e.setCancelled(true); if(p.getLocation().getY()<50) p.teleport(lobby); } return; }
        if(!playing(p,r)) { e.setCancelled(true); if(e.getCause()==EntityDamageEvent.DamageCause.VOID) p.teleport(r.waiting()); return; }
        if(e instanceof EntityDamageByEntityEvent) {
            Entity attacker=((EntityDamageByEntityEvent)e).getDamager();
            UUID defender=r.summoned.get(attacker.getUniqueId());if(defender!=null && defender.equals(p.getUniqueId())){e.setCancelled(true);return;}
            Player source=attacker instanceof Player?(Player)attacker:attacker instanceof Projectile && ((Projectile)attacker).getShooter() instanceof Player?(Player)((Projectile)attacker).getShooter():null;
            if(source!=null && ((!source.getUniqueId().equals(p.getUniqueId()) && !r.enemies(source.getUniqueId(),p.getUniqueId())) || membership.get(source.getUniqueId())!=r || !r.alive(source.getUniqueId()) || r.respawning.contains(source.getUniqueId()) || (r.mode==Mode.BEDFIGHT && !Rules.sameDuel(r.teams.get(source.getUniqueId()),r.teams.get(p.getUniqueId()))))) { e.setCancelled(true); return; }
            UUID credited=source!=null?source.getUniqueId():r.bots.containsKey(attacker.getUniqueId())?attacker.getUniqueId():defender;
            if(credited==null && attacker.hasMetadata("treino-owner"))try {credited=UUID.fromString(attacker.getMetadata("treino-owner").get(0).asString());}catch(IllegalArgumentException ignored){}
            if(credited!=null && !credited.equals(p.getUniqueId()) && !r.enemies(credited,p.getUniqueId())){e.setCancelled(true);return;}
            if(credited!=null)r.spawnProtection.remove(credited);
            if(r.spawnProtection.getOrDefault(p.getUniqueId(),0)>r.elapsed){e.setCancelled(true);return;}
            if(credited!=null && !credited.equals(p.getUniqueId()) && r.players.contains(credited) && e.getFinalDamage()>0) {r.lastAttack.put(p.getUniqueId(),credited);r.lastHit.put(p.getUniqueId(),r.elapsed);}
        }
        if(e.getFinalDamage()>=p.getHealth() || e.getCause()==EntityDamageEvent.DamageCause.VOID) { e.setCancelled(true); knockOut(p,r); }
    }
    @EventHandler(ignoreCancelled=true) public void botDamage(EntityDamageEvent e) {
        Room r=roomWorld(e.getEntity().getWorld());if(r==null)return;PracticeBot bot=r.bots.get(e.getEntity().getUniqueId());if(bot==null)return;
        if(r.phase!=Phase.PLAYING || !r.alive(bot.id) || r.respawning.contains(bot.id)){e.setCancelled(true);return;}
        if(e instanceof EntityDamageByEntityEvent) {
            Entity damager=((EntityDamageByEntityEvent)e).getDamager();UUID source=damager.getUniqueId();
            if(damager instanceof Projectile && ((Projectile)damager).getShooter() instanceof Entity)source=((Entity)((Projectile)damager).getShooter()).getUniqueId();
            if(r.summoned.containsKey(source))source=r.summoned.get(source);
            if(damager.hasMetadata("treino-owner"))try{source=UUID.fromString(damager.getMetadata("treino-owner").get(0).asString());}catch(RuntimeException ignored){}
            if(!r.enemies(source,bot.id)){e.setCancelled(true);return;}r.spawnProtection.remove(source);
            if(r.spawnProtection.getOrDefault(bot.id,0)>r.elapsed){e.setCancelled(true);return;}r.lastAttack.put(bot.id,source);r.lastHit.put(bot.id,r.elapsed);
        }
        if(e.getFinalDamage()>=bot.entity.getHealth() || e.getCause()==EntityDamageEvent.DamageCause.VOID){e.setCancelled(true);r.botDown(bot);}
    }
    @EventHandler public void botSun(EntityCombustEvent e){if(e.getEntity().hasMetadata("treino-bot"))e.setCancelled(true);}
    @EventHandler public void mobDeath(EntityDeathEvent e) {if(roomWorld(e.getEntity().getWorld())!=null && !(e.getEntity() instanceof Player)){e.getDrops().clear();e.setDroppedExp(0);}}
    @EventHandler public void death(PlayerDeathEvent e) { Room r=membership.get(e.getEntity().getUniqueId()); if(r!=null) { e.getDrops().clear(); e.setDroppedExp(0); } }
    @EventHandler public void respawn(PlayerRespawnEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); e.setRespawnLocation(r==null?lobby:r.waiting()); if(r!=null) new BukkitRunnable(){public void run(){knockOut(e.getPlayer(),r);}}.runTaskLater(this,1); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        UUID id=e.getPlayer().getUniqueId();
        if(parties.get(id)!=null)partySay(id,e.getPlayer().getName()+" desconectou e saiu da party.");
        parties.leave(id);leave(e.getPlayer());sidebarLines.remove(id);shopPages.remove(id);slingCooldown.remove(id);
    }
    @EventHandler public void join(PlayerJoinEvent e) { grantAdmin(e.getPlayer());hub(e.getPlayer());repairVisibility(e.getPlayer()); }
    @EventHandler public void food(FoodLevelChangeEvent e) { if(e.getEntity() instanceof Player && e.getEntity().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    private void consume(Player p) {ItemStack item=p.getItemInHand();if(item.getAmount()<=1)p.setItemInHand(null);else {item.setAmount(item.getAmount()-1);p.setItemInHand(item);}}
    private Room roomWorld(World world) {for(Room r:rooms.values())if(r.world==world)return r;return null;}
    @EventHandler public void merchant(PlayerInteractEntityEvent e) {
        if(!e.getRightClicked().hasMetadata("treino-display"))return;e.setCancelled(true);
        if(e.getRightClicked().hasMetadata("treino-shop"))shop(e.getPlayer());
    }
    @EventHandler public void merchantAt(PlayerInteractAtEntityEvent e) {merchant(e);}
    @EventHandler public void protectDisplay(EntityDamageEvent e) {if(e.getEntity().hasMetadata("treino-display"))e.setCancelled(true);}
    @EventHandler public void protectDisplayEquipment(PlayerArmorStandManipulateEvent e) {if(e.getRightClicked().hasMetadata("treino-display"))e.setCancelled(true);}
    @EventHandler public void target(EntityTargetEvent e) {
        Room r=roomWorld(e.getEntity().getWorld());if(r==null)return;
        if(r.bots.containsKey(e.getEntity().getUniqueId())) {
            UUID id=e.getEntity().getUniqueId();if(e.getTarget()!=null && (r.phase!=Phase.PLAYING || r.respawning.contains(id) || !r.enemies(id,e.getTarget().getUniqueId()) || r.respawning.contains(e.getTarget().getUniqueId())))e.setCancelled(true);return;
        }
        if(!r.summoned.containsKey(e.getEntity().getUniqueId()))return;
        if(!(e.getTarget() instanceof Player)){if(e.getTarget()!=null)e.setCancelled(true);return;}
        Player p=(Player)e.getTarget();if(!playing(p,r) || !r.enemies(p.getUniqueId(),r.summoned.get(e.getEntity().getUniqueId())))e.setCancelled(true);
    }
    @EventHandler public void golemDamage(EntityDamageByEntityEvent e) {
        Room r=roomWorld(e.getDamager().getWorld());if(r==null)return;
        UUID owner=r.summoned.get(e.getDamager().getUniqueId());
        if(owner!=null && (!r.enemies(owner,e.getEntity().getUniqueId()) || r.respawning.contains(e.getEntity().getUniqueId())))e.setCancelled(true);
        if(r.summoned.containsKey(e.getEntity().getUniqueId())) {
            if(!(e.getDamager() instanceof Player) || !playing((Player)e.getDamager(),r) || !r.enemies(r.summoned.get(e.getEntity().getUniqueId()),e.getDamager().getUniqueId()))e.setCancelled(true);
        }
    }
    @EventHandler public void explosion(EntityExplodeEvent e) {
        if(!e.getLocation().getWorld().getName().startsWith("treino_"))return;
        Room r=roomWorld(e.getLocation().getWorld());
        if(r==null || r.phase!=Phase.PLAYING || r.mode==Mode.SKYWARS || !e.getEntity().hasMetadata("treino-owner")){e.setCancelled(true);return;}
        UUID bomber=null;
        if(r.mode==Mode.BEDFIGHT)try {bomber=UUID.fromString(e.getEntity().getMetadata("treino-owner").get(0).asString());}catch(RuntimeException ignored) {e.setCancelled(true);return;}
        Integer bomberTeam=bomber==null?null:r.teams.get(bomber);
        e.setYield(0);Iterator<Block> blocks=e.blockList().iterator();while(blocks.hasNext()) {
            Block b=blocks.next();Material m=b.getType();
            boolean resistant=m==Material.OBSIDIAN || m==Material.STAINED_GLASS || m==Material.GLASS || (e.getEntity() instanceof Fireball && m==Material.ENDER_STONE);
            Integer placedTeam=r.placedTeams.get(key(b));
            boolean otherDuel=r.mode==Mode.BEDFIGHT && (bomberTeam==null || placedTeam==null || !Rules.sameDuel(bomberTeam,placedTeam));
            if(!r.placed.contains(key(b)) || resistant || otherDuel)blocks.remove();else {r.placed.remove(key(b));r.placedTeams.remove(key(b));}
        }
    }
    @EventHandler public void fluid(BlockFromToEvent e) {
        Room r=roomWorld(e.getBlock().getWorld());if(r!=null && (e.getToBlock().getY()<60 || Math.abs(e.getToBlock().getX())>65 || Math.abs(e.getToBlock().getZ())>65))e.setCancelled(true);
    }
    @EventHandler public void bucket(PlayerBucketEmptyEvent e) {Room r=membership.get(e.getPlayer().getUniqueId());if(!playing(e.getPlayer(),r) || r.mode==Mode.SKYWARS)e.setCancelled(true);}
    @EventHandler public void bucketFill(PlayerBucketFillEvent e) {Room r=membership.get(e.getPlayer().getUniqueId());if(!playing(e.getPlayer(),r) || r.mode==Mode.SKYWARS)e.setCancelled(true);}
    @EventHandler public void burn(BlockBurnEvent e) { if(e.getBlock().getWorld().getName().startsWith("treino_")) e.setCancelled(true); }
    @EventHandler public void drop(PlayerDropItemEvent e) { Room r=membership.get(e.getPlayer().getUniqueId()); if(e.getPlayer().getWorld()==lobby.getWorld() || (r!=null && (r.phase!=Phase.PLAYING || !r.alive(e.getPlayer().getUniqueId()) || r.respawning.contains(e.getPlayer().getUniqueId())))) e.setCancelled(true); }
}
