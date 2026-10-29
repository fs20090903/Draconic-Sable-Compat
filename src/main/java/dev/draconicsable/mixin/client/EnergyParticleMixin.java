package dev.draconicsable.mixin.client;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;

@Mixin(ParticleEnergy.class)
public abstract class EnergyParticleMixin {
    @Shadow public Vec3D targetPos;
    @WrapMethod(method="tick")
    private void draconicSable$worldTarget(Operation<Void> original) {
        var raw=targetPos; var body=Sable.HELPER.getContainingClient(new Vec3(raw.x,raw.y,raw.z));
        if(body==null){original.call();return;}
        var p=body.logicalPose().transformPosition(new Vec3(raw.x,raw.y,raw.z));targetPos=new Vec3D(p.x,p.y,p.z);
        try{original.call();}finally{targetPos=raw;}
    }
}
