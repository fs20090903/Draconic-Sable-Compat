/*
 * SPDX-License-Identifier: MIT AND LicenseRef-Draconic-Evolution
 * DE-derived portions: Copyright (c) 2013-2023 brandon3055.
 * Original contributions: Copyright (c) 2026 Draconic Sable Compatibility contributors.
 * Adapted from Draconic Evolution's ProcessExplosion.trace (destruction and resistance rules in trace).
 * Modified for Sable compatibility and/or bounded calculation; see LICENSING.md
 * for the specific adaptations and LICENSES/ for the applicable license texts.
 */
package dev.draconicsable.explosion;

import com.mojang.serialization.Codec;
import com.brandon3055.brandonscore.handlers.IProcess;
import com.brandon3055.brandonscore.handlers.ProcessHandler;
import dev.draconicsable.LoadedAccess;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A world-space voxel snapshot of loaded bodies intersecting DE's blast calculation. */
public final class WorldBlast implements AutoCloseable {
    private static final org.slf4j.Logger LOG=com.mojang.logging.LogUtils.getLogger();
    public static final SubLevelLoadingTicketType<String> TICKET=SubLevelLoadingTicketType.create(
        ResourceLocation.fromNamespaceAndPath("draconic_sable_compat","blast_in_progress"),Codec.STRING);
    private static final Map<ServerLevel,Set<String>> LIVE=new WeakHashMap<>();
    private static final Set<WorldBlast> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ServerSubLevel,Integer> REMOVING=new WeakHashMap<>();
    private record Voxel(UUID body,BlockPos pos,BlockState state) {}
    private record Section(ServerSubLevel body,Pose3d pose,LevelChunkSection section,int x,int y,int z) {}
    private final ServerLevel level;
    private final String id=UUID.randomUUID().toString();
    private final List<ServerSubLevel> held=new ArrayList<>();
    private final Long2ObjectOpenHashMap<List<Voxel>> cells=new Long2ObjectOpenHashMap<>();
    public record ColumnSpan(int minY,int maxY) {}
    private final Long2ObjectOpenHashMap<ColumnSpan> columns=new Long2ObjectOpenHashMap<>();
    public ColumnSpan columnSpan(int x,int z){return columns.get(BlockPos.asLong(x,0,z));}
    private final List<Voxel> destroyed=new ArrayList<>();
    private final Map<ServerSubLevel,Pose3d> poses=new IdentityHashMap<>();
    private final ArrayDeque<Section> sections=new ArrayDeque<>();
    private final Vec3 origin;
    private final int radius;
    private final BoundingBox3d bounds;
    private int cursor,removalCursor;
    private boolean applying;
    private boolean closed;
    private int minY,maxY;
    private int captured,traced,removed,wrongOwner,unloaded,replaced,failedRemoval;
    private long nextReport;
    private boolean capturedReported;
    private BlastHold hold;
    private long removalStarted;
    public static final int MAX_REMOVALS_PER_SLICE=8192;
    private int removalSlices,timeLimitedSlices,capLimitedSlices,maxBatch;
    private long removalNanos,maxSliceNanos;

    public WorldBlast(ServerLevel level,Vec3 origin,int radius) {
        this.level=level;this.origin=origin;this.radius=radius;minY=level.getMinBuildHeight();maxY=level.getMaxBuildHeight();
        LIVE.computeIfAbsent(level,k->new HashSet<>()).add(id);
        ACTIVE.add(this);
        var container=SubLevelContainer.getContainer(level);
        // DE's vertical traces are bounded to 100 steps, independently of horizontal radius.
        bounds=new BoundingBox3d(origin.x-radius-2,origin.y-102,origin.z-radius-2,
            origin.x+radius+2,origin.y+102,origin.z+radius+2);
        try {
            for(var body:List.copyOf(container.getAllSubLevels())) {
                if(body.isRemoved() || !body.boundingBox().intersects(bounds))continue;
                var pose=new Pose3d(body.logicalPose());poses.put(body,pose);
                container.addForceLoadTicket(body,TICKET,id);held.add(body);
                for(var holder:List.copyOf(body.getPlot().getLoadedChunks())) {
                    var chunk=holder.getChunk();if(chunk==null)continue;
                    var sections=chunk.getSections();int baseX=chunk.getPos().getMinBlockX(),baseZ=chunk.getPos().getMinBlockZ();
                    for(int sy=0;sy<sections.length;sy++) {
                        var section=sections[sy];if(section.hasOnlyAir())continue;
                        int baseY=level.getMinBuildHeight()+sy*16;
                        this.sections.add(new Section(body,pose,section,baseX,baseY,baseZ));
                    }
                }
            }
            LOG.info("[DE blast {}] prepared dimension={} origin={} radius={} bodies={} sections={}",
                id,level.dimension().location(),origin,radius,held.size(),sections.size());
            nextReport=level.getGameTime()+200;
        } catch(RuntimeException|Error e){close();throw e;}
    }

