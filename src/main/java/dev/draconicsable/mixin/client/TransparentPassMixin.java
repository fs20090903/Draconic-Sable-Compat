package dev.draconicsable.mixin.client;
import com.brandon3055.brandonscore.client.BCClientEventHandler;
import com.brandon3055.brandonscore.client.render.BlockEntityRendererTransparent;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.draconicsable.LoadedAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BCClientEventHandler.class)
public abstract class TransparentPassMixin {
    @Inject(method="renderTransparent",at=@At("HEAD"),cancellable=true)
    private void draconicSable$avoidWrongWorldPass(Camera camera,BlockEntityRendererTransparent<?> renderer,BlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,CallbackInfo ci) {
        if(tile instanceof TileReactorCore) ci.cancel();
    }
}
