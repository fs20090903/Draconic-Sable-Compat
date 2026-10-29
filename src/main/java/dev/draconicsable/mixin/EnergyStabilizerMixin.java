package dev.draconicsable.mixin;

import com.brandon3055.brandonscore.lib.datamanager.ManagedPos;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import dev.draconicsable.LoadedAccess;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEnergyCoreStabilizer.class)
public abstract class EnergyStabilizerMixin {
    @Shadow @Final public ManagedPos coreOffset;
    @Inject(method="validateStructure",at=@At("HEAD"),cancellable=true)
    private void draconicSable$deferMissingParts(CallbackInfoReturnable<Boolean> ci) {
        var self=(TileEnergyCoreStabilizer)(Object)this;
        if(self.getLevel()==null || !self.isValidMultiBlock.get())return;
        for(var offset:com.brandon3055.brandonscore.utils.FacingUtils.getAroundAxis(self.multiBlockAxis.get()))
            if(!LoadedAccess.ready(self.getLevel(),self.getBlockPos().offset(offset))) {ci.setReturnValue(true);return;}
    }
    @Inject(method="getCore",at=@At("HEAD"),cancellable=true)
    private void draconicSable$preserveUnloadedLink(CallbackInfoReturnable<TileEnergyCore> ci) {
        var self=(TileEnergyCoreStabilizer)(Object)this;
        if(self.getLevel()==null || coreOffset.get()==null) return;
        var pos=self.getBlockPos().subtract(coreOffset.get());
        if(!LoadedAccess.ready(self.getLevel(),pos)) ci.setReturnValue(null);
    }
}
