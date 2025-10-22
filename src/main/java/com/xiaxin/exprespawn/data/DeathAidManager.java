package com.xiaxin.exprespawn.data;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathAidManager {
    private static final Map<UUID, DeathAidRequest> activeRequests = new ConcurrentHashMap<>();
    // 记录哪些玩家已经响应过哪些援助请求（响应者ID -> 死亡玩家ID集合）
    private static final Map<UUID, Set<UUID>> respondedPlayers = new ConcurrentHashMap<>();
    
    public static void createAidRequest(ServerPlayer deadPlayer) {
        DeathAidRequest request = new DeathAidRequest(deadPlayer);
        activeRequests.put(deadPlayer.getUUID(), request);
        
        // 清理过期请求
        cleanupExpiredRequests();
    }
    
    public static DeathAidRequest getAidRequest(UUID playerId) {
        DeathAidRequest request = activeRequests.get(playerId);
        if (request != null && request.isExpired()) {
            activeRequests.remove(playerId);
            return null;
        }
        return request;
    }
    
    public static void removeAidRequest(UUID playerId) {
        activeRequests.remove(playerId);
        // 同时清理该死亡玩家的响应记录
        respondedPlayers.values().forEach(set -> set.remove(playerId));
    }
    
    public static void cleanupExpiredRequests() {
        Iterator<Map.Entry<UUID, DeathAidRequest>> iterator = activeRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DeathAidRequest> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }
    
    public static boolean hasActiveRequest(UUID playerId) {
        DeathAidRequest request = activeRequests.get(playerId);
        if (request != null && request.isExpired()) {
            activeRequests.remove(playerId);
            return false;
        }
        return request != null && request.isActive();
    }
    
    public static Collection<DeathAidRequest> getAllActiveRequests() {
        cleanupExpiredRequests();
        return new ArrayList<>(activeRequests.values());
    }
    
    /**
     * 检查玩家是否已经响应过指定的援助请求
     */
    public static boolean hasPlayerResponded(UUID responderId, UUID deadPlayerId) {
        Set<UUID> respondedSet = respondedPlayers.get(responderId);
        return respondedSet != null && respondedSet.contains(deadPlayerId);
    }
    
    /**
     * 记录玩家对援助请求的响应
     */
    public static void recordPlayerResponse(UUID responderId, UUID deadPlayerId) {
        respondedPlayers.computeIfAbsent(responderId, k -> ConcurrentHashMap.newKeySet()).add(deadPlayerId);
    }
    
    /**
     * 清理过期的响应记录
     */
    public static void cleanupExpiredResponses() {
        // 只保留活跃请求的响应记录
        Set<UUID> activeDeadPlayerIds = activeRequests.keySet();
        respondedPlayers.values().forEach(set -> set.retainAll(activeDeadPlayerIds));
        
        // 移除空的集合
        respondedPlayers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}