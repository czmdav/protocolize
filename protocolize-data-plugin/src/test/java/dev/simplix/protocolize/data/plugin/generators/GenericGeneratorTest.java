package dev.simplix.protocolize.data.plugin.generators;

import com.google.gson.Gson;
import dev.simplix.protocolize.data.registries.GenericRegistry;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenericGeneratorTest {

    private static GenericRegistry registry(String... keys) {
        StringBuilder json = new StringBuilder("{\"entries\":{");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(keys[i]).append("\":{\"protocol_id\":").append(i).append('}');
        }
        json.append("}}");
        return new Gson().fromJson(json.toString(), GenericRegistry.class);
    }

    @Test
    public void latestVersionKeysKeepUnprefixedNames() {
        List<String> constants = GenericGenerator.constants(
            Collections.singletonList(registry("minecraft:stone", "minecraft:grass_block")));

        assertEquals(Arrays.asList("GRASS_BLOCK", "STONE"), constants);
    }

    @Test
    public void keysOnlyInOlderVersionsGetLegacyPrefix() {
        List<String> constants = GenericGenerator.constants(Arrays.asList(
            registry("minecraft:stone"),
            registry("minecraft:stone", "minecraft:grass")));

        assertTrue(constants.contains("STONE"));
        assertTrue(constants.contains("LEGACY_GRASS"));
        assertFalse(constants.contains("GRASS"));
        assertFalse(constants.contains("LEGACY_STONE"));
    }

    @Test
    public void sharedKeyAppearsOnceAndKeepsLatestName() {
        List<String> constants = GenericGenerator.constants(Arrays.asList(
            registry("minecraft:stone"),
            registry("minecraft:stone"),
            registry("minecraft:stone")));

        assertEquals(Collections.singletonList("STONE"), constants);
    }

    @Test
    public void legacyNamesAreAddedFromOlderVersionsToo() {
        List<String> constants = GenericGenerator.constants(Arrays.asList(
            registry("minecraft:stone"),
            registry("minecraft:stone", "minecraft:grass"),
            registry("minecraft:stone", "minecraft:grass", "minecraft:dirt")));

        assertEquals(Arrays.asList("LEGACY_DIRT", "LEGACY_GRASS", "STONE"), constants);
    }

    @Test
    public void latestVersionKeyIsNotDuplicatedByOlderVersion() {
        // grass_block exists in both versions; it must be emitted once, unprefixed.
        // grass exists only in the older version, so it gets the prefix.
        List<String> constants = GenericGenerator.constants(Arrays.asList(
            registry("minecraft:grass_block"),
            registry("minecraft:grass_block", "minecraft:grass")));

        assertEquals(Arrays.asList("LEGACY_GRASS", "GRASS_BLOCK"), constants);
    }

    @Test
    public void dotsAndSlashesBecomeUnderscores() {
        List<String> constants = GenericGenerator.constants(
            Collections.singletonList(registry("minecraft:music_disc.cat", "minecraft:banner/pattern")));

        assertEquals(Arrays.asList("BANNER_PATTERN", "MUSIC_DISC_CAT"), constants);
    }

    @Test
    public void outputIsSortedByRegistryKey() {
        List<String> constants = GenericGenerator.constants(Arrays.asList(
            registry("minecraft:zebra"),
            registry("minecraft:apple", "minecraft:mango")));

        assertEquals(Arrays.asList("LEGACY_APPLE", "LEGACY_MANGO", "ZEBRA"), constants);
    }
}
