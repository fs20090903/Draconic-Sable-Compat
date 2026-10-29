/*
 * SPDX-License-Identifier: MIT AND LicenseRef-Draconic-Evolution
 * DE-derived portions: Copyright (c) 2013-2023 brandon3055.
 * Original contributions: Copyright (c) 2026 Draconic Sable Compatibility contributors.
 * Adapted from Draconic Evolution's TileReactorCore / TileReactorComponent (component discovery rules).
 * Modified for Sable compatibility and/or bounded calculation; see LICENSING.md
 * for the specific adaptations and LICENSES/ for the applicable license texts.
 */
package dev.draconicsable;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.*;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** Virtual mechanical edges across DE's intentional air gaps, within one coordinate space. */
public final class MultiblockLinks {
    public static List<BlockPos> connected(Level level, BlockPos pos) {
        var chunk=LoadedAccess.chunk(level,pos);
        if(chunk==null || !chunk.getBlockState(pos).hasBlockEntity())return List.of();
        BlockEntity tile = LoadedAccess.tile(level, pos);
        if(!(tile instanceof TileReactorCore || tile instanceof TileReactorComponent || tile instanceof TileEnergyCore || tile instanceof TileEnergyCoreStabilizer))return List.of();
        List<BlockPos> result = new ArrayList<>(6);
        if (tile instanceof TileReactorCore) {
            for (Direction dir : Direction.values()) {
                for (int i = 4; i < 8; i++) {
                    BlockPos other = pos.relative(dir, i);
                    var otherChunk = LoadedAccess.chunk(level, other);
                    if (otherChunk == null) continue;
                    if (otherChunk.getBlockState(other).isAir()) continue;
                    if (LoadedAccess.tile(level, other) instanceof TileReactorComponent c && c.facing.get() == dir.getOpposite()) result.add(other);
                    break;
                }
            }
        } else if (tile instanceof TileReactorComponent component) {
            for (int i = 4; i < 8; i++) {
                BlockPos other = pos.relative(component.facing.get(), i);
                if (LoadedAccess.tile(level, other) instanceof TileReactorCore && connected(level, other).contains(pos)) {
                    result.add(other); break;
                }
            }
        } else if (tile instanceof TileEnergyCore core) {
            for (var offset : core.stabilizerPositions) {
                if (offset.get() == null) continue;
                BlockPos other = pos.subtract(offset.get());
                if (LoadedAccess.tile(level, other) instanceof TileEnergyCoreStabilizer stab && stab.getCore() == core) result.add(other);
            }
        } else if (tile instanceof TileEnergyCoreStabilizer stab) {
            TileEnergyCore core = stab.getCore();
            if (core != null) result.add(core.getBlockPos());
        }
        var owner = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, pos);
        result.removeIf(p -> dev.ryanhcode.sable.Sable.HELPER.getContaining(level, p) != owner);
        return result;
    }

    public static boolean reactorReady(TileReactorCore core) {
        if (core.getLevel() == null) return false;
        for (var offset : core.componentPositions) {
            BlockPos delta = offset.get().getPos();
            if (!delta.equals(BlockPos.ZERO) && !LoadedAccess.ready(core.getLevel(), core.getBlockPos().subtract(delta))) return false;
        }
        return true;
    }
}
