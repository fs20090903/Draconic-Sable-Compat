package dev.draconicsable.mixin.client;
import com.brandon3055.draconicevolution.client.render.effect.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.draconicsable.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({CrystalFXIO.class,CrystalFXRing.class})
public abstract class CrystalStaticFxMixin extends Particle {
    protected CrystalStaticFxMixin(ClientLevel l,double x,double y,double z){super(l,x,y,z);}
    @WrapMethod(method="render")
    private void draconicSable$followRenderPose(VertexConsumer b,Camera c,float p,Operation<Void> original) {
        Vec3 v=RenderCoordinates.project(((CrystalAnchor)this).draconicSable$source());
        double px=x,py=y,pz=z;x=v.x;y=v.y;z=v.z;
        try{original.call(b,c,p);}finally{x=px;y=py;z=pz;}
    }
    @Override public AABB getRenderBoundingBox(float partial) {
        Vec3 v=RenderCoordinates.project(((CrystalAnchor)this).draconicSable$source());return new AABB(v,v).inflate(1);
    }
}
