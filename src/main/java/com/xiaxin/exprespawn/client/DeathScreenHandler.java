package com.xiaxin.exprespawn.client;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.Config;
import com.xiaxin.exprespawn.data.ClientDeathData;
import com.xiaxin.exprespawn.network.RespawnHerePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ExpRespawnRework.MODID, value = Dist.CLIENT)
public class DeathScreenHandler {

    private static Button respawnHereButton = null;

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof DeathScreen && Config.RESPAWN_HERE_ENABLED.get()) {
            // Death screen is opening, we'll handle button creation in the init event
            
            // Check if we have death location data from server
            if (!ClientDeathData.hasDeathLocation()) {
                // If no death location, request it from server (will be handled by network)
                ExpRespawnRework.LOGGER.debug("No death location data available");
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof DeathScreen deathScreen && Config.RESPAWN_HERE_ENABLED.get()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            // Check if player has death location
            if (!ClientDeathData.hasDeathLocation()) {
                return;
            }

            // Create respawn here button with appropriate text based on mode
            Component buttonText;
            if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
                // Item consumption mode
                String itemId = Config.RESPAWN_HERE_ITEM_ID.get();
                int itemCount = Config.RESPAWN_HERE_ITEM_COUNT.get();
                String itemName = getItemDisplayName(itemId);
                buttonText = Component.translatable("exprespawnrework.respawn_here.button.item", itemCount, itemName);
            } else {
                // Experience consumption mode
                int requiredExp = Config.RESPAWN_HERE_EXP_COST.get();
                buttonText = Component.translatable("exprespawnrework.respawn_here.button", requiredExp);
            }
            
            respawnHereButton = Button.builder(buttonText, DeathScreenHandler::onRespawnHereClicked)
                    .bounds(deathScreen.width / 2 - 100, deathScreen.height / 4 + 120, 200, 20)
                    .build();

            event.addListener(respawnHereButton);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof DeathScreen && respawnHereButton != null) {
            // Update button state based on player's current resources
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                boolean canRespawn = checkCanRespawnClient(minecraft.player);
                respawnHereButton.active = canRespawn;
                
                // Update tooltip if cannot respawn
                if (!canRespawn) {
                    String tooltipKey = Config.RESPAWN_HERE_USE_ITEM_MODE.get() ? 
                        "exprespawnrework.respawn_here.not_enough_items" : 
                        "exprespawnrework.respawn_here.not_enough_exp";
                    respawnHereButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable(tooltipKey)));
                }
            }
        }
    }

    private static void onRespawnHereClicked(Button button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            // Send packet to server to handle respawn here
            minecraft.getConnection().send(RespawnHerePacket.create());
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof DeathScreen) {
            respawnHereButton = null;
        }
    }

    private static boolean checkCanRespawnClient(Player player) {
        if (Config.RESPAWN_HERE_USE_ITEM_MODE.get()) {
            // In item mode, we can't accurately check inventory from client side
            // So we just return true and let server side validation handle it
            return true;
        } else {
            // Experience mode - check experience levels
            int requiredExp = Config.RESPAWN_HERE_EXP_COST.get();
            return player.experienceLevel >= requiredExp;
        }
    }

    private static String getItemDisplayName(String itemId) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return formatItemId(itemId); // Fallback to formatted ID
            }
            
            // Parse the item ID
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            
            // Get the item from the registry
            Item item = minecraft.player.clientLevel.registryAccess()
                .registryOrThrow(Registries.ITEM)
                .get(itemLocation);
                
            if (item == null) {
                return formatItemId(itemId); // Fallback to formatted ID
            }
            
            // Get the localized name from the item's description
            return item.getDescription().getString();
            
        } catch (Exception e) {
            return formatItemId(itemId); // Fallback to formatted ID
        }
    }
    
    private static String formatItemId(String itemId) {
        // Fallback method to format the item ID into a readable string
        try {
            String[] parts = itemId.split(":");
            String itemName = parts.length > 1 ? parts[1] : itemId;
            // Convert snake_case or camelCase to readable format
            return itemName.replaceAll("([a-z])([A-Z])", "$1 $2")
                           .replaceAll("_", " ")
                           .toLowerCase();
        } catch (Exception e) {
            return itemId; // Final fallback to raw item ID
        }
    }
}