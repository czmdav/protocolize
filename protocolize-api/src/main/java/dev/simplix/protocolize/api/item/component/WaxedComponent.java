package dev.simplix.protocolize.api.item.component;

import dev.simplix.protocolize.api.Protocolize;

public interface WaxedComponent extends DataComponent {

    static WaxedComponent create() {
        return Protocolize.getService(Factory.class).create();
    }

    interface Factory {

        WaxedComponent create();

    }

}
