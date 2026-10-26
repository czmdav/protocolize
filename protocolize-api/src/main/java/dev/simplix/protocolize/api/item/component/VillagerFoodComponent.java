package dev.simplix.protocolize.api.item.component;

import dev.simplix.protocolize.api.Protocolize;

public interface VillagerFoodComponent extends DataComponent {

    int getNutrition();

    void setNutrition(int nutrition);

    static VillagerFoodComponent create(int nutrition) {
        return Protocolize.getService(Factory.class).create(nutrition);
    }

    interface Factory {

        VillagerFoodComponent create(int nutrition);

    }

}
