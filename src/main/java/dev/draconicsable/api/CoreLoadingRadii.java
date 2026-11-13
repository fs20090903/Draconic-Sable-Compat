package dev.draconicsable.api;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;

/** Startup-only extension contract; see LOADING_API.md. No world references retained. */
public final class CoreLoadingRadii {
    public static final String IMC_METHOD = "minimum_core_loading_radius_v1";
    public static final int MAX_RADIUS = 32;
    private static volatile Map<BlockEntityType<?>, Integer> minimums = Map.of();
    private CoreLoadingRadii() {}

    public static void process(InterModProcessEvent event) {
        var registered = new HashMap<BlockEntityType<?>, Integer>();
        InterModComms.getMessages("draconic_sable_compat", IMC_METHOD::equals).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (!(payload instanceof Map<?, ?> entries)) {
                LogUtils.getLogger().warn("Ignoring invalid core loading radius payload from {}", message.senderModId());
                return;
            }
            for (var entry : entries.entrySet()) {
                if (!(entry.getKey() instanceof ResourceLocation id)
                    || !(entry.getValue() instanceof Integer radius) || radius < 0 || radius > MAX_RADIUS
                    || !BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id)) {
                    LogUtils.getLogger().warn("Ignoring invalid core loading radius entry from {}: {}", message.senderModId(), entry);
                    continue;
                }
                registered.merge(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id), radius, Math::max);
            }
        });
        minimums = Map.copyOf(registered);
    }

    /** Called only for active supported cores on the logical server. */
    public static int radius(BlockEntity core) {
        int base = core instanceof TileReactorCore ? 1 : 2;
        return Math.max(base, minimums.getOrDefault(core.getType(), base));
    }
}
