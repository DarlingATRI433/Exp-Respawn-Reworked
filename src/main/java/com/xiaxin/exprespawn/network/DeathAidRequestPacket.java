package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.DeathAidRequest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public record DeathAidRequestPacket(UUID deadPlayerId, String deadPlayerName) implements CustomPacketPayload {
    
    public static final Type<DeathAidRequestPacket> TYPE = new Type<>(ExpRespawnRework.resource("death_aid_request"));
    
    public static final StreamCodec<FriendlyByteBuf, DeathAidRequestPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUUID(packet.deadPlayerId);
            buf.writeUtf(packet.deadPlayerName);
        },
        buf -> new DeathAidRequestPacket(
            buf.readUUID(),
            buf.readUtf()
        )
    );

    public DeathAidRequestPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeathAidRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // 在客户端显示援助请求消息
                com.xiaxin.exprespawn.client.DeathAidClientHandler.showAidRequest(packet.deadPlayerId, packet.deadPlayerName);
            }
        });
    }
}