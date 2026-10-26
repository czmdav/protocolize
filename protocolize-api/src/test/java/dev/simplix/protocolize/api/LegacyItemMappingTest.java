package dev.simplix.protocolize.api;

import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import dev.simplix.protocolize.data.DataModule;
import dev.simplix.protocolize.data.ItemType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LegacyItemMappingTest {

    private SimpleMappingProvider provider;

    @Before
    public void registerMappings() {
        provider = new SimpleMappingProvider();
        new DataModule().registerMappings(provider);
    }

    @Test
    public void legacyOnlyItemIsMappedAtOldVersion() {
        ProtocolIdMapping mapping = provider.mapping(ItemType.LEGACY_GRASS, ProtocolVersions.MINECRAFT_1_13);

        assertNotNull(mapping);
        assertEquals(76, mapping.id());
    }

    @Test
    public void legacyOnlyItemRoundTripsFromIdAtOldVersion() {
        assertEquals(ItemType.LEGACY_GRASS,
            provider.mapIdToEnum(76, ProtocolVersions.MINECRAFT_1_13, ItemType.class));
    }

    @Test
    public void primaryItemRoundTripsWithoutLegacyPrefixAtOldVersion() {
        assertEquals(ItemType.STONE,
            provider.mapIdToEnum(1, ProtocolVersions.MINECRAFT_1_13, ItemType.class));
    }

    @Test
    public void latestItemRoundTripsThroughItsId() {
        ProtocolIdMapping mapping = provider.mapping(ItemType.GRASS_BLOCK, ProtocolVersions.MINECRAFT_LATEST);

        assertNotNull(mapping);
        assertEquals(ItemType.GRASS_BLOCK,
            provider.mapIdToEnum(mapping.id(), ProtocolVersions.MINECRAFT_LATEST, ItemType.class));
    }
}
