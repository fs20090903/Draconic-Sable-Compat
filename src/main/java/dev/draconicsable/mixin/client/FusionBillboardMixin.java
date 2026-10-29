package dev.draconicsable.mixin.client;

import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileFusionCraftingCore;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.ryanhcode.sable.Sable;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTileFusionCraftingCore.class)
public abstract class FusionBillboardMixin {
    @Unique private TileFusionCraftingCore draconicSable$rendering;
    @WrapMethod(method="render(Lcom/brandon3055/draconicevolution/blocks/tileentity/TileFusionCraftingCore;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V")
    private void draconicSable$context(TileFusionCraftingCore tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,Operation<Void> original) {
        var previous=draconicSable$rendering;draconicSable$rendering=tile;
        try{original.call(tile,partial,pose,buffers,light,overlay);}finally{draconicSable$rendering=previous;}
    }
    @ModifyExpressionValue(method={"renderEffects","renderIngredientEffect"},at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;rotation()Lorg/joml/Quaternionf;"))
    private Quaternionf draconicSable$localBillboard(Quaternionf camera) {
        if(draconicSable$rendering==null)return camera;
        var body=Sable.HELPER.getContainingClient(draconicSable$rendering.getBlockPos());
        return body==null?camera:new Quaternionf(body.renderPose().orientation()).conjugate().mul(camera);
    }
}
