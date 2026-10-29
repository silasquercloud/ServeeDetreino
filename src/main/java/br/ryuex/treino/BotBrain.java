package br.ryuex.treino;

/** Deterministic decision rules, independent of Bukkit's mob pathfinding. */
final class BotBrain {
    static final class Memory {
        double previousHealth=20, previousDistance=100;int side, nextSwitch;
        void observe(double distance,double health,int tick){
            if(health<previousHealth && tick>=nextSwitch){side^=1;nextSwitch=tick+30;}
            previousHealth=health;previousDistance=distance;
        }
        boolean retreat(double health,double enemyHealth){return health<=6 && enemyHealth>health+4;}
    }
    enum State { COMBAT, RUSH_BED, HUNT, RESUPPLY }
    static State state(double enemyDistanceSquared,boolean enemyBed,boolean restocking){
        if(enemyDistanceSquared<=49)return State.COMBAT;
        if(restocking)return State.RESUPPLY;
        return enemyBed?State.RUSH_BED:State.HUNT;
    }
    static boolean canHit(double distanceSquared,double height,boolean lineOfSight,int tick,int nextAttack){return distanceSquared<=8.41 && Math.abs(height)<=2.3 && lineOfSight && tick>=nextAttack;}
    static double[] movement(double dx,double dz,double distanceSquared,int tick,int parity,boolean hard){
        double length=Math.sqrt(dx*dx+dz*dz);if(length<0.001)return new double[]{0,0};dx/=length;dz/=length;
        double forward=distanceSquared<2.89?-0.12:distanceSquared>6.25?(hard?0.25:0.19):0.035;
        double strafe=distanceSquared<=16?(((tick/20+parity)%2==0?1:-1)*(hard?0.13:0.08)):0;
        return new double[]{dx*forward-dz*strafe,dz*forward+dx*strafe};
    }
    static int mineTicks(String material,boolean shears,boolean axe,boolean pick){
        if(material.equals("WOOL"))return shears?6:18;
        if(material.equals("WOOD") || material.equals("LOG"))return axe?12:45;
        if(material.equals("OBSIDIAN"))return pick?160:600;
        return pick?24:60;
    }
    static boolean validBridgeCell(int x,int y,int z){return Math.abs(x)<=65 && Math.abs(z)<=65 && y>=60 && y<=80;}
}
