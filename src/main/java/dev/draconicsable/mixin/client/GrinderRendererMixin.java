package dev.draconicsable.mixin.client;

import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.blocks.tileentity.TileGrinder;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileGrinder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTileGrinder.class)
public abstract class GrinderRendererMixin {
    @ModifyExpressionValue(method="render(Lcom/brandon3055/draconicevolution/blocks/tileentity/TileGrinder;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lcom/brandon3055/draconicevolution/client/render/tile/RenderTileGrinder;getEntityMovingVec(Lnet/minecraft/world/entity/Entity;F)Lcodechicken/lib/vec/Vector3;"))
    private Vector3 draconicSable$localTarget(Vector3 target,@Local(argsOnly=true) TileGrinder tile) {
        var body=Sable.HELPER.getContainingClient(tile.getBlockPos());
        return body==null?target:new Vector3(body.renderPose().transformPositionInverse(new Vec3(target.x,target.y,target.z)));
    }
}
