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
            // 物品模式
            if (Config.ITEM_CONSUMPTION_REQUIRE_ONLY.get()) {
                // 只需要物品存在模式，不消耗物品
                return hasRequiredItems(player);
            } else {
                // 正常消耗模式
                return hasRequiredItems(player);
            }
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
            
            // 检查是否为虚空环境
            ServerLevel level = player.getServer().getLevel(deathLocation.getDimension());
            boolean isVoidDeath = deathLocation.getPosition().getY() < level.getMinBuildHeight();
            
            if (isVoidDeath) {
                // 虚空环境死亡，发送特殊提示
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.void_location"));
                ExpRespawnRework.LOGGER.info("玩家 {} 死于虚空环境，重生至原始重生点", 
                    player.getName().getString());
            } else {
                // 其他不安全环境
                player.sendSystemMessage(Component.translatable("exprespawnrework.respawn_here.unsafe_location"));
                ExpRespawnRework.LOGGER.info("玩家 {} 死亡位置不安全，重生至原始重生点", 
                    player.getName().getString());
            }
            
            if (isHardcore) {
                // 极限模式下，在原始重生点重生但仍消耗资源
                ExpRespawnRework.LOGGER.info("极限模式玩家 {} 在不安全位置死亡，在原始重生点重生并消耗资源", 
                    player.getName().getString());
                
                // 即使在极限模式下也要为不安全位置消耗资源
                if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
                    // 物品模式
                    if (!Config.ITEM_CONSUMPTION_REQUIRE_ONLY.get()) {
                        // 只有在正常消耗模式下才消耗物品
                        consumeRequiredItems(player);
                    }
                } else {
                    // 扣除经验
                    int cost = Config.RESPAWN_HERE_EXP_COST.get();
                    player.giveExperienceLevels(-cost);
                }
                
                // 在原始重生点重生
                handleUnsafeRespawnLocation(player);
            } else {
                // 普通模式：在原始重生点重生且不消耗资源
                handleUnsafeRespawnLocation(player);
            }
            return;
        }
        
        // 安全位置死亡，记录日志
        ExpRespawnRework.LOGGER.info("玩家 {} 死亡位置安全，允许原地重生", 
            player.getName().getString());
        
        // Deduct cost based on mode
        if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            // 物品模式
            if (Config.ITEM_CONSUMPTION_REQUIRE_ONLY.get()) {
                // 只需要物品存在模式，不消耗物品
                ExpRespawnRework.LOGGER.info("玩家 {} 使用物品检测模式原地重生，不消耗物品", 
                    player.getName().getString());
            } else {
                // 正常消耗模式
                consumeRequiredItems(player);
            }
        } else {
            // 经验模式
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
     * 修改逻辑：允许在空中死亡时原地重生，但保持虚空检测不变
     * 
     * 环境检测规则：
     * - 虚空环境（Y坐标低于世界最小高度）：不允许原地重生
     * - 空中环境（下方是空气）：允许原地重生（修改后的逻辑）
     * - 熔岩环境：不允许原地重生
     * - 火环境：不允许原地重生  
     * - 水环境：不允许原地重生
     * - 超出世界高度：不允许原地重生
     */
    private static boolean isRespawnLocationSafe(ServerPlayer player, DeathLocation deathLocation) {
        try {
            ServerLevel level = player.getServer().getLevel(deathLocation.getDimension());
            if (level == null) {
                return false;
            }
            
            BlockPos pos = deathLocation.getPosition();
            
            // 检查是否在虚空中（Y坐标低于世界最小高度）
            if (pos.getY() < level.getMinBuildHeight()) {
                return false;
            }
            
            // 检查是否超出世界最大高度
            if (pos.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            
            // 检查当前位置和下方位置的方块状态
            BlockState blockState = level.getBlockState(pos);
            BlockState blockBelow = level.getBlockState(pos.below());
            
            // 检查是否站在熔岩中
            if (blockState.getBlock() == Blocks.LAVA) {
                return false;
            }
            
            // 检查是否站在火中
            if (blockState.getBlock() == Blocks.FIRE) {
                return false;
            }
            
            // 检查是否在水中（应该触发原始重生点）
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty() && fluidState.isSource()) {
                return false;
            }
            
            // 检查下方方块是否足够坚固可以站立
            if (!blockBelow.isSolidRender(level, pos.below())) {
                // 修改逻辑：允许在空中重生，只检测真正的危险方块
                if (blockBelow.getBlock() == Blocks.LAVA || blockBelow.getBlock() == Blocks.FIRE) {
                    return false;
                }
                // 移除对空气的检测，允许在空中重生
            }
            
            return true;
            
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("检查重生位置安全性时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Handle unsafe respawn location by respawning at original spawn point without consuming resources
     * 处理不安全重生位置，在原始重生点重生
     */
    private static void handleUnsafeRespawnLocation(ServerPlayer player) {
        try {
            ExpRespawnRework.LOGGER.info("玩家 {} 的死亡位置不安全，在原始重生点重生", 
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