package br.ryuex.treino;

import java.io.*;
import java.util.*;

/** Single geometry source for the Bukkit world and every gallery projection. */
public final class MapArt {
    public enum Theme {
        POKEMON("Pokémon",14), EVANGELION("Evangelion",10), SONIC("Sonic",11), ONEPIECE("One Piece",1);
        public final String title; public final int color;
        Theme(String title,int color) { this.title=title; this.color=color; }
        public static Theme parse(String text) { return valueOf(text.toUpperCase(Locale.ROOT).replace("_", "").replace("-", "")); }
    }
    public static final class Cell {
        public final int x,y,z,data; public final String material;
        Cell(int x,int y,int z,String material,int data) { this.x=x;this.y=y;this.z=z;this.material=material;this.data=data; }
    }
    public static final class Scene {
        private final Map<String,Cell> blocks=new LinkedHashMap<>();
        public void set(int x,int y,int z,String material,int data) {
            String key=x+":"+y+":"+z;
            if(material.equals("AIR")) blocks.remove(key); else blocks.put(key,new Cell(x,y,z,material,data));
        }
        public void set(int x,int y,int z,String material) { set(x,y,z,material,0); }
        public Cell at(int x,int y,int z) { return blocks.get(x+":"+y+":"+z); }
        public Collection<Cell> cells() { return Collections.unmodifiableCollection(blocks.values()); }
        public void add(Scene other,int dx,int dy,int dz) { for(Cell b:other.cells()) set(b.x+dx,b.y+dy,b.z+dz,b.material,b.data); }
        public void box(int x1,int y1,int z1,int x2,int y2,int z2,String material,int data) {
            for(int x=x1;x<=x2;x++) for(int y=y1;y<=y2;y++) for(int z=z1;z<=z2;z++) set(x,y,z,material,data);
        }
        public void disk(int cx,int y,int cz,int r,String material,int data) {
            for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) if(x*x+z*z<=r*r) set(cx+x,y,cz+z,material,data);
        }
    }
    private static void wool(Scene s,int x,int y,int z,int color) { s.set(x,y,z,"WOOL",color); }
    private static void tree(Scene s,int x,int z,boolean palm) {
        for(int y=1;y<=6;y++) s.set(x+(palm?y/4:0),y,z,"LOG");
        int crownX=x+(palm?1:0);
        if(palm) {
            for(int i=-3;i<=3;i++) { s.set(crownX+i,7-Math.abs(i)/2,z,"LEAVES",4);s.set(crownX,7-Math.abs(i)/2,z+i,"LEAVES",4); }
            s.set(crownX,8,z,"LEAVES",4);
        } else {
            s.disk(x,5,z,2,"LEAVES",4);s.disk(x,6,z,2,"LEAVES",4);s.disk(x,7,z,1,"LEAVES",4);
        }
    }
    private static void ball(Scene s,int cx,int cy,int cz) {
        for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) if(x*x+y*y+z*z<=6) {
            wool(s,cx+x,cy+y,cz+z,y==0?15:y>0?14:0);
        }
        wool(s,cx,cy,cz-3,0);
    }
    private static void ring(Scene s,int cx,int cy,int cz,int radius,String material,int data) {
        for(int x=-radius;x<=radius;x++) for(int y=-radius;y<=radius;y++) {
            double d=Math.sqrt(x*x+y*y);
            if(d<=radius+0.35 && d>=radius-0.65) s.set(cx+x,cy+y,cz,material,data);
        }
    }
    private static void decorate(Scene s,Theme theme,int shift) {
        switch(theme) {
        case POKEMON:
            // Open-front Pokémon center with a red stepped roof, windows and medical cross.
            s.box(-3,0,3+shift,3,0,7+shift,"QUARTZ_BLOCK",0);
            s.box(-3,1,3+shift,3,4,7+shift,"WOOL",0);
            s.box(-2,1,4+shift,2,3,6+shift,"AIR",0);
            s.box(0,1,3+shift,0,2,3+shift,"AIR",0);
            for(int x:new int[]{-2,2}) s.box(x,2,3+shift,x,3,3+shift,"STAINED_GLASS",3);
            s.box(-4,5,2+shift,4,5,8+shift,"WOOL",14);
            s.box(-3,6,3+shift,3,6,7+shift,"WOOL",14);
            for(int a=-1;a<=1;a++) { s.set(a,7,5+shift,"QUARTZ_BLOCK");s.set(0,7,5+shift+a,"QUARTZ_BLOCK"); }
            ball(s,-5,3,shift); tree(s,5,4+shift,false);
            break;
        case EVANGELION:
            // Tokyo-3 towers, NERV-inspired pyramid and an angular EVA-01 head.
            for(int x:new int[]{-5,5}) {
                int h=x<0?12:9;
                s.box(x-1,1,3+shift,x+1,h,5+shift,"STAINED_CLAY",9);
                for(int y=2;y<h;y+=2) for(int dx=-1;dx<=1;dx++) s.set(x+dx,y,2+shift,"STAINED_GLASS",5);
                s.set(x,h+1,4+shift,"SEA_LANTERN");
            }
            for(int y=1;y<=4;y++) s.box(-4+y,y,6+shift-4+y,4-y,y,6+shift+4-y,"STAINED_CLAY",8);
            s.box(-2,5,5+shift,2,10,6+shift,"WOOL",10);
            s.box(-1,6,4+shift,1,9,4+shift,"WOOL",15);
            wool(s,-1,8,3+shift,0);wool(s,1,8,3+shift,0);wool(s,0,6,3+shift,5);
            for(int x:new int[]{-2,2}) { s.box(x,8,4+shift,x,12,4+shift,"WOOL",5);wool(s,x,13,4+shift,10); }
            for(int x:new int[]{-6,6}) { s.box(x,1,-3+shift,x,4,-3+shift,"WOOL",10);wool(s,x,5,-3+shift,5); }
            break;
        case SONIC:
            // Green Hill loop, gold rings, palm and checkerboard cliffs.
            ring(s,0,5,5+shift,4,"STAINED_CLAY",1);
            ring(s,0,5,6+shift,4,"STAINED_CLAY",12);
            for(int x:new int[]{-5,5}) ring(s,x,3,-1+shift,1,"GOLD_BLOCK",0);
            tree(s,-5,4+shift,true);
            s.box(4,1,4+shift,6,1,6+shift,"STAINED_CLAY",1);
            s.box(5,2,5+shift,6,2,6+shift,"GRASS",0);
            break;
        case ONEPIECE:
            // Pirate ship, mast, broad sail, pixel Jolly Roger and straw-hat monument.
            s.box(-5,1,3+shift,5,2,7+shift,"WOOD",1);
            s.box(-6,2,4+shift,6,3,6+shift,"WOOD",0);
            s.box(-5,4,3+shift,5,4,3+shift,"FENCE",0);
            s.box(-5,4,7+shift,5,4,7+shift,"FENCE",0);
            s.box(-1,4,3+shift,1,4,3+shift,"AIR",0);
            s.box(-6,4,4+shift,-6,4,6+shift,"FENCE",0);
            s.box(6,4,4+shift,6,4,6+shift,"FENCE",0);
            // Stair-sized approach leaves the central spawn lane clear.
            s.box(-1,1,2+shift,1,1,2+shift,"WOOD",0);
            s.box(-1,2,3+shift,1,2,3+shift,"WOOD",0);
            s.box(0,4,5+shift,0,16,5+shift,"LOG",0);
            s.box(-4,8,4+shift,4,14,4+shift,"WOOL",0);
            int[][] skull={{-1,12},{0,12},{1,12},{-2,11},{2,11},{-2,10},{0,10},{2,10},{-1,9},{0,9},{1,9},{-3,8},{3,8},{-3,13},{3,13}};
            for(int[] p:skull) wool(s,p[0],p[1],3+shift,15);
            s.box(1,16,5+shift,3,17,5+shift,"WOOL",15);
            s.disk(-5,2,-2+shift,2,"STAINED_CLAY",4);s.disk(-5,3,-2+shift,1,"WOOL",14);s.disk(-5,4,-2+shift,1,"HAY_BLOCK",0);
            tree(s,6,-1+shift,true);
            break;
        default: throw new IllegalArgumentException("Unknown theme");
        }
    }
    public static Scene island(Theme theme,int radius) {
        Scene s=new Scene();
        for(int depth=0;depth<=6;depth++) {
            int r=depth==0?radius:Math.max(1,radius-(depth-1)*2);
            s.disk(0,-depth,0,r,depth==0?"GRASS":depth<3?"DIRT":"STONE",0);
        }
        if(theme==Theme.SONIC) {
            for(Cell b:new ArrayList<>(s.cells())) if(b.y<0) s.set(b.x,b.y,b.z,"STAINED_CLAY",(Math.floorMod(b.x,4)/2+Math.floorMod(b.z,4)/2+Math.floorMod(b.y,4)/2)%2==0?12:1);
        } else if(theme==Theme.EVANGELION) {
            s.disk(0,0,0,radius,"SMOOTH_BRICK",0);
            for(int x=-radius;x<=radius;x++) if(x*x+9<=radius*radius) { wool(s,x,0,-3,x%2==0?4:15); }
        } else if(theme==Theme.ONEPIECE) {
            s.disk(0,0,0,radius,"SANDSTONE",0);
            for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) if(x*x+z*z<=radius*radius && x*x+z*z>(radius-1)*(radius-1)) s.set(x,0,z,"STAINED_GLASS",3);
        }
        decorate(s,theme,radius>=13?4:0);
        return s;
    }
    public static int[] diamondPosition(int index) {double angle=Math.PI/4+index*Math.PI/2;return new int[]{(int)Math.round(Math.cos(angle)*24),(int)Math.round(Math.sin(angle)*24)};}
    public static Scene diamondOutpost(Theme theme) {
        Scene s=new Scene();s.disk(0,0,0,4,"SMOOTH_BRICK",0);s.disk(0,-1,0,3,"STONE",0);s.disk(0,-2,0,2,"STONE",0);
        s.set(0,0,0,"DIAMOND_BLOCK");for(int x:new int[]{-3,3})for(int z:new int[]{-1,1})s.set(x,0,z,"WOOL",theme.color);
        return s;
    }
    public static Scene bedwarsArena(Theme theme,int count) {
        Scene s=arena(theme,count,true);for(int i=0;i<4;i++){int[] p=diamondPosition(i);s.add(diamondOutpost(theme),p[0],0,p[1]);}return s;
    }
    public static Scene center(Theme theme) { return island(theme,13); }
    public static Scene lobby() {
        Scene s=new Scene();
        for(int depth=0;depth<=5;depth++) s.disk(0,-depth,0,14-depth*2,depth==0?"QUARTZ_BLOCK":"STAINED_CLAY",depth==0?0:15);
        for(int x=-13;x<=13;x++) for(int z=-13;z<=13;z++) {
            int r=x*x+z*z;
            if(r>=156 && r<=180) s.set(x,0,z,"SEA_LANTERN");
            if(r>9 && r<90 && (Math.abs(x)<=1 || Math.abs(z)<=1)) wool(s,x,0,z,x>1?14:x< -1?10:z>1?11:1);
        }
        Theme[] themes=Theme.values();
        int[][] positions={{36,0},{-36,0},{0,36},{0,-36}};
        for(int i=0;i<4;i++) {
            int cx=positions[i][0],cz=positions[i][1];Theme t=themes[i];
            s.add(island(t,10),cx,0,cz);
            for(int n=12;n<=28;n++) for(int a=-3;a<=3;a++) {
                int x=cx==0?a:(cx>0?n:-n),z=cz==0?a:(cz>0?n:-n);
                s.set(x,-1,z,"STAINED_CLAY",15);s.set(x,0,z,Math.abs(a)==3?"SEA_LANTERN":"QUARTZ_BLOCK");
                if(a==0) wool(s,x,0,z,t.color);
                if(Math.abs(a)==3) s.set(x,1,z,"STAINED_GLASS",t.color);
            }
            // A colored gate on each wing, connected by safe walkable bridges.
            if(cx!=0) {
                int x=cx>0?23:-23;
                for(int z:new int[]{-4,4}) s.box(x,1,z,x,6,z,"QUARTZ_BLOCK",0);
                s.box(x,7,-4,x,7,4,"WOOL",t.color);
            } else {
                int z=cz>0?23:-23;
                for(int x:new int[]{-4,4}) s.box(x,1,z,x,6,z,"QUARTZ_BLOCK",0);
                s.box(-4,7,z,4,7,z,"WOOL",t.color);
            }
        }
        // Four corner beacons keep the spawn unobstructed and frame the plaza.
        for(int x:new int[]{-8,8}) for(int z:new int[]{-8,8}) {
            s.box(x,1,z,x,3,z,"QUARTZ_BLOCK",0);s.set(x,4,z,"SEA_LANTERN");
        }
        return s;
    }
    public static Scene arena(Theme theme,int count,boolean beds) {
        Scene s=center(theme);
        for(int i=0;i<count;i++) {
            double angle=2*Math.PI*i/count;int x=(int)Math.round(Math.cos(angle)*42),z=(int)Math.round(Math.sin(angle)*42);
            s.add(island(theme,8),x,0,z);
            if(beds) {
                s.set(x+2,1,z,"BED_BLOCK",0);s.set(x+2,1,z+1,"BED_BLOCK",8);s.set(x-2,1,z,"WORKBENCH");
            } else { s.set(x+2,1,z,"CHEST");s.set(x-2,1,z,"CHEST"); }
        }
        if(!beds) {s.set(0,1,0,"CHEST");s.set(3,1,3,"CHEST");s.set(-3,1,-3,"CHEST");}
        return s;
    }
    public static void export(Scene s,File file) throws IOException {
        try(PrintWriter out=new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"))) {
            out.print("[");boolean first=true;
            for(Cell b:s.cells()) { if(!first)out.print(",");first=false;out.print("["+b.x+","+b.y+","+b.z+",\""+b.material+"\","+b.data+"]"); }
            out.print("]");
        }
    }
    public static void main(String[] args) throws IOException {
        File dir=new File(args.length==0?".cache/scenes":args[0]);dir.mkdirs();
        export(lobby(),new File(dir,"lobby.json"));
        export(bedwarsArena(Theme.POKEMON,4),new File(dir,"bedwars.json"));
        export(arena(Theme.ONEPIECE,2,true),new File(dir,"bedfight.json"));
        for(Theme t:Theme.values()) { export(island(t,8),new File(dir,t.name().toLowerCase(Locale.ROOT)+".json"));export(arena(t,6,false),new File(dir,"arena-"+t.name().toLowerCase(Locale.ROOT)+".json")); }
        System.out.println("Exported exact map geometry to "+dir);
    }
}
