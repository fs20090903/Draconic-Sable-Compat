package dev.draconicsable.mixin;
import com.brandon3055.draconicevolution.blocks.tileentity.MultiBlockController;
import com.brandon3055.draconicevolution.blocks.tileentity.TileStructureBlock;
import dev.draconicsable.LoadedAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileStructureBlock.class)
public abstract class StructureBlockMixin {
    @Shadow private BlockPos getControllerPos() { throw new AssertionError(); }
    @Inject(method="getController",at=@At("HEAD"),cancellable=true)
    private void draconicSable$deferReversion(CallbackInfoReturnable<MultiBlockController> ci) {
        var self=(TileStructureBlock)(Object)this;
        var pos=getControllerPos();
        if(pos!=null && self.getLevel()!=null && !LoadedAccess.ready(self.getLevel(),pos)) ci.setReturnValue(null);
    }
}
