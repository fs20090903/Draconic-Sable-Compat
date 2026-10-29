package dev.draconicsable.tests;

import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.brandon3055.draconicevolution.blocks.tileentity.*;
import net.minecraft.world.item.*;

/** Registry-complete render invocation audit. This supplements, rather than replaces, scene screenshots. */
public final class VisualAudit {
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void audit(Minecraft mc,UUID bodyId) {
        var body=SubLevelContainer.getContainer(mc.level).getSubLevel(bodyId);
        var local=body.getPlot().getCenterBlock().above(20);
        var ground=BlockPos.containing(body.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atLowerCornerOf(local)));
        StringBuilder out=new StringBuilder("block,render_shape,renderer,terrain_vertices,sublevel_vertices,notes\n");
        int index=0;
        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix().identity();RenderSystem.applyModelViewMatrix();
        int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight();
        RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,width,height,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
        try {for(var entry:DEContent.BLOCKS.getEntries()) {
            if(index%6==0){mc.getMainRenderTarget().bindWrite(true);RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.clearColor(.12f,.14f,.17f,1);RenderSystem.clear(16640,Minecraft.ON_OSX);}
            var b=entry.get();var state=b.defaultBlockState();String rendererName="baked model";int[] counts={0,0};String notes="placed model and populated display slots; active multiblocks tested separately";
                for(int i=0;i<2;i++) {
                    var pos=i==0?ground:local;var old=mc.level.getBlockState(pos);
                    mc.level.setBlock(pos,state,16);
                    var tile=b instanceof EntityBlock eb?eb.newBlockEntity(pos,state):null;
                    if(tile!=null){tile.setLevel(mc.level);configure(tile);}
                    int cell=(index%6)*2+i;int x=cell%4*(width/4),y=cell/4*(height/3);
                    var pose=new PoseStack();pose.translate(x+width/8.0,y+height/5.0,0);pose.scale(50,-50,50);
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-35));pose.translate(-.5,-.5,-.5);
                    var buffers=mc.renderBuffers().bufferSource();
                    mc.getBlockRenderer().renderSingleBlock(state,pose,buffers,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    if(tile!=null) {
                    BlockEntityRenderer renderer=mc.getBlockEntityRenderDispatcher().getRenderer(tile);
                    if(renderer==null){rendererName="none";} else {
                    rendererName=renderer.getClass().getSimpleName();
                    var consumer=new ClientSmoke.CountingVertices();
                    try {renderer.render(tile,.5f,new PoseStack(),type->consumer,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);counts[i]=consumer.count;if(consumer.maxAbsolute>64)throw new IllegalStateException("Renderer emitted distant vertices: "+consumer.maxAbsolute);renderer.render(tile,.5f,pose,buffers,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}
                    catch(Throwable e){notes="ERROR "+e.getClass().getSimpleName()+": "+e.getMessage();}
                    }}
                    buffers.endBatch();
                    mc.font.drawInBatch(entry.getId().getPath(),x+3,y+3,0xFFFFFF,false,new org.joml.Matrix4f(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,0xF000F0);
                    mc.font.drawInBatch(i==0?"Terrain":"Sublevel",x+3,y+14,0xAAAAAA,false,new org.joml.Matrix4f(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,0xF000F0);
                    buffers.endBatch();mc.level.setBlock(pos,old,16);
                }
            out.append(entry.getId()).append(',').append(state.getRenderShape()).append(',').append(rendererName).append(',').append(counts[0]).append(',').append(counts[1]).append(',').append('"').append(notes.replace("\"","'")).append('"').append('\n');
            index++;
            if(index%6==0 || index==DEContent.BLOCKS.getEntries().size())try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("catalog-"+((index-1)/6)+".png"));}catch(Exception e){throw new RuntimeException(e);}
        }} finally {RenderSystem.getModelViewStack().popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.restoreProjectionMatrix();}
        try{Files.writeString(Path.of("all-block-render-audit.csv"),out);}catch(Exception e){throw new RuntimeException(e);}
        if(out.indexOf("ERROR")>=0)throw new IllegalStateException("Block renderer audit failed; see all-block-render-audit.csv");
    }
    private static void configure(net.minecraft.world.level.block.entity.BlockEntity tile) {
        if(tile instanceof TileFusionCraftingInjector t)t.itemHandler.setStackInSlot(0,new ItemStack(Items.DIAMOND));
        if(tile instanceof TileFusionCraftingCore t)t.itemHandler.setStackInSlot(0,new ItemStack(Items.DIAMOND));
        if(tile instanceof TileDislocatorPedestal t){var stack=new ItemStack(DEContent.DISLOCATOR.get());stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Test destination"));t.itemHandler.setStackInSlot(0,stack);}
        if(tile instanceof TileGrinder t){var target=new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE,t.getLevel());var p=dev.draconicsable.client.RenderCoordinates.project(net.minecraft.world.phys.Vec3.atCenterOf(t.getBlockPos()).add(1,0,1));target.setPos(p);target.xo=p.x;target.yo=p.y;target.zo=p.z;t.targetA=target;t.animA=.25f;}
        if(tile instanceof TilePlacedItem t)t.itemHandler.setStackInSlot(0,new ItemStack(Items.DIAMOND));
        if(tile instanceof TileDisenchanter t){t.itemHandler.setStackInSlot(0,new ItemStack(Items.DIAMOND_SWORD));t.itemHandler.setStackInSlot(1,new ItemStack(Items.BOOK));}
        if(tile instanceof TileEnergyPylon t)t.structureValid.set(true);
        if(tile instanceof TileEnergyCore t)t.active.set(true);
    }
}

