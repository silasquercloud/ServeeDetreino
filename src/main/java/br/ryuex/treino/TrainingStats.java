package br.ryuex.treino;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

/** Local persistent friend leaderboard, separated by mode and UUID. */
final class TrainingStats {
    private final File file; private final YamlConfiguration data; private boolean dirty;
    TrainingStats(File directory){file=new File(directory,"stats.yml");data=YamlConfiguration.loadConfiguration(file);}
    void name(UUID id,String name){data.set("players."+id+".name",name);dirty=true;}
    String name(UUID id){return data.getString("players."+id+".name",id.toString().substring(0,8));}
    int get(UUID id,String mode,String key){return data.getInt("players."+id+"."+mode+"."+key,0);}
    static String today(){return java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();}
    int dailyBest(UUID id,String mode){return data.getInt("players."+id+"."+mode+".daily."+today()+".best",0);}
    void loss(UUID id,String mode){data.set("players."+id+"."+mode+".streak",0);data.set("players."+id+"."+mode+".daily."+today()+".current",0);dirty=true;}
    void add(UUID id,String mode,String key){
        data.set("players."+id+"."+mode+"."+key,get(id,mode,key)+1);dirty=true;
        if(key.equals("wins")) {
            String base="players."+id+"."+mode;int streak=get(id,mode,"streak")+1;data.set(base+".streak",streak);data.set(base+".best-streak",Math.max(streak,get(id,mode,"best-streak")));
            String daily=base+".daily."+today();int current=data.getInt(daily+".current",0)+1;data.set(daily+".current",current);data.set(daily+".best",Math.max(current,data.getInt(daily+".best",0)));
        }
    }
    List<UUID> topDaily(String mode){List<UUID> ids=top(mode);ids.removeIf(id->dailyBest(id,mode)<=0);ids.sort(Comparator.comparingInt((UUID id)->dailyBest(id,mode)).reversed().thenComparing(id->name(id)));return ids;}
    List<UUID> top(String mode){
        List<UUID> ids=new ArrayList<>();if(data.getConfigurationSection("players")!=null)for(String id:data.getConfigurationSection("players").getKeys(false))try {ids.add(UUID.fromString(id));}catch(IllegalArgumentException ignored){}
        ids.sort(Comparator.comparingInt((UUID id)->get(id,mode,"wins")).reversed().thenComparing(id->name(id)));return ids;
    }
    List<String> favorites(UUID id) {
        String path="players."+id+".favorites";
        return data.contains(path)?new ArrayList<>(data.getStringList(path)):new ArrayList<>(Arrays.asList("item:WOOL","item:FIREBALL","item:TNT","item:MAGMA_CREAM","item:MONSTER_EGG","item:GOLDEN_APPLE"));
    }
    void toggleFavorite(UUID id,String key) {List<String> list=favorites(id);if(!list.remove(key) && list.size()<27)list.add(key);data.set("players."+id+".favorites",list);dirty=true;}
    void favoriteSlot(UUID id,String key,int slot){if(slot<0 || slot>=27)throw new IllegalArgumentException("Slot inválido.");List<String> list=favorites(id);while(list.size()<27)list.add("");for(int i=0;i<list.size();i++)if(list.get(i).equals(key))list.set(i,"");list.set(slot,key);data.set("players."+id+".favorites",list);dirty=true;}
    UUID find(String name){if(data.getConfigurationSection("players")==null)return null;for(String value:data.getConfigurationSection("players").getKeys(false))try{UUID id=UUID.fromString(value);if(name(id).equalsIgnoreCase(name))return id;}catch(IllegalArgumentException ignored){}return null;}
    void copyFavorites(UUID target,UUID source){data.set("players."+target+".favorites",new ArrayList<>(favorites(source)));dirty=true;}
    int hotbar(UUID id,String key){return data.getInt("players."+id+".hotbar."+key.replace(':','_'),-1);}
    void hotbar(UUID id,String key,int slot){if(slot<0 || slot>8)throw new IllegalArgumentException("Slot inválido.");data.set("players."+id+".hotbar."+key.replace(':','_'),slot);dirty=true;}
    void save() throws IOException {
        if(!dirty)return;file.getParentFile().mkdirs();File temp=new File(file.getParentFile(),"stats.yml.tmp");data.save(temp);
        try {Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        dirty=false;
    }
}
