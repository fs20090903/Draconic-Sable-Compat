package dev.draconicsable;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Never generates a world chunk or a new sparse plot chunk merely to look up a link. */
public final class LoadedAccess {
    public static LevelChunk chunk(Level level, BlockPos pos) {
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
        var chunkPos = new net.minecraft.world.level.ChunkPos(pos);
        if (container != null && container.inBounds(chunkPos)) return container.getChunk(chunkPos);
        var c = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        return c instanceof LevelChunk lc ? lc : null;
    }
    public static BlockEntity tile(Level level, BlockPos pos) {
        var chunk = chunk(level, pos);
        if (chunk == null) return null;
        var tile = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        return tile == null || tile.isRemoved() ? null : tile;
    }
    public static boolean onboard(Level level, BlockPos pos) {
        return level != null && Sable.HELPER.getContaining(level, pos) != null;
    }
    public static boolean ready(Level level, BlockPos pos) {
        var chunk = chunk(level, pos);
        if (chunk == null) return false;
        // During assembly/loading the state can arrive before its block entity.
        if(!chunk.getBlockState(pos).hasBlockEntity())return true;
        var tile=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK);
        return tile!=null && !tile.isRemoved();
    }
}
