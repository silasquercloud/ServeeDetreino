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
    void add(UUID id,String mode,String key){data.set("players."+id+"."+mode+"."+key,get(id,mode,key)+1);dirty=true;}
    List<UUID> top(String mode){
        List<UUID> ids=new ArrayList<>();if(data.getConfigurationSection("players")!=null)for(String id:data.getConfigurationSection("players").getKeys(false))try {ids.add(UUID.fromString(id));}catch(IllegalArgumentException ignored){}
        ids.sort(Comparator.comparingInt((UUID id)->get(id,mode,"wins")).reversed().thenComparing(id->name(id)));return ids;
    }
    List<String> favorites(UUID id) {
        String path="players."+id+".favorites";
        return data.contains(path)?new ArrayList<>(data.getStringList(path)):new ArrayList<>(Arrays.asList("item:WOOL","item:FIREBALL","item:TNT","item:MAGMA_CREAM","item:MONSTER_EGG","item:GOLDEN_APPLE"));
    }
    void toggleFavorite(UUID id,String key) {List<String> list=favorites(id);if(!list.remove(key) && list.size()<27)list.add(key);data.set("players."+id+".favorites",list);dirty=true;}
    void save() throws IOException {
        if(!dirty)return;file.getParentFile().mkdirs();File temp=new File(file.getParentFile(),"stats.yml.tmp");data.save(temp);
        try {Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        dirty=false;
    }
}
