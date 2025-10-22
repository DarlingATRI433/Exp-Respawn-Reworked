package com.xiaxin.exprespawn.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import java.util.UUID;

public class DeathAidRequest {
    private final UUID deadPlayerId;
    private final String deadPlayerName;
    private final long requestTime;
    private boolean isActive;
    
    public DeathAidRequest(ServerPlayer deadPlayer) {
        this.deadPlayerId = deadPlayer.getUUID();
        this.deadPlayerName = deadPlayer.getName().getString();
        this.requestTime = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public UUID getDeadPlayerId() {
        return deadPlayerId;
    }
    
    public String getDeadPlayerName() {
        return deadPlayerName;
    }
    
    public long getRequestTime() {
        return requestTime;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public boolean isExpired() {
        // 使用配置的超时时间（转换为毫秒），如果配置未加载则使用默认值60秒
        int timeoutSeconds = com.xiaxin.exprespawn.Config.getIntValue(
            com.xiaxin.exprespawn.Config.DEATH_AID_REQUEST_TIMEOUT, 60);
        return System.currentTimeMillis() - requestTime > timeoutSeconds * 1000L;
    }
    
    public Component createAidRequestMessage() {
        // 创建可点击的援助请求消息
        MutableComponent message = Component.translatable("exprespawnrework.death_aid.request_message", deadPlayerName);
        
        // 添加"是"按钮
        MutableComponent yesButton = Component.translatable("exprespawnrework.death_aid.yes_button")
            .setStyle(Style.EMPTY
                .withColor(net.minecraft.ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deathaid " + deadPlayerId + " yes")));
        
        // 添加"否"按钮
        MutableComponent noButton = Component.translatable("exprespawnrework.death_aid.no_button")
            .setStyle(Style.EMPTY
                .withColor(net.minecraft.ChatFormatting.RED)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deathaid " + deadPlayerId + " no")));
        
        return message.append(" [").append(yesButton).append("] [").append(noButton).append("]");
    }
}