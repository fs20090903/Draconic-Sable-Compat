package dev.draconicsable.mixin;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import dev.draconicsable.LoadedAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEnergyCore.class)
public abstract class EnergyCoreMixin {
    @Inject(method={"tick","toggleActivation","deactivateCore"},at=@At("TAIL"))
    private void draconicSable$keepActiveCoreLoaded(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        dev.draconicsable.CoreLoading.update((TileEnergyCore)(Object)this);
    }
    @Inject(method="validateStructure",at=@At("HEAD"),cancellable=true)
    private void draconicSable$deferIncompleteValidation(CallbackInfoReturnable<Boolean> ci) {
        var self=(TileEnergyCore)(Object)this;
        if(self.getLevel()==null || self.getLevel().isClientSide || !self.active.get()) return;
        for(var offset:self.stabilizerPositions) {
            if(offset.get()!=null && !LoadedAccess.ready(self.getLevel(),self.getBlockPos().subtract(offset.get()))) {
                ci.setReturnValue(self.isStructureValid()); return;
            }
        }
        var definition=self.getMultiBlockDef();
        if(definition!=null) for(var entry:definition.getBlocksAt(self.getBlockPos()).entrySet()) {
            if(entry.getValue() instanceof com.brandon3055.brandonscore.multiblock.EmptyPart)continue;
            var pos=entry.getKey();
            if(!LoadedAccess.ready(self.getLevel(),pos)) { ci.setReturnValue(self.isStructureValid()); return; }
        }
    }
}
