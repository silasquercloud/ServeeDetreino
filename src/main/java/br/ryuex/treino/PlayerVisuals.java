package br.ryuex.treino;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.*;

/** 1.8_R3 outbound player appearance. Server entity IDs remain authoritative for interaction. */
final class PlayerVisuals {
    private final JavaPlugin plugin;
    private final Map<Integer,Visual> actors=new ConcurrentHashMap<>();
    private final Map<UUID,Object> channels=new HashMap<>();
    private final Map<String,String[]> skins=new ConcurrentHashMap<>();
    private final Set<String> fetching=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
    private final String prefix="net.minecraft.server.v1_8_R3.";
    private boolean warned;
    private static final String HANDLER="treino_player_visuals";
    private static final class Visual {
        final LivingEntity entity;final String name,skin;final UUID uuid=UUID.randomUUID();
        volatile Object add,remove,watcher,metadata;
        Visual(LivingEntity e,String n,String s){entity=e;name=n;skin=s;}
    }
    PlayerVisuals(JavaPlugin p){plugin=p;}
    private Class<?> type(String name)throws Exception{return Class.forName(prefix+name);}
    private static Object get(Object o,String field)throws Exception{Field f=o.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String field,Object value)throws Exception{Field f=o.getClass().getDeclaredField(field);f.setAccessible(true);f.set(o,value);}
    private static Object call(Object o,String method,Class<?>[] types,Object...args)throws Exception{return o.getClass().getMethod(method,types).invoke(o,args);}
    private void warn(Exception e){if(!warned){warned=true;plugin.getLogger().warning("Aparência de jogador indisponível: "+e.toString()+". Mantendo entidade original.");}}
    void register(LivingEntity entity,String name,String skin){
        try{Visual v=new Visual(entity,name.substring(0,Math.min(16,name.length())),skin);refresh(v);actors.put(entity.getEntityId(),v);for(Player p:Bukkit.getOnlinePlayers())if(p.getWorld()==entity.getWorld())show(p,v);fetch(skin);}
        catch(Exception e){warn(e);}
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private Object info(Visual v,boolean remove)throws Exception{
        Object packet=type("PacketPlayOutPlayerInfo").newInstance();
        set(packet,"a",Enum.valueOf((Class)type("PacketPlayOutPlayerInfo$EnumPlayerInfoAction"),remove?"REMOVE_PLAYER":"ADD_PLAYER"));
        Class<?> profileClass=Class.forName("com.mojang.authlib.GameProfile");Object profile=profileClass.getConstructor(UUID.class,String.class).newInstance(v.uuid,v.name);
        String[] skin=skins.get(v.skin);if(skin!=null){Object properties=call(profile,"getProperties",new Class<?>[0]);Object property=Class.forName("com.mojang.authlib.properties.Property").getConstructor(String.class,String.class,String.class).newInstance("textures",skin[0],skin[1]);Class.forName("com.google.common.collect.Multimap").getMethod("put",Object.class,Object.class).invoke(properties,"textures",property);}
        Object mode=Enum.valueOf((Class)type("WorldSettings$EnumGamemode"),"SURVIVAL");
        Object label=type("ChatComponentText").getConstructor(String.class).newInstance("§7[NPC] §f"+v.name);
        Object row=type("PacketPlayOutPlayerInfo$PlayerInfoData").getConstructor(type("PacketPlayOutPlayerInfo"),profileClass,int.class,type("WorldSettings$EnumGamemode"),type("IChatBaseComponent")).newInstance(packet,profile,0,mode,label);
        ((List)get(packet,"b")).add(row);return packet;
    }
    private void refresh(Visual v)throws Exception{
        Object watcher=type("DataWatcher").getConstructor(type("Entity")).newInstance(new Object[]{null});
        Method add=watcher.getClass().getMethod("a",int.class,Object.class);
        byte flags=(byte)(v.entity.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)?32:0);
        add.invoke(watcher,0,flags);add.invoke(watcher,1,(short)300);add.invoke(watcher,6,(float)v.entity.getHealth());add.invoke(watcher,10,(byte)127);
        v.watcher=watcher;v.metadata=type("PacketPlayOutEntityMetadata").getConstructor(int.class,type("DataWatcher"),boolean.class).newInstance(v.entity.getEntityId(),watcher,true);
        if(v.add==null){v.add=info(v,false);v.remove=info(v,true);}
    }
    private Object spawn(Visual v,Object original)throws Exception{
        Object packet=type("PacketPlayOutNamedEntitySpawn").newInstance();set(packet,"a",v.entity.getEntityId());set(packet,"b",v.uuid);
        for(String f:new String[]{"c","d","e"})set(packet,f,get(original,f));set(packet,"f",get(original,"i"));set(packet,"g",get(original,"j"));set(packet,"h",0);set(packet,"i",v.watcher);return packet;
    }
    void tick(){for(Visual v:new ArrayList<>(actors.values())){if(!v.entity.isValid() || v.entity.isDead()){actors.remove(v.entity.getEntityId());for(Player p:Bukkit.getOnlinePlayers())send(p,v.remove);continue;}try{refresh(v);}catch(Exception e){warn(e);}}}
    void attach(Player player){
        if(channels.containsKey(player.getUniqueId()))return;
        try{
            Object connection=Native18.handle(player).getClass().getField("playerConnection").get(Native18.handle(player));Object network=connection.getClass().getField("networkManager").get(connection);Object channel=network.getClass().getField("channel").get(network);
            Class<?> channelType=Class.forName("io.netty.channel.Channel"),handlerType=Class.forName("io.netty.channel.ChannelHandler");
            Object pipeline=channelType.getMethod("pipeline").invoke(channel);Class<?> pipelineType=Class.forName("io.netty.channel.ChannelPipeline");
            Object handler=outboundHandler();
            pipelineType.getMethod("addBefore",String.class,String.class,handlerType).invoke(pipeline,"packet_handler",HANDLER,handler);channels.put(player.getUniqueId(),channel);
        }catch(Exception e){warn(e);}
    }
    private Object outboundHandler()throws Exception{
        Class<?> ctxType=Class.forName("io.netty.channel.ChannelHandlerContext");
        return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Class.forName("io.netty.channel.ChannelOutboundHandler")},(proxy,method,args)->{
                String name=method.getName();if(name.equals("toString"))return HANDLER;if(name.equals("hashCode"))return System.identityHashCode(proxy);if(name.equals("equals"))return proxy==args[0];
                if(name.equals("handlerAdded") || name.equals("handlerRemoved"))return null;
                if(name.equals("exceptionCaught")){ctxType.getMethod("fireExceptionCaught",Throwable.class).invoke(args[0],args[1]);return null;}
                if(name.equals("write")){
                    Object packet=args[1];String packetName=packet.getClass().getSimpleName();
                    try{
                        if(packetName.equals("PacketPlayOutSpawnEntityLiving")){Visual v=actors.get((Integer)get(packet,"a"));if(v!=null){ctxType.getMethod("write",Object.class).invoke(args[0],v.add);args[1]=spawn(v,packet);}}
                        else if(packetName.equals("PacketPlayOutEntityMetadata")){Visual v=actors.get((Integer)get(packet,"a"));if(v!=null)args[1]=v.metadata;}
                        else if(packetName.equals("PacketPlayOutEntityDestroy")){for(int id:(int[])get(packet,"a")){Visual v=actors.get(id);if(v!=null)ctxType.getMethod("write",Object.class).invoke(args[0],v.remove);}}
                    }catch(Exception e){warn(e);}
                }
                Class<?>[] signature=Arrays.copyOfRange(method.getParameterTypes(),1,method.getParameterCount());Object[] forwarded=Arrays.copyOfRange(args,1,args.length);return ctxType.getMethod(name,signature).invoke(args[0],forwarded);
            });
    }
    void detach(Player player){Object channel=channels.remove(player.getUniqueId());if(channel==null)return;try{Object pipeline=Class.forName("io.netty.channel.Channel").getMethod("pipeline").invoke(channel);Class.forName("io.netty.channel.ChannelPipeline").getMethod("remove",String.class).invoke(pipeline,HANDLER);}catch(Exception ignored){}}
    private void send(Player p,Object packet){try{Object h=Native18.handle(p);Object c=h.getClass().getField("playerConnection").get(h);c.getClass().getMethod("sendPacket",type("Packet")).invoke(c,packet);}catch(Exception e){warn(e);}}
    private void show(Player p,Visual v)throws Exception{
        Object raw=type("PacketPlayOutSpawnEntityLiving").getConstructor(type("EntityLiving")).newInstance(Native18.handle(v.entity));
        send(p,type("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance((Object)new int[]{v.entity.getEntityId()}));send(p,raw);
        org.bukkit.inventory.EntityEquipment gear=v.entity.getEquipment();org.bukkit.inventory.ItemStack[] items={gear.getItemInHand(),gear.getBoots(),gear.getLeggings(),gear.getChestplate(),gear.getHelmet()};
        Method convert=Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack").getMethod("asNMSCopy",org.bukkit.inventory.ItemStack.class);
        for(int slot=0;slot<items.length;slot++)send(p,type("PacketPlayOutEntityEquipment").getConstructor(int.class,int.class,type("ItemStack")).newInstance(v.entity.getEntityId(),slot,convert.invoke(null,items[slot])));
    }
    void close(){for(Player p:Bukkit.getOnlinePlayers()){for(Visual v:actors.values())send(p,v.remove);detach(p);}actors.clear();}
    private JsonObject read(String address)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestProperty("User-Agent","TreinoHub/1.5");
        try(InputStream in=c.getInputStream();Reader reader=new InputStreamReader(in,"UTF-8")){return new JsonParser().parse(reader).getAsJsonObject();}finally{c.disconnect();}
    }
    private void fetch(String name){if(!name.matches("[A-Za-z0-9_]{1,16}") || skins.containsKey(name) || !fetching.add(name))return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{
            JsonObject account=read("https://api.mojang.com/users/profiles/minecraft/"+name);String id=account.get("id").getAsString();if(!id.matches("[a-fA-F0-9]{32}"))return;
            JsonObject profile=read("https://sessionserver.mojang.com/session/minecraft/profile/"+id+"?unsigned=false");
            for(JsonElement entry:profile.getAsJsonArray("properties")){JsonObject property=entry.getAsJsonObject();if(property.get("name").getAsString().equals("textures") && property.has("signature"))skins.put(name,new String[]{property.get("value").getAsString(),property.get("signature").getAsString()});}
            if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->{for(Visual v:actors.values())if(v.skin.equals(name))try{v.add=info(v,false);v.remove=info(v,true);for(Player p:Bukkit.getOnlinePlayers())if(p.getWorld()==v.entity.getWorld())show(p,v);}catch(Exception e){warn(e);}});
        }catch(Exception e){plugin.getLogger().warning("Skin "+name+" indisponível; usando modelo padrão. "+e.getClass().getSimpleName());}});
    }
}
