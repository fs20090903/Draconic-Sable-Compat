package dev.draconicsable.mixin.client;

import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileChaosCrystal;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTileChaosCrystal.class)
public abstract class ChaosRendererMixin {
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/tileentity/TileChaosCrystal;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float draconicSable$yaw(float value,@Local(argsOnly=true) TileChaosCrystal tile) {
        var body=Sable.HELPER.getContainingClient(tile.getBlockPos());if(body==null)return value;
        var look=body.renderPose().transformNormalInverse(Minecraft.getInstance().player.getLookAngle());
        return (float)Math.toDegrees(Math.atan2(-look.x,look.z));
    }
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/tileentity/TileChaosCrystal;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private float draconicSable$pitch(float value,@Local(argsOnly=true) TileChaosCrystal tile) {
        var body=Sable.HELPER.getContainingClient(tile.getBlockPos());if(body==null)return value;
        var look=body.renderPose().transformNormalInverse(Minecraft.getInstance().player.getLookAngle());
        return (float)-Math.toDegrees(Math.atan2(look.y,Math.hypot(look.x,look.z)));
    }
}


