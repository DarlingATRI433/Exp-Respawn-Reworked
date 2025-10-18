package com.xiaxin.exprespawn.data;

import com.xiaxin.exprespawn.ExpRespawnRework;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathData {
    private static final Map<UUID, DeathLocation> playerDeathLocations = new HashMap<>();
    private static final Map<UUID, OriginalSpawnData> originalSpawnData = new HashMap<>();
    private static final Map<UUID, Boolean> unsafeRespawnFlags = new HashMap<>();
    private static final Map<UUID, Boolean> hardcoreUnsafeRespawnFlags = new HashMap<>();

    public static void setDeathLocation(Player player, DeathLocation location) {
        playerDeathLocations.put(player.getUUID(), location);
        ExpRespawnRework.LOGGER.debug("Set death location for player {}: {}", player.getName().getString(), location);
    }

    public static DeathLocation getDeathLocation(Player player) {
        return playerDeathLocations.get(player.getUUID());
    }

    public static void clearDeathLocation(Player player) {
        playerDeathLocations.remove(player.getUUID());
        ExpRespawnRework.LOGGER.debug("Cleared death location for player {}", player.getName().getString());
    }

    public static boolean hasDeathLocation(Player player) {
        return playerDeathLocations.containsKey(player.getUUID());
    }

    public static void clearAll() {
        playerDeathLocations.clear();
        originalSpawnData.clear();
        unsafeRespawnFlags.clear();
        hardcoreUnsafeRespawnFlags.clear();
    }

    // Unsafe respawn flag management
    public static void setUnsafeRespawnFlag(Player player, boolean flag) {
        unsafeRespawnFlags.put(player.getUUID(), flag);
        ExpRespawnRework.LOGGER.debug("Set unsafe respawn flag for player {}: {}", player.getName().getString(), flag);
    }
    
    public static boolean hasUnsafeRespawnFlag(Player player) {
        return unsafeRespawnFlags.containsKey(player.getUUID()) && unsafeRespawnFlags.get(player.getUUID());
    }
    
    public static void clearUnsafeRespawnFlag(Player player) {
        unsafeRespawnFlags.remove(player.getUUID());
        ExpRespawnRework.LOGGER.debug("Cleared unsafe respawn flag for player {}", player.getName().getString());
    }
    
    // Hardcore unsafe respawn flag management
    public static void setHardcoreUnsafeRespawnFlag(Player player, boolean flag) {
        hardcoreUnsafeRespawnFlags.put(player.getUUID(), flag);
        ExpRespawnRework.LOGGER.debug("Set hardcore unsafe respawn flag for player {}: {}", player.getName().getString(), flag);
    }
    
    public static boolean hasHardcoreUnsafeRespawnFlag(Player player) {
        return hardcoreUnsafeRespawnFlags.containsKey(player.getUUID()) && hardcoreUnsafeRespawnFlags.get(player.getUUID());
    }
    
    public static void clearHardcoreUnsafeRespawnFlag(Player player) {
        hardcoreUnsafeRespawnFlags.remove(player.getUUID());
        ExpRespawnRework.LOGGER.debug("Cleared hardcore unsafe respawn flag for player {}", player.getName().getString());
    }
    
    // Original spawn data management
    public static void setOriginalSpawnData(Player player, BlockPos spawnPos, boolean forced, Level spawnDimension) {
        originalSpawnData.put(player.getUUID(), new OriginalSpawnData(spawnPos, forced, spawnDimension.dimension()));

        ExpRespawnRework.LOGGER.debug("Set original spawn data for player {}: pos={}, forced={}, dimension={}",
                player.getName().getString(), spawnPos, forced, spawnDimension.dimension());
    }
    
    public static OriginalSpawnData getOriginalSpawnData(Player player) {
        return originalSpawnData.get(player.getUUID());
    }
    
    public static boolean hasOriginalSpawnData(Player player) {
        return originalSpawnData.containsKey(player.getUUID());
    }
    
    public static void clearOriginalSpawnData(Player player) {
        originalSpawnData.remove(player.getUUID());
        ExpRespawnRework.LOGGER.debug("Cleared original spawn data for player {}", player.getName().getString());
    }
    
    // Record for storing original spawn data
    public record OriginalSpawnData(BlockPos position, boolean forced, ResourceKey<Level> dimension) {}
}