package com.xiaxin.exprespawn.handler;

import com.xiaxin.exprespawn.Config;
import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.DeathLocation;
import com.xiaxin.exprespawn.data.PlayerDeathData;
import com.xiaxin.exprespawn.network.ClearDeathLocationPacket;
import com.xiaxin.exprespawn.network.SyncDeathLocationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;

@EventBusSubscriber(modid = ExpRespawnRework.MODID)
public class PlayerDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!Config.RESPAWN_HERE_ENABLED.get()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // Store death location
            ResourceKey<Level> dimension = player.level().dimension();
            BlockPos pos = player.blockPosition();
            BlockPos deathPos = new BlockPos((int)(pos.getX() + 0.5), pos.getY(), (int)(pos.getZ() + 0.5));
            DeathLocation deathLocation = new DeathLocation(
                dimension,
                deathPos,
                player.getYRot(),
                player.getXRot()
            );
            
            PlayerDeathData.setDeathLocation(player, deathLocation);
            
            // Sync death location to client
            PacketDistributor.sendToPlayer(player, new SyncDeathLocationPacket(deathLocation));
            
            ExpRespawnRework.LOGGER.debug("Player {} died at {} in dimension {}", 
                player.getName().getString(), pos, dimension.location());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Handle unsafe respawn location first
            if (PlayerDeathData.hasUnsafeRespawnFlag(player)) {
                // This is an unsafe location respawn - show special message
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.unsafe_location"));
                
                // Check if this is a hardcore unsafe respawn that needs spectator mode handling
                boolean isHardcoreUnsafeRespawn = PlayerDeathData.hasHardcoreUnsafeRespawnFlag(player);
                
                // Clear the flags
                PlayerDeathData.clearUnsafeRespawnFlag(player);
                PlayerDeathData.clearHardcoreUnsafeRespawnFlag(player);
                PlayerDeathData.clearDeathLocation(player);
                PacketDistributor.sendToPlayer(player, new ClearDeathLocationPacket());
                
                ExpRespawnRework.LOGGER.info("Player {} respawned at original spawn due to unsafe death location", 
                    player.getName().getString());
                
                // If this was a hardcore unsafe respawn, handle spectator mode switching
                if (isHardcoreUnsafeRespawn) {
                    // Set player to spectator mode first
                    player.setGameMode(GameType.SPECTATOR);
                    
                    // Schedule task to switch back to survival after 0.5 seconds (10 ticks)
                    player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 10, () -> {
                        if (player.isAlive()) {
                            player.setGameMode(GameType.SURVIVAL);
                            ExpRespawnRework.LOGGER.info("Switched hardcore player {} back to survival mode after unsafe respawn (0.5s)", player.getName().getString());
                        }
                    }));
                    
                    ExpRespawnRework.LOGGER.info("Switched hardcore player {} to spectator mode for unsafe respawn (0.5s)", player.getName().getString());
                }
                
                return;
            }
            
            // Handle original spawn restoration after our mod's respawn
            if (PlayerDeathData.hasOriginalSpawnData(player)) {
                // This is a respawn triggered by our mod - restore original spawn point
                PlayerDeathData.OriginalSpawnData originalSpawn = PlayerDeathData.getOriginalSpawnData(player);
                
                // Restore the original spawn position
                player.setRespawnPosition(originalSpawn.dimension(), originalSpawn.position(), 0.0F, originalSpawn.forced(), false);
                
                // Clear the temporary data
                PlayerDeathData.clearOriginalSpawnData(player);
                PlayerDeathData.clearDeathLocation(player); // Also clear death location
                PacketDistributor.sendToPlayer(player, new ClearDeathLocationPacket());
                
                ExpRespawnRework.LOGGER.debug("Restored original spawn for player {} after respawn-here",
                    player.getName().getString());
                    
                // Send success message based on mode
                if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
                    String itemId = Config.RESPAWN_HERE_ITEM_ID.get();
                    int itemCount = Config.RESPAWN_HERE_ITEM_COUNT.get();
                    String itemName = getItemDisplayName(itemId);
                    player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.success.item", itemCount, itemName));
                } else {
                    int expCost = Config.RESPAWN_HERE_EXP_COST.get();
                    player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.success", expCost));
                }
                
                ExpRespawnRework.LOGGER.info("Player {} respawned at death location successfully", 
                    player.getName().getString());
                
                // Special handling for hardcore mode: switch to spectator mode first, then back to survival after 0.5 seconds
                boolean isHardcore = player.serverLevel().getLevelData().isHardcore();
                if (isHardcore) {
                    player.setGameMode(GameType.SPECTATOR);
                    
                    // Schedule task to switch back to survival after 0.5 seconds (10 ticks)
                    player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 10, () -> {
                        if (player.isAlive()) {
                            player.setGameMode(GameType.SURVIVAL);
                            ExpRespawnRework.LOGGER.info("Switched hardcore player {} back to survival mode after 0.5s", player.getName().getString());
                        }
                    }));
                    
                    ExpRespawnRework.LOGGER.info("Switched hardcore player {} to spectator mode for 0.5s", player.getName().getString());
                }
            } else if (PlayerDeathData.hasDeathLocation(player) && !event.isEndConquered()) {
                // This is a normal respawn, clear the death location
                PlayerDeathData.clearDeathLocation(player);
                PacketDistributor.sendToPlayer(player, new ClearDeathLocationPacket());
                
                ExpRespawnRework.LOGGER.debug("Cleared death location for player {} after normal respawn", 
                    player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Clear death location when player changes dimension
        PlayerDeathData.clearDeathLocation(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up when player logs out
        PlayerDeathData.clearDeathLocation(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Silently enable keepInventory for the player when they join the world
            // No message will be sent to the player
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                "gamerule keepInventory true"
            );
            ExpRespawnRework.LOGGER.info("Silently enabled keepInventory for player: {}", player.getName().getString());
        }
    }

    private static String getItemDisplayName(String itemId) {
        try {
            // Parse the item ID
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            
            // Get the item from the registry
            Item item = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
                .registryAccess()
                .registryOrThrow(Registries.ITEM)
                .get(itemLocation);
                
            if (item == null) {
                return formatItemId(itemId); // Fallback to formatted ID
            }
            
            // Get the localized name from the item's description
            return item.getDescription().getString();
            
        } catch (Exception e) {
            return formatItemId(itemId); // Fallback to formatted ID
        }
    }
    
    private static String formatItemId(String itemId) {
        // Fallback method to format the item ID into a readable string
        try {
            String[] parts = itemId.split(":");
            String itemName = parts.length > 1 ? parts[1] : itemId;
            // Convert snake_case or camelCase to readable format
            return itemName.replaceAll("([a-z])([A-Z])", "$1 $2")
                           .replaceAll("_", " ")
                           .toLowerCase();
        } catch (Exception e) {
            return itemId; // Final fallback to raw item ID
        }
    }
}