    /** Scan only occupied plot sections, in resumable batches, never at idle. */
    public boolean capture(long deadline) {
        int scanned=0;
        while(!sections.isEmpty()) {
            if((scanned++&63)==0 && System.nanoTime()>=deadline)return false;
            var s=sections.peek();int x=cursor&15,z=(cursor>>4)&15,y=cursor>>8;
            var state=s.section.getBlockState(x,y,z);
            if(!state.isAir()) {
                var pos=new BlockPos(s.x+x,s.y+y,s.z+z);
                var world=s.pose.transformPosition(Vec3.atCenterOf(pos));
                if(Math.abs(world.x-origin.x)<=radius+1 && Math.abs(world.z-origin.z)<=radius+1 && Math.abs(world.y-origin.y)<=101) {
                    var cell=BlockPos.containing(world);
                    cells.computeIfAbsent(cell.asLong(),k->new ArrayList<>(1)).add(new Voxel(s.body.getUniqueId(),pos,state));
                    long column=BlockPos.asLong(cell.getX(),0,cell.getZ());
                    var previous=columns.get(column);
                    columns.put(column,new ColumnSpan(previous==null?cell.getY():Math.min(previous.minY(),cell.getY()),
                        previous==null?cell.getY():Math.max(previous.maxY(),cell.getY())));
                    captured++;
                    minY=Math.min(minY,cell.getY()-1);maxY=Math.max(maxY,cell.getY()+2);
                }
            }
            if(++cursor==4096){cursor=0;sections.remove();}
        }
        if(!capturedReported){capturedReported=true;LOG.info("[DE blast {}] snapshot captured={} worldCells={}",id,captured,cells.size());}
        return true;
    }

    /** Only called once per bounded calculation slice; no additional block scanning. */
    public void reportProgress(int ring,boolean complete) {
        if(complete || level.getGameTime()>=nextReport) {
            LOG.info("[DE blast {}] calculation ring={}/{} complete={} captured={} traced={} selected={}",
                id,ring,radius,complete,captured,traced,destroyed.size());
            nextReport=level.getGameTime()+200;
        }
    }

    /** Cheap pose/body-membership check; no block scan. Translation/rotation tolerance 1/16 block. */
    public boolean matches() {
        int count=0;
        for(var body:SubLevelContainer.getContainer(level).getAllSubLevels()) {
            if(body.isRemoved() || !body.boundingBox().intersects(bounds))continue;
            var old=poses.get(body);if(old==null)return false;count++;
            var center=Vec3.atCenterOf(body.getPlot().getCenterBlock());
            for(var delta:List.of(Vec3.ZERO,new Vec3(radius,0,0),new Vec3(0,radius,0),new Vec3(0,0,radius))) {
                var p=center.add(delta);
                if(old.transformPosition(p).distanceToSqr(body.logicalPose().transformPosition(p))>1.0/256)return false;
            }
        }
        return count==poses.size();
    }

    /** DE's same per-cell resistance and break threshold, applied to every overlapping body. */
    public double trace(BlockPos worldCell,double power,int remaining,int travelled,int ring,int maxRadius) {
        var voxels=cells.remove(worldCell.asLong());if(voxels==null)return 0;
        double resistance=0;
        for(var voxel:voxels) {
            traced++;
            var state=voxel.state();double r=state.getBlock().getExplosionResistance();
            double threshold=r;
            if(threshold>25 && threshold<1_000_000 && remaining<maxRadius*.75)threshold=25;
            double effective=power/10 * remaining/(double)(remaining+travelled);
            if(effective>=threshold)destroyed.add(voxel);
            else if(state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                if(effective>5)destroyed.add(voxel);
                r=10;
            }
            resistance+=Math.min(r,1000)/Math.max(1,ring)/Math.max(1,travelled);
        }
        return resistance;
    }

