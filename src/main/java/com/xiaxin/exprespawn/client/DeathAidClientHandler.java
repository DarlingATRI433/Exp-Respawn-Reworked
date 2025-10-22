package com.xiaxin.exprespawn.client;

import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.data.DeathAidRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

import java.util.UUID;

public class DeathAidClientHandler {
    
    public static void showAidRequest(UUID deadPlayerId, String deadPlayerName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        
        // 创建援助请求消息
        MutableComponent message = Component.translatable("exprespawnrework.death_aid.request_message", deadPlayerName);
        
        // 添加"是"按钮
        MutableComponent yesButton = Component.translatable("exprespawnrework.death_aid.yes_button")
            .setStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deathaid " + deadPlayerId + " yes")));
        
        // 添加"否"按钮
        MutableComponent noButton = Component.translatable("exprespawnrework.death_aid.no_button")
            .setStyle(Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deathaid " + deadPlayerId + " no")));
        
        Component fullMessage = message.append(" [").append(yesButton).append("] [").append(noButton).append("]");
        
        // 在聊天栏显示消息
        minecraft.gui.getChat().addMessage(fullMessage);
        
        ExpRespawnRework.LOGGER.debug("显示死亡援助请求消息给玩家: {}", minecraft.player.getName().getString());
    }
}