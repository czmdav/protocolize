package dev.simplix.protocolize.api.item.component;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.util.ProtocolUtil;

import java.util.List;

public interface DataComponentType<T extends DataComponent> {

    String getName();

    default dev.simplix.protocolize.data.DataComponentType getDataComponentType() {
        String name = ProtocolUtil.removeNamespace(getName().toUpperCase());
        try {
            return dev.simplix.protocolize.data.DataComponentType.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Component was removed from the latest registry and only exists as a LEGACY_ constant
            return dev.simplix.protocolize.data.DataComponentType.valueOf("LEGACY_" + name);
        }
    }

    default List<ProtocolIdMapping> getMappings() {
        return Protocolize.mappingProvider().mappings(getDataComponentType());
    }

    T createEmpty();

}
