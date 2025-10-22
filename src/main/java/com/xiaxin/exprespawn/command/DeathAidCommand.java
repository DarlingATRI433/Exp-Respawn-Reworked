package com.xiaxin.exprespawn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xiaxin.exprespawn.ExpRespawnRework;
import com.xiaxin.exprespawn.handler.DeathAidHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class DeathAidCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("deathaid")
            .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("playerId", StringArgumentType.string())
                .then(Commands.literal("yes")
                    .executes(context -> handleAidResponse(context, true)))
                .then(Commands.literal("no")
                    .executes(context -> handleAidResponse(context, false))));
        
        dispatcher.register(command);
    }
    
    private static int handleAidResponse(CommandContext<CommandSourceStack> context, boolean acceptAid) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer responder = source.getPlayer();
            
            if (responder == null) {
                source.sendFailure(Component.translatable("exprespawnrework.death_aid.must_be_player"));
                return 0;
            }
            
            String playerIdStr = StringArgumentType.getString(context, "playerId");
            UUID deadPlayerId;
            try {
                deadPlayerId = UUID.fromString(playerIdStr);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Invalid player ID format"));
                return 0;
            }
            
            // 处理援助响应
            DeathAidHandler.handleAidResponse(responder, deadPlayerId, acceptAid);
            
            return 1;
        } catch (Exception e) {
            ExpRespawnRework.LOGGER.error("执行死亡援助命令时出错: {}", e.getMessage());
            context.getSource().sendFailure(Component.translatable("exprespawnrework.death_aid.command_error"));
            return 0;
        }
    }
}