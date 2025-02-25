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
                MessageSender.sendToRemote(
                    sender.getName().getString(),
                    message.getContent().getString(),
                    ModConfig.getInstance().getDefaultChatPrefix()
                );
            }
        };
    }

    @Override
    public void enable() throws Exception {
        if (!enabled) {
            ServerMessageEvents.CHAT_MESSAGE.register(chatListener);
            enabled = true;
            AetherBridge.LOGGER.info("消息发送功能已启用");
        }
    }

    @Override
    public void disable() throws Exception {
        if (enabled) {
            // 注意：Fabric API可能不支持注销事件监听器
            // 我们通过enabled标志来控制功能
            enabled = false;
            AetherBridge.LOGGER.info("消息发送功能已禁用");
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