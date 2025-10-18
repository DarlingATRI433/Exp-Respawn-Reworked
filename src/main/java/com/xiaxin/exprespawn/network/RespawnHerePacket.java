package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.handler.RespawnHereHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RespawnHerePacket implements CustomPacketPayload {
    
    public static final Type<RespawnHerePacket> TYPE = new Type<>(ExpRespawnRework.resource("respawn_here"));
    public static final StreamCodec<FriendlyByteBuf, RespawnHerePacket> STREAM_CODEC = StreamCodec.ofMember(
            RespawnHerePacket::encode,
            RespawnHerePacket::new
    );
    
    public RespawnHerePacket() {
        // Empty constructor for packet
    }

    public RespawnHerePacket(FriendlyByteBuf buf) {
        // No data to read for this packet
    }

    public void encode(FriendlyByteBuf buf) {
        // No data to write for this packet
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static RespawnHerePacket create() {
        return new RespawnHerePacket();
    }

    public static void handle(RespawnHerePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                try {
                    RespawnHereHandler.handleRespawnHere(player);
                } catch (Exception e) {
                    ExpRespawnRework.LOGGER.error("Error handling respawn here packet for player {}", 
                            player.getName().getString(), e);
                }
            }
        });
        
        // Packet is automatically marked as handled in IPayloadContext
    }
}