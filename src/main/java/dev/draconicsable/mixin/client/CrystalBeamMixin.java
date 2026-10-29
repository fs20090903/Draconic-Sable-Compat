package dev.draconicsable.mixin.client;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.client.render.effect.CrystalFXBeam;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.draconicsable.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;

@Mixin(CrystalFXBeam.class)
public abstract class CrystalBeamMixin extends Particle {
    @Shadow @Final private Vec3D linkTarget;
    protected CrystalBeamMixin(ClientLevel l,double x,double y,double z){super(l,x,y,z);}
    @WrapMethod(method="render")
    private void draconicSable$renderBothEnds(VertexConsumer buffer,Camera camera,float partial,Operation<Void> original) {
        Vec3 raw=((CrystalAnchor)this).draconicSable$source();
        Vec3 source=RenderCoordinates.project(raw);
        Vec3 target=RenderCoordinates.project(new Vec3(linkTarget.x,linkTarget.y,linkTarget.z));
        double px=x,py=y,pz=z,tx=linkTarget.x,ty=linkTarget.y,tz=linkTarget.z;
        x=source.x; y=source.y; z=source.z;
        linkTarget.x=target.x; linkTarget.y=target.y; linkTarget.z=target.z;
        try { original.call(buffer,camera,partial); }
        finally {x=px;y=py;z=pz;linkTarget.x=tx;linkTarget.y=ty;linkTarget.z=tz;}
    }
    @Override public AABB getRenderBoundingBox(float partial) {
        Vec3 source=RenderCoordinates.project(((CrystalAnchor)this).draconicSable$source());
        Vec3 target=RenderCoordinates.project(new Vec3(linkTarget.x,linkTarget.y,linkTarget.z));
        return new AABB(source,target).inflate(0.5);
    }
}
