package dev.draconicsable.mixin;

import com.brandon3055.brandonscore.lib.datamanager.ManagedPos;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import dev.draconicsable.LoadedAccess;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEnergyPylon.class)
public abstract class PylonMixin {
    @Shadow @Final public ManagedPos coreOffset;
    @Shadow private TileEnergyCore core;
    @Inject(method="getCore",at=@At("HEAD"),cancellable=true)
    private void draconicSable$loadedCore(CallbackInfoReturnable<TileEnergyCore> ci) {
        var self=(TileEnergyPylon)(Object)this;
        if (!LoadedAccess.onboard(self.getLevel(),self.getBlockPos())) return;
        if (coreOffset.get()==null) { core=null; ci.setReturnValue(null); return; }
        var pos=self.getBlockPos().subtract(coreOffset.get());
        if (!LoadedAccess.ready(self.getLevel(),pos)) { core=null; ci.setReturnValue(null); return; }
        var tile=LoadedAccess.tile(self.getLevel(),pos);
        core=tile instanceof TileEnergyCore c ? c : null;
        if(core==null && !self.getLevel().isClientSide) coreOffset.set(null);
        ci.setReturnValue(core);
    }
}
