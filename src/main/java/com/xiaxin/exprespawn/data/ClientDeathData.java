package com.xiaxin.exprespawn.data;

import com.xiaxin.exprespawn.ExpRespawnRework;

public class ClientDeathData {
    private static DeathLocation clientDeathLocation = null;

    public static void setDeathLocation(DeathLocation location) {
        clientDeathLocation = location;
        ExpRespawnRework.LOGGER.debug("Set client death location: {}", location);
    }

    public static DeathLocation getDeathLocation() {
        return clientDeathLocation;
    }

    public static void clearDeathLocation() {
        clientDeathLocation = null;
        ExpRespawnRework.LOGGER.debug("Cleared client death location");
    }

    public static boolean hasDeathLocation() {
        return clientDeathLocation != null;
    }
}