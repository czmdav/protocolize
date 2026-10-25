package dev.simplix.protocolize.api.util;

import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Context for deciding whether Protocolize should bypass processing for a packet.
 * <p>
 * Note: {@code player()} may be null during early handshake/status phases.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class PacketBypassContext {

    private final Direction streamDirection;
    private final PacketDirection packetDirection;
    private final int protocolVersion;
    private final ProtocolizePlayer player;
    private final Object server;
    private final Class<?> packetClass;
}
