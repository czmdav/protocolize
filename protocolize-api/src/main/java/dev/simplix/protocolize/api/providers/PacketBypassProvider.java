package dev.simplix.protocolize.api.providers;

import dev.simplix.protocolize.api.util.PacketBypassContext;

/**
 * Allows platforms and plugins to bypass Protocolize processing for certain packets.
 * <p>
 * If {@link #shouldBypass(PacketBypassContext)} returns {@code true}, Protocolize should
 * not decode/convert/dispatch the packet to Protocolize listeners for the given context.
 */
public interface PacketBypassProvider {

    /**
     * @param context The bypass decision context
     * @return true if Protocolize should bypass processing for this packet
     */
    boolean shouldBypass(PacketBypassContext context);
}
