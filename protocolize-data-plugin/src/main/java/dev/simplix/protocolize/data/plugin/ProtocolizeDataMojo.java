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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Registries latestRegistries = getRegistry(MINECRAFT_LATEST);

        List<Generator> generators = new ArrayList<>(Arrays.asList(
            new GenericGenerator(outputDirectory, latestRegistries.itemRegistry(), "ItemType"),
            new GenericGenerator(outputDirectory, latestRegistries.soundRegistry(), "Sound"),
            new GenericGenerator(outputDirectory, latestRegistries.mobEffectRegistry(), "MobEffect"),
            new GenericGenerator(outputDirectory, latestRegistries.potionRegistry(), "Potion"),
            new GenericGenerator(outputDirectory, latestRegistries.attributeRegistry(), "Attribute"),
            new GenericGenerator(outputDirectory, latestRegistries.consumeEffectTypeRegistry(), "ConsumeEffectType"),
            new GenericGenerator(outputDirectory, latestRegistries.entityTypeRegistry(), "EntityType"),
            new GenericGenerator(outputDirectory, latestRegistries.blockRegistry(), "Block"),
            new GenericGenerator(outputDirectory, latestRegistries.enchantmentRegistry(), "Enchantment"),
            new GenericGenerator(outputDirectory, latestRegistries.instrumentRegistry(), "Instrument"),
            new GenericGenerator(outputDirectory, latestRegistries.damageTypeRegistry(), "DamageType"),
            new GenericGenerator(outputDirectory, latestRegistries.trimMaterialRegistry(), "TrimMaterial"),
            new GenericGenerator(outputDirectory, latestRegistries.trimPatternRegistry(), "TrimPattern"),
            new GenericGenerator(outputDirectory, latestRegistries.armorMaterialRegistry(), "ArmorMaterial"),
            new GenericGenerator(outputDirectory, latestRegistries.bannerPatternRegistry(), "BannerPattern"),
            new GenericGenerator(outputDirectory, latestRegistries.villagerTypeRegistry(), "VillagerType"),
            new GenericGenerator(outputDirectory, latestRegistries.wolfVariantRegistry(), "WolfVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.wolfSoundVariantRegistry(), "WolfSoundVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.foxVariantRegistry(), "FoxVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.salmonVariantRegistry(), "SalmonVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.parrotVariantRegistry(), "ParrotVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.tropicalFishPatternRegistry(), "TropicalFishPattern"),
            new GenericGenerator(outputDirectory, latestRegistries.mooshroomVariantRegistry(), "MooshroomVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.rabbitVariantRegistry(), "RabbitVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.pigVariantRegistry(), "PigVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.cowVariantRegistry(), "CowVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.chickenVariantRegistry(), "ChickenVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.frogVariantRegistry(), "FrogVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.horseVariantRegistry(), "HorseVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.paintingVariantRegistry(), "PaintingVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.llamaVariantRegistry(), "LlamaVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.axolotlVariantRegistry(), "AxolotlVariant"),
            new GenericGenerator(outputDirectory, latestRegistries.catVariantRegistry(), "CatVariant")
        ));


        /*
         * This will register all DataComponentType's for all versions 1.20.5-Latest as some were already removed.
         * It will still use the mapping provider to grab correct versions or warn if they cannot be used on a
         * specific version.
         */
        List<GenericRegistry> dataComponentRegistries = new ArrayList<>();
        for(Registries registry : getRegistries(MINECRAFT_1_20_5, MINECRAFT_LATEST)){
            try {
                dataComponentRegistries.add(registry.dataComponentTypeRegistry());
            } catch (Exception ignored) { }
        }
        generators.add(new GenericGenerator(outputDirectory, dataComponentRegistries, "DataComponentType"));

        List<GenericRegistry> dataComponentPredicateRegistries = new ArrayList<>();
        for(Registries registry : getRegistries(MINECRAFT_1_21_5, MINECRAFT_LATEST)){
            try {
                dataComponentPredicateRegistries.add(registry.getDataComponentPredicateTypeRegistry());
            } catch (Exception ignored) { }
        }
        generators.add(new GenericGenerator(outputDirectory, dataComponentPredicateRegistries, "DataComponentPredicateType"));

        for (Generator generator : generators) {
            long now = System.currentTimeMillis();
            getLog().info("Running generator for " + generator.getName() + "...");
            generator.generate();
            getLog().info("Generator for " + generator.getName() + " done (" + (System.currentTimeMillis() - now) + " ms)");
        }
    }

    private Registries getRegistry(int protocolVersion) {
        if(parsedRegistries.containsKey(protocolVersion)){
            return parsedRegistries.get(protocolVersion);
        } else {
            try {
                Registries registry;
                JsonObject minecraftRegistry = JsonParser.parseString(readFile(new File(registriesDirectory, protocolVersion + "/registries.json").getAbsolutePath())).getAsJsonObject();
                // some items like enchantments, instruments, and damage types are not in the generated registries.json
                // so we can use an extended.json file for any internal or custom registries and merge them with the unmodified registries.json
                File extendedRegistryFile = new File(registriesDirectory, protocolVersion + "/extended.json");
                if(extendedRegistryFile.exists()){
                    JsonObject extendedRegistry = JsonParser.parseString(readFile(extendedRegistryFile.getAbsolutePath())).getAsJsonObject();
                    minecraftRegistry = jsonMerge(minecraftRegistry, extendedRegistry);
                }

                registry = GSON.fromJson(minecraftRegistry, Registries.class);
                parsedRegistries.put(protocolVersion, registry);
                return registry;
            } catch (Exception e){
                getLog().warn("Failed to get registry for version " + protocolVersion, e);
            }
        }
        return null;
    }

    private List<Registries> getRegistries(int startVersion, int endVersion) throws FileNotFoundException {
        List<Registries> registries = new ArrayList<>();
        for(int i = startVersion; i <= endVersion; i++){
            Registries registry = getRegistry(i);
            if(registry != null){
                registries.add(registry);
            }
        }
        return registries;
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
