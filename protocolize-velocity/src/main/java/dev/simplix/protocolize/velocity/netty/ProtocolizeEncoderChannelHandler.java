package dev.simplix.protocolize.velocity.netty;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.providers.ProtocolizePlayerProvider;
import dev.simplix.protocolize.api.util.PacketBypassContext;
import dev.simplix.protocolize.api.util.ReflectionUtil;
import dev.simplix.protocolize.velocity.providers.VelocityPacketListenerProvider;
import dev.simplix.protocolize.velocity.util.ConversionUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Date: 23.08.2021
 *
 * @author Exceptionflug
 */
public final class ProtocolizeEncoderChannelHandler extends MessageToMessageEncoder<MinecraftPacket> {

    private static final VelocityPacketListenerProvider LISTENER_PROVIDER = (VelocityPacketListenerProvider) Protocolize.listenerProvider();
    private static final ProtocolizePlayerProvider PLAYER_PROVIDER = Protocolize.playerProvider();
    private static final Field CONNECTION_FIELD = ReflectionUtil.fieldOrNull(InitialInboundConnection.class, "connection", true);

    private final Direction streamDirection;
    private InboundConnection inboundConnection;
    private ServerConnection serverConnection;
    private PacketDirection packetDirection;
    private Protocol protocol;
    private ProtocolVersion protocolVersion;

    public ProtocolizeEncoderChannelHandler(Direction streamDirection) {
        this.streamDirection = streamDirection;
    }

    public void connection(InboundConnection connection) throws IllegalAccessException {
        this.inboundConnection = connection;
        protocolVersion = connection.getProtocolVersion();
        if (connection instanceof ConnectedPlayer) {
            packetDirection = PacketDirection.SERVERBOUND;
            protocol = ConversionUtils.protocolizeProtocol(((ConnectedPlayer) connection).getConnection().getState());
        } else if (connection instanceof InitialInboundConnection) {
            packetDirection = PacketDirection.SERVERBOUND;
            protocol = ConversionUtils.protocolizeProtocol(((MinecraftConnection) CONNECTION_FIELD.get(connection)).getState());
        } else {
            throw new IllegalArgumentException("Unsupported InboundConnection instance: " + connection.getClass().getName());
        }
    }

    public void connection(ServerConnection connection) {
        this.serverConnection = connection;
        protocolVersion = connection.getPlayer().getProtocolVersion();
        if (connection instanceof VelocityServerConnection) {
            packetDirection = PacketDirection.CLIENTBOUND;
            protocol = ConversionUtils.protocolizeProtocol(((VelocityServerConnection) connection).getConnection().getState());
        } else {
            throw new IllegalArgumentException("Unsupported ServerConnection instance: " + connection.getClass().getName());
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MinecraftPacket msg, List<Object> out) throws Exception {
        if (msg != null) {
            try {
                if (!shouldBypass(msg)) {
                    msg = LISTENER_PROVIDER.handleOutboundPacket(msg, inboundConnection, serverConnection);
                }
                out.add(Objects.requireNonNullElseGet(msg, KeepAlivePacket::new));
            } finally {
                ReferenceCountUtil.retain(msg);
            }
        }
    }

    private boolean shouldBypass(MinecraftPacket minecraftPacket) {
        try {
            ProtocolizePlayer protocolizePlayer = null;
            Object serverInfo = null;

            if (serverConnection != null) {
                var player = serverConnection.getPlayer();
                protocolizePlayer = player != null ? PLAYER_PROVIDER.player(player.getUniqueId()) : null;
                serverInfo = serverConnection.getServerInfo();
            } else if (inboundConnection instanceof com.velocitypowered.api.proxy.Player) {
                var player = (com.velocitypowered.api.proxy.Player) inboundConnection;
                protocolizePlayer = PLAYER_PROVIDER.player(player.getUniqueId());
                serverInfo = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo() : null;
            }

            PacketBypassContext c = new PacketBypassContext(
                streamDirection,
                packetDirection,
                protocolVersion != null ? protocolVersion.getProtocol() : 0,
                protocolizePlayer,
                serverInfo,
                minecraftPacket.getClass()
            );
            return Protocolize.packetBypassProvider() != null && Protocolize.packetBypassProvider().shouldBypass(c);
        } catch (Throwable ignored) {
            return false;
        }
    }

}
