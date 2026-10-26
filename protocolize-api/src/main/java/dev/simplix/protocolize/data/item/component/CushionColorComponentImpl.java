package dev.simplix.protocolize.data.item.component;

import dev.simplix.protocolize.api.item.component.CushionColorComponent;
import dev.simplix.protocolize.api.item.component.DataComponentType;
import dev.simplix.protocolize.api.item.enums.DyeColor;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

@Data
@AllArgsConstructor
public class CushionColorComponentImpl implements CushionColorComponent {

    private DyeColor color;

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) throws IOException {
        color = DyeColor.values()[ProtocolUtil.readVarInt(byteBuf)];
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) throws IOException {
        ProtocolUtil.writeVarInt(byteBuf, color.ordinal());
    }

    @Override
    public DataComponentType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements DataComponentType<CushionColorComponent>, Factory {

        public static Type INSTANCE = new Type();

        @Override
        public String getName() {
            return "minecraft:cushion_color";
        }

        @Override
        public CushionColorComponent createEmpty() {
            return new CushionColorComponentImpl(null);
        }

        @Override
        public CushionColorComponent create(DyeColor color) {
            return new CushionColorComponentImpl(color);
        }
    }

}
