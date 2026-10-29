/*
 * SPDX-License-Identifier: MIT AND LicenseRef-Draconic-Evolution
 * DE-derived portions: Copyright (c) 2013-2023 brandon3055.
 * Original contributions: Copyright (c) 2026 Draconic Sable Compatibility contributors.
 * Adapted from Draconic Evolution's ProcessExplosion.updateCalculation (annulus and blast formulas).
 * Modified for Sable compatibility and/or bounded calculation; see LICENSING.md
 * for the specific adaptations and LICENSES/ for the applicable license texts.
 */
package dev.draconicsable.explosion;

import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.utils.SimplexNoise;
import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import net.minecraft.server.level.ServerLevel;
import java.util.HashSet;

/** DE's annulus, power and resistance formulas, resumed between ray columns. */
public final class BlastRings {
    public static final int MAX_COLUMNS=16384;
    @FunctionalInterface public interface Trace {
        double run(Vector3 p,double power,int distance,int direction,double resistance,int travel);
        default boolean prepare(int x,int z){return true;}
    }
    private int ring=-1,x,z,outer,inner;
    private boolean row;
    // Exact accepted column order of the original square scan, without its rejected interior.
    public boolean step(ProcessExplosion p,ServerLevel level,Trace trace,long deadline) {
        var origin=p.origin.pos();int columns=0;
        while(p.radius<p.maxRadius) {
        if(ring!=p.radius){ring=p.radius;x=-ring;row=false;}
        while(x<ring) {
            if(System.nanoTime()>=deadline || columns>=MAX_COLUMNS)return false;
            if(!row) {
                int square=ring*ring-x*x;
                if(square<=0){x++;continue;}
                outer=(int)Math.ceil(Math.sqrt(square))-1;
                inner=(int)Math.ceil(Math.sqrt(Math.max(0,(ring-1)*(ring-1)-x*x)));
                z=-outer;row=true;
            }
            if(inner>0 && z>-inner && z<inner)z=inner;
            if(z>outer){x++;row=false;continue;}
            if(!trace.prepare(origin.getX()+x,origin.getZ()+z))return false;
            column(p,level,trace,origin.getX()+x,origin.getZ()+z);z++;columns++;
        }
        double sum=0;for(double r:p.angularResistance)sum+=r;
        p.meanResistance=sum/p.angularResistance.length;
        p.radius++;p.circumference=Math.PI*2*p.radius;
        p.destroyedBlocks.add(p.destroyedCache);p.destroyedCache=new HashSet<>();p.scannedCache=new HashSet<>();
        if(System.nanoTime()>=deadline)return p.radius>=p.maxRadius;
        }
        return true;
    }
    private static void column(ProcessExplosion p,ServerLevel level,Trace trace,int x,int z) {
        Vector3 up=new Vector3(x+.5,p.origin.y,z+.5),down=new Vector3(up);
        double angle=p.getRadialAngle(up),load=p.meanResistance/p.getRadialResistance(angle);
        double radial=1-(double)p.radius/p.maxRadius;
        double falloff=Math.max(0,(radial-.8)*5);falloff=1-(1-falloff)*(1-falloff)*(1-falloff);
        double height=falloff*20*((double)p.maxRadius/150);
        double noise=Math.max(0,(-radial+.2)*5),scatter=noise*level.random.nextInt(10);
        double sim=SimplexNoise.noise((double)x/50,(double)z/50);
        noise=1+Math.abs(sim)*noise*8;
        double power=10000*radial*radial*radial*load*noise+scatter;
        double upHeight=20+(5+(double)p.radius/10)*load;
        double downHeight=height+(5+(double)p.radius/10)*load*(1-falloff);
        double resistance=trace.run(up,power,(int)(upHeight+Math.abs(sim)*4+level.random.nextDouble())*3,1,0,0);
        resistance+=trace.run(down.subtract(0,1,0),power,(int)(downHeight+Math.abs(sim)*4+level.random.nextDouble()),-1,0,0);
        if(radial<.8)p.addRadialResistance(angle,resistance/load);
    }
}
