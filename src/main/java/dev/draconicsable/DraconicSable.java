package dev.draconicsable;

import net.neoforged.fml.common.Mod;

@Mod("draconic_sable_compat")
public final class DraconicSable {
    public DraconicSable(net.neoforged.bus.api.IEventBus bus) {
        // Register Sable's persistent codec before a world can load its saved ticket table.
        CoreLoading.SUBLEVEL.name();
        dev.draconicsable.explosion.WorldBlast.TICKET.name();
        bus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent e)->e.register(CoreLoading.WORLD));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CoreLoading::maintenance);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(dev.draconicsable.explosion.BlastTrace::stopping);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(dev.draconicsable.explosion.WorldBlast::stopping);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(dev.draconicsable.explosion.BlastHold::tick);
    }
}
