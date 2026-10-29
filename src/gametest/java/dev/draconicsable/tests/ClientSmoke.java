package dev.draconicsable.tests;

import com.brandon3055.draconicevolution.blocks.energynet.tileentity.*;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.*;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.draconicsable.*;
import dev.draconicsable.client.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

@Mod(value="draconic_sable_tests",dist=Dist.CLIENT)
public class ClientSmoke {
    private static int ticks,stage,due;
    private static volatile UUID bodyId;
    private static volatile BlockPos reactorPos,energyPos,sourcePos,targetPos;
    private static volatile String failure;
    private static volatile boolean transferred;
    private static volatile BlockPos sceneOrigin;
    public ClientSmoke() { if(Boolean.getBoolean("draconicSable.clientSmoke")) NeoForge.EVENT_BUS.addListener(ClientSmoke::tick); }
    private static void require(boolean b,String text){if(!b)throw new IllegalStateException(text);}
    private static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(++ticks>2400) throw new IllegalStateException("Client smoke timeout at stage "+stage+" failure="+failure);
        if(failure!=null)throw new IllegalStateException(failure);
        if(mc.getOverlay()!=null || ticks<due)return;
        if(stage==0 && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.hideGui=true;
            mc.createWorldOpenFlows().createFreshLevel("draconic-smoke-"+System.currentTimeMillis(),
                new net.minecraft.world.level.LevelSettings("Draconic compatibility smoke",net.minecraft.world.level.GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                new net.minecraft.world.level.levelgen.WorldOptions(123,false,false),
                r->r.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).get(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).createWorldDimensions(),mc.screen);
            stage=1;
        } else if(stage==1 && mc.player!=null && mc.level!=null) {
            stage=2;due=ticks+100;
            UUID playerId=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{
                try{setup(mc,playerId);}catch(Throwable e){failure=e.toString();e.printStackTrace();}
            });
        } else if(stage==2 && bodyId!=null) {
            var body=SubLevelContainer.getContainer(mc.level).getSubLevel(bodyId);
            if(body==null){due=ticks+20;return;}
            require(LoadedAccess.tile(mc.level,reactorPos) instanceof TileReactorCore,"Client received reactor BE");
            require(LoadedAccess.tile(mc.level,sourcePos) instanceof TileCrystalBase,"Client received linked crystals");
            com.brandon3055.draconicevolution.client.handler.ClientEventHandler.playerHoldingWrench=true;
            mc.player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(DEContent.CRYSTAL_BINDER.get()));
            stage=3;due=ticks+60;
        } else if(stage==3) {
            // Exercise actual transformed render methods and validate beam geometry, not only class loading.
            var source=(TileCrystalBase)LoadedAccess.tile(mc.level,sourcePos);
            var target=(TileCrystalBase)LoadedAccess.tile(mc.level,targetPos);
            var beam=new com.brandon3055.draconicevolution.client.render.effect.CrystalFXBeam<>(mc.level,source,target);
            com.brandon3055.draconicevolution.client.handler.ClientEventHandler.playerHoldingWrench=true;
            var consumer=new CountingVertices();
            beam.render(consumer,mc.gameRenderer.getMainCamera(),0.5f);
            require(consumer.count>0 && consumer.maxAbsolute<200,"Beam emits nearby finite vertices, not storage-coordinate streaks: "+consumer.maxAbsolute);
            Vec3Check.assertBeamBounds(beam,source,target);
            capture(mc,"draconic-sublevel-render.png");
            stage=4;due=ticks+40;
            mc.getSingleplayerServer().execute(()->{
                try{
                    var level=mc.getSingleplayerServer().overworld();
                    var e=(TileEnergyCore)LoadedAccess.tile(level,energyPos);
                    transferred=e!=null && e.energy.getOPStored()>0;
                    if(!transferred) {
                        var s=(TileCrystalBase)LoadedAccess.tile(level,sourcePos);
                        var t=(TileCrystalDirectIO)LoadedAccess.tile(level,targetPos);
                        var py=(TileEnergyPylon)LoadedAccess.tile(level,targetPos.east());
                        failure="No transfer: core="+e.active.get()+" capacity="+e.energy.getMaxOPStored()+" source="+s.getEnergyStored()+" target="+t.getEnergyStored()+" facing="+t.facing.get()+" output="+t.outputMode.get()+" links="+s.getLinks()+" pylonvalid="+py.structureValid.get()+" offset="+py.coreOffset.get()+" mode="+py.ioMode.get()+" core="+py.getCore();
                    }
                }catch(Throwable e){failure=e.toString();}
            });
        } else if(stage==4 && transferred) {
            VisualAudit.audit(mc,bodyId);
            BufferIsolationAudit.run(mc,reactorPos,energyPos);
            stage=5;due=ticks+30;camera(mc,energyPos,true);
        } else if(stage==5) {
            capture(mc,"energy-core-sublevel.png");stage=6;due=ticks+30;camera(mc,sceneOrigin.offset(-23,4,0),false);
        } else if(stage==6) {
            capture(mc,"energy-core-terrain.png");stage=7;due=ticks+30;camera(mc,reactorPos,true);
        } else if(stage==7) {
            capture(mc,"reactor-sublevel.png");stage=8;due=ticks+30;camera(mc,sceneOrigin.offset(-40,4,0),false);
        } else if(stage==8) {
            capture(mc,"reactor-terrain.png");
            try{Files.writeString(Path.of("client-smoke-passed.txt"),"PASS: integrated server/client assembly sync, rotated reactor renderer, crystal beam finite world-space geometry and bounds, real crystal-to-pylon-to-core energy transfer. Screenshot: draconic-sublevel-render.png\n");}
            catch(Exception e){throw new RuntimeException(e);}
            stage=9;mc.stop();
        }
    }
    private static void camera(Minecraft mc,BlockPos target,boolean onboard) {
        var center=net.minecraft.world.phys.Vec3.atCenterOf(target);
        boolean large=target.equals(energyPos)||target.equals(sceneOrigin.offset(-23,4,0));
        var eye=center.add(0,large?6:3,large?-19:-9);
        if(onboard) {
            var body=SubLevelContainer.getContainer(mc.level).getSubLevel(bodyId);
            center=body.logicalPose().transformPosition(center);eye=body.logicalPose().transformPosition(eye);
        }
        var look=center.subtract(eye);var feet=eye.add(0,-mc.player.getEyeHeight(),0);
        float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z)),pitch=(float)-Math.toDegrees(Math.atan2(look.y,Math.sqrt(look.x*look.x+look.z*look.z)));
        mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).connection.teleport(feet.x,feet.y,feet.z,yaw,pitch));
    }
    private static void setup(Minecraft mc,UUID playerId) {
        var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayer(playerId);
        var p=new BlockPos(player.getBlockX(),80,player.getBlockZ()+16);
        sceneOrigin=p;
        Set<BlockPos> blocks=new LinkedHashSet<>();
        java.util.function.BiConsumer<BlockPos,Block> put=(at,b)->{level.setBlockAndUpdate(at,b.defaultBlockState());blocks.add(at);};
        for(int x=-6;x<=26;x++)for(int z=-8;z<=8;z++)put.accept(p.offset(x,-1,z),Blocks.SMOOTH_STONE);
        var rp=p.above(4);put.accept(rp,DEContent.REACTOR_CORE.get());
        for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            var q=rp.relative(d,5);put.accept(q,DEContent.REACTOR_STABILIZER.get());((TileReactorComponent)level.getBlockEntity(q)).facing.set(d.getOpposite());
            for(int y=0;y<4;y++)put.accept(p.relative(d,5).above(y),Blocks.IRON_BLOCK);
        }
        put.accept(rp.above(5),DEContent.REACTOR_INJECTOR.get());((TileReactorComponent)level.getBlockEntity(rp.above(5))).facing.set(Direction.DOWN);
        var reactor=(TileReactorCore)level.getBlockEntity(rp);reactor.attemptInitialization();
        reactor.reactableFuel.set(1296);reactor.temperature.set(4000);reactor.maxShieldCharge.set(1000000);reactor.shieldCharge.set(500000);reactor.reactorState.set(TileReactorCore.ReactorState.COOLING);
        var ep=p.offset(17,4,0);put.accept(ep,DEContent.ENERGY_CORE.get());var core=(TileEnergyCore)level.getBlockEntity(ep);
        core.tier.set((byte)8);core.customColour.set(true);
        for(var part:core.getMultiBlockDef().getBlocksAt(ep).entrySet()) {
            if(part.getKey().equals(ep))continue;
            if(part.getValue() instanceof com.brandon3055.brandonscore.multiblock.EmptyPart) {
                level.removeBlock(part.getKey(),false);blocks.remove(part.getKey());
            } else put.accept(part.getKey(),part.getValue().getFirstValidBlock());
        }
        for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            var stab=ep.relative(d,7);put.accept(stab,DEContent.ENERGY_CORE_STABILIZER.get());
            for(var offset:com.brandon3055.brandonscore.utils.FacingUtils.getAroundAxis(d.getAxis()))put.accept(stab.offset(offset),DEContent.ENERGY_CORE_STABILIZER.get());
            ((TileEnergyCoreStabilizer)level.getBlockEntity(stab)).onPlaced();
            for(int y=0;y<3;y++)put.accept(p.offset(17,0,0).relative(d,7).above(y),Blocks.IRON_BLOCK);
        }
        core.toggleActivation();require(core.active.get(),"Energy core actually activates with real stabilizers");
        var pp=p.offset(17,0,-3);put.accept(pp,DEContent.ENERGY_PYLON.get());put.accept(pp.above(),Blocks.GLASS);
        var pylon=(TileEnergyPylon)level.getBlockEntity(pp);pylon.validateStructure();pylon.ioMode.set(com.brandon3055.draconicevolution.blocks.machines.EnergyPylon.Mode.INPUT);pylon.selectNextCore();require(pylon.getCore()==core,"Real pylon search selects core");
        var sp=p.offset(13,1,1);var tp=pp.west();put.accept(sp,DEContent.DRACONIC_RELAY_CRYSTAL.get());put.accept(tp,DEContent.DRACONIC_IO_CRYSTAL.get());
        var source=(TileCrystalBase)level.getBlockEntity(sp);var target=(TileCrystalDirectIO)level.getBlockEntity(tp);target.facing.set(Direction.EAST);target.outputMode.set(true);
        source.onTilePlaced(null,source.getBlockState());
        source.createLink(target);target.createLink(source);source.modifyEnergyStored(1000000);
        require(source.getEnergyStored()==1000000,"Source fixture really contains energy before assembly");
        // Identical terrain reference, including active state and energy links.
        var copyOffset=new BlockPos(-40,0,0);
        for(var at:blocks)level.setBlockAndUpdate(at.offset(copyOffset),level.getBlockState(at));
        for(var at:blocks) {
            var original=level.getBlockEntity(at);var copy=level.getBlockEntity(at.offset(copyOffset));
            if(original!=null && copy!=null)copy.loadWithComponents(original.saveWithoutMetadata(level.registryAccess()),level.registryAccess());
        }
        var body=SubLevelAssemblyHelper.assembleBlocks(level,p,blocks,BoundingBox3i.from(blocks));
        var center=body.getPlot().getCenterBlock();
        // Assembly uses the supplied anchor as its plot centre.
        reactorPos=center.offset(rp.subtract(p));energyPos=center.offset(ep.subtract(p));sourcePos=center.offset(sp.subtract(p));targetPos=center.offset(tp.subtract(p));
        var c=SubLevelContainer.getContainer(level);c.physicsSystem().setPaused(true);
        c.physicsSystem().getPipeline().teleport(body,new org.joml.Vector3d(body.logicalPose().position()),new org.joml.Quaterniond().rotateXYZ(Math.toRadians(10),Math.toRadians(15),Math.toRadians(8)));
        CoreLoading.update(LoadedAccess.tile(level,reactorPos));CoreLoading.update(LoadedAccess.tile(level,energyPos));bodyId=body.getUniqueId();
        player.connection.teleport(p.getX()+9.5,89,p.getZ()-22.5,0,10);
        player.getAbilities().flying=true;player.onUpdateAbilities();
        level.setDayTime(6000);
    }
    private static void capture(Minecraft mc,String file) {
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of(file));}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static final class Vec3Check {
        static void assertBeamBounds(com.brandon3055.draconicevolution.client.render.effect.CrystalFXBeam<?> beam,TileCrystalBase source,TileCrystalBase target) {
            var a=source.getBeamLinkPos(target.getBlockPos());var b=target.getBeamLinkPos(source.getBlockPos());
            require(beam.getRenderBoundingBox(.5f).contains(RenderCoordinates.project(new net.minecraft.world.phys.Vec3(a.x,a.y,a.z))),"Bounds contain source");
            require(beam.getRenderBoundingBox(.5f).contains(RenderCoordinates.project(new net.minecraft.world.phys.Vec3(b.x,b.y,b.z))),"Bounds contain target");
        }
    }
    public static final class CountingVertices implements com.mojang.blaze3d.vertex.VertexConsumer {
        public int count;public double maxAbsolute;
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){require(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Finite vertex");count++;maxAbsolute=Math.max(maxAbsolute,Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z))));return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}

