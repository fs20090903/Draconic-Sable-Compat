package dev.draconicsable.tests;

import com.brandon3055.brandonscore.lib.Vec3I;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.*;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.draconicsable.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("draconic_sable_tests")
@PrefixGameTestTemplate(false)
public class CompatibilityTests {
    private static ServerSubLevel ship(GameTestHelper h) {
        clearChunkCache(h);
        BlockPos p=h.absolutePos(new BlockPos(4,4,4));
        h.getLevel().setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
        return SubLevelAssemblyHelper.assembleBlocks(h.getLevel(),p,List.of(p),BoundingBox3i.from(List.of(p)));
    }
    private static void cleanup(GameTestHelper h,ServerSubLevel body) {
        var c=SubLevelContainer.getContainer(h.getLevel());
        if(c.getSubLevel(body.getUniqueId())==body) c.removeSubLevel(body,SubLevelRemovalReason.REMOVED);
        clearChunkCache(h);
    }
    private static void clearChunkCache(GameTestHelper h) {
        // Tests remove and reuse plots within one server tick; vanilla normally clears this on the next tick.
        try {var m=net.minecraft.server.level.ServerChunkCache.class.getDeclaredMethod("clearCache");m.setAccessible(true);m.invoke(h.getLevel().getChunkSource());}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static TileReactorCore reactor(GameTestHelper h,BlockPos p) {
        var l=h.getLevel();
        l.setBlockAndUpdate(p,DEContent.REACTOR_CORE.get().defaultBlockState());
        var core=(TileReactorCore)l.getBlockEntity(p);
        for(var dir:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            var s=p.relative(dir,5);
            l.setBlockAndUpdate(s,DEContent.REACTOR_STABILIZER.get().defaultBlockState());
            ((TileReactorComponent)l.getBlockEntity(s)).facing.set(dir.getOpposite());
        }
        var inj=p.above(5);
        l.setBlockAndUpdate(inj,DEContent.REACTOR_INJECTOR.get().defaultBlockState());
        ((TileReactorComponent)l.getBlockEntity(inj)).facing.set(Direction.DOWN);
        core.attemptInitialization();
        h.assertTrue(core.structureValid.get(),"All real reactor components bind");
        return core;
    }
    @GameTest(template="empty") public void verticalPlacementMath(GameTestHelper h) {
        var p=new BlockPos(20481032,100,20483101);
        h.assertTrue(Placement.localRotation(p,new Vec3(p.getX()+.5,103,p.getZ()+.5),new Vec3(0,-1,0))==Direction.DOWN,"Above places injector facing UP after DE opposite");
        h.assertTrue(Placement.localRotation(p,new Vec3(p.getX()+.5,99,p.getZ()+.5),new Vec3(0,1,0))==Direction.UP,"Below places injector facing DOWN");
        h.assertTrue(Placement.localRotation(p,new Vec3(p.getX()+3,101,p.getZ()),new Vec3(-1,0,0))==Direction.WEST,"Horizontal uses local look vector");
        h.succeed();
    }
    @GameTest(template="empty") public void sparsePylonActuallyTransfersEnergy(GameTestHelper h) {
        var body=ship(h);
        try {
            var l=h.getLevel(); var p=body.getPlot().getCenterBlock().above();
            l.setBlockAndUpdate(p,DEContent.ENERGY_CORE.get().defaultBlockState());
            var core=(TileEnergyCore)l.getBlockEntity(p);
            core.tier.set((byte)1); core.active.set(true); core.energy.validateStorage();
            var q=p.east(3); l.setBlockAndUpdate(q,DEContent.ENERGY_PYLON.get().defaultBlockState());
            var pylon=(TileEnergyPylon)l.getBlockEntity(q);
            pylon.coreOffset.set(q.subtract(p)); pylon.structureValid.set(true);
            pylon.ioMode.set(com.brandon3055.draconicevolution.blocks.machines.EnergyPylon.Mode.INPUT);
            h.assertTrue(!l.isAreaLoaded(p,16),"Fixture really has missing surrounding sparse chunks");
            h.assertTrue(pylon.getCore()==core,"Sparse lookup resolves live core: pos="+p+" chunk="+LoadedAccess.chunk(l,p)+" ready="+LoadedAccess.ready(l,p)+" removed="+core.isRemoved()+" tile="+LoadedAccess.tile(l,p)+" offset="+pylon.coreOffset.get()+" lookup="+pylon.getCore());
            long before=core.energy.getOPStored();
            long accepted=pylon.opAdapter.receiveOP(12345,false);
            h.assertTrue(accepted==12345 && core.energy.getOPStored()==before+accepted,"Real pylon adapter deposits energy into core");
            pylon.ioMode.set(com.brandon3055.draconicevolution.blocks.machines.EnergyPylon.Mode.OUTPUT);
            long out=pylon.opAdapter.extractOP(2345,false);
            h.assertTrue(out==2345 && core.energy.getOPStored()==before+10000,"Real output conserves energy");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    @GameTest(template="empty") public void reactorAcrossChunkBoundaryStillRuns(GameTestHelper h) {
        var body=ship(h);
        try {
            var core=reactor(h,body.getPlot().getCenterBlock().offset(6,7,6));
            core.reactableFuel.set(1296); core.temperature.set(4000); core.maxSaturation.set(1000000); core.saturation.set(500000);
            core.maxShieldCharge.set(1000000); core.shieldCharge.set(500000); core.reactorState.set(TileReactorCore.ReactorState.RUNNING);
            long before=core.saturation.get(); core.tick();
            h.assertTrue(core.saturation.get()>before,"Loaded multi-chunk reactor generates real energy");
            h.assertTrue(core.reactorState.get()==TileReactorCore.ReactorState.RUNNING,"Chunk boundary alone is not a failure");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    @GameTest(template="empty") public void incompleteLoadPausesButBrokenBlockDoesNot(GameTestHelper h) {
        var body=ship(h);
        try {
            var p=body.getPlot().getCenterBlock().above(6); var core=reactor(h,p);
            core.reactableFuel.set(1296); core.temperature.set(4000); core.maxSaturation.set(1000000); core.saturation.set(500000);
            core.maxShieldCharge.set(1000000); core.shieldCharge.set(500000); core.reactorState.set(TileReactorCore.ReactorState.RUNNING);
            var original=new Vec3I(core.componentPositions[1].get().getPos());
            core.componentPositions[1].set(new Vec3I(-160,0,-160));
            h.assertTrue(!MultiblockLinks.reactorReady(core),"Missing saved injector chunk is detected");
            core.tick();
            h.assertTrue(core.shieldCharge.get()==500000 && core.saturation.get()==500000,"No shield drain or free production while incomplete");
            core.componentPositions[1].set(original);
            core.tick();
            h.assertTrue(core.shieldCharge.get()<500000,"Normal drain resumes after all components available");
            core.reactorState.set(TileReactorCore.ReactorState.COLD);
            h.getLevel().removeBlock(p.above(5),false);
            h.assertTrue(MultiblockLinks.reactorReady(core),"Actual removed block is not treated as unloaded");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    @GameTest(template="empty") public void structureDoesNotRevertDuringControllerLoad(GameTestHelper h) {
        var body=ship(h);
        try {
            var p=body.getPlot().getCenterBlock().above();
            h.getLevel().setBlockAndUpdate(p,DEContent.STRUCTURE_BLOCK.get().defaultBlockState());
            var part=(TileStructureBlock)h.getLevel().getBlockEntity(p);
            part.controllerOffset.set(new BlockPos(-160,0,-160));
            h.assertTrue(part.getController()==null && !part.reverting.get(),"Absent controller chunk does not schedule destruction");
            part.controllerOffset.set(new BlockPos(-1,0,0));
            h.assertTrue(part.getController()==null && part.reverting.get(),"Actually missing loaded controller still reverts");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    @GameTest(template="empty") public void virtualEdgesPreserveCoreAndStillClearBlocks(GameTestHelper h) {
        var body=ship(h);
        try {
            var p=body.getPlot().getCenterBlock().above(6); var core=reactor(h,p);
            // Connect the four stabilizer columns to a solid floor, leaving the core suspended.
            for(int x=-6;x<=6;x++) for(int z=-6;z<=6;z++) h.getLevel().setBlockAndUpdate(p.offset(x,-5,z),Blocks.STONE.defaultBlockState());
            for(var dir:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))
                for(int y=-4;y<0;y++) h.getLevel().setBlockAndUpdate(p.relative(dir,5).offset(0,y,0),Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
            core.reactableFuel.set(1296); core.temperature.set(4000);
            for(int i=0;i<105;i++) core.checkBlockIntrusions();
            h.assertTrue(h.getLevel().getBlockState(p.below()).isAir(),"Original intrusion destruction remains functional");
            for(int i=0;i<50;i++) body.getHeatMapManager().tick();
            h.assertTrue(h.getLevel().getBlockEntity(p)==core && Sable.HELPER.getContaining(core)==body,"Core stays in original body across the cleared air gap");
            h.assertTrue(core.validateStructure(),"Stabilizer links remain functional after heatmap splitting passes");
            var gather=SubLevelAssemblyHelper.gatherConnectedBlocks(p,h.getLevel(),4096,null);
            h.assertTrue(gather.blocks()!=null && gather.blocks().contains(p.north(5)) && gather.blocks().contains(p.above(5)),"Assembly gathering traverses functional air-gap links");
        } finally {cleanup(h,body);}
        h.succeed();
    }

    @GameTest(template="empty") public void bothCoreTypesOwnIndependentPersistentTickets(GameTestHelper h) {
        var body=ship(h);
        try {
            var p=body.getPlot().getCenterBlock().above(6); var r=reactor(h,p);
            var epos=p.east(3); h.getLevel().setBlockAndUpdate(epos,DEContent.ENERGY_CORE.get().defaultBlockState());
            var e=(TileEnergyCore)h.getLevel().getBlockEntity(epos); e.active.set(true);
            r.reactorState.set(TileReactorCore.ReactorState.WARMING_UP);
            CoreLoading.update(r); CoreLoading.update(e);
            var c=SubLevelContainer.getContainer(h.getLevel());
            var tickets=c.collectForceLoadTickets().get(body);
            h.assertTrue(tickets.stream().filter(t->t.type()==CoreLoading.SUBLEVEL).count()==2,"Both core types own force-loading tickets");
            c.getHoldingChunkMap().saveAll();
            h.assertTrue(body.getLastSerializationPointer()!=null,"Body saved before ticket serialization, matching normal world-save order");
            var tag=dev.ryanhcode.sable.sublevel.storage.SubLevelTicketsSavedData.getOrLoad(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
            h.assertTrue(tag.toString().contains("draconic_sable_compat:active_core"),"Ticket type and owners serialize with world saved data");
            h.assertTrue(tag.toString().contains("pointer"),"Persistent ticket includes the disk pointer required for restart loading");
            r.reactorState.set(TileReactorCore.ReactorState.COLD);CoreLoading.update(r);
            h.assertTrue(c.collectForceLoadTickets().get(body).stream().filter(t->t.type()==CoreLoading.SUBLEVEL).count()==1,"Cooling down one reactor does not release another core's load");
            e.active.set(false);CoreLoading.update(e);
            h.assertTrue(!c.collectForceLoadedSubLevels().contains(body),"Last inactive owner releases force load");
        } finally {cleanup(h,body);}
        h.succeed();
    }

    @GameTest(template="empty") public void assemblyRotationPreservesFunctionalReactor(GameTestHelper h) {
        var body=ship(h);
        try {
            var p=body.getPlot().getCenterBlock().above(6); var core=reactor(h,p);
            core.reactableFuel.set(1296);core.temperature.set(4000);core.maxShieldCharge.set(1000000);core.shieldCharge.set(500000);
            core.reactorState.set(TileReactorCore.ReactorState.RUNNING);
            var blocks=new ArrayList<BlockPos>();blocks.add(p);
            for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.UP))blocks.add(p.relative(d,5));
            var target=p.offset(32,0,32);
            var transform=new SubLevelAssemblyHelper.AssemblyTransform(p,target,-1,net.minecraft.world.level.block.Rotation.CLOCKWISE_90,h.getLevel());
            SubLevelAssemblyHelper.moveBlocks(h.getLevel(),transform,blocks);
            var moved=(TileReactorCore)h.getLevel().getBlockEntity(target);
            h.assertTrue(moved!=null && moved.reactorState.get()==TileReactorCore.ReactorState.RUNNING,"Moving hot components is not treated as breaking them");
            h.assertTrue(moved.validateStructure(),"Rotated reactor offsets and facing still validate");
            h.assertTrue(moved.shieldCharge.get()==500000,"Assembly preserved shield energy");
            var stab=(TileReactorComponent)h.getLevel().getBlockEntity(transform.apply(p.north(5)));
            h.assertTrue(stab.facing.get()==Direction.WEST && stab.getCachedCore()==moved,"Moved stabilizer points to and resolves actual moved core");
        } finally {cleanup(h,body);}
        h.succeed();
    }

    @GameTest(template="empty") public void removedOwnersReleaseMultipleBodies(GameTestHelper h) {
        var a=ship(h);var b=ship(h);var level=h.getLevel();
        try {
            var c=SubLevelContainer.getContainer(level);
            // Persisted owners can outlive their sparse plot chunks after moves/removal.
            c.addForceLoadTicket(a,CoreLoading.SUBLEVEL,a.getPlot().getCenterBlock().offset(64,10,64));
            c.addForceLoadTicket(b,CoreLoading.SUBLEVEL,b.getPlot().getCenterBlock().offset(64,10,64));
            CoreLoading.pruneOwners(level);
            h.assertTrue(!c.collectForceLoadedSubLevels().contains(a) && !c.collectForceLoadedSubLevels().contains(b),"All removed owners release their tickets without mutating a live map iterator");
        } finally {cleanup(h,a);cleanup(h,b);}
        h.succeed();
    }

    @GameTest(template="empty") public void energyCoreAirGapSurvivesSupportRemoval(GameTestHelper h) {
        var body=ship(h);
        try {
            var l=h.getLevel();var p=body.getPlot().getCenterBlock().above(6);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)l.setBlockAndUpdate(p.offset(x,-5,z),Blocks.STONE.defaultBlockState());
            l.setBlockAndUpdate(p,DEContent.ENERGY_CORE.get().defaultBlockState());
            for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
                l.setBlockAndUpdate(p.relative(d,5),DEContent.ENERGY_CORE_STABILIZER.get().defaultBlockState());
                for(int y=-4;y<0;y++)l.setBlockAndUpdate(p.relative(d,5).offset(0,y,0),Blocks.STONE.defaultBlockState());
            }
            var core=(TileEnergyCore)l.getBlockEntity(p);core.toggleActivation();
            h.assertTrue(core.active.get(),"Real energy multiblock activates");
            core.energy.receiveOP(123456,false);
            l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
            for(int i=0;i<20;i++)body.getHeatMapManager().tick();
            l.removeBlock(p.below(),false);
            for(int i=0;i<50;i++)body.getHeatMapManager().tick();
            h.assertTrue(LoadedAccess.tile(l,p)==core && Sable.HELPER.getContaining(core)==body,"Energy core stays with stabilizers across air gap");
            h.assertTrue(core.active.get() && core.validateStructure() && core.energy.getOPStored()==123456,"Core remains functional and energy conserved");
            var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(p,l,4096,null);
            h.assertTrue(gathered.blocks().contains(p.north(5)) && gathered.blocks().contains(p.east(5)),"Energy stabilizers gathered through virtual edges");
            var transform=new SubLevelAssemblyHelper.AssemblyTransform(p,p.offset(32,0,32),-1,net.minecraft.world.level.block.Rotation.CLOCKWISE_90,l);
            SubLevelAssemblyHelper.moveBlocks(l,transform,new ArrayList<>(gathered.blocks()));
            var moved=(TileEnergyCore)LoadedAccess.tile(l,transform.apply(p));
            h.assertTrue(moved!=null && moved.active.get() && moved.validateStructure() && moved.energy.getOPStored()==123456,"Rotating assembly preserves functional energy-core links and energy");
        } finally {cleanup(h,body);}
        h.succeed();
    }

    @GameTest(template="empty") public void allRegisteredBlockPlacementParity(GameTestHelper h) {
        var body=ship(h);
        try {
            var level=h.getLevel();var local=body.getPlot().getCenterBlock().above(4);var ground=h.absolutePos(new BlockPos(4,6,4));
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            int count=0;
            for(var rotation:List.of(new org.joml.Quaterniond(),new org.joml.Quaterniond().rotateXYZ(.35,1.1,-.2))) {
                SubLevelContainer.getContainer(level).physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(body.logicalPose().position()),rotation);
                for(var d:Direction.values())for(var holder:DEContent.BLOCKS.getEntries()) {
                    var block=holder.get();var stack=new net.minecraft.world.item.ItemStack(block.asItem());
                    // Test placement result with matching local view, height and clicked face.
                    var look=Vec3.atLowerCornerOf(d.getNormal());
                    if(d.getAxis()==Direction.Axis.Y)look=look.add(.1,0,.15).normalize();
                    var relative=look.scale(-3).add(.5,1,.5);
                    level.setBlockAndUpdate(ground.below(),Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(local.below(),Blocks.STONE.defaultBlockState());
                    player.setPos(Vec3.atLowerCornerOf(ground).add(relative));setLook(player,look);
                    var a=new net.minecraft.world.item.context.BlockPlaceContext(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(ground.below()).add(0,.5,0),Direction.UP,ground.below(),false));
                    var expected=block.getStateForPlacement(a);
                    player.setPos(body.logicalPose().transformPosition(Vec3.atLowerCornerOf(local).add(relative)));setLook(player,body.logicalPose().transformNormal(look));
                    var b=new net.minecraft.world.item.context.BlockPlaceContext(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(local.below()).add(0,.5,0),Direction.UP,local.below(),false));
                    var actual=block.getStateForPlacement(b);
                    h.assertTrue(Objects.equals(expected,actual),"Placement parity "+holder.getId()+" view="+d+" expected="+expected+" actual="+actual);
                    count++;
                }
            }
            h.assertTrue(count>400,"Every registered block tested across six views and two body orientations");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    @GameTest(template="empty") public void placementCallbacksAndCopiedFacing(GameTestHelper h) {
        var body=ship(h);
        try {
            var level=h.getLevel();var p=body.getPlot().getCenterBlock().above(4);var world=h.absolutePos(new BlockPos(4,6,4));
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            SubLevelContainer.getContainer(level).physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(body.logicalPose().position()),new org.joml.Quaterniond().rotateXYZ(.25,1.2,-.3));
            for(var block:List.of(DEContent.REACTOR_INJECTOR.get(),DEContent.REACTOR_STABILIZER.get(),DEContent.DISLOCATOR_PEDESTAL.get())) {
                level.setBlockAndUpdate(world,block.defaultBlockState());level.setBlockAndUpdate(p,block.defaultBlockState());
                for(var delta:List.of(new Vec3(.5,3,.5),new Vec3(.5,-3,.5),new Vec3(3,0,0),new Vec3(0,0,3),new Vec3(.5,.3,.5)))for(var d:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
                    var look=Vec3.atLowerCornerOf(d.getNormal());
                    player.setPos(Vec3.atLowerCornerOf(world).add(delta));setLook(player,look);
                    block.setPlacedBy(level,world,block.defaultBlockState(),player,net.minecraft.world.item.ItemStack.EMPTY);
                    player.setPos(body.logicalPose().transformPosition(Vec3.atLowerCornerOf(p).add(delta)));setLook(player,body.logicalPose().transformNormal(look));
                    block.setPlacedBy(level,p,block.defaultBlockState(),player,net.minecraft.world.item.ItemStack.EMPTY);
                    var a=level.getBlockEntity(world);var b=level.getBlockEntity(p);
                    if(a instanceof TileReactorComponent x) h.assertTrue(x.facing.get()==((TileReactorComponent)b).facing.get(),"Reactor placement callback matches terrain at "+delta+" look "+d);
                    if(a instanceof TileDislocatorPedestal x) h.assertTrue(x.rotation.get()==((TileDislocatorPedestal)b).rotation.get(),"Pedestal local yaw matches terrain");
                }
            }
            var block=DEContent.REACTOR_INJECTOR.get();level.setBlockAndUpdate(p,block.defaultBlockState());
            var tag=new net.minecraft.nbt.CompoundTag();var data=new net.minecraft.nbt.CompoundTag();var facing=new net.minecraft.nbt.CompoundTag();
            facing.putByte("value",(byte)Direction.UP.get3DDataValue());data.put("facing",facing);tag.put("bc_managed_data",data);
            var stack=new net.minecraft.world.item.ItemStack(block);
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            block.setPlacedBy(level,p,block.defaultBlockState(),player,stack);
            h.assertTrue(((TileReactorComponent)level.getBlockEntity(p)).facing.get()==Direction.UP,"Explicit copied injector direction survives placement");
        } finally {cleanup(h,body);}
        h.succeed();
    }
    private static void setLook(net.minecraft.world.entity.Entity player,Vec3 look) {
        player.setYRot((float)Math.toDegrees(Math.atan2(-look.x,look.z)));
        player.setXRot((float)-Math.toDegrees(Math.atan2(look.y,Math.sqrt(look.x*look.x+look.z*look.z))));
        player.setYHeadRot(player.getYRot());player.yRotO=player.getYRot();player.xRotO=player.getXRot();
    }

    @GameTest(template="empty") public void diskReloadPreservesPylonTransferAndEnergy(GameTestHelper h) throws Exception {
        var body=ship(h);ServerSubLevel loaded=null;
        var c=SubLevelContainer.getContainer(h.getLevel());var holding=c.getHoldingChunkMap();
        try {
            var p=body.getPlot().getCenterBlock().above();
            h.getLevel().setBlockAndUpdate(p,DEContent.ENERGY_CORE.get().defaultBlockState());
            var core=(TileEnergyCore)h.getLevel().getBlockEntity(p);
            core.tier.set((byte)1);core.active.set(true);core.energy.validateStorage();core.energy.receiveOP(54321,false);
            var q=p.east(3);h.getLevel().setBlockAndUpdate(q,DEContent.ENERGY_PYLON.get().defaultBlockState());
            var pylon=(TileEnergyPylon)h.getLevel().getBlockEntity(q);pylon.coreOffset.set(q.subtract(p));pylon.structureValid.set(true);
            pylon.ioMode.set(com.brandon3055.draconicevolution.blocks.machines.EnergyPylon.Mode.OUTPUT);
            var id=body.getUniqueId();
            c.physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(-800000,100,-800000),body.logicalPose().orientation());body.updateBoundingBox();
            holding.moveToUnloaded(body,new net.minecraft.world.level.ChunkPos(-50000,-50000));
            var held=holding.getHoldingSubLevel(id);holding.saveAll();var pointer=held.pointer();
            h.assertTrue(pointer!=null,"Snapshot saved to actual disk");
            try(var disk=new dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage(holding.getStorage().getFolder())) {
                var saved=disk.attemptLoadSubLevel(pointer.chunkPos(),pointer.local());
                holding.loadHoldingSubLevel(new dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel(saved,pointer));
            }
            loaded=(ServerSubLevel)c.getSubLevel(id);
            h.assertTrue(loaded!=null && loaded!=body,"New body restored from disk");
            var restored=(TileEnergyCore)LoadedAccess.tile(h.getLevel(),p);
            var restoredPylon=(TileEnergyPylon)LoadedAccess.tile(h.getLevel(),q);
            h.assertTrue(restored!=null && restoredPylon!=null,"Both BEs restored");
            h.assertTrue(restored.energy.getOPStored()==54321,"Energy survived disk reload without loss or duplication");
            h.assertTrue(restoredPylon.getCore()==restored,"Pylon resolves new live core instance");
            h.assertTrue(restoredPylon.opAdapter.extractOP(321,false)==321 && restored.energy.getOPStored()==54000,"Transfer works after reload, not just rendering");
        } finally {if(loaded!=null)cleanup(h,loaded);cleanup(h,body);}
        h.succeed();
    }
}
