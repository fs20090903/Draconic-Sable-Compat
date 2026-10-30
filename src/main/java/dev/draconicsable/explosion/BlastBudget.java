package dev.draconicsable.explosion;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Cooperative budget: individual Minecraft chunk/block calls cannot be preempted. */
public final class BlastBudget implements AutoCloseable {
    public static final int REMOVAL_MILLIS=12;
    private static final Map<ServerLevel,FairBlastQueue> USAGE=new WeakHashMap<>();
    private final FairBlastQueue usage;
    private final long start=System.nanoTime();
    private final boolean admitted;
    public final long deadline;
    public BlastBudget(ServerLevel level,Object owner) {
        this(level,owner,2_000_000,6_000_000);
    }
    public static BlastBudget removal(ServerLevel level,Object owner) { return new BlastBudget(level,owner,REMOVAL_MILLIS*1_000_000L,REMOVAL_MILLIS*1_000_000L); }
    public static void forget(ServerLevel level,Object owner) { var usage=USAGE.get(level);if(usage!=null)usage.forget(owner); }
    private BlastBudget(ServerLevel level,Object owner,long slice,long total) {
        usage=USAGE.computeIfAbsent(level,k->new FairBlastQueue());
        long allowance=usage.allowance(owner,level.getGameTime(),slice,total);
        admitted=allowance>0;
        deadline=start+allowance;
    }
    @Override public void close(){if(admitted)usage.charge(System.nanoTime()-start);}
}
