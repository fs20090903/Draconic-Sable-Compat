package dev.draconicsable.mixin;

import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import dev.draconicsable.explosion.*;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ProcessExplosion.class)
public abstract class WorldExplosionMixin implements BlastPlan {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final public Vector3 origin;
    @Shadow public int radius;
    @Shadow public int maxRadius;
    @Unique private WorldBlast draconicSable$blast;
    @Unique private BlastTrace draconicSable$trace;
    @Unique private BlastTrace draconicSable$previousTerrain;
    @Unique private final BlastRings draconicSable$rings=new BlastRings();
    @Shadow protected boolean calculationComplete;
    @Shadow private double trace(Vector3 pos,double power,int remaining,int direction,double resistance,int travelled){throw new AssertionError();}
    @Override public void draconicSable$prepare(){if(draconicSable$blast==null)draconicSable$blast=new WorldBlast(level,new Vec3(origin.x,origin.y,origin.z),maxRadius);}
    @Override public boolean draconicSable$matches(){return draconicSable$blast!=null && draconicSable$blast.matches();}
    @Override public int draconicSable$terrainTickets(){return draconicSable$trace==null?0:draconicSable$trace.heldChunks();}
    @Override public BlastTrace draconicSable$takeTerrain(){var t=draconicSable$trace!=null?draconicSable$trace:draconicSable$previousTerrain;draconicSable$trace=null;draconicSable$previousTerrain=null;return t;}
    @Override public void draconicSable$reuseTerrain(BlastTrace previous){draconicSable$previousTerrain=previous;}
    @Override public void draconicSable$discard(){BlastBudget.forget(level,this);if(draconicSable$blast!=null){draconicSable$blast.close();draconicSable$blast=null;}if(draconicSable$trace!=null){draconicSable$trace.clear();draconicSable$trace=null;}if(draconicSable$previousTerrain!=null){draconicSable$previousTerrain.clear();draconicSable$previousTerrain=null;}}

    @Inject(method="<init>",at=@At("RETURN"))
    private void draconicSable$worldOrigin(BlockPos anchor,int radius,ServerLevel level,int delay,CallbackInfo ci) {
        var body=Sable.HELPER.getContaining(level,anchor);
        if(body!=null){var world=body.logicalPose().transformPosition(Vec3.atCenterOf(anchor));origin.set(world.x,world.y,world.z);}
    }
    @Inject(method="updateCalculation",at=@At("HEAD"),cancellable=true)
    private void draconicSable$capture(CallbackInfo ci) {
        ci.cancel();
        if(calculationComplete)return;
        try(var budget=new BlastBudget(level,this)) {
            if(System.nanoTime()>=budget.deadline)return;
            draconicSable$prepare();
            if(!draconicSable$blast.capture(budget.deadline))return;
            if(draconicSable$trace==null){draconicSable$trace=new BlastTrace((ProcessExplosion)(Object)this,level,draconicSable$blast);draconicSable$trace.reuse(draconicSable$previousTerrain);draconicSable$previousTerrain=null;}
            draconicSable$trace.reportLoading();
            calculationComplete=draconicSable$rings.step((ProcessExplosion)(Object)this,level,draconicSable$trace,budget.deadline);
            draconicSable$blast.reportProgress(radius,calculationComplete);
            if(calculationComplete) {
                BlastBudget.forget(level,this);
                draconicSable$trace.releasePrevious();
                com.mojang.logging.LogUtils.getLogger().info("[DE blast solver] radius={} visited={} skippedAir={} chunkLookups={}",maxRadius,
                    draconicSable$trace.visited,draconicSable$trace.skippedAir,draconicSable$trace.chunkLookups);
            }
        }catch(RuntimeException|Error e){BlastBudget.forget(level,this);throw e;}
    }
    @WrapOperation(method="trace",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;asLong(III)J"))
    private long draconicSable$floorNegativeCoordinates(int x,int y,int z,Operation<Long> original,@Local(argsOnly=true) Vector3 ray) {
        return BlockPos.containing(ray.x,ray.y,ray.z).asLong();
    }
    @WrapOperation(method="trace",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState draconicSable$traceBodies(ServerLevel level,BlockPos cell,Operation<BlockState> original,
        @Local(argsOnly=true,ordinal=0) double power,@Local(argsOnly=true,ordinal=0) int remaining,
        @Local(argsOnly=true,ordinal=2) int travelled,@Share("bodyResistance") LocalDoubleRef extra) {
        if(draconicSable$blast!=null)extra.set(draconicSable$blast.trace(cell,power,remaining,travelled,radius,maxRadius));
        return original.call(level,cell);
    }
    @WrapOperation(method="trace",at=@At(value="INVOKE",target="Lcom/brandon3055/draconicevolution/blocks/reactor/ProcessExplosion;trace(Lcodechicken/lib/vec/Vector3;DIIDI)D",ordinal=1))
    private double draconicSable$bodyResistance(ProcessExplosion self,Vector3 pos,double power,int remaining,int direction,double resistance,int travelled,Operation<Double> original,@Share("bodyResistance") LocalDoubleRef extra) {
        return original.call(self,pos,power-extra.get(),remaining,direction,resistance+extra.get(),travelled);
    }
    @ModifyExpressionValue(method="trace",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getMinBuildHeight()I"))
    private int draconicSable$belowTerrain(int value){return draconicSable$blast==null?value:draconicSable$blast.minY();}
    @ModifyExpressionValue(method="trace",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getMaxBuildHeight()I"))
    private int draconicSable$aboveTerrain(int value){return draconicSable$blast==null?value:draconicSable$blast.maxY();}
    @Inject(method="detonate",at=@At("RETURN"))
    private void draconicSable$applyBodies(CallbackInfoReturnable<Boolean> ci) {
        if(ci.getReturnValueZ() && draconicSable$blast!=null){draconicSable$blast.apply();draconicSable$blast=null;}
    }
    @WrapOperation(method="detonate",at=@At(value="NEW",target="(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lcom/brandon3055/draconicevolution/lib/ExplosionHelper;"))
    private com.brandon3055.draconicevolution.lib.ExplosionHelper draconicSable$releaseAfterTerrainRemoval(ServerLevel level,BlockPos pos,Operation<com.brandon3055.draconicevolution.lib.ExplosionHelper> original) {
        var helper=original.call(level,pos);
        if(draconicSable$trace!=null){var trace=draconicSable$trace;((RemovalCompletion)helper).draconicSable$onComplete(()->{trace.clear();if(draconicSable$trace==trace)draconicSable$trace=null;});}
        return helper;
    }
}
