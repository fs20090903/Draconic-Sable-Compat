package dev.draconicsable;

import codechicken.lib.util.RotationUtils;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Placement {
    public static Direction placedRotation(BlockPos pos, LivingEntity placer) {
        if (placer == null) return Direction.DOWN;
        var body = Sable.HELPER.getContaining(placer.level(), pos);
        if (body == null) return RotationUtils.getPlacedRotation(pos, placer);
        // Match CCL's 1.82-block placement reference rather than the player's
        // actual eye height, which otherwise shifts the vertical threshold.
        Vec3 eye = body.logicalPose().transformPositionInverse(placer.position()).add(0,1.82,0);
        Vec3 look = body.logicalPose().transformNormalInverse(placer.getLookAngle());
        return localRotation(pos, eye, look);
    }
    public static float yaw(BlockPos pos, LivingEntity placer) {
        var body=Sable.HELPER.getContaining(placer.level(),pos);
        if(body==null)return placer.getYRot();
        var look=body.logicalPose().transformNormalInverse(placer.getLookAngle());
        return (float)Math.toDegrees(Math.atan2(-look.x,look.z));
    }
    public static Direction localRotation(BlockPos pos, Vec3 eye, Vec3 look) {
        if (Math.abs(eye.x - pos.getX()) < 2 && Math.abs(eye.z - pos.getZ()) < 2) {
            if (eye.y - pos.getY() > 2) return Direction.DOWN;
            if (eye.y - pos.getY() < 0) return Direction.UP;
        }
        return Math.abs(look.x) > Math.abs(look.z)
            ? (look.x > 0 ? Direction.EAST : Direction.WEST)
            : (look.z > 0 ? Direction.SOUTH : Direction.NORTH);
    }
}
