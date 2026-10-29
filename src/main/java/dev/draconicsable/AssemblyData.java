package dev.draconicsable;
import com.brandon3055.brandonscore.utils.Utils;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Rotation;

/** Rotates DE's hidden BE directions and relative links alongside Sable's block-state rotation. */
public final class AssemblyData {
    public static void rotate(CompoundTag tag,Rotation rotation) {
        if(rotation==Rotation.NONE || !tag.getString("id").startsWith("draconicevolution:")) return;
        var data=tag.getCompound("bc_managed_data");
        if(tag.getString("id").equals("draconicevolution:dislocator_pedestal") && data.contains("rotation",Tag.TAG_INT)) {
            int steps=switch(rotation){case CLOCKWISE_90->4;case CLOCKWISE_180->8;case COUNTERCLOCKWISE_90->12;default->0;};
            data.putInt("rotation",Math.floorMod(data.getInt("rotation")+steps,16));
        }
        for(String name:new String[]{"facing","direction","core_direction"}) direction(data,name,rotation);
        for(String name:new String[]{"stabilizer_axis","multi_block_axis"}) {
            if(!data.contains(name,Tag.TAG_COMPOUND)) continue;
            var axis=data.getCompound(name);
            if(!axis.contains("null") && axis.contains("value") && (rotation==Rotation.CLOCKWISE_90 || rotation==Rotation.COUNTERCLOCKWISE_90)) {
                int old=axis.getByte("value"); if(old==0 || old==2) axis.putByte("value",(byte)(2-old));
            }
        }
        for(String name:new String[]{"core_offset","controller_offset","stabilizer_pos_0","stabilizer_pos_1","stabilizer_pos_2","stabilizer_pos_3"}) offset(data,name,rotation);
        Tag[] components=new Tag[6];
        for(Direction dir:Direction.values()) {
            String name="component_position"+dir.get3DDataValue();
            if(data.contains(name)) { offset(data,name,rotation); components[rotation.rotate(dir).get3DDataValue()]=data.get(name).copy(); }
        }
        for(int i=0;i<6;i++) if(components[i]!=null) data.put("component_position"+i,components[i]);
        var links=tag.getList("linked_crystals",Tag.TAG_BYTE_ARRAY);
        for(int i=0;i<links.size();i++) links.set(i,new ByteArrayTag(rotateBytes(((ByteArrayTag)links.get(i)).getAsByteArray(),rotation)));
        for(Tag t:tag.getList("linked_receivers",Tag.TAG_COMPOUND)) {
            var receiver=(CompoundTag)t;
            receiver.putByteArray("offset",rotateBytes(receiver.getByteArray("offset"),rotation));
            int side=receiver.getByte("side");
            if(side>=0 && side<6) receiver.putByte("side",(byte)rotation.rotate(Direction.from3DDataValue(side)).get3DDataValue());
        }
    }
    private static void direction(CompoundTag data,String name,Rotation rotation) {
        if(!data.contains(name,Tag.TAG_COMPOUND)) return;
        var value=data.getCompound(name); int side=value.getByte("value");
        if(value.contains("value") && !value.contains("null") && side>=0 && side<6)
            value.putByte("value",(byte)rotation.rotate(Direction.from3DDataValue(side)).get3DDataValue());
    }
    private static byte[] rotateBytes(byte[] value,Rotation rotation) {
        if(value.length!=3) return value;
        var p=new BlockPos(value[0],value[1],value[2]).rotate(rotation);
        return new byte[]{(byte)p.getX(),(byte)p.getY(),(byte)p.getZ()};
    }
    private static void offset(CompoundTag data,String name,Rotation rotation) {
        if(data.contains(name,Tag.TAG_LIST)) {
            var list=data.getList(name,Tag.TAG_INT); if(list.size()!=3) return;
            var p=new BlockPos(list.getInt(0),list.getInt(1),list.getInt(2)).rotate(rotation);
            ListTag out=new ListTag(); out.add(IntTag.valueOf(p.getX())); out.add(IntTag.valueOf(p.getY())); out.add(IntTag.valueOf(p.getZ())); data.put(name,out);
        } else if(data.contains(name,Tag.TAG_COMPOUND)) {
            var old=data.getCompound(name); if(old.contains("null")) return;
            data.put(name,Utils.writeBlockPos(Utils.readBlockPos(old).rotate(rotation)));
        }
    }
}
