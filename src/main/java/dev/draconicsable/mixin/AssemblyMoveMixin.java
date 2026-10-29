package dev.draconicsable.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.draconicsable.AssemblyData;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubLevelAssemblyHelper.class)
public abstract class AssemblyMoveMixin {
    @WrapOperation(method="moveBlocks",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"))
    private static void draconicSable$rotateData(BlockEntity tile,CompoundTag tag,HolderLookup.Provider provider,Operation<Void> original,
            @Local(argsOnly=true) SubLevelAssemblyHelper.AssemblyTransform transform) {
        AssemblyData.rotate(tag,transform.getRotation());
        original.call(tile,tag,provider);
    }
}
