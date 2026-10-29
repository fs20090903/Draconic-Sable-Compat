package dev.draconicsable.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.draconicsable.MultiblockLinks;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.*;

@Mixin(SubLevelAssemblyHelper.class)
public abstract class AssemblyGatherMixin {
    @WrapOperation(method="gatherConnectedBlocks",at=@At(value="INVOKE",target="Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private static boolean draconicSable$gatherAcrossAir(Set<BlockPos> blocks, Object value, Operation<Boolean> original,
            @Local(argsOnly=true) ServerLevel level, @Local(argsOnly=true) SubLevelAssemblyHelper.FrontierPredicate predicate,
            @Local(index=4) LinkedHashSet<Pair<BlockPos,BlockState>> frontier) {
        boolean added=original.call(blocks,value);
        BlockPos pos=(BlockPos)value;
        for(BlockPos other:MultiblockLinks.connected(level,pos)) {
            var state=level.getBlockState(other);
            if(!blocks.contains(other) && (predicate==null || predicate.isValidConnection(pos,level.getBlockState(pos),other,state,null)))
                frontier.add(Pair.of(other,state));
        }
        return added;
    }
}
