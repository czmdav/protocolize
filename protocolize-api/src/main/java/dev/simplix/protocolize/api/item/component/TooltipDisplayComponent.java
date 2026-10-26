package dev.simplix.protocolize.api.item.component;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.data.DataComponentType;

import java.util.List;

/**
 * Controls the tooltip of an item. Available since Minecraft 1.21.5 where it replaced {@link HideTooltipComponent},
 * {@link HideAdditionalTooltipComponent} and the {@code showInTooltip} flags of other components. Protocolize translates
 * these older components into this component for 1.21.5+ clients automatically.
 */
public interface TooltipDisplayComponent extends DataComponent {

    boolean isHideTooltip();

    void setHideTooltip(boolean hideTooltip);

    /**
     * @return The data component types whose tooltip lines are hidden
     */
    List<DataComponentType> getHiddenComponents();

    void setHiddenComponents(List<DataComponentType> hiddenComponents);

    static TooltipDisplayComponent create(boolean hideTooltip, List<DataComponentType> hiddenComponents) {
        return Protocolize.getService(Factory.class).create(hideTooltip, hiddenComponents);
    }

    interface Factory {

        TooltipDisplayComponent create(boolean hideTooltip, List<DataComponentType> hiddenComponents);

    }

}
