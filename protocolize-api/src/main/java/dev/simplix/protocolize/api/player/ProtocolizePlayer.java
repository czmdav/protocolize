package dev.simplix.protocolize.api.player;

import com.google.common.collect.Lists;
import dev.simplix.protocolize.api.Location;
import dev.simplix.protocolize.api.PlayerInteract;
import dev.simplix.protocolize.api.SoundCategory;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.PlayerInventory;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import dev.simplix.protocolize.api.util.SkullProfileResolver;
import dev.simplix.protocolize.data.Sound;
import dev.simplix.protocolize.data.packets.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Date: 26.08.2021
 *
 * @author Exceptionflug
 */
public interface ProtocolizePlayer {

    UUID uniqueId();

    PlayerInventory proxyInventory();

    void sendPacket(Object packet);

    void sendPacketToServer(Object packet);

    Map<Integer, Inventory> registeredInventories();

    int generateWindowId();

    int protocolVersion();

    <T> T handle();

    Location location();

    void onInteract(Consumer<PlayerInteract> interactConsumer);

    void handleInteract(PlayerInteract interact);

    default void playSound(Sound sound, SoundCategory category, float volume, float pitch) {
        playSound(location(), sound, category, volume, pitch);
    }

    default void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
        if (protocolVersion() >= ProtocolVersions.MINECRAFT_1_19_3) {
            sendPacket(new SoundEffect(sound, category, location.x(), location.y(), location.z(), volume, pitch));
        } else {
            sendPacket(new NamedSoundEffect(sound, category, location.x(), location.y(), location.z(), volume, pitch));
        }
    }

    default void registerInventory(int windowId, Inventory inventory) {
        if (inventory == null) {
            registeredInventories().remove(windowId);
        } else {
            registeredInventories().put(windowId, inventory);
        }
    }

    default void closeInventory() {
        registeredInventories().forEach((id, it) -> {
            sendPacket(new CloseWindow(id));
        });
        registeredInventories().clear();
    }

    default void openInventory(Inventory inventory) {
        boolean alreadyOpen = false;
        int windowId = -1;
        for (Integer id : registeredInventories().keySet()) {
            Inventory val = registeredInventories().get(id);
            if (val == inventory) {
                alreadyOpen = true;
                break;
            }
        }

        // Close all inventories if not opened
        if (!alreadyOpen) {
            closeInventory();
        }

        if (registeredInventories().containsValue(inventory)) {
            for (Integer id : registeredInventories().keySet()) {
                Inventory val = registeredInventories().get(id);
                if (val == inventory) {
                    windowId = id;
                    break;
                }
            }
            if (windowId == -1) {
                windowId = generateWindowId();
                registerInventory(windowId, inventory);
            }
        } else {
            windowId = generateWindowId();
            registerInventory(windowId, inventory);
        }

        if (!alreadyOpen) {
            sendPacket(new OpenWindow(windowId, inventory.type(), inventory.title()));
        }
        int protocolVersion;
        try {
            protocolVersion = protocolVersion();
        } catch (Throwable t) {
            protocolVersion = 47;
        }
        List<BaseItemStack> items = new ArrayList<>(Lists
            .newArrayList(inventory.itemsIndexed(protocolVersion)));
        sendPacket(new WindowItems((short) windowId, items, 0));
        resendWhenSkullOwnersResolved(windowId, inventory, items, protocolVersion);
    }

    /**
     * Legacy clients (before 1.13) look up skull owners which are only given by name on their render thread, which freezes
     * them. Protocolize resolves those names on the proxy instead and sends skulls which are not resolved yet with the
     * default skin. This re-sends the inventory contents once all skull owners of the given items are resolved.
     */
    default void resendWhenSkullOwnersResolved(int windowId, Inventory inventory, List<BaseItemStack> items, int protocolVersion) {
        if (protocolVersion >= ProtocolVersions.MINECRAFT_1_13) {
            return;
        }
        List<CompletableFuture<SkullProfileResolver.Profile>> pending = new ArrayList<>();
        for (BaseItemStack item : items) {
            if (item == null || !SkullProfileResolver.needsResolving(item.nbtData())) {
                continue;
            }
            CompletableFuture<SkullProfileResolver.Profile> future = SkullProfileResolver.resolve(SkullProfileResolver.ownerName(item.nbtData()));
            if (!future.isDone()) {
                pending.add(future);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).thenRun(() -> {
            if (registeredInventories().get(windowId) != inventory) {
                return; // Inventory has been closed or replaced in the meantime
            }
            try {
                sendPacket(new WindowItems((short) windowId, new ArrayList<>(inventory.itemsIndexed(protocolVersion)), 0));
            } catch (Exception ignored) {
                // Player disconnected
            }
        });
    }

}
