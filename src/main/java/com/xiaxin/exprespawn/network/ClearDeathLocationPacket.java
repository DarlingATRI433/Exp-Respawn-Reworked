package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.ClientDeathData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClearDeathLocationPacket() implements CustomPacketPayload {
    
    public static final Type<ClearDeathLocationPacket> TYPE = new Type<>(ExpRespawnRework.resource("clear_death_location"));
    
    public static final StreamCodec<FriendlyByteBuf, ClearDeathLocationPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {}, // No data to write
        buf -> new ClearDeathLocationPacket()
    );

    public ClearDeathLocationPacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearDeathLocationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // On client, clear death data
                ClientDeathData.clearDeathLocation();
                ExpRespawnRework.LOGGER.debug("Cleared death location on client");
            }
        });
    }
}