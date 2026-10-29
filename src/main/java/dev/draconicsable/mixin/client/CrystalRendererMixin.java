package dev.draconicsable.mixin.client;
import com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalBase;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCrystal;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.draconicsable.client.EffectBuffers;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTileEnergyCrystal.class)
public abstract class CrystalRendererMixin {
    @WrapMethod(method="render(Lcom/brandon3055/draconicevolution/blocks/energynet/tileentity/TileCrystalBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V")
    private void draconicSable$isolateCrystalEffects(TileCrystalBase crystal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,Operation<Void> original) {
        // DE flushes after each crystal to preserve its tier/distance shader uniforms.
        // Keep that boundary, but do not flush Iris's unrelated scene geometry.
        var selected=EffectBuffers.select(buffers);
        try{original.call(crystal,partial,pose,selected,light,overlay);}finally{EffectBuffers.finish(selected,buffers);}
    }
    @Unique private Vec3 draconicSable$localViewer(TileCrystalBase tile) {
        var p=Minecraft.getInstance().player.position();var body=Sable.HELPER.getContainingClient(tile.getBlockPos());
        return body==null?p:body.renderPose().transformPositionInverse(p);
    }
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/energynet/tileentity/TileCrystalBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getX()D"))
    private double draconicSable$x(double value,@Local(argsOnly=true) TileCrystalBase tile){return draconicSable$localViewer(tile).x;}
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/energynet/tileentity/TileCrystalBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getY()D"))
    private double draconicSable$y(double value,@Local(argsOnly=true) TileCrystalBase tile){return draconicSable$localViewer(tile).y;}
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/energynet/tileentity/TileCrystalBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getZ()D"))
    private double draconicSable$z(double value,@Local(argsOnly=true) TileCrystalBase tile){return draconicSable$localViewer(tile).z;}
}

