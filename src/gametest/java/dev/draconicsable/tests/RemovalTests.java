package dev.draconicsable.tests;

import dev.draconicsable.explosion.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import java.util.*;

/** Separate namespace: live physics must not share the globally paused solver tests. */
@GameTestHolder("draconic_removal_tests") @PrefixGameTestTemplate(false)
public class RemovalTests {
    @GameTest(template="empty",timeoutTicks=500)
    public void activePhysicsOverlappingHoldsMassBatchAndBulkRemoval(GameTestHelper h) {
        var l=h.getLevel();var source=new BlockPos(-9000,200,-9000);
        var blocks=new ArrayList<BlockPos>();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<8;y++) {
            var p=source.offset(x,y,z);blocks.add(p);l.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        }
        l.setBlock(source,Blocks.BEDROCK.defaultBlockState(),2);
        var b=SubLevelAssemblyHelper.assembleBlocks(l,source,blocks,BoundingBox3i.from(blocks));
        var c=SubLevelContainer.getContainer(l);var system=c.physicsSystem();var pipeline=system.getPipeline();
        c.addForceLoadTicket(b,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
        h.assertTrue(!system.getPaused(),"Real physics running for freeze regression");
        var p=b.getPlot().getCenterBlock();var start=new Vector3d(b.logicalPose().position());
        var pinned=new Vec3[1];var drift=new double[1];
        h.onEachTick(()->{
            if(pinned[0]!=null && BlastHold.held(b) && !b.isRemoved())
                drift[0]=Math.max(drift[0],pinned[0].distanceTo(b.logicalPose().transformPosition(Vec3.atCenterOf(p))));
        });
        // Existing user/staff-style anchor must survive our independent temporary anchors.
        var user=pipeline.addConstraint(null,b,new FixedConstraintConfiguration(new Vector3d(start),new Vector3d(b.logicalPose().rotationPoint()),new Quaterniond(b.logicalPose().orientation())));
        h.assertTrue(user!=null && user.isValid(),"Native constraint backend available");
        pipeline.addLinearAndAngularVelocity(b,new Vector3d(1,0,0),new Vector3d());
        var first=new BlastHold(List.of(b));var second=new BlastHold(List.of(b));
        h.assertTrue(BlastHold.anchored(b),"Native temporary anchor created");
        first.close();
        h.startSequence().thenIdle(6).thenExecute(()->{
            h.assertTrue(BlastHold.anchored(b),"First release does not release overlapping hold");
            h.assertTrue(b.logicalPose().position().distance(start)<0.05,"Held body remains stationary with physics active");
            second.close();
        }).thenIdle(4).thenExecute(()->{
            h.assertTrue(!BlastHold.held(b),"Last release removes transient state after grace period");
            h.assertTrue(user.isValid(),"Existing user anchor preserved");user.remove();pipeline.resetVelocity(b);
        }).thenExecute(()->{
            var origin=b.logicalPose().transformPosition(Vec3.atCenterOf(p));
            var blast=new WorldBlast(l,origin,40);
            h.assertTrue(blast.capture(Long.MAX_VALUE),"Body snapshot ready");
            for(int x=-30;x<=30;x++)for(int z=-30;z<=30;z++)for(int y=-15;y<=15;y++)
                blast.trace(BlockPos.containing(origin).offset(x,y,z),10000,40,1,1,40);
            h.assertTrue(blast.pendingBlocks()==2047,"All stone selected and bedrock excluded: "+blast.pendingBlocks());
            blast.apply();
            pinned[0]=b.logicalPose().transformPosition(Vec3.atCenterOf(p));
            h.assertTrue(BlastHold.anchored(b),"Removal freezes the damaged body");
            // Per-block bookkeeping changes immediately, merged/native upload once on close.
            double before=b.getMassTracker().getMass();
            var stone=blocks.stream().map(world-> {
                var offset=world.subtract(source);return p.offset(offset);
            }).filter(pos->l.getBlockState(pos).is(Blocks.STONE)).findFirst().orElseThrow();
            try(var batch=new BlastMassBatch()) {
                l.removeBlock(stone,false);
                h.assertTrue(b.getMassTracker().getMass()==before,"Merged mass deferred during the slice");
                h.assertTrue(b.getSelfMassTracker().getMass()<before,"Per-block mass bookkeeping remains immediate");
            }
            h.assertTrue(b.getMassTracker().getMass()<before,"Merged mass flushed at slice boundary");
            h.assertTrue(BlastHold.anchored(b),"COM changes preserve temporary constraint");
        }).thenWaitUntil(()->h.assertTrue(!WorldBlast.removing(b),"Bulk removal and hold release finish"))
        .thenExecute(()->{
            h.assertTrue(!b.isRemoved(),"Bedrock survivor still exists");
            int remaining=0;
            for(var holder:b.getPlot().getLoadedChunks()) {
                var chunk=holder.getChunk();if(chunk==null)continue;
                for(var section:chunk.getSections())if(!section.hasOnlyAir())for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)
                    if(!section.getBlockState(x,y,z).isAir())remaining++;
            }
            h.assertTrue(remaining==1,"Only bedrock remains; got "+remaining);
            h.assertTrue(!BlastHold.held(b),"No persistent freeze");
            h.assertTrue(drift[0]<0.05,"Body stayed anchored through removal and COM shifts; drift="+drift[0]);
            com.mojang.logging.LogUtils.getLogger().info("[DE removal smoke] maximum held-point drift={} blocks",drift[0]);
            var tickets=c.collectForceLoadTickets().get(b);
            h.assertTrue(tickets==null || tickets.stream().noneMatch(t->t.type()==WorldBlast.TICKET),"All temporary tickets released");
        }).thenIdle(5).thenExecute(()->{
            h.assertTrue(pipeline.getLinearVelocity(b,new Vector3d()).y<0,"Survivor falls normally after hold release");
            // Cancellation while removal is queued must release just like completion.
            var origin=b.logicalPose().transformPosition(Vec3.atCenterOf(p));
            l.setBlock(p.above(),Blocks.STONE.defaultBlockState(),2);b.getPlot().updateBoundingBox();b.updateBoundingBox();
            var cancelled=new WorldBlast(l,origin,8);cancelled.capture(Long.MAX_VALUE);
            cancelled.trace(BlockPos.containing(b.logicalPose().transformPosition(Vec3.atCenterOf(p.above()))),10000,8,1,1,8);
            cancelled.apply();h.assertTrue(BlastHold.anchored(b),"Cancellation fixture held");cancelled.close();
        }).thenIdle(4).thenExecute(()->{
            h.assertTrue(!BlastHold.held(b),"Cancelled queue releases hold");
            h.assertTrue(l.getBlockState(p.above()).is(Blocks.STONE),"Cancelled process does not remove blocks later");
            new BlastHold(List.of(b));
            c.removeSubLevel(b,SubLevelRemovalReason.REMOVED);
        }).thenIdle(3).thenExecute(()->{
            h.assertTrue(!BlastHold.held(b),"Removed body releases outstanding hold");
        }).thenSucceed();
    }
}
