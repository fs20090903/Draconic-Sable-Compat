package dev.draconicsable.mixin;

import dev.draconicsable.explosion.BlastHold;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSubLevel.class)
public abstract class BlastHoldMixin {
    // Keep mass/collider updates and queued-force reset. Temporarily skip force producers
    // while anchored, so engines cannot spend deletion time fighting the hold.
    @Inject(method={"prePhysicsTick","applyQueuedForces"},at=@At("HEAD"),cancellable=true)
    private void draconicSable$holdForces(CallbackInfo ci) {
        if(BlastHold.held((ServerSubLevel)(Object)this))ci.cancel();
    }
}
