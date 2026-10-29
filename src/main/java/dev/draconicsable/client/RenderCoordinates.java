package dev.draconicsable.client;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
public final class RenderCoordinates {
    public static Vec3 project(Vec3 point) {
        var body=Sable.HELPER.getContainingClient(point);
        return body==null ? point : body.renderPose().transformPosition(point);
    }
}
