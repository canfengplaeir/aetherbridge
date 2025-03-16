package com.devcl.aetherbridge.feature;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.devcl.aetherbridge.network.MessageSender;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class MessageSenderFeature implements Feature {
    private final MinecraftServer server;
    private boolean enabled = false;
    private final ServerMessageEvents.ChatMessage chatListener;

    public MessageSenderFeature(MinecraftServer server) {
        this.server = server;
        this.chatListener = (message, sender, params) -> {
            if (enabled) {
                AetherBridge.LOGGER.debug("捕获到聊天消息: " + 
                                       "玩家=" + sender.getName().getString() + 
                                       ", UUID=" + sender.getUuid() + 
                                       ", 消息=" + message.getContent().getString());
                
                try {
                    String prefix = ModConfig.getInstance().getDefaultChatPrefix();
                    AetherBridge.LOGGER.debug("使用前缀: " + prefix);
                    
                    MessageSender.sendToRemote(
                        sender.getUuid(),
                        sender.getName().getString(),
                        message.getContent().getString(),
                        prefix
                    );
                } catch (Exception e) {
                    AetherBridge.LOGGER.error("处理聊天消息时发生错误", e);
                }
            } else {
                AetherBridge.LOGGER.debug("捕获到聊天消息，但功能已禁用，不进行转发");
            }
        };
    }

    @Override
    public void enable() throws Exception {
        if (!enabled) {
            AetherBridge.LOGGER.info("正在启用消息发送功能...");
            ServerMessageEvents.CHAT_MESSAGE.register(chatListener);
            enabled = true;
            AetherBridge.LOGGER.info("消息发送功能已启用，将转发聊天消息到: " + ModConfig.getInstance().getApiUrl());
        } else {
            AetherBridge.LOGGER.debug("消息发送功能已经处于启用状态");
        }
    }

    @Override
    public void disable() throws Exception {
        if (enabled) {
            AetherBridge.LOGGER.info("正在禁用消息发送功能...");
            // 注意：Fabric API可能不支持注销事件监听器
            // 我们通过enabled标志来控制功能
            enabled = false;
            AetherBridge.LOGGER.info("消息发送功能已禁用");
        } else {
            AetherBridge.LOGGER.debug("消息发送功能已经处于禁用状态");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getId() {
        return "messageSender";
    }
} 