package com.xiaxin.exprespawn.handler;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.Config;
import com.xiaxin.exprespawn.data.DeathAidManager;
import com.xiaxin.exprespawn.data.DeathAidRequest;
import com.xiaxin.exprespawn.data.PlayerDeathData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class DeathAidHandler {
    
    public static void handleAidResponse(ServerPlayer responder, UUID deadPlayerId, boolean acceptAid) {
        // 获取死亡玩家
        ServerPlayer deadPlayer = responder.getServer().getPlayerList().getPlayer(deadPlayerId);
        if (deadPlayer == null) {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.player_not_found"));
            return;
        }
        
        // 检查死亡玩家是否还有死亡位置数据（如果玩家已经自己重生，这个数据会被清除）
        if (!PlayerDeathData.hasDeathLocation(deadPlayer)) {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.request_expired"));
            return;
        }
        
        // 检查是否有活跃的援助请求
        DeathAidRequest aidRequest = DeathAidManager.getAidRequest(deadPlayerId);
        if (aidRequest == null) {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.request_expired"));
            return;
        }
        
        // 检查响应者是否已经响应过这个请求
        if (DeathAidManager.hasPlayerResponded(responder.getUUID(), deadPlayerId)) {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.already_responded"));
            return;
        }
        
        if (!acceptAid) {
            // 玩家选择不提供援助
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.declined", deadPlayer.getName().getString()));
            // 记录玩家已经响应过这个请求
            DeathAidManager.recordPlayerResponse(responder.getUUID(), deadPlayerId);
            return;
        }
        
        // 检查响应者是否有足够的资源
        if (!checkResponderResources(responder)) {
            return;
        }
        
        // 消耗响应者的资源
        if (!consumeResponderResources(responder)) {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.consume_failed"));
            return;
        }
        
        // 执行复活逻辑
        if (performAidRespawn(deadPlayer, responder)) {
            // 记录玩家已经响应过这个请求
            DeathAidManager.recordPlayerResponse(responder.getUUID(), deadPlayerId);
            
            // 移除援助请求
            DeathAidManager.removeAidRequest(deadPlayerId);
            
            // 发送成功消息
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.success", deadPlayer.getName().getString()));
            deadPlayer.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.respawned", responder.getName().getString()));
            
            ExpRespawnRework.LOGGER.info("玩家 {} 帮助 {} 原地复活", responder.getName().getString(), deadPlayer.getName().getString());
        } else {
            responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.failed"));
        }
    }
    
    private static boolean checkResponderResources(ServerPlayer responder) {
        if (Config.getBooleanValue(Config.RESPAWN_HERE_USE_ITEM_MODE, false)) {
            // 物品模式
            String itemId = Config.getStringValue(Config.RESPAWN_HERE_ITEM_ID, "minecraft:apple");
            int requiredCount = Config.getIntValue(Config.RESPAWN_HERE_ITEM_COUNT, 1);
            
            // 解析物品ID
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            Item item = responder.getServer().registryAccess()
                .registryOrThrow(Registries.ITEM)
                .get(itemLocation);
                
            if (item == null) {
                responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.invalid_item"));
                return false;
            }
            
            // 检查物品数量
            int playerCount = 0;
            for (ItemStack stack : responder.getInventory().items) {
                if (stack.getItem() == item) {
                    playerCount += stack.getCount();
                }
            }
            
            if (playerCount < requiredCount) {
                String itemName = item.getDescription().getString();
                responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.not_enough_items", requiredCount, itemName));
                return false;
            }
        } else {
            // 经验模式
            int requiredExp = Config.getIntValue(Config.RESPAWN_HERE_EXP_COST, 10);
            if (responder.experienceLevel < requiredExp) {
                responder.sendSystemMessage(Component.translatable("exprespawnrework.death_aid.not_enough_exp", requiredExp));
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean consumeResponderResources(ServerPlayer responder) {
        try {
            if (Config.getBooleanValue(Config.RESPAWN_HERE_USE_ITEM_MODE, false)) {
                // 物品模式
                String itemId = Config.getStringValue(Config.RESPAWN_HERE_ITEM_ID, "minecraft:apple");
                int requiredCount = Config.getIntValue(Config.RESPAWN_HERE_ITEM_COUNT, 1);
                
                // 解析物品ID
                ResourceLocation itemLocation = ResourceLocation.parse(itemId);
                Item item = responder.getServer().registryAccess()
                    .registryOrThrow(Registries.ITEM)
                    .get(itemLocation);
                
                if (item == null) {
                    return false;
                }
                
                // 消耗物品
                int remaining = requiredCount;
                for (ItemStack stack : responder.getInventory().items) {
                    if (stack.getItem() == item && remaining > 0) {
                        int toRemove = Math.min(stack.getCount(), remaining);
                        stack.shrink(toRemove);
                        remaining -= toRemove;
                        if (remaining <= 0) break;
                    }
                }
                
                return remaining <= 0;
            } else {
                // 经验模式
                int requiredExp = Config.getIntValue(Config.RESPAWN_HERE_EXP_COST, 10);
                responder.giveExperienceLevels(-requiredExp);
                return true;
            }
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("消耗援助者资源时出错: {}", e.getMessage());
            return false;
        }
    }
    
    private static boolean performAidRespawn(ServerPlayer deadPlayer, ServerPlayer responder) {
        try {
            // 使用现有的复活逻辑
            RespawnHereHandler.handleRespawnHere(deadPlayer);
            return true;
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("执行援助复活时出错: {}", e.getMessage());
            return false;
        }
    }
}