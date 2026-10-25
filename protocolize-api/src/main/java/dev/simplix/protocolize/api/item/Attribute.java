package dev.simplix.protocolize.api.item;

import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.data.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Attribute {

    private AttributeType type;
    private UUID uuid;
    private AttributeModifier modifier;
    private EquipmentSlotGroup slot;

    /**
     * How the modifier is displayed in the tooltip. Null means the default display.
     *
     * @since Minecraft 1.21.6
     */
    private Display display;

    public Attribute(AttributeType type, UUID uuid, AttributeModifier modifier, EquipmentSlotGroup slot) {
        this(type, uuid, modifier, slot, null);
    }

    /* These may change in the future */
    public enum EquipmentSlotGroup {
        /* BY ID */
        ANY,
        MAIN_HAND,
        OFF_HAND,
        HAND,
        FEET,
        LEGS,
        CHEST,
        HEAD,
        ARMOR,
        BODY,
        /**
         * @since Minecraft 1.21.5
         */
        SADDLE;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttributeModifier {

        private String id;
        private double amount;
        private Operation operation;

        /* These may change in the future */
        public enum Operation {
            ADD_VALUE,
            ADD_MULTIPLIED_BASE,
            ADD_MULTIPLIED_TOTAL
        }

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Display {

        private Type type;
        /**
         * The text replacing the modifier line. Only used for {@link Type#OVERRIDE}.
         */
        private ChatElement<?> text;

        /* BY ID */
        public enum Type {
            DEFAULT,
            HIDDEN,
            OVERRIDE
        }

    }

}
