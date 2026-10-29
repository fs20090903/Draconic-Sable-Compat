package dev.draconicsable.mixin.client;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.client.render.effect.CrystalFXBase;
import dev.draconicsable.client.CrystalAnchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrystalFXBase.class)
public abstract class CrystalBaseMixin implements CrystalAnchor {
    @Unique private Vec3 draconicSable$anchor;
    @Inject(method="<init>",at=@At("TAIL"))
    private void draconicSable$rememberOrigin(ClientLevel level,BlockEntity tile,CallbackInfo ci) { draconicSable$anchor=Vec3.atCenterOf(tile.getBlockPos()); }
    @Inject(method="setPosition",at=@At("TAIL"))
    private void draconicSable$rememberLinkOrigin(Vec3D pos,CallbackInfo ci) { draconicSable$anchor=new Vec3(pos.x,pos.y,pos.z); }
    public Vec3 draconicSable$source() { return draconicSable$anchor; }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="getRenderVectors")
    private org.joml.Vector3f[] draconicSable$orientRing(net.minecraft.client.Camera camera,float x,float y,float z,float scale,com.llamalad7.mixinextras.injector.wrapoperation.Operation<org.joml.Vector3f[]> original) {
        if((Object)this instanceof com.brandon3055.draconicevolution.client.render.effect.CrystalFXRing) {
            var body=dev.ryanhcode.sable.Sable.HELPER.getContainingClient(draconicSable$anchor);
            if(body!=null) {
                var center=body.renderPose().transformPosition(draconicSable$anchor).subtract(camera.getPosition());
                var delta=body.renderPose().transformNormal(new Vec3(x-center.x,y-center.y,z-center.z));
                x=(float)(center.x+delta.x);y=(float)(center.y+delta.y);z=(float)(center.z+delta.z);
            }
        }
        return original.call(camera,x,y,z,scale);
    }
}
