package dev.draconicsable.explosion;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.*;

/** Only defer native mass notifications inside our synchronous server removal slice.
 * Per-block mass bookkeeping, colliders, invalid-mass checks and callbacks still run. */
public final class BlastMassBatch implements AutoCloseable {
    private static final ThreadLocal<BlastMassBatch> CURRENT=new ThreadLocal<>();
    private final BlastMassBatch parent=CURRENT.get();
    private final Set<ServerSubLevel> dirty=Collections.newSetFromMap(new IdentityHashMap<>());
    public BlastMassBatch(){CURRENT.set(this);}
    public static boolean defer(ServerSubLevel body) {
        var batch=CURRENT.get();
        if(batch==null || !WorldBlast.removing(body))return false;
        batch.dirty.add(body);return true;
    }
    @Override public void close() {
        if(parent==null)CURRENT.remove();else CURRENT.set(parent);
        if(parent!=null){parent.dirty.addAll(dirty);return;}
        for(var body:dirty)if(!body.isRemoved()) {
            var system=SubLevelPhysicsSystem.get(body.getLevel());
            if(system!=null){body.updateMergedMassData((float)system.getPartialPhysicsTick());system.getPipeline().onStatsChanged(body);}
        }
    }
}
