package dev.draconicsable.mixin;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.*;
import dev.draconicsable.LoadedAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileReactorComponent.class)
public abstract class ReactorComponentMixin {
    @Shadow protected abstract BlockPos getCorePos();
    @Inject(method={"getCachedCore","checkAndGetCore","tryGetCore"},at=@At("HEAD"),cancellable=true)
    private void draconicSable$waitForCore(CallbackInfoReturnable<TileReactorCore> ci) {
        var self=(TileReactorComponent)(Object)this;
        if(self.getLevel()==null || !self.isBound.get()) { ci.setReturnValue(null); return; }
        if(!LoadedAccess.ready(self.getLevel(),getCorePos())) { ci.setReturnValue(null); return; }
        // Always resolve the current BE, never reuse a pre-unload instance.
        if(LoadedAccess.tile(self.getLevel(),getCorePos()) instanceof TileReactorCore core)
            ci.setReturnValue(self.getLevel().isClientSide || dev.draconicsable.MultiblockLinks.reactorReady(core) ? core : null);
    }
}
