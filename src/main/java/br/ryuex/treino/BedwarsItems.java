package br.ryuex.treino;

import java.util.*;

/** Own catalogue: prices and behaviour are explicit, not scraped from another network. */
final class BedwarsItems {
    static final String[] CATEGORIES={"Blocos","Espadas","Armaduras","Ferramentas","Arcos","Poções","Utilidades","Melhorias"};
    static final String[] ICONS={"WOOL","IRON_SWORD","IRON_BOOTS","IRON_PICKAXE","BOW","POTION","FIREBALL","DIAMOND"};
    static final class Entry {
        final int category,amount,cost,data; final String material,currency,name,action;
        Entry(int category,String material,int amount,int data,int cost,String currency,String name,String action) {
            this.category=category;this.material=material;this.amount=amount;this.data=data;this.cost=cost;this.currency=currency;this.name=name;this.action=action;
        }
    }
    static final List<Entry> ALL=new ArrayList<>();
    static void add(int c,String m,int n,int d,int cost,String currency,String name,String action) { ALL.add(new Entry(c,m,n,d,cost,currency,name,action)); }
    static {
        add(0,"WOOL",16,0,4,"IRON_INGOT","16 lãs","item");
        add(0,"WOOD",16,0,4,"GOLD_INGOT","16 madeiras","item");
        add(0,"ENDER_STONE",12,0,24,"IRON_INGOT","12 pedras do End","item");
        add(0,"STAINED_GLASS",4,0,12,"IRON_INGOT","4 vidros resistentes a explosões","item");
        add(0,"OBSIDIAN",4,0,4,"EMERALD","4 obsidianas","item");
        add(0,"LADDER",8,0,4,"IRON_INGOT","8 escadas","item");
        add(1,"STONE_SWORD",1,0,10,"IRON_INGOT","Espada de pedra","item");
        add(1,"IRON_SWORD",1,0,7,"GOLD_INGOT","Espada de ferro","item");
        add(1,"DIAMOND_SWORD",1,0,4,"EMERALD","Espada de diamante","item");
        add(1,"STICK",1,0,5,"GOLD_INGOT","Graveto • repulsão I","knockback");
        add(2,"CHAINMAIL_BOOTS",1,0,40,"IRON_INGOT","Malha permanente • calça/botas","armor");
        add(2,"IRON_BOOTS",1,0,12,"GOLD_INGOT","Ferro permanente • calça/botas","armor");
        add(2,"DIAMOND_BOOTS",1,0,6,"EMERALD","Diamante permanente • calça/botas","armor");
        add(3,"SHEARS",1,0,20,"IRON_INGOT","Tesoura permanente","shears");
        add(3,"WOOD_PICKAXE",1,0,10,"IRON_INGOT","Picareta • mantém ao morrer","pickaxe");
        add(3,"IRON_PICKAXE",1,0,3,"GOLD_INGOT","Picareta de ferro • mantém ao morrer","pickaxe");
        add(3,"DIAMOND_PICKAXE",1,0,6,"GOLD_INGOT","Picareta de diamante • mantém ao morrer","pickaxe");
        add(3,"WOOD_AXE",1,0,10,"IRON_INGOT","Machado • mantém ao morrer","axe");
        add(3,"IRON_AXE",1,0,3,"GOLD_INGOT","Machado de ferro • mantém ao morrer","axe");
        add(4,"BOW",1,0,12,"GOLD_INGOT","Arco","item");
        add(4,"ARROW",8,0,2,"GOLD_INGOT","8 flechas","item");
        add(5,"POTION",1,8194,1,"EMERALD","Poção de velocidade","item");
        add(5,"POTION",1,8203,1,"EMERALD","Poção de salto","item");
        add(6,"FIREBALL",1,0,40,"IRON_INGOT","Bola de fogo","item");
        add(6,"MAGMA_CREAM",1,0,8,"GOLD_INGOT","Estilingue • impulso na direção da mira","item");
        add(6,"MONSTER_EGG",1,99,120,"IRON_INGOT","Golem defensor • 45 segundos","item");
        add(6,"TNT",1,0,4,"GOLD_INGOT","TNT • acende ao colocar","item");
        add(6,"ENDER_PEARL",1,0,4,"EMERALD","Pérola do End","item");
        add(6,"GOLDEN_APPLE",1,0,3,"GOLD_INGOT","Maçã dourada","item");
        add(6,"WATER_BUCKET",1,0,3,"GOLD_INGOT","Balde de água","item");
        add(7,"IRON_SWORD",1,0,4,"DIAMOND","Afiação I permanente","sharpness");
        add(7,"IRON_CHESTPLATE",1,0,2,"DIAMOND","Proteção I permanente","protection");
        add(7,"FURNACE",1,0,4,"DIAMOND","Forja II • ferro a cada segundo","forge");
    }
    static List<Entry> category(int category) { List<Entry> result=new ArrayList<>(); for(Entry e:ALL)if(e.category==category)result.add(e);return result; }
    static String currencyName(String currency) {return currency.equals("IRON_INGOT")?"ferros":currency.equals("GOLD_INGOT")?"ouros":currency.equals("DIAMOND")?"diamantes":"esmeraldas";}
    static String key(Entry entry) {return entry.action+":"+entry.material;}
    static int tier(String material) { return material.startsWith("DIAMOND")?3:material.startsWith("IRON")?2:1; }
    static int interval(String resource,int elapsed,boolean forge) {
        int tier=elapsed>=480?3:elapsed>=240?2:1;
        if(resource.equals("IRON_INGOT"))return forge?1:2;
        if(resource.equals("GOLD_INGOT"))return forge?4:8;
        if(resource.equals("DIAMOND"))return tier==3?15:tier==2?20:30;
        return tier==3?30:tier==2?40:60;
    }
    static boolean canBreakBed(boolean playing,boolean alive,boolean respawning,int own,int target,boolean duel) {
        return playing && alive && !respawning && own!=target && (!duel || Rules.sameDuel(own,target));
    }
}
