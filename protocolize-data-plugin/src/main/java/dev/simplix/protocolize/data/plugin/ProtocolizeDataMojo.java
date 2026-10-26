package dev.simplix.protocolize.data.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplix.protocolize.data.plugin.generator.Generator;
import dev.simplix.protocolize.data.plugin.generators.GenericGenerator;
import dev.simplix.protocolize.data.registries.GenericRegistry;
import dev.simplix.protocolize.data.registries.Registries;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

/**
 * Date: 23.08.2021
 *
 * @author Exceptionflug
 */
@Mojo(name = "compile")
public class ProtocolizeDataMojo extends AbstractMojo {

    private static final Gson GSON = new Gson();
    private static final Map<Integer, Registries> parsedRegistries = new HashMap<>();

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/protocolize-data", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/registries", required = true)
    private File registriesDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @SneakyThrows
    @Override
    public void execute() {
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        List<Registries> versions = latestFirstVersions();

        List<Generator> generators = new ArrayList<>(Arrays.asList(
            new GenericGenerator(outputDirectory, itemRegistries(), "ItemType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::soundRegistry), "Sound"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::mobEffectRegistry), "MobEffect"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::potionRegistry), "Potion"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::attributeRegistry), "Attribute"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::consumeEffectTypeRegistry), "ConsumeEffectType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::entityTypeRegistry), "EntityType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::blockEntityTypeRegistry), "BlockEntityType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::environmentAttributeRegistry), "EnvironmentAttribute"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::blockRegistry), "Block"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::enchantmentRegistry), "Enchantment"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::instrumentRegistry), "Instrument"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::damageTypeRegistry), "DamageType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::trimMaterialRegistry), "TrimMaterial"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::trimPatternRegistry), "TrimPattern"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::armorMaterialRegistry), "ArmorMaterial"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::bannerPatternRegistry), "BannerPattern"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::villagerTypeRegistry), "VillagerType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::wolfVariantRegistry), "WolfVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::wolfSoundVariantRegistry), "WolfSoundVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::catSoundVariantRegistry), "CatSoundVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::chickenSoundVariantRegistry), "ChickenSoundVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::cowSoundVariantRegistry), "CowSoundVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::pigSoundVariantRegistry), "PigSoundVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::foxVariantRegistry), "FoxVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::salmonVariantRegistry), "SalmonVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::parrotVariantRegistry), "ParrotVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::tropicalFishPatternRegistry), "TropicalFishPattern"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::mooshroomVariantRegistry), "MooshroomVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::rabbitVariantRegistry), "RabbitVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::pigVariantRegistry), "PigVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::cowVariantRegistry), "CowVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::chickenVariantRegistry), "ChickenVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::frogVariantRegistry), "FrogVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::horseVariantRegistry), "HorseVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::paintingVariantRegistry), "PaintingVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::llamaVariantRegistry), "LlamaVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::axolotlVariantRegistry), "AxolotlVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::catVariantRegistry), "CatVariant"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::dataComponentTypeRegistry), "DataComponentType"),
            new GenericGenerator(outputDirectory, collect(versions, Registries::getDataComponentPredicateTypeRegistry), "DataComponentPredicateType")
        ));

        for (Generator generator : generators) {
            long now = System.currentTimeMillis();
            getLog().info("Running generator for " + generator.getName() + "...");
            generator.generate();
            getLog().info("Generator for " + generator.getName() + " done (" + (System.currentTimeMillis() - now) + " ms)");
        }
    }

    /**
     * Registries from the latest version down to 1.13, so the first entry is the most recent definition of each key.
     */
    private List<Registries> latestFirstVersions() {
        List<Registries> versions = new ArrayList<>();
        for (int version = MINECRAFT_LATEST; version >= MINECRAFT_1_13; version--) {
            Registries registries = getRegistry(version);
            if (registries != null) {
                versions.add(registries);
            }
        }
        return versions;
    }

    private static List<GenericRegistry> collect(List<Registries> versions, Function<Registries, GenericRegistry> accessor) {
        List<GenericRegistry> result = new ArrayList<>();
        for (Registries registries : versions) {
            GenericRegistry registry = accessor.apply(registries);
            if (registry != null) {
                result.add(registry);
            }
        }
        return result;
    }

    /**
     * 1.13 to 1.14 items have no registries.json, so their items.json is read as the item registry.
     */
    private List<GenericRegistry> itemRegistries() {
        List<GenericRegistry> result = new ArrayList<>();
        for (int version = MINECRAFT_LATEST; version >= MINECRAFT_1_13; version--) {
            GenericRegistry registry = itemRegistry(version);
            if (registry != null) {
                result.add(registry);
            }
        }
        return result;
    }

    private GenericRegistry itemRegistry(int protocolVersion) {
        Registries registries = getRegistry(protocolVersion);
        if (registries != null && registries.itemRegistry() != null) {
            return registries.itemRegistry();
        }
        File itemsFile = new File(registriesDirectory, protocolVersion + "/items.json");
        if (!itemsFile.exists()) {
            return null;
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("entries", JsonParser.parseString(readFile(itemsFile.getAbsolutePath())));
        return GSON.fromJson(wrapper, GenericRegistry.class);
    }

    private Registries getRegistry(int protocolVersion) {
        if(parsedRegistries.containsKey(protocolVersion)){
            return parsedRegistries.get(protocolVersion);
        }
        File registriesFile = new File(registriesDirectory, protocolVersion + "/registries.json");
        if(!registriesFile.exists()){
            return null;
        }
        try {
            JsonObject minecraftRegistry = JsonParser.parseString(readFile(registriesFile.getAbsolutePath())).getAsJsonObject();
            // some items like enchantments, instruments, and damage types are not in the generated registries.json
            // so we can use an extended.json file for any internal or custom registries and merge them with the unmodified registries.json
            File extendedRegistryFile = new File(registriesDirectory, protocolVersion + "/extended.json");
            if(extendedRegistryFile.exists()){
                JsonObject extendedRegistry = JsonParser.parseString(readFile(extendedRegistryFile.getAbsolutePath())).getAsJsonObject();
                minecraftRegistry = jsonMerge(minecraftRegistry, extendedRegistry);
            }

            Registries registry = GSON.fromJson(minecraftRegistry, Registries.class);
            parsedRegistries.put(protocolVersion, registry);
            return registry;
        } catch (Exception e){
            getLog().warn("Failed to get registry for version " + protocolVersion, e);
        }
        return null;
    }

    private String readFile(String path){
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLog().warn("Failed to read file " + path);
            return "";
        }
    }

    private static JsonObject jsonMerge(JsonObject jsonA, JsonObject jsonB) {
        for (Map.Entry<String, JsonElement> sourceEntry : jsonA.entrySet()) {
            String key = sourceEntry.getKey();
            JsonElement value = sourceEntry.getValue();
            if (!jsonB.has(key)) {
                if (!value.isJsonNull()) {
                    jsonB.add(key, value);
                }
            } else {
                if (!value.isJsonNull()) {
                    if (value.isJsonObject()) {
                        jsonMerge(value.getAsJsonObject(), jsonB.get(key).getAsJsonObject());
                    } else {
                        jsonB.add(key, value);
                    }
                } else {
                    jsonB.remove(key);
                }
            }
        }
        return jsonB;
    }

}