    public int minY(){return minY;}
    public int maxY(){return maxY;}
    public int pendingBlocks(){return destroyed.size();}

    public void apply() {
        if(applying || closed)return;
        applying=true;cells.clear();sections.clear();
        removalStarted=level.getGameTime();
        LOG.info("[DE blast {}] applying sublevel damage captured={} traced={} selected={}",id,captured,traced,destroyed.size());
        for(var body:held)REMOVING.merge(body,1,Integer::sum);
        try {
            var damaged=new HashSet<UUID>();for(var voxel:destroyed)damaged.add(voxel.body());
            hold=new BlastHold(held.stream().filter(b->damaged.contains(b.getUniqueId())).toList());
        ProcessHandler.addProcess(new IProcess(){
            @Override public void updateProcess(){
                if(closed)return;
                long started=System.nanoTime();int blocks=0;
                try(var budget=BlastBudget.removal(level);var mass=new BlastMassBatch()) {
                    while(removalCursor<destroyed.size() && blocks<MAX_REMOVALS_PER_SLICE && System.nanoTime()<budget.deadline) {
                        blocks++;
                        var voxel=destroyed.get(removalCursor++);
                        var owner=Sable.HELPER.getContaining(level,voxel.pos());
                        if(owner==null || !owner.getUniqueId().equals(voxel.body())){wrongOwner++;continue;}
                        var chunk=LoadedAccess.chunk(level,voxel.pos());
                        if(chunk==null){unloaded++;continue;}
                        if(chunk.getBlockState(voxel.pos()).getBlock()!=voxel.state().getBlock()){replaced++;continue;}
                        if(level.removeBlock(voxel.pos(),false))removed++;else failedRemoval++;
                    }
                    if(removalCursor<destroyed.size()) {
                        if(blocks>=MAX_REMOVALS_PER_SLICE)capLimitedSlices++;else timeLimitedSlices++;
                    }
                }catch(RuntimeException|Error e){close();throw e;}
                long elapsed=System.nanoTime()-started;
                removalSlices++;maxBatch=Math.max(maxBatch,blocks);removalNanos+=elapsed;maxSliceNanos=Math.max(maxSliceNanos,elapsed);
                if(removalCursor==destroyed.size())close();
            }
            @Override public boolean isDead(){return closed;}
        });
        }catch(RuntimeException|Error e){close();throw e;}
    }
    public static boolean removing(ServerSubLevel body){return REMOVING.containsKey(body) || BlastHold.held(body);}

    @Override public void close() {
        if(closed)return;closed=true;
        ACTIVE.remove(this);
        if(hold!=null){hold.close();hold=null;}
        if(applying)LOG.info("[DE blast {}] removal ended ticks={} cap={} budgetMs={} slices={} timeLimited={} capLimited={} maxBatch={} workMs={} maxSliceMs={}",
            id,level.getGameTime()-removalStarted,MAX_REMOVALS_PER_SLICE,BlastBudget.REMOVAL_MILLIS,
            removalSlices,timeLimitedSlices,capLimitedSlices,maxBatch,removalNanos/1_000_000.0,maxSliceNanos/1_000_000.0);
        LOG.info("[DE blast {}] closed applying={} captured={} traced={} selected={} processed={} removed={} wrongOwner={} unloaded={} replaced={} failedRemoval={}",
            id,applying,captured,traced,destroyed.size(),removalCursor,removed,wrongOwner,unloaded,replaced,failedRemoval);
        var container=SubLevelContainer.getContainer(level);
        for(var body:held){container.removeForceLoadTicket(body,TICKET,id);if(applying)REMOVING.computeIfPresent(body,(b,n)->n==1?null:n-1);}
        var active=LIVE.get(level);if(active!=null){active.remove(id);if(active.isEmpty())LIVE.remove(level);}
        held.clear();poses.clear();sections.clear();cells.clear();columns.clear();destroyed.clear();
    }

    /** ProcessHandler is not persisted by DE. Never leave its temporary tickets behind after restart. */
    public static boolean orphaned(ServerLevel level,String key){return !LIVE.getOrDefault(level,Set.of()).contains(key) && !BlastHold.ownsTicket(key);}
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for(var blast:List.copyOf(ACTIVE))if(blast.level.getServer()==event.getServer())blast.close();
        BlastHold.stopping(event);
    }
}
