package dev.simplix.protocolize.data.packets;

import dev.simplix.protocolize.api.BlockFace;
import dev.simplix.protocolize.api.BlockPosition;
import dev.simplix.protocolize.api.Hand;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.item.ItemStackSerializer;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.DebugUtil;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import dev.simplix.protocolize.api.util.exception.ExceptionUtil;
import dev.simplix.protocolize.api.util.exception.ProtocolizeException;
import dev.simplix.protocolize.data.util.BlockPositionSerializer;
import io.netty.buffer.ByteBuf;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

/**
 * Date: 25.08.2021
 *
 * @author Exceptionflug
 */
@Slf4j(topic = "Protocolize")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class BlockPlacement extends AbstractPacket {

    /* ServerboundUseItemOnPacket */

    public final static List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x29),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_15_2, 0x2C),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x2D),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_18_2, 0x2E),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x30),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_20_1, 0x31),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x34),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_3, MINECRAFT_1_20_4, 0x35),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_5, MINECRAFT_1_21_1, 0x38),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_2, MINECRAFT_1_21_3, 0x3A),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_4, MINECRAFT_1_21_4, 0x3C),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_5, MINECRAFT_1_21_5, 0x3E),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_6, MINECRAFT_1_21_11, 0x3F),
        AbstractProtocolMapping.rangedIdMapping(MINECRAFT_26_1, MINECRAFT_26_3, 0x42)
    );

    private BlockPosition position;
    private BlockFace face;
    private Hand hand;
    private float hitVecX;
    private float hitVecY;
    private float hitVecZ;
    private boolean insideBlock;
    private boolean worldBorderHit;

    /**
     * @since protocol version 759 (1.19)
     */
    private int sequence;

    // skip decoding and save raw packet data
    boolean skipDecoding = false;
    byte[] packetData = null;

    @Override
    public void read(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        readEntirePacket(buf);
        StringBuilder sb = new StringBuilder();
        sb.append("BlockPlacement:");
        try {
            if (protocolVersion < MINECRAFT_1_14) {
                position = BlockPositionSerializer.read(buf, protocolVersion);
                sb.append("\n    Position: ").append(position.x()).append(" ").append(position.y()).append(" ").append(position.z());
                face = BlockFace.blockFace(ProtocolUtil.readVarInt(buf));
                sb.append("\n    Face: ").append(face.name());
                hand = Hand.handByProtocolId(ProtocolUtil.readVarInt(buf));
                sb.append("\n    Hand: ").append(hand.name());
                hitVecX = buf.readFloat();
                sb.append("\n    Hit Vec X: ").append(this.hitVecX);
                hitVecY = buf.readFloat();
                sb.append("\n    Hit Vec Y: ").append(this.hitVecY);
                hitVecZ = buf.readFloat();
                sb.append("\n    Hit Vec Z: ").append(this.hitVecZ);
            } else {
                hand = Hand.handByProtocolId(ProtocolUtil.readVarInt(buf));
                sb.append("\n    Hand: ").append(hand.name());
                position = BlockPositionSerializer.read(buf, protocolVersion);
                sb.append("\n    Position: ").append(position.x()).append(" ").append(position.y()).append(" ").append(position.z());
                face = BlockFace.blockFace(ProtocolUtil.readVarInt(buf));
                sb.append("\n    Face: ").append(face.name());
                hitVecX = buf.readFloat();
                sb.append("\n    Hit Vec X: ").append(this.hitVecX);
                hitVecY = buf.readFloat();
                sb.append("\n    Hit Vec Y: ").append(this.hitVecY);
                hitVecZ = buf.readFloat();
                sb.append("\n    Hit Vec Z: ").append(this.hitVecZ);
                insideBlock = buf.readBoolean();
                sb.append("\n    Inside: ").append(this.insideBlock);
                if (protocolVersion >= MINECRAFT_1_21_2) {
                    worldBorderHit = buf.readBoolean();
                    sb.append("\n    World Border: ").append(this.worldBorderHit);
                }
                if (protocolVersion >= MINECRAFT_1_19) {
                    sequence = ProtocolUtil.readVarInt(buf);
                    sb.append("\n    Sequence: ").append(this.sequence);
                }
            }
        } catch (Exception e) {
            if(DebugUtil.enabled) log.info(sb.toString());
            if(ExceptionUtil.getRootCause(e) instanceof ProtocolizeException){
                log.error("Skipping decoding BlockPlacement packet: {}", e.getMessage());
            } else {
                log.error("Skipping decoding BlockPlacement packet", e);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        if(packetData != null && skipDecoding){
            buf.writeBytes(packetData);
            return;
        }

        if (protocolVersion < MINECRAFT_1_14) {
            BlockPositionSerializer.write(buf, position, protocolVersion);
            ProtocolUtil.writeVarInt(buf, face.protocolId());
            ProtocolUtil.writeVarInt(buf, hand.protocolId());
            buf.writeFloat(hitVecX);
            buf.writeFloat(hitVecY);
            buf.writeFloat(hitVecZ);
        } else {
            ProtocolUtil.writeVarInt(buf, hand.protocolId());
            BlockPositionSerializer.write(buf, position, protocolVersion);
            ProtocolUtil.writeVarInt(buf, face.protocolId());
            buf.writeFloat(hitVecX);
            buf.writeFloat(hitVecY);
            buf.writeFloat(hitVecZ);
            buf.writeBoolean(insideBlock);
            if(protocolVersion >= MINECRAFT_1_21_2) {
                buf.writeBoolean(worldBorderHit);
            }
            if (protocolVersion >= MINECRAFT_1_19) {
                ProtocolUtil.writeVarInt(buf, sequence);
            }
        }
    }

    private void readEntirePacket(ByteBuf buf){
        int index = buf.readerIndex();
        packetData = new byte[buf.readableBytes()];
        buf.readBytes(packetData);
        buf.readerIndex(index);
    }

}
