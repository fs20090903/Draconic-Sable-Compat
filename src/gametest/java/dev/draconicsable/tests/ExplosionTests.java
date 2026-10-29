/*
 * SPDX-License-Identifier: MIT AND LicenseRef-Draconic-Evolution
 * DE-derived portions: Copyright (c) 2013-2023 brandon3055.
 * Original contributions: Copyright (c) 2026 Draconic Sable Compatibility contributors.
 * Adapted from Draconic Evolution's ProcessExplosion.updateCalculation (reference annulus regression scan).
 * Modified for Sable compatibility and/or bounded calculation; see LICENSING.md
 * for the specific adaptations and LICENSES/ for the applicable license texts.
 */
package dev.draconicsable.tests;

import com.brandon3055.brandonscore.handlers.*;
import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import dev.draconicsable.explosion.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("draconic_sable_tests") @PrefixGameTestTemplate(false)
public class ExplosionTests {
    private static int fixtures;
    private static ServerSubLevel body(GameTestHelper h,Vec3 world) {
        BlockPos p=h.absolutePos(new BlockPos(5,6,5));
        h.getLevel().setBlockAndUpdate(p,Blocks.BEDROCK.defaultBlockState());
        var b=SubLevelAssemblyHelper.assembleBlocks(h.getLevel(),p,List.of(p),BoundingBox3i.from(List.of(p)));
        var c=SubLevelContainer.getContainer(h.getLevel());
        if(fixtures++==0)c.physicsSystem().setPaused(true);
        c.addForceLoadTicket(b,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
        move(h,b,world);return b;
    }
    private static void move(GameTestHelper h,ServerSubLevel b,Vec3 world) {
        var c=SubLevelContainer.getContainer(h.getLevel());
        Vec3 current=b.logicalPose().transformPosition(Vec3.atCenterOf(b.getPlot().getCenterBlock()));
        var target=new org.joml.Vector3d(b.logicalPose().position()).add(world.x-current.x,world.y-current.y,world.z-current.z);
        c.physicsSystem().getPipeline().teleport(b,target,b.logicalPose().orientation());
        b.getPlot().updateBoundingBox();b.updateBoundingBox();
    }
    private static void cleanup(GameTestHelper h,ServerSubLevel b){
        SubLevelContainer.getContainer(h.getLevel()).removeSubLevel(b,SubLevelRemovalReason.REMOVED);
        if(--fixtures==0)SubLevelContainer.getContainer(h.getLevel()).physicsSystem().setPaused(false);
    }
    private static void drive(ProcessExplosion p){ProcessHandler.addProcess(new IProcess(){
        public void updateProcess(){if(!p.isCalculationComplete())p.updateCalculation();}
        public boolean isDead(){return p.isCalculationComplete();}
    });}
    @GameTest(template="empty",timeoutTicks=400)
    public void annulusMatchesOriginalOrderAndSlices(GameTestHelper h) {
        for(int r:List.of(1,2,3,4,5,8,16,31,64,127,350)) {
            var p=new ProcessExplosion(BlockPos.ZERO,r+1,h.getLevel(),-1);p.radius=r;p.meanResistance=100;
            var actual=new ArrayList<Long>();var expected=new ArrayList<Long>();
            for(int x=-r;x<r;x++)for(int z=-r;z<r;z++) {
                double d=Math.sqrt(x*x+z*z);if(d<r && d>=r-1)expected.add(BlockPos.asLong(x,0,z));
            }
            var rings=new BlastRings();boolean done=false;int calls=0;
            while(!done){int before=actual.size();done=rings.step(p,h.getLevel(),(pos,power,dist,dir,res,travel)->{
                if(dir==1)actual.add(BlockPos.containing(pos.x,0,pos.z).asLong());return 0;
            },Long.MAX_VALUE);h.assertTrue(actual.size()-before<=BlastRings.MAX_COLUMNS,"A slice respects its column ceiling");calls++;}
            h.assertTrue(actual.equals(expected),"Original DE annulus order at radius "+r);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public void terrainExplosionActuallyDamagesRotatedBody(GameTestHelper h) {
        var world=new Vec3(-3000.5,150.5,-3000.5);var b=body(h,world);var l=h.getLevel();
        var p=b.getPlot().getCenterBlock();
        l.setBlockAndUpdate(p,Blocks.BEDROCK.defaultBlockState());
        l.setBlockAndUpdate(p.east(),Blocks.STONE.defaultBlockState());
        var c=SubLevelContainer.getContainer(l);
        c.physicsSystem().getPipeline().teleport(b,new org.joml.Vector3d(b.logicalPose().position()),new org.joml.Quaterniond().rotateY(Math.PI/2));move(h,b,world);
        var near=BlockPos.containing(world).west(2);l.setBlockAndUpdate(near,Blocks.STONE.defaultBlockState());
        var far=BlockPos.containing(world).east(20);l.setBlockAndUpdate(far,Blocks.STONE.defaultBlockState());
        var blast=new ProcessExplosion(BlockPos.containing(world),8,l,-1);blast.lava=false;blast.enableEffect=false;drive(blast);
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Prepared before detonation"))
            .thenExecute(()->{h.assertTrue(l.getBlockState(p.east()).is(Blocks.STONE),"Preparation does not remove blocks");blast.detonate();})
            .thenWaitUntil(()->h.assertTrue(l.getBlockState(p.east()).isAir() && l.getBlockState(near).isAir() && !WorldBlast.removing(b) && ((BlastPlan)blast).draconicSable$terrainTickets()==0,"Removal and terrain-ticket release finished")).thenExecute(()->{
                h.assertTrue(l.getBlockState(p.east()).isAir(),"Rotated sublevel stone destroyed by terrain blast");
                h.assertTrue(l.getBlockState(p).is(Blocks.BEDROCK),"Bedrock resistance respected");
                h.assertTrue(l.getBlockState(near).isAir(),"Nearby terrain removed");
                h.assertTrue(l.getBlockState(far).is(Blocks.STONE),"Far terrain survives");
                h.assertTrue(!WorldBlast.removing(b),"Removal guard released");
                var tickets=c.collectForceLoadTickets().get(b);
                h.assertTrue(tickets==null || tickets.stream().noneMatch(t->t.type()==WorldBlast.TICKET),"Temporary blast ticket released");
                cleanup(h,b);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public void optimizedTraceMatchesOriginalBlocksResistanceAndRandom(GameTestHelper h) throws Exception {
        var level=h.getLevel();int x=-5201,z=-5201;
        for(int y=75;y<165;y++) {
            var state=switch(y%7) {
                case 0 -> Blocks.STONE.defaultBlockState();
                case 1 -> Blocks.OBSIDIAN.defaultBlockState();
                case 2 -> Blocks.WATER.defaultBlockState();
                case 3 -> Blocks.SAND.defaultBlockState();
                case 4 -> Blocks.LAVA.defaultBlockState();
                default -> Blocks.AIR.defaultBlockState();
            };
            level.setBlock(new BlockPos(x,y,z),state,2);
        }
        var method=ProcessExplosion.class.getDeclaredMethod("trace",codechicken.lib.vec.Vector3.class,double.class,int.class,int.class,double.class,int.class);
        method.setAccessible(true);int cases=0;
        try(var bodies=new WorldBlast(level,new Vec3(x+.5,120.5,z+.5),350)) {
            h.assertTrue(bodies.capture(Long.MAX_VALUE),"No neighboring fixtures in reference snapshot");
            for(int ring:List.of(1,8,120,349))for(double height:List.of(120.5,200.5,319.5,-63.5))for(int direction:List.of(-1,1))for(boolean lava:List.of(false,true)) {
                var reference=new ProcessExplosion(new BlockPos(x,120,z),350,level,-1);
                var optimized=new ProcessExplosion(new BlockPos(x,120,z),350,level,-1);
                reference.radius=optimized.radius=ring;reference.lava=optimized.lava=lava;
                double power=ring==349?2:10000;
                level.random.setSeed(19181L);
                double expected=(double)method.invoke(reference,new codechicken.lib.vec.Vector3(x+.5,height,z+.5),power,100,direction,0.0,0);
                long expectedRandom=level.random.nextLong();
                level.random.setSeed(19181L);
                var solver=new BlastTrace(optimized,level,bodies);
                double actual=solver.trace(new codechicken.lib.vec.Vector3(x+.5,height,z+.5),power,100,direction,0,0);
                long actualRandom=level.random.nextLong();solver.clear();
                h.assertTrue(Math.abs(expected-actual)<1e-10*Math.max(1,Math.abs(expected)),"Equivalent resistance within floating-point roundoff case "+cases+": "+expected+" vs "+actual);
                h.assertTrue(reference.destroyedCache.equals(optimized.destroyedCache),"Same destroyed blocks case "+cases);
                h.assertTrue(reference.blocksToUpdate.equals(optimized.blocksToUpdate),"Same update blocks case "+cases);
                h.assertTrue(reference.lavaPositions.equals(optimized.lavaPositions),"Same lava case "+cases);
                h.assertTrue(expectedRandom==actualRandom,"Same RNG progression case "+cases);cases++;
            }
        }
        h.assertTrue(cases==64,"64 reference comparisons");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public void fullRadiusSparseBlastCompletesWithoutRingPerTickDelay(GameTestHelper h) {
        var l=h.getLevel();var world=new Vec3(-5800.5,500.5,-5800.5);var b=body(h,world);var p=b.getPlot().getCenterBlock();
        l.setBlockAndUpdate(p.east(),Blocks.STONE.defaultBlockState());b.getPlot().updateBoundingBox();b.updateBoundingBox();
        var blast=new ProcessExplosion(p,350,l,-1);blast.lava=false;blast.enableEffect=false;
        long started=l.getGameTime(),wall=System.nanoTime();drive(blast);
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Full 350-radius plan completed"))
            .thenExecute(()->{
                long ticks=l.getGameTime()-started;
                com.mojang.logging.LogUtils.getLogger().info("[DE benchmark] radius=350 sparse ticks={} wallMs={}",ticks,(System.nanoTime()-wall)/1_000_000);
                h.assertTrue(ticks<200,"Sparse max-radius preparation under 10 simulated seconds; got "+ticks+" ticks");
                blast.detonate();
            }).thenWaitUntil(()->h.assertTrue(l.getBlockState(p.east()).isAir() && !WorldBlast.removing(b),"Full-radius body removal finished")).thenExecute(()->{
                h.assertTrue(l.getBlockState(p.east()).isAir(),"Full-radius sublevel damage actually applied");cleanup(h,b);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=2800)
    public void fullRadiusColdTerrainAndWarmReuse(GameTestHelper h) {
        var l=h.getLevel();var center=new BlockPos(-7000,-20,-7000);
        l.setBlock(center.below(8),Blocks.STONE.defaultBlockState(),2);
        var blast=new ProcessExplosion(center,350,l,-1);blast.lava=false;blast.enableEffect=false;
        long started=l.getGameTime(),wall=System.nanoTime();drive(blast);
        var warm=new ProcessExplosion[1];long[] warmStart=new long[2];
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Full radius terrain plan ready"))
            .thenExecute(()->{
                long ticks=l.getGameTime()-started;
                int selected=blast.destroyedBlocks.stream().mapToInt(Set::size).sum();
                com.mojang.logging.LogUtils.getLogger().info("[DE benchmark] radius=350 terrain ticks={} wallMs={} selected={}",ticks,(System.nanoTime()-wall)/1_000_000,selected);
                h.assertTrue(blast.destroyedBlocks.stream().anyMatch(s->s.contains(center.below(8).asLong())),"Nearby terrain selected");
                // Cold terrain includes generation of ~1600 new chunks. Measure it separately
                // rather than weakening the warm solver's latency requirement to hide I/O.
                warm[0]=new ProcessExplosion(center,350,l,-1);warm[0].lava=false;warm[0].enableEffect=false;
                ((BlastPlan)warm[0]).draconicSable$reuseTerrain(((BlastPlan)blast).draconicSable$takeTerrain());
                ((BlastPlan)blast).draconicSable$discard();
                warmStart[0]=l.getGameTime();warmStart[1]=System.nanoTime();drive(warm[0]);
            }).thenWaitUntil(()->h.assertTrue(warm[0].isCalculationComplete(),"Warm full radius plan ready"))
            .thenExecute(()->{
                long ticks=l.getGameTime()-warmStart[0];
                com.mojang.logging.LogUtils.getLogger().info("[DE benchmark] radius=350 warm ticks={} wallMs={}",ticks,(System.nanoTime()-warmStart[1])/1_000_000);
                h.assertTrue(ticks<200,"Prepared terrain recomputation under ten simulated seconds: "+ticks);
                h.assertTrue(warm[0].destroyedBlocks.stream().anyMatch(s->s.contains(center.below(8).asLong())),"Warm terrain damage selected");
                ((BlastPlan)warm[0]).draconicSable$discard();
                h.assertTrue(((BlastPlan)warm[0]).draconicSable$terrainTickets()==0,"Cancelled warm plan releases transferred tickets");
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=500)
    public void realReactorRemainsUntilReplacementPlanDetonates(GameTestHelper h) throws Exception {
        var l=h.getLevel();var b=body(h,new Vec3(-4400.5,150.5,-4400.5));var p=b.getPlot().getCenterBlock().above();
        l.setBlockAndUpdate(p,com.brandon3055.draconicevolution.init.DEContent.REACTOR_CORE.get().defaultBlockState());
        var core=(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore)l.getBlockEntity(p);
        core.reactableFuel.set(144);core.reactorState.set(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState.BEYOND_HOPE);
        core.updateCriticalState();
        var field=core.getClass().getDeclaredField("explosionProcess");field.setAccessible(true);
        var blast=(AnchoredReactorExplosion)field.get(core);blast.maxRadius=8;blast.enableEffect=false;blast.lava=false;
        core.updateCriticalState();
        h.assertTrue(!blast.isCalculationComplete() && core.explosionCountdown.get()>0,"Safety countdown overlaps preparation without a false completion flag");
        var initial=new ProcessExplosion[1];
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Initial real reactor plan ready"))
            .thenExecute(()->{
                initial[0]=blast.scheduled();core.explosionCountdown.set(1000);
                move(h,b,new Vec3(-4440.5,150.5,-4400.5));
            }).thenIdle(25).thenExecute(()->{
                h.assertTrue(blast.scheduled()==initial[0],"Motion during early safety countdown does not repeatedly discard prepared work");
                core.explosionCountdown.set(1);core.updateCriticalState();
                h.assertTrue(l.getBlockEntity(p)==core,"Countdown expiry must not remove core before actual detonation");
                h.assertTrue(!blast.isCalculationComplete(),"New preparation clears stale completion flag");
                h.assertTrue(!blast.actuallyDetonated(),"No false detonation success");
            }).thenWaitUntil(()->h.assertTrue(blast.actuallyDetonated(),"Replacement plan detonated"))
            .thenWaitUntil(()->h.assertTrue(!WorldBlast.removing(b),"Queued body removal finished"))
            .thenExecute(()->{h.assertTrue(l.getBlockState(p).isAir(),"Core removed with actual detonation");cleanup(h,b);}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=500)
    public void movingReactorRefreshesBeforeDetonationAndReusesPreparedPlan(GameTestHelper h) {
        Vec3 start=new Vec3(-3200.5,150.5,-3200.5),end=new Vec3(-3240.5,150.5,-3200.5);
        var b=body(h,start);var l=h.getLevel();var p=b.getPlot().getCenterBlock();
        l.setBlockAndUpdate(p.east(),Blocks.STONE.defaultBlockState());b.getPlot().updateBoundingBox();b.updateBoundingBox();
        var old=BlockPos.containing(start).west(2);var target=BlockPos.containing(end).west(2);
        l.setBlockAndUpdate(old,Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        var blast=new AnchoredReactorExplosion(p,8,l,-1);blast.lava=false;blast.enableEffect=false;ProcessHandler.addProcess(blast);
        var prepared=new ProcessExplosion[1];
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Initial calculation finished during countdown"))
            .thenExecute(()->{prepared[0]=blast.scheduled();move(h,b,end);})
            .thenWaitUntil(()->h.assertTrue(blast.scheduled()!=prepared[0] && blast.scheduled().isCalculationComplete(),"Movement refreshed during countdown"))
            .thenExecute(()->{
                h.assertTrue(l.getBlockState(target).is(Blocks.STONE),"No premature detonation");
                h.assertTrue(((BlastPlan)blast.scheduled()).draconicSable$matches(),"Prepared snapshot still matches pose: "+b.logicalPose());
                var actual=b.logicalPose().transformPosition(Vec3.atCenterOf(p));
                h.assertTrue(new Vec3(blast.scheduled().origin.x,blast.scheduled().origin.y,blast.scheduled().origin.z).distanceToSqr(actual)<=1.0/256,"Prepared origin="+blast.scheduled().origin+" current="+actual);
                prepared[0]=blast.scheduled();blast.detonate();
                h.assertTrue(blast.scheduled()==prepared[0] && blast.isDead(),"Stable completed plan reused immediately");
                h.assertTrue(Math.abs(blast.scheduled().origin.x-end.x)<.01,"Detonation uses actual world position");
            }).thenIdle(25).thenExecute(()->{
                h.assertTrue(l.getBlockState(target).isAir(),"New location receives damage");
                h.assertTrue(l.getBlockState(old).is(Blocks.STONE),"Old location is not damaged");
                h.assertTrue(l.getBlockState(p.east()).isAir(),"Source sublevel receives damage too");cleanup(h,b);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=500)
    public void lastMomentMovementUsesNewWorldFrameAndHitsNearbyBody(GameTestHelper h) {
        Vec3 start=new Vec3(-3400.5,150.5,-3400.5),end=new Vec3(-3440.5,150.5,-3400.5);
        var b=body(h,start);var nearby=body(h,end.add(3,0,0));var l=h.getLevel();
        var p=b.getPlot().getCenterBlock();var q=nearby.getPlot().getCenterBlock();
        l.setBlockAndUpdate(p.east(),Blocks.STONE.defaultBlockState());
        l.setBlockAndUpdate(q.above(),Blocks.STONE.defaultBlockState());
        b.getPlot().updateBoundingBox();b.updateBoundingBox();nearby.getPlot().updateBoundingBox();nearby.updateBoundingBox();
        var blast=new AnchoredReactorExplosion(p,12,l,-1);blast.lava=false;blast.enableEffect=false;ProcessHandler.addProcess(blast);
        var pig=net.minecraft.world.entity.EntityType.PIG.create(l);pig.setNoGravity(true);pig.moveTo(end.x-2,end.y,end.z,0,0);l.addFreshEntity(pig);
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Initial plan ready"))
            .thenExecute(()->{
                var old=blast.scheduled();move(h,b,end);blast.detonate();
                h.assertTrue(blast.scheduled()!=old && !blast.isDead(),"Last-moment move queues bounded catch-up instead of synchronous scan");
                h.assertTrue(l.getBlockState(q.above()).is(Blocks.STONE),"New target not removed in detonate call");
            }).thenWaitUntil(()->h.assertTrue(blast.isDead(),"Catch-up completed"))
            .thenIdle(60).thenExecute(()->{
                h.assertTrue(l.getBlockState(p.east()).isAir(),"Source body hit");
                h.assertTrue(l.getBlockState(q.above()).isAir(),"Separate nearby body hit in world coordinates");
                h.assertTrue(!pig.isAlive(),"Delayed entity damage is centred on the moved world location");
                cleanup(h,b);cleanup(h,nearby);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public void sublevelAboveTerrainBuildHeightStillReceivesBlast(GameTestHelper h) {
        var world=new Vec3(-3600.5,500.5,-3600.5);var b=body(h,world);var l=h.getLevel();var p=b.getPlot().getCenterBlock();
        l.setBlockAndUpdate(p.east(),Blocks.STONE.defaultBlockState());b.getPlot().updateBoundingBox();b.updateBoundingBox();
        var blast=new ProcessExplosion(p,8,l,-1);blast.lava=false;blast.enableEffect=false;
        h.assertTrue(Math.abs(blast.origin.y-world.y)<.01,"Raw sublevel anchor projects above terrain limit");drive(blast);
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"High altitude calculation complete"))
            .thenExecute(blast::detonate).thenWaitUntil(()->h.assertTrue(l.getBlockState(p.east()).isAir() && !WorldBlast.removing(b),"High-altitude removal completed")).thenExecute(()->{
                h.assertTrue(l.getBlockState(p.east()).isAir(),"Body block above terrain build height removed");cleanup(h,b);
            }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=400)
    public void realCriticalReactorPreparesAndCancelsWhenRemoved(GameTestHelper h) throws Exception {
        var b=body(h,new Vec3(-3800.5,150.5,-3800.5));var l=h.getLevel();var p=b.getPlot().getCenterBlock().above();
        l.setBlockAndUpdate(p,com.brandon3055.draconicevolution.init.DEContent.REACTOR_CORE.get().defaultBlockState());
        var core=(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore)l.getBlockEntity(p);
        core.reactableFuel.set(144);core.reactorState.set(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState.BEYOND_HOPE);
        core.updateCriticalState();
        var field=core.getClass().getDeclaredField("explosionProcess");field.setAccessible(true);
        var blast=(ProcessExplosion)field.get(core);h.assertTrue(blast instanceof AnchoredReactorExplosion,"Actual critical-state path installs anchored preparation");blast.maxRadius=8;
        h.startSequence().thenWaitUntil(()->h.assertTrue(blast.isCalculationComplete(),"Actual meltdown prepares before countdown finishes"))
            .thenExecute(()->{
                h.assertTrue(core.explosionCountdown.get()>0,"Countdown still running after preparation");
                core.reactorState.set(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState.COLD);
                l.removeBlock(p,false);
            }).thenIdle(3).thenExecute(()->{
                h.assertTrue(blast.isDead(),"Removed reactor cancels unfinished countdown process");
                var tickets=SubLevelContainer.getContainer(l).collectForceLoadTickets().get(b);
                h.assertTrue(tickets==null || tickets.stream().noneMatch(t->t.type()==WorldBlast.TICKET),"Cancelled plan releases tickets");cleanup(h,b);
            }).thenSucceed();
    }
    @GameTest(template="empty")
    public void smallReactorBlastAndDebrisUseWorldCoordinates(GameTestHelper h) throws Exception {
        var world=new Vec3(-4000.5,150.5,-4000.5);var b=body(h,world);var l=h.getLevel();var p=b.getPlot().getCenterBlock().above();
        try {
            l.setBlockAndUpdate(p,com.brandon3055.draconicevolution.init.DEContent.REACTOR_CORE.get().defaultBlockState());
            var core=(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore)l.getBlockEntity(p);
            var boom=core.getClass().getDeclaredMethod("minimalBoom");boom.setAccessible(true);boom.invoke(core);
            var debris=l.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new net.minecraft.world.phys.AABB(world,world).inflate(10));
            h.assertTrue(debris.size()>=25,"Small blast spawns lava debris at actual world position");
            h.assertTrue(l.getBlockState(p).isAir(),"Local core removed");
            for(var e:debris)e.discard();
            var c=SubLevelContainer.getContainer(l);
            c.addForceLoadTicket(b,WorldBlast.TICKET,"test-orphan-after-restart");
            dev.draconicsable.CoreLoading.pruneOwners(l);
            h.assertTrue(c.collectForceLoadTickets().get(b).stream().noneMatch(t->t.type()==WorldBlast.TICKET),"Orphan transient ticket cleared without removing independent ticket");
        } finally {cleanup(h,b);}
        h.succeed();
    }
}
