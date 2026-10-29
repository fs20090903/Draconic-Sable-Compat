package dev.draconicsable.mixin.client;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCore;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Matrix4;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.draconicsable.client.EffectBuffers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** DE's custom colour uniforms belong to one core; buffered vertices must not borrow the next core's colours. */
@Mixin(RenderTileEnergyCore.class)
public abstract class EnergyCoreRendererMixin {
    @WrapMethod(method="render(Lcom/brandon3055/draconicevolution/blocks/tileentity/TileEnergyCore;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V")
    private void draconicSable$preserveCoreDrawOrder(TileEnergyCore core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,Operation<Void> original) {
        // Keep the opaque inner core before its translucent surface, without draining the scene.
        var selected=EffectBuffers.select(buffers);
        try{original.call(core,partial,pose,selected,light,overlay);}finally{EffectBuffers.finish(selected,buffers);}
    }
    @WrapMethod(method="renderFancyOuterCore")
    private void draconicSable$isolateColourUniforms(TileEnergyCore core,CCRenderState state,Matrix4 matrix,MultiBufferSource buffers,float partial,float rotation,double scale,Operation<Void> original) {
        var selected=EffectBuffers.select(buffers);
        try{original.call(core,state,matrix,selected,partial,rotation,scale);}finally{EffectBuffers.finish(selected,buffers);}
    }
}
