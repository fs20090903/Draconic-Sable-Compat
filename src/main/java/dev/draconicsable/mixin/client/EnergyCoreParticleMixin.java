package dev.draconicsable.mixin.client;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergyCoreFX;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEnergyCoreFX.class)
public abstract class EnergyCoreParticleMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lcom/brandon3055/brandonscore/lib/Vec3D;getDirectionVec(Lcom/brandon3055/brandonscore/lib/Vec3D;Lcom/brandon3055/brandonscore/lib/Vec3D;)Lcom/brandon3055/brandonscore/lib/Vec3D;"))
    private Vec3D draconicSable$projectSpiral(Vec3D source,Vec3D target,Operation<Vec3D> original) {
        var body=Sable.HELPER.getContainingClient(new Vec3(target.x,target.y,target.z));
        if(body!=null){var v=body.logicalPose().transformPosition(new Vec3(target.x,target.y,target.z));target.x=v.x;target.y=v.y;target.z=v.z;}
        return original.call(source,target);
    }
}
