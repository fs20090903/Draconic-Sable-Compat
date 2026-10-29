package dev.draconicsable.mixin;

import com.brandon3055.brandonscore.handlers.IProcess;
import com.brandon3055.draconicevolution.lib.ExplosionHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.draconicsable.explosion.RemovalCompletion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExplosionHelper.class)
public abstract class BlastRemovalMixin implements RemovalCompletion {
    @Unique private Runnable draconicSable$completed;
    @Override public void draconicSable$onComplete(Runnable action){draconicSable$completed=action;}
    @WrapOperation(method="finish",at=@At(value="INVOKE",target="Lcom/brandon3055/brandonscore/handlers/ProcessHandler;addProcess(Lcom/brandon3055/brandonscore/handlers/IProcess;)V"))
    private void draconicSable$holdChunksUntilRemoved(IProcess removal,Operation<Void> original) {
        if(draconicSable$completed==null){original.call(removal);return;}
        Runnable release=draconicSable$completed;draconicSable$completed=null;
        original.call(new IProcess(){
            private boolean released;
            private void release(){if(!released){released=true;release.run();}}
            public void updateProcess(){try{removal.updateProcess();if(removal.isDead())release();}catch(RuntimeException|Error e){release();throw e;}}
            public boolean isDead(){boolean dead=removal.isDead();if(dead)release();return dead;}
        });
    }
}
