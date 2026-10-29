package dev.draconicsable.explosion;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Cooperative budget: individual Minecraft chunk/block calls cannot be preempted. */
public final class BlastBudget implements AutoCloseable {
    public static final int REMOVAL_MILLIS=12;
    private static final Map<ServerLevel,Usage> USAGE=new WeakHashMap<>();
    private static final class Usage { long tick=Long.MIN_VALUE, nanos; }
    private final Usage usage;
    private final long start=System.nanoTime();
    public final long deadline;
    public BlastBudget(ServerLevel level) {
        this(level,2_000_000,6_000_000);
    }
    public static BlastBudget removal(ServerLevel level) { return new BlastBudget(level,REMOVAL_MILLIS*1_000_000L,REMOVAL_MILLIS*1_000_000L); }
    private BlastBudget(ServerLevel level,long slice,long total) {
        usage=USAGE.computeIfAbsent(level,k->new Usage());
        if(usage.tick!=level.getGameTime()){usage.tick=level.getGameTime();usage.nanos=0;}
        deadline=start+Math.max(0,Math.min(slice,total-usage.nanos));
    }
    @Override public void close(){usage.nanos+=System.nanoTime()-start;}
}
