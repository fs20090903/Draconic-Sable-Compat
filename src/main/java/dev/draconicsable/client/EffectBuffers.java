package dev.draconicsable.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;

/** Render-thread-owned scratch buffer for DE's mutable shader uniforms.
 * Iris's shared endBatch(type) is a no-op; endBatch() drains the entire scene.
 * Keep these immediate effects separate from that scene-wide batching machinery.
 */
public final class EffectBuffers {
    private static MultiBufferSource.BufferSource immediate;
    public static MultiBufferSource select(MultiBufferSource original) {
        // Preserve non-drawing consumers such as geometry inspection tools.
        if(!(original instanceof MultiBufferSource.BufferSource))return original;
        if(immediate==null)immediate=MultiBufferSource.immediate(new ByteBufferBuilder(65536));
        return immediate;
    }
    public static void finish(MultiBufferSource selected,MultiBufferSource original) {
        if(selected!=original)((MultiBufferSource.BufferSource)selected).endBatch();
    }
}
