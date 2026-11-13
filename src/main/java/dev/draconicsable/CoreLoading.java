package dev.draconicsable;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** Persistent owned tickets, not command tickets and not a global Sable config change. */
public final class CoreLoading {
    public static final SubLevelLoadingTicketType<BlockPos> SUBLEVEL = SubLevelLoadingTicketType.create(
        ResourceLocation.fromNamespaceAndPath("draconic_sable_compat","active_core"),BlockPos.CODEC);
    private static final Map<ServerLevel,Map<BlockPos,Set<Long>>> WORLD_OWNERS=new WeakHashMap<>();
    private record Binding(BlockPos pos,UUID body,boolean active,int radius) {}
    private static final Map<BlockEntity,Binding> BINDINGS=new WeakHashMap<>();
    public static final TicketController WORLD=new TicketController(
        ResourceLocation.fromNamespaceAndPath("draconic_sable_compat","active_core"), (level,helper)-> {
            var owners=WORLD_OWNERS.computeIfAbsent(level,k->new HashMap<>());
            for(var entry:helper.getBlockTickets().entrySet()) {
                // Validation callback is specifically allowed to load the saved owner chunk.
                if(!active(level.getBlockEntity(entry.getKey()))) { helper.removeAllTickets(entry.getKey()); continue; }
                owners.put(entry.getKey(),new HashSet<>(entry.getValue().ticking()));
            }
        });
    public static boolean active(BlockEntity tile) {
        if(tile==null || tile.isRemoved()) return false;
        return tile instanceof TileReactorCore r ? r.reactorState.get().isShieldActive()
             : tile instanceof TileEnergyCore e && e.active.get();
    }
    public static void update(BlockEntity tile) {
        if(!(tile.getLevel() instanceof ServerLevel level)) return;
        boolean active=active(tile);
        var body=Sable.HELPER.getContaining(tile);
        var pos=tile.getBlockPos();
        var id=body==null?null:body.getUniqueId();
        int radius=active && body==null ? dev.draconicsable.api.CoreLoadingRadii.radius(tile) : 0;
        var oldBinding=BINDINGS.get(tile);
        if(oldBinding!=null && oldBinding.active()==active && oldBinding.radius()==radius && oldBinding.pos().equals(pos) && Objects.equals(oldBinding.body(),id))return;
        BINDINGS.put(tile,new Binding(pos.immutable(),id,active,radius));
        if(body instanceof ServerSubLevel serverBody) {
            var c=SubLevelContainer.getContainer(level);
            if(active) c.addForceLoadTicket(serverBody,SUBLEVEL,pos);
            else c.removeForceLoadTicket(serverBody,SUBLEVEL,pos);
            return;
        }
        var owners=WORLD_OWNERS.computeIfAbsent(level,k->new HashMap<>());
        Set<Long> desired=new HashSet<>();
        if(active) {
            // Reactor: full surrounding 3x3, including short injector/flux-gate feeds.
            // Energy core: 5x5 covers its shell, stabilizers and ordinary nearby pylons.
            // Addons can request a larger minimum through the startup IMC API.
            int cx=pos.getX()>>4,cz=pos.getZ()>>4;
            for(int x=cx-radius;x<=cx+radius;x++) for(int z=cz-radius;z<=cz+radius;z++) desired.add(ChunkPos.asLong(x,z));
        }
        Set<Long> old=owners.getOrDefault(pos,Set.of());
        for(long chunk:desired) if(!old.contains(chunk)) WORLD.forceChunk(level,pos,ChunkPos.getX(chunk),ChunkPos.getZ(chunk),true,true);
        for(long chunk:old) if(!desired.contains(chunk)) WORLD.forceChunk(level,pos,ChunkPos.getX(chunk),ChunkPos.getZ(chunk),false,true);
        if(desired.isEmpty()) owners.remove(pos); else owners.put(pos,desired);
    }
    public static void maintenance(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level) || level.getGameTime()%20!=0) return;
        pruneOwners(level);
    }
    public static void pruneOwners(ServerLevel level) {
        var c=SubLevelContainer.getContainer(level);
        for(var entry:List.copyOf(c.collectForceLoadTickets().entrySet())) {
            for(var ticket:List.copyOf(entry.getValue())) {
                if(ticket.type()==dev.draconicsable.explosion.WorldBlast.TICKET && ticket.key() instanceof String key && dev.draconicsable.explosion.WorldBlast.orphaned(level,key))
                    c.removeForceLoadTicket(entry.getKey(),dev.draconicsable.explosion.WorldBlast.TICKET,key);
                if(ticket.type()!=SUBLEVEL || !(ticket.key() instanceof BlockPos pos)) continue;
                // A removed/moved core may leave no sparse chunk at its old address.
                // Active tickets are attached to loaded bodies, so no such chunk is
                // different from a present block still waiting for its block entity.
                boolean stale=Sable.HELPER.getContaining(level,pos)!=entry.getKey()
                    || LoadedAccess.chunk(level,pos)==null
                    || (LoadedAccess.ready(level,pos) && !active(LoadedAccess.tile(level,pos)));
                if(stale)c.removeForceLoadTicket(entry.getKey(),SUBLEVEL,pos);
            }
        }
        var owners=WORLD_OWNERS.get(level);
        if(owners==null) return;
        for(var pos:List.copyOf(owners.keySet())) {
            if(!LoadedAccess.ready(level,pos)) continue;
            var tile=LoadedAccess.tile(level,pos);
            if(active(tile)) continue;
            for(long chunk:owners.remove(pos)) WORLD.forceChunk(level,pos,ChunkPos.getX(chunk),ChunkPos.getZ(chunk),false,true);
        }
    }
}
