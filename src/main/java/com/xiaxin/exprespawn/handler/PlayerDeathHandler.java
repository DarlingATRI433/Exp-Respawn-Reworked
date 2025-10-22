package com.xiaxin.exprespawn.handler;

import com.xiaxin.exprespawn.Config;
import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.DeathLocation;
import com.xiaxin.exprespawn.data.PlayerDeathData;
import com.xiaxin.exprespawn.network.ClearDeathLocationPacket;
import com.xiaxin.exprespawn.network.SyncDeathLocationPacket;
import com.xiaxin.exprespawn.network.DeathAidRequestPacket;
import com.xiaxin.exprespawn.data.DeathAidManager;
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
        if (!Config.getBooleanValue(Config.RESPAWN_HERE_ENABLED, true)) {
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
            
            // 创建死亡援助请求并广播给所有玩家（如果启用）
            if (Config.getBooleanValue(Config.DEATH_AID_ENABLED, true) && 
                Config.getBooleanValue(Config.DEATH_AID_SHOW_MESSAGES, true)) {
                DeathAidManager.createAidRequest(player);
                DeathAidRequestPacket aidRequestPacket = new DeathAidRequestPacket(player.getUUID(), player.getName().getString());
                PacketDistributor.sendToAllPlayers(aidRequestPacket);
            }
            
            ExpRespawnRework.LOGGER.debug("Player {} died at {} in dimension {}", 
                player.getName().getString(), pos, dimension.location());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Handle unsafe respawn location first
            if (PlayerDeathData.hasUnsafeRespawnFlag(player)) {
                // This is an unsafe location respawn - message was already sent in RespawnHereHandler
                // so we don't send it again here to avoid duplicate messages
                
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
                
                // 重要：恢复原始重生点数据，确保下次死亡时还能回到正确的重生点
                if (PlayerDeathData.hasOriginalSpawnData(player)) {
                    PlayerDeathData.OriginalSpawnData originalSpawn = PlayerDeathData.getOriginalSpawnData(player);
                    player.setRespawnPosition(originalSpawn.dimension(), originalSpawn.position(), 0.0F, originalSpawn.forced(), false);
                    PlayerDeathData.clearOriginalSpawnData(player);
                    ExpRespawnRework.LOGGER.debug("Restored original spawn data for player {} after unsafe respawn", player.getName().getString());
                }
                
                // 检查是否有待传送的死亡位置（水中/岩浆中/半砖上死亡的情况）
                if (PlayerDeathData.hasPendingTeleportLocation(player)) {
                    DeathLocation deathLocation = PlayerDeathData.getPendingTeleportLocation(player);
                    if (deathLocation != null) {
                        // 延迟1秒后执行传送命令，确保玩家完全重生
                        player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 20, () -> {
                            if (player.isAlive()) {
                                // 构建传送命令：/execute in <维度> run tp <玩家名> x y z
                                ResourceKey<Level> deathDimension = deathLocation.getDimension();
                                String dimensionId;
                                
                                // 根据维度类型获取正确的维度ID
                                if (deathDimension == Level.OVERWORLD) {
                                    dimensionId = "overworld";
                                } else if (deathDimension == Level.NETHER) {
                                    dimensionId = "the_nether";
                                } else if (deathDimension == Level.END) {
                                    dimensionId = "the_end";
                                } else {
                                    // 对于模组维度，使用location的path部分
                                    dimensionId = deathDimension.location().getPath();
                                }
                                
                                String playerName = player.getName().getString();
                                BlockPos pos = deathLocation.getPosition();
                                String command = String.format("execute in %s run tp %s %d %d %d", 
                                    dimensionId, playerName, pos.getX(), pos.getY(), pos.getZ());
                                
                                ExpRespawnRework.LOGGER.info("执行传送命令: {} (玩家 {} 从维度 {} 传送到 {} 在维度 {})", 
                                    command, playerName, player.level().dimension().location(), pos, dimensionId);
                                
                                // 执行命令
                                player.getServer().getCommands().performPrefixedCommand(
                                    player.getServer().createCommandSourceStack(),
                                    command
                                );
                                
                                PlayerDeathData.clearPendingTeleportLocation(player);
                                ExpRespawnRework.LOGGER.info("已将玩家 {} 传送回死亡地点: {} 在维度 {}", 
                                    playerName, pos, dimensionId);
                            }
                        }));
                        
                        ExpRespawnRework.LOGGER.info("计划将玩家 {} 传送回死亡地点", player.getName().getString());
                    } else {
                        PlayerDeathData.clearPendingTeleportLocation(player);
                    }
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
                if (Config.getBooleanValue(Config.RESPAWN_HERE_USE_ITEM_MODE, false)) {
                    String itemId = Config.getStringValue(Config.RESPAWN_HERE_ITEM_ID, "minecraft:apple");
                    int itemCount = Config.getIntValue(Config.RESPAWN_HERE_ITEM_COUNT, 1);
                    String itemName = getItemDisplayName(itemId);
                    player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.success.item", itemCount, itemName));
                } else {
                    int expCost = Config.getIntValue(Config.RESPAWN_HERE_EXP_COST, 10);
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