package dev.simplix.protocolize.velocity.providers;

import dev.simplix.protocolize.api.providers.PacketBypassProvider;
import dev.simplix.protocolize.api.util.PacketBypassContext;

/**
 * Velocity implementation for selectively bypassing Protocolize processing.
 */
public final class VelocityPacketBypassProvider implements PacketBypassProvider {

    // 1.8.x protocol (47) and 1.7.10 (5) are treated as "legacy" by many proxies.
    private static final int PROTOCOL_1_8_X = 47;

    @Override
    public boolean shouldBypass(PacketBypassContext context) {
        if (context == null || context.packetClass() == null) {
            return false;
        }

        // Only apply for 1.8.x clients
        if (context.protocolVersion() != PROTOCOL_1_8_X) {
            return false;
        }

        // Velocity packet class names for these packets (package differs by Velocity version).
        String simpleName = context.packetClass().getSimpleName();

        // BlockPlacement / PlayerBlockPlacement
        if ("BlockPlacement".equals(simpleName) || "PlayerBlockPlacement".equals(simpleName)) {
            return true;
        }

        // UseEntity / Interact
        if ("UseEntity".equals(simpleName) || "Interact".equals(simpleName)) {
            return true;
        }

        return false;
    }
}
