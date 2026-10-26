package dev.simplix.protocolize.data.item.component;

import dev.simplix.protocolize.api.item.component.DataComponentType;
import dev.simplix.protocolize.api.item.component.VillagerFoodComponent;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

@Data
@AllArgsConstructor
public class VillagerFoodComponentImpl implements VillagerFoodComponent {

    private int nutrition;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) throws IOException {
        nutrition = ProtocolUtil.readVarInt(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) throws IOException {
        ProtocolUtil.writeVarInt(byteBuf, nutrition);
    }

    @Override
    public DataComponentType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements DataComponentType<VillagerFoodComponent>, Factory {

        public static Type INSTANCE = new Type();

        @Override
        public String getName() {
            return "minecraft:villager_food";
        }

        @Override
        public VillagerFoodComponent createEmpty() {
            return new VillagerFoodComponentImpl(0);
        }

        @Override
        public VillagerFoodComponent create(int nutrition) {
            return new VillagerFoodComponentImpl(nutrition);
        }
    }

}
