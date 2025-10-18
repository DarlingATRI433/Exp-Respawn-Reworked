package com.xiaxin.exprespawn;

import net.neoforged.neoforge.common.ModConfigSpec;

// Configuration for ExpRespawnRework mod
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Respawn Here feature configuration
    private static final ModConfigSpec.Builder RESPAWN_HERE_BUILDER = BUILDER
            .translation("exprespawnrework.config.section.respawn_here")
            .comment("Respawn Here Configuration")
            .push("respawn_here");

    public static final ModConfigSpec.IntValue RESPAWN_HERE_EXP_COST = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.expCost")
            .comment("Experience levels required for respawn here feature")
            .defineInRange("expCost", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_ENABLED = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.enabled")
            .comment("Enable respawn here feature")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_ALLOW_HARDCORE = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.allowInHardcore")
            .comment("Allow respawn here in hardcore mode")
            .define("allowInHardcore", false);

    public static final ModConfigSpec.BooleanValue RESPAWN_HERE_USE_ITEM_MODE = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.useItemMode")
            .comment("Use item consumption mode instead of experience levels")
            .define("useItemMode", false);

    public static final ModConfigSpec.ConfigValue<String> RESPAWN_HERE_ITEM_ID = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.itemId")
            .comment("Item ID to consume for respawn (e.g., minecraft:apple)")
            .define("itemId", "minecraft:apple");

    public static final ModConfigSpec.IntValue RESPAWN_HERE_ITEM_COUNT = RESPAWN_HERE_BUILDER
            .translation("exprespawnrework.config.respawn_here.itemCount")
            .comment("Number of items to consume for respawn")
            .defineInRange("itemCount", 1, 1, 64);

    static final ModConfigSpec SPEC = BUILDER.build();
}
