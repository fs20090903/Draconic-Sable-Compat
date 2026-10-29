package dev.draconicsable.mixin;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import dev.draconicsable.MultiblockLinks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileReactorCore.class)
public abstract class ReactorCoreMixin {
    @Inject(method={"tick","chargeReactor","activateReactor","shutdownReactor"},at=@At("TAIL"))
    private void draconicSable$keepActiveReactorLoaded(CallbackInfo ci) {
        dev.draconicsable.CoreLoading.update((TileReactorCore)(Object)this);
    }
    @Inject(method="updateCoreLogic",at=@At("HEAD"),cancellable=true)
    private void draconicSable$waitForComponents(CallbackInfo ci) {
        var self=(TileReactorCore)(Object)this;
        // Shield drain must not run while a saved component is temporarily inaccessible.
        // Actual missing blocks in a loaded chunk retain the original failure behaviour.
        if(self.getLevel()!=null && !self.getLevel().isClientSide && self.reactorState.get()!=TileReactorCore.ReactorState.BEYOND_HOPE && !MultiblockLinks.reactorReady(self)) ci.cancel();
    }
}
