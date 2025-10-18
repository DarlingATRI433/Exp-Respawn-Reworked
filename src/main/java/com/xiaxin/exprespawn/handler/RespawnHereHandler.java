package com.xiaxin.exprespawn.handler;

import com.xiaxin.exprespawn.Config;
import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.DeathLocation;
import com.xiaxin.exprespawn.data.PlayerDeathData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.Blocks;
public class RespawnHereHandler {

    public static boolean canRespawnHere(ServerPlayer player) {
        if (!Config.RESPAWN_HERE_ENABLED.get()) {
            return false;
        }

        // Check if in hardcore mode and hardcore respawn is not allowed
        boolean isHardcore = player.serverLevel().getLevelData().isHardcore();
        if (isHardcore && !Config.RESPAWN_HERE_ALLOW_HARDCORE.get()) {
            return false;
        }

        if (!PlayerDeathData.hasDeathLocation(player)) {
            return false;
        }

        // Check requirements based on mode
        if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            // Item consumption mode
            return hasRequiredItems(player);
        } else {
            // Experience consumption mode
            int requiredExp = Config.RESPAWN_HERE_EXP_COST.get();
            return player.experienceLevel >= requiredExp;
        }
    }

    public static void handleRespawnHere(ServerPlayer player) {
        if (!canRespawnHere(player)) {
            // Check specific reasons and send appropriate messages
            boolean isHardcore = player.serverLevel().getLevelData().isHardcore();
            if (isHardcore && !Config.RESPAWN_HERE_ALLOW_HARDCORE.get()) {
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.hardcore_not_allowed"));
            } else if (!PlayerDeathData.hasDeathLocation(player)) {
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.no_location"));
            } else if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.not_enough_items"));
            } else {
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.not_enough_exp"));
            }
            return;
        }
        
        // Get death location
        DeathLocation deathLocation = PlayerDeathData.getDeathLocation(player);
        if (deathLocation == null) {
            player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.no_location"));
            return;
        }
        
        // Check if death location is safe for respawning
        if (!isRespawnLocationSafe(player, deathLocation)) {
            // Death location is unsafe
            boolean isHardcore = player.serverLevel().getLevelData().isHardcore();
            
            if (isHardcore) {
                // In hardcore mode, respawn at original spawn point but still consume resources
                ExpRespawnRework.LOGGER.info("Hardcore player {} died in unsafe location, respawning at original spawn point with resource consumption", 
                    player.getName().getString());
                
                // Consume resources even for unsafe location in hardcore mode
                if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
                    // Consume items
                    consumeRequiredItems(player);
                } else {
                    // Deduct experience
                    int cost = Config.RESPAWN_HERE_EXP_COST.get();
                    player.giveExperienceLevels(-cost);
                }
                
                // Respawn at original spawn point
                handleUnsafeRespawnLocation(player);
            } else {
                // Normal mode: respawn at original spawn point without consuming resources
                handleUnsafeRespawnLocation(player);
            }
            return;
        }
        
        // Deduct cost based on mode
        if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            // Consume items
            consumeRequiredItems(player);
        } else {
            // Deduct experience
            int cost = Config.RESPAWN_HERE_EXP_COST.get();
            player.giveExperienceLevels(-cost);
        }
        
        // Store original spawn position for restoration after respawn
        BlockPos originalSpawn = player.getRespawnPosition();
        ResourceKey<Level> originalDimension = player.getRespawnDimension();
        float originalAngle = player.getRespawnAngle();
        boolean originalForced = player.isRespawnForced();
        
        // Store this data temporarily for post-respawn cleanup
        PlayerDeathData.setOriginalSpawnData(player, originalSpawn, originalForced, player.getServer().getLevel(originalDimension));

        // Set temporary respawn point at death location - this is the key!
        // This tells Minecraft to respawn the player at the death location
        player.setRespawnPosition(deathLocation.getDimension(), deathLocation.getPosition(), deathLocation.getYRot(), true, false);

        // Trigger native respawn through player connection - let Minecraft handle the entire respawn process
        player.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        
        // Note: The actual respawn will happen asynchronously, we'll restore the original spawn point in the PlayerRespawnEvent
    }

    private static void sendNotEnoughExpMessage(Player player) {
        Component message = Component.translatable("exprespawnrework.respawn_here.not_enough_exp");
        player.sendSystemMessage(message);
    }

    private static boolean hasRequiredItems(ServerPlayer player) {
        if (!Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            return false;
        }
        
        String itemId = Config.RESPAWN_HERE_ITEM_ID.get();
        int requiredCount = Config.RESPAWN_HERE_ITEM_COUNT.get();
        
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            Item requiredItem = player.server.registryAccess().registryOrThrow(Registries.ITEM).get(itemLocation);
            
            if (requiredItem == null) {
                ExpRespawnRework.LOGGER.error("Configured item not found: {}", itemId);
                return false;
            }
            
            int foundCount = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.is(requiredItem)) {
                    foundCount += stack.getCount();
                    if (foundCount >= requiredCount) {
                        return true;
                    }
                }
            }
            
            return foundCount >= requiredCount;
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("Error checking for required items: {}", e.getMessage());
            return false;
        }
    }

    private static void consumeRequiredItems(ServerPlayer player) {
        if (!Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            return;
        }
        
        String itemId = Config.RESPAWN_HERE_ITEM_ID.get();
        int requiredCount = Config.RESPAWN_HERE_ITEM_COUNT.get();
        
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            Item requiredItem = player.server.registryAccess().registryOrThrow(Registries.ITEM).get(itemLocation);
            
            if (requiredItem == null) {
                ExpRespawnRework.LOGGER.error("Configured item not found: {}", itemId);
                return;
            }
            
            int remainingToConsume = requiredCount;
            
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.is(requiredItem)) {
                    int consumeFromStack = Math.min(remainingToConsume, stack.getCount());
                    stack.shrink(consumeFromStack);
                    remainingToConsume -= consumeFromStack;
                    
                    if (remainingToConsume <= 0) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("Error consuming required items: {}", e.getMessage());
        }
    }

    /**
     * Check if the death location is safe for respawning
     */
    private static boolean isRespawnLocationSafe(ServerPlayer player, DeathLocation deathLocation) {
        try {
            ServerLevel level = player.getServer().getLevel(deathLocation.getDimension());
            if (level == null) {
                return false;
            }
            
            BlockPos pos = deathLocation.getPosition();
            
            // Check if position is in void (Y < level min build height)
            if (pos.getY() < level.getMinBuildHeight()) {
                return false;
            }
            
            // Check if position is above world height
            if (pos.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            
            // Check the block at the position and below
            BlockState blockState = level.getBlockState(pos);
            BlockState blockBelow = level.getBlockState(pos.below());
            
            // Check if standing in lava
            if (blockState.getBlock() == Blocks.LAVA) {
                return false;
            }
            
            // Check if standing in fire
            if (blockState.getBlock() == Blocks.FIRE) {
                return false;
            }
            
            // Check if in water/ocean (should trigger original spawn)
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty() && fluidState.isSource()) {
                return false;
            }
            
            // Check if the block below is solid enough to stand on
            if (!blockBelow.isSolidRender(level, pos.below())) {
                // Check if it's a dangerous block
                if (blockBelow.isAir() || blockBelow.getBlock() == Blocks.LAVA || blockBelow.getBlock() == Blocks.FIRE) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("Error checking respawn location safety: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Handle unsafe respawn location by respawning at original spawn point without consuming resources
     */
    private static void handleUnsafeRespawnLocation(ServerPlayer player) {
        try {
            ExpRespawnRework.LOGGER.info("Death location is unsafe for player {}, respawning at original spawn point", 
                player.getName().getString());
            
            // Store original spawn data for restoration after respawn
            BlockPos originalSpawn = player.getRespawnPosition();
            ResourceKey<Level> originalDimension = player.getRespawnDimension();
            float originalAngle = player.getRespawnAngle();
            boolean originalForced = player.isRespawnForced();
            
            // Store this data temporarily for post-respawn cleanup
            PlayerDeathData.setOriginalSpawnData(player, originalSpawn, originalForced, player.getServer().getLevel(originalDimension));

            // Set flag to indicate this is an unsafe location respawn
            PlayerDeathData.setUnsafeRespawnFlag(player, true);
            
            // Special flag for hardcore mode unsafe respawn - needs spectator mode handling
            boolean isHardcore = player.serverLevel().getLevelData().isHardcore();
            if (isHardcore) {
                PlayerDeathData.setHardcoreUnsafeRespawnFlag(player, true);
            }

            // Trigger native respawn through player connection
            player.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("Error handling unsafe respawn location: {}", e.getMessage());
            // Fallback to normal respawn
            player.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
    }
}