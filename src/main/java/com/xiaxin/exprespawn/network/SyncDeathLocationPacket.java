package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.ClientDeathData;
import com.xiaxin.exprespawn.data.DeathLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncDeathLocationPacket(DeathLocation deathLocation) implements CustomPacketPayload {
    
    public static final Type<SyncDeathLocationPacket> TYPE = new Type<>(ExpRespawnRework.resource("sync_death_location"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncDeathLocationPacket> STREAM_CODEC = StreamCodec.of(
        SyncDeathLocationPacket::write,
        SyncDeathLocationPacket::new
    );

    public SyncDeathLocationPacket(FriendlyByteBuf buf) {
        this(read(buf));
    }

    private static DeathLocation read(FriendlyByteBuf buf) {
        String dimensionStr = buf.readUtf();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        
        ResourceKey<Level> dimension = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.parse(dimensionStr)
        );
        BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
        
        return new DeathLocation(dimension, pos, yRot, xRot);
    }

    private static void write(FriendlyByteBuf buf, SyncDeathLocationPacket packet) {
        buf.writeUtf(packet.deathLocation().getDimension().location().toString());
        buf.writeDouble(packet.deathLocation().getPosition().getX());
        buf.writeDouble(packet.deathLocation().getPosition().getY());
        buf.writeDouble(packet.deathLocation().getPosition().getZ());
        buf.writeFloat(packet.deathLocation().getYRot());
        buf.writeFloat(packet.deathLocation().getXRot());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDeathLocationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // On client, update client death data
                ClientDeathData.setDeathLocation(packet.deathLocation());
                ExpRespawnRework.LOGGER.debug("Received death location from server: {}", packet.deathLocation());
            }
        });
    }
}