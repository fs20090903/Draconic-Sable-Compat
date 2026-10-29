/*
 * SPDX-License-Identifier: MIT AND LicenseRef-Draconic-Evolution
 * DE-derived portions: Copyright (c) 2013-2023 brandon3055.
 * Original contributions: Copyright (c) 2026 Draconic Sable Compatibility contributors.
 * Adapted from Draconic Evolution's ProcessExplosion.trace (destruction, resistance, fluid and lava rules).
 * Modified for Sable compatibility and/or bounded calculation; see LICENSING.md
 * for the specific adaptations and LICENSES/ for the applicable license texts.
 */
package dev.draconicsable.explosion;

import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.chunk.LevelChunk;

/** DE's vertical ray, without recursion, boxed air sets, or repeated chunk lookups.
 * All access stays on the server thread. Empty-section skips retain air resistance.
 */
public final class BlastTrace implements BlastRings.Trace {
    private static final java.util.Set<BlastTrace> LIVE=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        int count=0;
        for(var trace:java.util.List.copyOf(LIVE))if(trace.level.getServer()==event.getServer()){trace.clear();count++;}
        if(count>0)com.mojang.logging.LogUtils.getLogger().info("[DE blast] released {} terrain preparations on server stop",count);
    }
    private static final double[] HARMONIC=new double[101];
    static {for(int i=1;i<HARMONIC.length;i++)HARMONIC[i]=HARMONIC[i-1]+1.0/i;}
    private static final net.minecraft.server.level.TicketType<String> TICKET=net.minecraft.server.level.TicketType.create("draconic_sable_blast",String::compareTo);
    private final String ticketId=java.util.UUID.randomUUID().toString();
    private final java.util.Set<net.minecraft.world.level.ChunkPos> held=new java.util.HashSet<>();
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet heldKeys=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    private long nextLoadReport;
    private BlastTrace previous;
    public void reuse(BlastTrace previous){this.previous=previous;if(previous!=null)chunks.putAll(previous.chunks);}
    public void releasePrevious(){if(previous!=null){previous.clear();previous=null;}}
    private void hold(net.minecraft.world.level.ChunkPos p){if(heldKeys.add(p.toLong())){held.add(p);level.getChunkSource().addRegionTicket(TICKET,p,0,ticketId);}}
    private final ProcessExplosion process;
    private final ServerLevel level;
    private final WorldBlast bodies;
    private final Long2ObjectOpenHashMap<LevelChunk> chunks=new Long2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
    public long visited,skippedAir,chunkLookups;
    public BlastTrace(ProcessExplosion process,ServerLevel level,WorldBlast bodies) {
        this.process=process;this.level=level;this.bodies=bodies;
        LIVE.add(this);
    }
    private boolean needsTerrain(){return process.origin.y+100>=level.getMinBuildHeight() && process.origin.y-101<level.getMaxBuildHeight();}
    public void reportLoading() {
        if(level.getGameTime()>=nextLoadReport) {
            nextLoadReport=level.getGameTime()+200;
            com.mojang.logging.LogUtils.getLogger().info("[DE blast terrain {}] cached={} held={}",ticketId,chunks.size(),held.size());
        }
    }
    @Override public boolean prepare(int x,int z) {
        if(!needsTerrain())return true;
        long key=net.minecraft.world.level.ChunkPos.asLong(x>>4,z>>4);
        if(!heldKeys.contains(key))hold(new net.minecraft.world.level.ChunkPos(x>>4,z>>4));
        if(!chunks.containsKey(key)) {
            // Keep Minecraft's supported synchronous chunk lifecycle. The enclosing solver
            // yields after this column if loading consumed the slice. One native load can
            // exceed the cooperative budget; never disguise ungenerated terrain as air.
            chunks.put(key,level.getChunk(x>>4,z>>4));chunkLookups++;
        }
        return true;
    }
    public int heldChunks(){return held.size();}
    public void clear(){
        releasePrevious();
        for(var p:held)level.getChunkSource().removeRegionTicket(TICKET,p,0,ticketId);
        held.clear();heldKeys.clear();chunks.clear();LIVE.remove(this);
    }
    @Override public double run(Vector3 p,double power,int distance,int direction,double resistance,int travel){return trace(p,power,distance,direction,resistance,travel);}
    public double trace(Vector3 start,double power,int remaining,int direction,double resistance,int travel) {
        remaining=Math.min(100,remaining);
        int x=(int)Math.floor(start.x),y=(int)Math.floor(start.y),z=(int)Math.floor(start.z);
        // Preserve the upstream floating-height boundary checks, including fractional origin Y.
        double rayY=start.y;
        int min=bodies.minY(),max=bodies.maxY();
        var span=bodies.columnSpan(x,z);
        LevelChunk chunk=null;int terrainTop=Integer.MAX_VALUE;
        while(remaining>0 && power>0 && rayY>=min && rayY<=max) {
            int sectionIndex=level.getSectionIndex(y);
            boolean outside=y<level.getMinBuildHeight() || y>=level.getMaxBuildHeight();
            if(!outside && chunk==null) {
                long key=net.minecraft.world.level.ChunkPos.asLong(x>>4,z>>4);
                chunk=chunks.get(key);
                if(chunk==null){chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)throw new IllegalStateException("Blast column was not prepared");chunks.put(key,chunk);chunkLookups++;}
                // WORLD_SURFACE counts every non-air block, including fluids. Adding one is
                // conservative with respect to heightmap's top/first-available convention.
                terrainTop=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1;
            }
            boolean empty=outside || chunk.getSections()[sectionIndex].hasOnlyAir();
            int run=Math.min(remaining,direction>0?16-(y&15):(y&15)+1);
            if(!outside && y>terrainTop){empty=true;run=direction>0?remaining:Math.min(remaining,y-terrainTop);}
            run=Math.min(run,direction>0?(int)Math.floor(max-rayY)+1:(int)Math.floor(rayY-min)+1);
            // The final downward lava roll is an observable RNG call even through air.
            if(direction<0 && process.lava)run=Math.min(run,Math.max(0,remaining-2));
            if(span!=null) {
                if(direction>0 && y<=span.maxY())run=Math.min(run,Math.max(0,span.minY()-y));
                if(direction<0 && y>=span.minY())run=Math.min(run,Math.max(0,y-span.maxY()));
            }
            if(empty && run>0) {
                // Sum the original 1/(radius * travel) air drag in O(1). Near exhaustion
                // retain the original step-by-step stop point. Difference from recursive
                // addition is roundoff only; block/lava/RNG parity is regression tested.
                double cost=(HARMONIC[travel+run]-HARMONIC[travel])/process.radius;
                int count=0;
                if(power>cost){power-=cost;resistance+=cost;travel+=run;remaining-=run;count=run;}
                else while(count<run && power>0) {
                    double air=1.0/process.radius/(travel+1);
                    power-=air;resistance+=air;travel++;remaining--;count++;
                }
                skippedAir+=count;y+=direction*count;rayY+=direction*count;continue;
            }
            remaining--;travel++;visited++;pos.set(x,y,z);
            double extra=span==null?0:bodies.trace(pos,power,remaining,travel,process.radius,process.maxRadius);
            var state=outside?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);
            double r=1;
            if(!state.isAir()) {
                double effective=power/10 * remaining/(double)(remaining+travel);
                r=state.getBlock().getExplosionResistance();double threshold=r;
                if(threshold>25 && threshold<1_000_000 && remaining<process.maxRadius*.75)threshold=25;
                long packed=pos.asLong();
                if(effective>=threshold)process.destroyedCache.add(packed);
                else if(state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                    if(effective>5)process.destroyedCache.add(packed);else process.blocksToUpdate.add(packed);
                    r=10;
                } else if(!state.getFluidState().isEmpty() || state.getBlock() instanceof FallingBlock)
                    process.blocksToUpdate.add(packed);
                r=Math.min(r,1000);
            }
            r=r/process.radius/travel;resistance+=r;power-=r;
            if(remaining==1 && direction==-1 && process.lava && level.random.nextInt(250)==0 && !level.isEmptyBlock(pos.below())) {
                remaining=0;long packed=pos.asLong();process.destroyedCache.remove(packed);
                process.lavaPositions.add(packed);process.blocksToUpdate.add(packed);
            }
            power-=extra;resistance+=extra;y+=direction;rayY+=direction;
        }
        return resistance;
    }
}
