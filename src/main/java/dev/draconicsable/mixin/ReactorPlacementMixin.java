package dev.draconicsable.mixin;

import com.brandon3055.draconicevolution.blocks.reactor.ReactorComponent;
import dev.draconicsable.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ReactorComponent.class)
public class ReactorPlacementMixin {
    @Redirect(method="setPlacedBy", at=@At(value="INVOKE", target="Lcodechicken/lib/util/RotationUtils;getPlacedRotation(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/core/Direction;"))
    private Direction draconicSable$localPlacement(BlockPos pos, LivingEntity placer,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.world.item.ItemStack stack) {
        var copy=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if(copy!=null) {
            var value=copy.copyTag().getCompound("bc_managed_data").getCompound("facing");
            if(value.contains("value",net.minecraft.nbt.Tag.TAG_BYTE)) {
                int side=value.getByte("value");
                if(side>=0 && side<6)return Direction.from3DDataValue(side).getOpposite();
            }
        }
        return Placement.placedRotation(pos, placer);
    }
}
