package dev.draconicsable.mixin.client;

import com.brandon3055.draconicevolution.blocks.tileentity.TileDislocatorPedestal;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileDislocatorPedestal;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTileDislocatorPedestal.class)
public abstract class PedestalLabelMixin {
    @Unique private Vec3 draconicSable$viewer(TileDislocatorPedestal tile) {
        var p=Minecraft.getInstance().player.position();
        var body=Sable.HELPER.getContainingClient(tile.getBlockPos());
        return body==null?p:body.renderPose().transformPositionInverse(p);
    }
    @ModifyExpressionValue(method="drawName",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double draconicSable$x(double v,@Local(argsOnly=true) TileDislocatorPedestal tile){return draconicSable$viewer(tile).x;}
    @ModifyExpressionValue(method="drawName",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double draconicSable$y(double v,@Local(argsOnly=true) TileDislocatorPedestal tile){return draconicSable$viewer(tile).y;}
    @ModifyExpressionValue(method="drawName",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double draconicSable$z(double v,@Local(argsOnly=true) TileDislocatorPedestal tile){return draconicSable$viewer(tile).z;}
    @ModifyExpressionValue(method="drawName",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getEyeHeight()F"))
    private float draconicSable$eye(float height,@Local(argsOnly=true) TileDislocatorPedestal tile){
        var body=Sable.HELPER.getContainingClient(tile.getBlockPos());
        return body==null?height:(float)body.renderPose().transformNormalInverse(new Vec3(0,height,0)).y;
    }
}

