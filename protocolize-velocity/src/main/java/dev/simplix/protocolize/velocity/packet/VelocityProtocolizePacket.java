package dev.simplix.protocolize.velocity.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.providers.ProtocolizePlayerProvider;
import dev.simplix.protocolize.api.util.DebugUtil;
import dev.simplix.protocolize.api.util.PacketBypassContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Date: 22.08.2021
 *
 * @author Exceptionflug
 */
@Getter
@Setter
@Accessors(fluent = true)
public class VelocityProtocolizePacket implements MinecraftPacket {

    private static final ProtocolizePlayerProvider PLAYER_PROVIDER = Protocolize.playerProvider();

    private AbstractPacket wrapper;
    private byte[] skipBuffer;

    public VelocityProtocolizePacket() {
        Class<? extends AbstractPacket> wrapperClass = obtainProtocolizePacketClass();
        if (wrapperClass == null) {
            throw new RuntimeException("Unable to determine protocolize packet type.");
        }
        try {
            wrapper = wrapperClass.getConstructor().newInstance();
        } catch (Exception exception) {
            throw new RuntimeException("Unable to construct instance of " + wrapperClass.getName() + ". Please ensure that the "
                + "default constructor is existent and accessible.");
        }
    }

    public Class<? extends AbstractPacket> obtainProtocolizePacketClass() {
        return null; // Will be overridden by ByteBuddy
    }

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        try {
            Class<? extends AbstractPacket> packetClass = obtainProtocolizePacketClass();

            // Bypass hook (player may be unknown here; context still includes version + class)
            if (Protocolize.packetBypassProvider() != null) {
                PacketBypassContext ctx = new PacketBypassContext(
                    direction == ProtocolUtils.Direction.CLIENTBOUND ? Direction.DOWNSTREAM : Direction.UPSTREAM,
                    direction == ProtocolUtils.Direction.CLIENTBOUND ? PacketDirection.CLIENTBOUND : PacketDirection.SERVERBOUND,
                    protocolVersion.getProtocol(),
                    (ProtocolizePlayer) null,
                    null,
                    packetClass
                );
                if (Protocolize.packetBypassProvider().shouldBypass(ctx)) {
                    skipBuffer = new byte[byteBuf.readableBytes()];
                    byteBuf.readBytes(skipBuffer);
                    return;
                }
            }

            if (Protocolize.listenerProvider().listenersForType(packetClass).isEmpty()) {
                skipBuffer = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(skipBuffer); // Don't decode packet if nobody has interest in it
                return;
            }
            wrapper.read(byteBuf,
                direction == ProtocolUtils.Direction.CLIENTBOUND ? PacketDirection.CLIENTBOUND : PacketDirection.SERVERBOUND,
                protocolVersion.getProtocol());
            if (byteBuf.isReadable() && DebugUtil.enabled) {
                DebugUtil.writeDump(byteBuf, new CorruptedFrameException("Protocolize is unable to read packet " + packetClass.getName()
                    + " at protocol version " + protocolVersion + " in direction " + direction.name()));
            }
        } catch (Throwable throwable) {
            CorruptedFrameException corruptedFrameException = new CorruptedFrameException("Protocolize is unable to read packet " + obtainProtocolizePacketClass().getName()
                + " at protocol version " + protocolVersion + " in direction " + direction.name(), throwable);
            if (DebugUtil.enabled) {
                DebugUtil.writeDump(byteBuf, corruptedFrameException);
            }
            throw corruptedFrameException;
        }
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        if (skipBuffer != null) {
            byteBuf.writeBytes(skipBuffer);
            return;
        }
        wrapper.write(byteBuf,
            direction == ProtocolUtils.Direction.CLIENTBOUND ? PacketDirection.CLIENTBOUND : PacketDirection.SERVERBOUND,
            protocolVersion.getProtocol());
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

}
