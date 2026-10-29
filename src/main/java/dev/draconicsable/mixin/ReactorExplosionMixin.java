package dev.draconicsable.mixin;

import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.draconicsable.explosion.AnchoredReactorExplosion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TileReactorCore.class)
public abstract class ReactorExplosionMixin {
    @Shadow private ProcessExplosion explosionProcess;
    @WrapOperation(method="updateCriticalState",at=@At(value="INVOKE",target="Lcom/brandon3055/draconicevolution/blocks/reactor/ProcessExplosion;isCalculationComplete()Z"))
    private boolean draconicSable$prepareDuringSafetyCountdown(ProcessExplosion process,Operation<Boolean> original) {
        // DE's existing safety delay now overlaps preparation instead of waiting for it.
        // This does not change the process's real completion flag or permit early removal.
        return process instanceof AnchoredReactorExplosion || original.call(process);
    }
    @WrapOperation(method="updateCriticalState",at=@At(value="INVOKE",target="Lcom/brandon3055/draconicevolution/blocks/reactor/ProcessExplosion;detonate()Z"))
    private boolean draconicSable$holdExpiredCountdown(ProcessExplosion process,Operation<Boolean> original) {
        boolean result=original.call(process);
        if(process instanceof AnchoredReactorExplosion anchored && !anchored.actuallyDetonated())
            ((TileReactorCore)(Object)this).explosionCountdown.set(0);
        return result;
    }
    @WrapOperation(method="updateCriticalState",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean draconicSable$removeOnlyAfterActualDetonation(Level level,BlockPos pos,boolean moving,Operation<Boolean> original) {
        if(explosionProcess instanceof AnchoredReactorExplosion anchored && !anchored.actuallyDetonated())return false;
        return original.call(level,pos,moving);
    }
    @WrapOperation(method="updateCriticalState",at=@At(value="NEW",target="(Lnet/minecraft/core/BlockPos;ILnet/minecraft/server/level/ServerLevel;I)Lcom/brandon3055/draconicevolution/blocks/reactor/ProcessExplosion;"))
    private ProcessExplosion draconicSable$anchorUntilDetonation(BlockPos pos,int radius,ServerLevel level,int delay,Operation<ProcessExplosion> original) {
        return new AnchoredReactorExplosion(pos,radius,level,delay);
    }
    @ModifyExpressionValue(method="minimalBoom",at=@At(value="INVOKE",target="Lcom/brandon3055/brandonscore/lib/Vec3D;getCenter(Lnet/minecraft/core/BlockPos;)Lcom/brandon3055/brandonscore/lib/Vec3D;"))
    private Vec3D draconicSable$smallBlastAndDebris(Vec3D local) {
        var core=(TileReactorCore)(Object)this;var body=Sable.HELPER.getContaining(core);
        if(body==null)return local;
        var world=body.logicalPose().transformPosition(new Vec3(local.x,local.y,local.z));
        return new Vec3D(world.x,world.y,world.z);
    }
}
