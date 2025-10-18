package com.xiaxin.exprespawn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ExpRespawnConfig {
    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_ENABLED;
    public static final ModConfigSpec.IntValue RESPAWN_HERE_COST;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.comment("ExpRespawnRework Configuration")
               .push("respawn_here");
        
        RESPAWN_HERE_ENABLED = builder
            .comment("Enable respawn here feature")
            .define("enabled", true);
            
        RESPAWN_HERE_COST = builder
            .comment("Experience levels required for respawn here feature")
            .defineInRange("cost", 10, 0, Integer.MAX_VALUE);
        
        builder.pop();
    }
}