package dev.draconicsable.tests;

import com.mojang.blaze3d.vertex.*;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.client.render.tile.*;
import dev.draconicsable.LoadedAccess;
import dev.draconicsable.client.EffectBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import java.nio.file.*;
import java.util.LinkedHashMap;

/** Regression: immediate DE effects must never drain or append to the scene buffer. */
public final class BufferIsolationAudit {
    private static final class Spy extends MultiBufferSource.BufferSource {
        int reads,globalFlushes,typedFlushes;
        Spy(ByteBufferBuilder bytes){super(bytes,new LinkedHashMap<>());}
        @Override public VertexConsumer getBuffer(RenderType type){reads++;return new ClientSmoke.CountingVertices();}
        @Override public void endBatch(){globalFlushes++;}
        @Override public void endBatch(RenderType type){typedFlushes++;}
    }
    public static void run(Minecraft mc,BlockPos reactorPos,BlockPos energyPos) {
        try(var bytes=new ByteBufferBuilder(0)) {
            var spy=new Spy(bytes);
            var reactor=(TileReactorCore)LoadedAccess.tile(mc.level,reactorPos);
            var renderer=(RenderTileReactorCore)mc.getBlockEntityRenderDispatcher().getRenderer(reactor);
            renderer.renderTransparent(reactor,.5f,new PoseStack(),spy,15728880,0);
            var energy=(TileEnergyCore)LoadedAccess.tile(mc.level,energyPos);
            var energyRenderer=(RenderTileEnergyCore)mc.getBlockEntityRenderDispatcher().getRenderer(energy);
            energyRenderer.render(energy,.5f,new PoseStack(),spy,15728880,0);
            var state=codechicken.lib.render.CCRenderState.instance();state.reset();
            energyRenderer.renderFancyOuterCore(energy,state,new codechicken.lib.vec.Matrix4(),spy,.5f,0,1);
            int crystals=0;
            for(var entry:com.brandon3055.draconicevolution.init.DEContent.BLOCKS.getEntries()) {
                var block=entry.get();
                if(!(block instanceof net.minecraft.world.level.block.EntityBlock eb))continue;
                var tile=eb.newBlockEntity(energyPos,block.defaultBlockState());
                if(!(tile instanceof com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalBase crystal))continue;
                crystal.setLevel(mc.level);
                var crystalRenderer=(RenderTileEnergyCrystal)mc.getBlockEntityRenderDispatcher().getRenderer(crystal);
                // All tiers and I/O, relay, wireless forms; actual local-distance and far fallback paths.
                crystalRenderer.render(crystal,.5f,new PoseStack(),spy,15728880,0);
                var far=(com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalBase)eb.newBlockEntity(mc.player.blockPosition().offset(200,0,0),block.defaultBlockState());
                far.setLevel(mc.level);
                crystalRenderer.render(far,.5f,new PoseStack(),spy,15728880,0);
                crystals++;
            }
            if(crystals!=9)throw new IllegalStateException("Expected nine crystal variants, tested "+crystals);
            if(spy.reads!=0 || spy.globalFlushes!=0 || spy.typedFlushes!=0)
                throw new IllegalStateException("Shared scene buffer touched: "+spy.reads+"/"+spy.globalFlushes+"/"+spy.typedFlushes);
            var selected=EffectBuffers.select(spy);
            // ImmediatelyFast replaces the vanilla factory with its own independently owned buffer.
            if(selected.getClass()!=MultiBufferSource.BufferSource.class && !selected.getClass().getName().equals("net.raphimc.immediatelyfast.feature.core.BatchableBufferSource"))throw new IllegalStateException("Unexpected immediate buffer: "+selected.getClass());
            Files.writeString(Path.of("buffer-isolation-passed.txt"),"PASS: actual reactor, fancy Energy Core and all nine crystal variants (local and distant) issue zero shared-buffer writes/global flushes/typed flushes; private buffer="+selected.getClass().getName()+"\n");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
}
