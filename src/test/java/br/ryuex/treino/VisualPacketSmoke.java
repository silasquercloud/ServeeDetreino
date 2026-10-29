package br.ryuex.treino;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.entity.LivingEntity;
public class VisualPacketSmoke {
 public static void main(String[] args)throws Exception {
  LivingEntity entity=(LivingEntity)Proxy.newProxyInstance(VisualPacketSmoke.class.getClassLoader(),new Class[]{LivingEntity.class},(p,m,a)->{if(m.getName().equals("getEntityId"))return 42;if(m.getName().equals("getHealth"))return 20d;if(m.getReturnType()==boolean.class)return false;return null;});
  PlayerVisuals visual=new PlayerVisuals(null);Class<?> vc=Class.forName("br.ryuex.treino.PlayerVisuals$Visual");Constructor<?> ctor=vc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object v=ctor.newInstance(entity,"BOT_Test","Notch");Method refresh=PlayerVisuals.class.getDeclaredMethod("refresh",vc);refresh.setAccessible(true);refresh.invoke(visual,v);
  for(String f:new String[]{"add","remove","metadata"}){Field field=vc.getDeclaredField(f);field.setAccessible(true);Object packet=field.get(v);serialize(packet);}
  Class<?> mob=Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving");Object raw=mob.newInstance();for(String n:new String[]{"a","c","d","e"}){Field f=mob.getDeclaredField(n);f.setAccessible(true);f.set(raw,42);}
  Method spawn=PlayerVisuals.class.getDeclaredMethod("spawn",vc,Object.class);spawn.setAccessible(true);serialize(spawn.invoke(visual,v,raw));
  Field actors=PlayerVisuals.class.getDeclaredField("actors");actors.setAccessible(true);((java.util.Map)actors.get(visual)).put(42,v);
  Method handler=PlayerVisuals.class.getDeclaredMethod("outboundHandler");handler.setAccessible(true);Object h=handler.invoke(visual);Class<?> ch=Class.forName("io.netty.channel.ChannelHandler");Object handlers=java.lang.reflect.Array.newInstance(ch,1);java.lang.reflect.Array.set(handlers,0,h);Class<?> embedded=Class.forName("io.netty.channel.embedded.EmbeddedChannel");Object channel=embedded.getConstructor(handlers.getClass()).newInstance(handlers);
  embedded.getMethod("writeOutbound",Object[].class).invoke(channel,(Object)new Object[]{raw});Object first=embedded.getMethod("readOutbound").invoke(channel),second=embedded.getMethod("readOutbound").invoke(channel);if(!first.getClass().getSimpleName().equals("PacketPlayOutPlayerInfo") || !second.getClass().getSimpleName().equals("PacketPlayOutNamedEntitySpawn"))throw new AssertionError("wrong spawn order");serialize(first);serialize(second);embedded.getMethod("finish").invoke(channel);
  System.out.println("VisualPacketSmoke OK: real 1.8_R3 tab ADD/REMOVE, metadata and player spawn serialize");
 }
 static void serialize(Object packet)throws Exception{Class<?> buffer=Class.forName("io.netty.buffer.ByteBuf"),serializer=Class.forName("net.minecraft.server.v1_8_R3.PacketDataSerializer");Object b=Class.forName("io.netty.buffer.Unpooled").getMethod("buffer").invoke(null);Object s=serializer.getConstructor(buffer).newInstance(b);packet.getClass().getMethod("b",serializer).invoke(packet,s);buffer.getMethod("release").invoke(b);}
}
