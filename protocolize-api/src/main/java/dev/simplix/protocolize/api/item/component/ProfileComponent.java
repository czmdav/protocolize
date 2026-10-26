package dev.simplix.protocolize.api.item.component;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.util.Property;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface ProfileComponent extends DataComponent {

    @Nullable
    String getName();

    void setName(@Nullable String name);

    @Nullable
    UUID getUniqueId();

    void setUniqueId(@Nullable UUID uniqueId);

    List<Property> getProperties();

    void setProperties(List<Property> properties);

    /**
     * @return The texture asset overriding the skin, only used since Minecraft 1.21.9
     */
    @Nullable
    String getBodyTexture();

    void setBodyTexture(@Nullable String bodyTexture);

    /**
     * @return The texture asset overriding the cape, only used since Minecraft 1.21.9
     */
    @Nullable
    String getCapeTexture();

    void setCapeTexture(@Nullable String capeTexture);

    /**
     * @return The texture asset overriding the elytra, only used since Minecraft 1.21.9
     */
    @Nullable
    String getElytraTexture();

    void setElytraTexture(@Nullable String elytraTexture);

    /**
     * @return true to force the slim model, false to force the wide model and null to use the model of the skin.
     * Only used since Minecraft 1.21.9
     */
    @Nullable
    Boolean getSlimModel();

    void setSlimModel(@Nullable Boolean slimModel);

    static ProfileComponent create(String name, UUID uniqueId, List<Property> properties) {
        return Protocolize.getService(Factory.class).create(name, uniqueId, properties);
    }

    interface Factory {

        ProfileComponent create(String name, UUID uniqueId, List<Property> properties);

    }

}
