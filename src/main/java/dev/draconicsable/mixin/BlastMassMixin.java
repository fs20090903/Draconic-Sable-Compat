package dev.draconicsable.mixin;

import dev.draconicsable.explosion.BlastMassBatch;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(SubLevelPhysicsSystem.class)
public abstract class BlastMassMixin {
    @ModifyVariable(method="updateMassDataFromBlockChange",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private boolean draconicSable$batchMass(boolean notify,@Local(argsOnly=true) SubLevel body) {
        return notify && !(body instanceof ServerSubLevel server && BlastMassBatch.defer(server));
    }
}
