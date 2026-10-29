package dev.draconicsable.mixin.client;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEntityDetector;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TileEntityDetector.class)
public abstract class DetectorAnimationMixin {
    @WrapOperation(method="updateAnimation",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double draconicSable$worldDistance(Entity entity,Vec3 local,Operation<Double> original) {
        var tile=(TileEntityDetector)(Object)this;var body=Sable.HELPER.getContainingClient(tile.getBlockPos());
        return original.call(entity,body==null?local:body.renderPose().transformPosition(local));
    }
    @ModifyExpressionValue(method="updateAnimation",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 draconicSable$localEye(Vec3 world) {
        var body=Sable.HELPER.getContainingClient(((TileEntityDetector)(Object)this).getBlockPos());
        return body==null?world:body.renderPose().transformPositionInverse(world);
    }
}
