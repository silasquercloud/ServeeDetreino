package br.ryuex.treino;
import java.util.*;
public final class MapArtTest {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        MapArt.Scene forge=MapArt.forge(MapArt.Theme.SONIC);
        check(forge.at(0,0,-3).material.equals("IRON_BLOCK"),"forja tem ponto iluminado de coleta");
        check(forge.at(0,1,-3)==null && forge.at(0,2,-3)==null,"jogador consegue entrar na forja");
        check(forge.at(0,4,-3)==null,"forja aberta sem cobertura");
        check(MapArt.emeraldOutpost().at(0,0,0).material.equals("EMERALD_BLOCK"),"poço de esmeralda com núcleo visível");
        Set<String> signatures=new HashSet<>();
        for(MapArt.Theme theme:MapArt.Theme.values()) {
            MapArt.Scene island=MapArt.island(theme,8),base=MapArt.battleBase(theme);
            for(int x=-4;x<=5;x++)for(int z=-1;z<=4;z++)for(int y=1;y<=3;y++)check(base.at(x,y,z)==null,"apron around bed must remain open: "+theme);
            check(island.at(0,0,0)!=null && island.at(0,-6,0)!=null,"Solid floating island foundation: "+theme);
            for(int y=1;y<=2;y++) {
                check(island.at(0,y,0)==null,"Spawn headroom: "+theme);
                check(island.at(0,y,-2)==null,"Generator pickup lane: "+theme);
                check(island.at(-2,y,0)==null,"Chest/shop headroom: "+theme);
                check(island.at(2,y,0)==null,"Chest/bed headroom: "+theme);
                check(island.at(2,y,1)==null,"Bed head clearance: "+theme);
            }
            if(theme==MapArt.Theme.ONEPIECE) check(island.at(0,4,3)==null,"Open ship boarding entrance");
            MapArt.Scene center=MapArt.center(theme);
            for(int[] chest:new int[][]{{0,0},{3,3},{-3,-3}}) check(center.at(chest[0],2,chest[1])==null,"Center chest must open: "+theme);
            Set<String> palette=new HashSet<>();
            for(MapArt.Cell b:island.cells()) {
                check(b.y>=-8 && b.y<=19,"Playable height envelope");
                palette.add(b.material+":"+b.data);
                // Use the actual 1.8.8 API without coupling the geometry source to Bukkit.
                Class.forName("org.bukkit.Material").getMethod("valueOf",String.class).invoke(null,b.material);
            }
            signatures.add(palette.toString()+island.cells().size());
            MapArt.Scene arena=MapArt.arena(theme,12,false);
            check(arena.at(44,1,0).material.equals("CHEST"),"12-player arena chest setup");
        }
        check(signatures.size()==4,"All themes must have distinct palettes/geometry");
        MapArt.Scene lobby=MapArt.lobby();
        check(lobby.at(0,1,0)==null && lobby.at(0,2,0)==null,"Lobby spawn must be clear");
        for(int n=-36;n<=36;n++) {
            check(lobby.at(n,0,0)!=null,"Walkable east-west lobby bridge at "+n);
            check(lobby.at(0,0,n)!=null,"Walkable north-south lobby bridge at "+n);
        }
        System.out.println("MapArtTest: OK — 4 themes, spawn/bed/chest clearance, 12-player layout and lobby bridges");
    }
}
