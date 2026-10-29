package br.ryuex.treino;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.entity.LivingEntity;

/** Narrow 1.8.8 adapter; verified against Spigot's EntityInsentient/EntityLiving. */
final class Native18 {
    private static final Map<Class<?>,Method> HANDLES=new HashMap<>();
    static Object handle(Object entity)throws ReflectiveOperationException {
        Method method=HANDLES.get(entity.getClass());if(method==null){method=entity.getClass().getMethod("getHandle");HANDLES.put(entity.getClass(),method);}return method.invoke(entity);
    }
    static boolean freezeHandle(Object handle)throws ReflectiveOperationException {
        handle.getClass().getMethod("k",boolean.class).invoke(handle,true);
        return Boolean.TRUE.equals(handle.getClass().getMethod("ce").invoke(handle));
    }
    static boolean disableAI(LivingEntity entity){try{return freezeHandle(handle(entity));}catch(ReflectiveOperationException|RuntimeException ex){return false;}}
    static void swing(LivingEntity entity){try{Object h=handle(entity);h.getClass().getMethod("bw").invoke(h);}catch(ReflectiveOperationException|RuntimeException ignored){}}
    static void look(LivingEntity entity,float yaw,float pitch){try{
        Object h=handle(entity);h.getClass().getField("yaw").setFloat(h,yaw);h.getClass().getField("pitch").setFloat(h,pitch);h.getClass().getField("aK").setFloat(h,yaw);h.getClass().getField("aI").setFloat(h,yaw);
    }catch(ReflectiveOperationException|RuntimeException ignored){}}
}
