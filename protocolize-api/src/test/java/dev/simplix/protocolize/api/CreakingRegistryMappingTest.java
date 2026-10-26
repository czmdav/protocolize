package dev.simplix.protocolize.api;

import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import dev.simplix.protocolize.data.BlockEntityType;
import dev.simplix.protocolize.data.DataModule;
import dev.simplix.protocolize.data.EnvironmentAttribute;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CreakingRegistryMappingTest {

    private SimpleMappingProvider provider;

    @Before
    public void registerMappings() {
        provider = new SimpleMappingProvider();
        new DataModule().registerMappings(provider);
    }

    @Test
    public void creakingHeartBlockEntityMapsToItsLatestId() {
        ProtocolIdMapping mapping = provider.mapping(BlockEntityType.CREAKING_HEART, ProtocolVersions.MINECRAFT_LATEST);

        assertNotNull(mapping);
        assertEquals(10, mapping.id());
    }

    @Test
    public void creakingHeartBlockEntityRoundTripsThroughItsId() {
        assertEquals(BlockEntityType.CREAKING_HEART,
            provider.mapIdToEnum(10, ProtocolVersions.MINECRAFT_LATEST, BlockEntityType.class));
    }

    @Test
    public void creakingHeartBlockEntityIsAbsentBeforeItExisted() {
        assertNull(provider.mapping(BlockEntityType.CREAKING_HEART, ProtocolVersions.MINECRAFT_1_14));
    }

    @Test
    public void creakingActiveEnvironmentAttributeMapsToItsLatestId() {
        ProtocolIdMapping mapping = provider.mapping(EnvironmentAttribute.GAMEPLAY_CREAKING_ACTIVE, ProtocolVersions.MINECRAFT_LATEST);

        assertNotNull(mapping);
        assertEquals(41, mapping.id());
    }

    @Test
    public void creakingActiveEnvironmentAttributeRoundTripsThroughItsId() {
        assertEquals(EnvironmentAttribute.GAMEPLAY_CREAKING_ACTIVE,
            provider.mapIdToEnum(41, ProtocolVersions.MINECRAFT_LATEST, EnvironmentAttribute.class));
    }

    @Test
    public void environmentAttributeIsAbsentBeforeItsRegistryExisted() {
        assertNull(provider.mapping(EnvironmentAttribute.GAMEPLAY_CREAKING_ACTIVE, ProtocolVersions.MINECRAFT_1_21_4));
    }
}
