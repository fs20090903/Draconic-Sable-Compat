package dev.draconicsable.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.draconicsable.MultiblockLinks;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(SubLevelHeatMapManager.class)
public abstract class HeatmapMixin {
    @Shadow @Final private ServerSubLevel subLevel;
    // Retain the last edges of removed endpoints so clearing can propagate after block removal.
    // Owned by the heatmap, so it is discarded together with the sublevel on unload.
    @Unique private final Map<BlockPos, List<BlockPos>> draconicSable$edges = new HashMap<>();
    // Delay splitting only while budgeted blast removals still address this plot.
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void draconicSable$finishBlastFirst(CallbackInfo ci){
        if(dev.draconicsable.explosion.WorldBlast.removing(subLevel))ci.cancel();
    }
    @Unique private BlockPos[] draconicSable$offsets(BlockPos[] original, BlockPos pos) {
        var level = subLevel.getLevel();
        var edges = MultiblockLinks.connected(level, pos);
        if (!edges.isEmpty()) draconicSable$edges.put(pos.immutable(), edges);
        else if (draconicSable$edges.containsKey(pos) && level.getBlockState(pos).isAir()) edges = draconicSable$edges.get(pos);
        if (edges.isEmpty()) return original;
        BlockPos[] all = Arrays.copyOf(original, original.length + edges.size());
        for (int i=0; i<edges.size(); i++) all[original.length+i] = edges.get(i).subtract(pos);
        return all;
    }
    @ModifyExpressionValue(method="step", at=@At(value="FIELD", target="Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;DIRECTION_OFFSETS:[Lnet/minecraft/core/BlockPos;", ordinal=0))
    private BlockPos[] draconicSable$fill(BlockPos[] original, @Local(index=1) BlockPos pos) { return draconicSable$offsets(original,pos); }
    @ModifyExpressionValue(method="step", at=@At(value="FIELD", target="Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;DIRECTION_OFFSETS:[Lnet/minecraft/core/BlockPos;", ordinal=1))
    private BlockPos[] draconicSable$clear(BlockPos[] original, @Local(index=1) BlockPos pos) { return draconicSable$offsets(original,pos); }
    @ModifyExpressionValue(method="step", at=@At(value="FIELD", target="Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;DIRECTION_OFFSETS:[Lnet/minecraft/core/BlockPos;", ordinal=2))
    private BlockPos[] draconicSable$removed(BlockPos[] original, @Local(index=3) BlockPos pos) { return draconicSable$offsets(original,pos); }
    @ModifyExpressionValue(method="step", at=@At(value="FIELD", target="Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;DIRECTION_OFFSETS:[Lnet/minecraft/core/BlockPos;", ordinal=3))
    private BlockPos[] draconicSable$alternate(BlockPos[] original, @Local(index=9) BlockPos pos) { return draconicSable$offsets(original,pos); }
    @ModifyExpressionValue(method="onSolidAdded", at=@At(value="FIELD", target="Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;DIRECTION_OFFSETS:[Lnet/minecraft/core/BlockPos;"))
    private BlockPos[] draconicSable$added(BlockPos[] original, @Local(argsOnly=true) BlockPos pos) { return draconicSable$offsets(original,pos); }
}
