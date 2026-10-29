package dev.draconicsable.tests;

import com.brandon3055.draconicevolution.blocks.reactor.ProcessExplosion;
import dev.draconicsable.explosion.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Separate test run: server stops immediately with outstanding terrain requests. */
@GameTestHolder("draconic_shutdown_tests") @PrefixGameTestTemplate(false)
public class PendingShutdownTest {
    @GameTest(template="empty",timeoutTicks=200)
    public void outstandingChunkPreparationDoesNotPreventServerShutdown(GameTestHelper h) {
        var p=new ProcessExplosion(new BlockPos(9000,100,9000),350,h.getLevel(),-1);
        var plan=(BlastPlan)p;
        h.startSequence().thenWaitUntil(()->{
            p.updateCalculation();
            h.assertTrue(plan.draconicSable$terrainTickets()>0,"Terrain loading requests issued");
        }).thenExecute(()->{
            h.assertTrue(!p.isCalculationComplete(),"Shutdown occurs before preparation is complete");
            com.mojang.logging.LogUtils.getLogger().info("[DE shutdown smoke] stopping with {} terrain tickets",plan.draconicSable$terrainTickets());
            var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,5,5));
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            var body=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),
                dev.ryanhcode.sable.companion.math.BoundingBox3i.from(java.util.List.of(pos)));
            new BlastHold(java.util.List.of(body));
            h.assertTrue(BlastHold.anchored(body),"Live anchor remains outstanding on shutdown");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.event.server.ServerStoppingEvent event)->{
                    if(event.getServer()!=level.getServer())return;
                    if(BlastHold.held(body))throw new AssertionError("Blast hold survived server-stopping cleanup");
                    var tickets=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).collectForceLoadTickets().get(body);
                    if(tickets!=null && tickets.stream().anyMatch(t->t.type()==WorldBlast.TICKET))throw new AssertionError("Temporary hold ticket survived shutdown");
                    com.mojang.logging.LogUtils.getLogger().info("[DE shutdown smoke] active hold and ticket released before save");
                });
        }).thenSucceed();
    }
}
