package com.xiaxin.exprespawn;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.resources.ResourceLocation;
import com.xiaxin.exprespawn.network.NetworkHandler;
import com.xiaxin.exprespawn.command.DeathAidCommand;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExpRespawnRework.MODID)
public class ExpRespawnRework {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "exprespawnrework";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Helper method to create ResourceLocation with mod ID
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ExpRespawnRework(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExpRespawnRework) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        
        // Register network packets
        modEventBus.addListener(NetworkHandler::register);
        
        // Register command event
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("ExpRespawnRework mod loaded successfully!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Register death aid command
        DeathAidCommand.register(event.getDispatcher());
        LOGGER.info("Registered death aid command");
    }
}
