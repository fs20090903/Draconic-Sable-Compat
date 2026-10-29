package dev.draconicsable.mixin.client;
import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.client.render.effect.CrystalFXWireless;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.draconicsable.client.*;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrystalFXWireless.class)
public abstract class CrystalWirelessMixin extends Particle {
    @Shadow @Final private BlockPos linkTarget;
    protected CrystalWirelessMixin(ClientLevel l,double x,double y,double z){super(l,x,y,z);}
    @WrapMethod(method="render")
    private void draconicSable$source(VertexConsumer b,Camera c,float p,Operation<Void> original) {
        Vec3 v=RenderCoordinates.project(((CrystalAnchor)this).draconicSable$source());
        double px=x,py=y,pz=z;x=v.x;y=v.y;z=v.z;
        try{original.call(b,c,p);}finally{x=px;y=py;z=pz;}
    }
    @ModifyExpressionValue(method="render",at=@At(value="INVOKE",target="Lcodechicken/lib/vec/Vector3;fromBlockPos(Lnet/minecraft/core/BlockPos;)Lcodechicken/lib/vec/Vector3;"))
    private Vector3 draconicSable$target(Vector3 raw) {
        Vec3 v=RenderCoordinates.project(new Vec3(raw.x,raw.y,raw.z)); return new Vector3(v.x,v.y,v.z);
    }
    @ModifyExpressionValue(method="render",at=@At(value="FIELD",target="Lcom/brandon3055/draconicevolution/client/render/effect/CrystalFXWireless$PTracker;tOffset:Lcodechicken/lib/vec/Vector3;"))
    private Vector3 draconicSable$targetOffset(Vector3 raw) {
        var body=Sable.HELPER.getContainingClient(linkTarget);
        if(body==null)return raw;
        Vec3 v=body.renderPose().transformNormal(new Vec3(raw.x,raw.y,raw.z));return new Vector3(v.x,v.y,v.z);
    }
    @Override public AABB getRenderBoundingBox(float partial) {
        return new AABB(RenderCoordinates.project(((CrystalAnchor)this).draconicSable$source()),RenderCoordinates.project(Vec3.atCenterOf(linkTarget))).inflate(1);
    }
}
