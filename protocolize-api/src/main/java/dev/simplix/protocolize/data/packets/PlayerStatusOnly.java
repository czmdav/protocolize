package dev.simplix.protocolize.data.packets;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import io.netty.buffer.ByteBuf;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

/**
 * ServerboundMovePlayerPacket.StatusOnly
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class PlayerStatusOnly extends AbstractPacket {

    public static final List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21, MINECRAFT_1_21, 0x1D),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_2, MINECRAFT_1_21_5, 0x1F),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_6, MINECRAFT_1_21_11, 0x20),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_26_1, MINECRAFT_26_3, 0x21)
    );

    private boolean onGround;
    private boolean horizontalCollision;

    @Override
    public void read(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        if(protocolVersion >= MINECRAFT_1_21_2){
            short flags = buf.readUnsignedByte();
            this.onGround = PlayerPositionLook.unpackOnGround(flags);
            this.horizontalCollision = PlayerPositionLook.unpackHorizontalCollision(flags);
        } else {
            this.onGround = buf.readBoolean();
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        if(protocolVersion >= MINECRAFT_1_21_2){
            buf.writeByte(PlayerPositionLook.packFlags(this.onGround, this.horizontalCollision));
        } else {
            buf.writeBoolean(this.onGround);
        }
    }

}
