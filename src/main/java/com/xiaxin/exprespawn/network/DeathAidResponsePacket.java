package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public record DeathAidResponsePacket(UUID deadPlayerId, boolean acceptAid) implements CustomPacketPayload {
    
    public static final Type<DeathAidResponsePacket> TYPE = new Type<>(ExpRespawnRework.resource("death_aid_response"));
    
    public static final StreamCodec<FriendlyByteBuf, DeathAidResponsePacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUUID(packet.deadPlayerId);
            buf.writeBoolean(packet.acceptAid);
        },
        buf -> new DeathAidResponsePacket(
            buf.readUUID(),
            buf.readBoolean()
        )
    );

    public DeathAidResponsePacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readBoolean());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeathAidResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof net.minecraft.server.level.ServerPlayer responder) {
                // 处理援助响应
                com.xiaxin.exprespawn.handler.DeathAidHandler.handleAidResponse(
                    responder, packet.deadPlayerId, packet.acceptAid);
            }
        });
    }
}