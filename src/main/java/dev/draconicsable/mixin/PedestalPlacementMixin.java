package dev.draconicsable.mixin;

import com.brandon3055.draconicevolution.blocks.DislocatorPedestal;
import dev.draconicsable.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(DislocatorPedestal.class)
public abstract class PedestalPlacementMixin {
    @Redirect(method="setPlacedBy",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float draconicSable$localYaw(LivingEntity entity,@Local(argsOnly=true) BlockPos pos){return Placement.yaw(pos,entity);}
}
