package dev.simplix.protocolize.data.item.component;

import dev.simplix.protocolize.api.item.component.DataComponentType;
import dev.simplix.protocolize.api.item.component.WaxedComponent;
import io.netty.buffer.ByteBuf;

public class WaxedComponentImpl implements WaxedComponent {

    @Override
    public void read(ByteBuf byteBuf, int protocolVersion) {
    }

    @Override
    public void write(ByteBuf byteBuf, int protocolVersion) {
    }

    @Override
    public DataComponentType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements DataComponentType<WaxedComponent>, Factory {

        public static Type INSTANCE = new Type();

        @Override
        public WaxedComponent create() {
            return new WaxedComponentImpl();
        }

        @Override
        public String getName() {
            return "minecraft:waxed";
        }

        @Override
        public WaxedComponent createEmpty() {
            return create();
        }

    }

}
