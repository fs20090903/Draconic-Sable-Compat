package dev.draconicsable.mixin.client;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorCore;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.draconicsable.LoadedAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.draconicsable.client.EffectBuffers;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTileReactorCore.class)
public abstract class ReactorRendererMixin {
    @Shadow public abstract void renderTransparent(TileReactorCore tile,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay);
    @WrapMethod(method="renderTransparent(Lcom/brandon3055/draconicevolution/blocks/reactor/tileentity/TileReactorCore;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V")
    private void draconicSable$isolateImmediateEffects(TileReactorCore tile,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay,Operation<Void> original) {
        var selected=EffectBuffers.select(buffer);
        try{original.call(tile,partial,pose,selected,light,overlay);}finally{EffectBuffers.finish(selected,buffer);}
    }
    @Inject(method="render(Lcom/brandon3055/draconicevolution/blocks/reactor/tileentity/TileReactorCore;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("TAIL"))
    private void draconicSable$renderInSublevelPass(TileReactorCore tile,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay,CallbackInfo ci) {
        // Render through the normal BE dispatcher on both terrain and sublevels.
        // BC's separate visibleSections walk is bypassed by alternative chunk renderers.
        renderTransparent(tile,partial,pose,buffer,light,overlay);
    }
}
