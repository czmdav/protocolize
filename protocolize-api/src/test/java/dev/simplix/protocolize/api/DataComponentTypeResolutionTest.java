package dev.simplix.protocolize.api;

import dev.simplix.protocolize.data.DataComponentType;
import dev.simplix.protocolize.data.item.component.FireResistantComponentImpl;
import dev.simplix.protocolize.data.item.component.FoodComponentImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataComponentTypeResolutionTest {

    @Test
    public void currentComponentResolvesToItsConstant() {
        assertEquals(DataComponentType.FOOD, FoodComponentImpl.Type.INSTANCE.getDataComponentType());
    }

    @Test
    public void componentRemovedFromLatestResolvesToLegacyConstant() {
        assertEquals(DataComponentType.LEGACY_FIRE_RESISTANT,
            FireResistantComponentImpl.Type.INSTANCE.getDataComponentType());
    }
}
