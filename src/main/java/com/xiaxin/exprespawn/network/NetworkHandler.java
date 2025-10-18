package com.xiaxin.exprespawn.network;

import com.xiaxin.exprespawn.ExpRespawnRework;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ExpRespawnRework.MODID);
        
        // Register serverbound packet (client to server)
        registrar.playToServer(
                RespawnHerePacket.TYPE,
                RespawnHerePacket.STREAM_CODEC,
                (packet, context) -> RespawnHerePacket.handle(packet, context)
        );
        
        // Register clientbound packet (server to client)
        registrar.playToClient(
            SyncDeathLocationPacket.TYPE,
            SyncDeathLocationPacket.STREAM_CODEC,
            SyncDeathLocationPacket::handle
        );
        
        // Register clear death location packet
        registrar.playToClient(
            ClearDeathLocationPacket.TYPE,
            ClearDeathLocationPacket.STREAM_CODEC,
            ClearDeathLocationPacket::handle
        );
    }
}