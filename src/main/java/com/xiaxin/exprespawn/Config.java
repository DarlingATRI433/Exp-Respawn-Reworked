package com.xiaxin.exprespawn;

import net.neoforged.neoforge.common.ModConfigSpec;

// Configuration for ExpRespawnRework mod
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // 安全获取配置值的方法
    public static boolean getBooleanValue(ModConfigSpec.BooleanValue configValue, boolean defaultValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            // 配置未加载时返回默认值
            return defaultValue;
        }
    }
    
    public static int getIntValue(ModConfigSpec.IntValue configValue, int defaultValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            // 配置未加载时返回默认值
            return defaultValue;
        }
    }
    
    public static String getStringValue(ModConfigSpec.ConfigValue<String> configValue, String defaultValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            // 配置未加载时返回默认值
            return defaultValue;
        }
    }

    // Respawn Here feature configuration
    static {
        BUILDER
                .translation("exprespawnrework.config.section.respawn_here")
                .comment("Respawn Here Configuration")
                .push("respawn_here");
    }

    public static final ModConfigSpec.IntValue RESPAWN_HERE_EXP_COST = BUILDER
            .translation("exprespawnrework.config.respawn_here.expCost")
            .comment("Experience levels required for respawn here feature")
            .defineInRange("expCost", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_ENABLED = BUILDER
            .translation("exprespawnrework.config.respawn_here.enabled")
            .comment("Enable respawn here feature")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_ALLOW_HARDCORE = BUILDER
            .translation("exprespawnrework.config.respawn_here.allowInHardcore")
            .comment("Allow respawn here in hardcore mode")
            .define("allowInHardcore", false);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_USE_ITEM_MODE = BUILDER
            .translation("exprespawnrework.config.respawn_here.useItemMode")
            .comment("Use item consumption mode instead of experience levels")
            .define("useItemMode", false);

    public static final ModConfigSpec.ConfigValue<String> RESPAWN_HERE_ITEM_ID = BUILDER
            .translation("exprespawnrework.config.respawn_here.itemId")
            .comment("Item ID to consume for respawn (e.g., minecraft:apple)")
            .define("itemId", "minecraft:apple");

    public static final ModConfigSpec.IntValue RESPAWN_HERE_ITEM_COUNT = BUILDER
            .translation("exprespawnrework.config.respawn_here.itemCount")
            .comment("Number of items to consume for respawn")
            .defineInRange("itemCount", 1, 1, 64);

    // Disable vanilla respawn button - forces players to use the "respawn here" feature
    public static final ModConfigSpec.BooleanValue DISABLE_VANILLA_RESPAWN = BUILDER
            .translation("exprespawnrework.config.respawn_here.disableVanillaRespawn")
            .comment("Disable vanilla respawn button, force players to use respawn here feature")
            .define("disableVanillaRespawn", false);

    // Item-only check mode - in item mode, just check inventory presence without consuming items
    public static final ModConfigSpec.BooleanValue ITEM_CONSUMPTION_REQUIRE_ONLY = BUILDER
            .translation("exprespawnrework.config.respawn_here.itemConsumptionRequireOnly")
            .comment("In item mode, only require items to be present in inventory (no consumption)")
            .define("itemConsumptionRequireOnly", false);

    // Death Aid feature configuration
    static {
        BUILDER.pop(); // Close respawn_here section
        BUILDER
                .translation("exprespawnrework.config.section.death_aid")
                .comment("Death Aid Configuration")
                .push("death_aid");
    }

    public static final ModConfigSpec.BooleanValue DEATH_AID_ENABLED = BUILDER
            .translation("exprespawnrework.config.death_aid.enabled")
            .comment("Enable death aid feature - allows other players to help respawn dead players")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue DEATH_AID_SHOW_MESSAGES = BUILDER
            .translation("exprespawnrework.config.death_aid.showMessages")
            .comment("Show death aid request messages to other players when someone dies")
            .define("showMessages", true);

    public static final ModConfigSpec.IntValue DEATH_AID_REQUEST_TIMEOUT = BUILDER
            .translation("exprespawnrework.config.death_aid.requestTimeout")
            .comment("Death aid request timeout in seconds")
            .defineInRange("requestTimeout", 60, 10, 300);

    static final ModConfigSpec SPEC = BUILDER.pop().build();
}
