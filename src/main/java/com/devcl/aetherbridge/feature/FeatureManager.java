package com.devcl.aetherbridge.feature;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.devcl.aetherbridge.network.MessageSender;
import net.minecraft.server.MinecraftServer;
import java.util.HashMap;
import java.util.Map;

public class FeatureManager {
    private static FeatureManager INSTANCE;
    private final Map<String, Feature> features = new HashMap<>();
    private final MinecraftServer server;

    private FeatureManager(MinecraftServer server) {
        this.server = server;
        registerFeatures();
    }

    public static FeatureManager getInstance(MinecraftServer server) {
        if (INSTANCE == null) {
            INSTANCE = new FeatureManager(server);
        }
        return INSTANCE;
    }

    private void registerFeatures() {
        // 注册消息发送功能
        features.put("messageSender", new MessageSenderFeature(server));
        // 注册消息接收功能
        features.put("messageReceiver", new MessageReceiverFeature(server));
        // 未来可以在这里添加更多功能
    }

    public void enableFeature(String featureId) {
        Feature feature = features.get(featureId);
        if (feature != null && !feature.isEnabled()) {
            try {
                feature.enable();
                AetherBridge.LOGGER.info("功能已启用: " + featureId);
            } catch (Exception e) {
                AetherBridge.LOGGER.error("启用功能失败: " + featureId, e);
            }
        }
    }

    public void disableFeature(String featureId) {
        Feature feature = features.get(featureId);
        if (feature != null && feature.isEnabled()) {
            try {
                feature.disable();
                AetherBridge.LOGGER.info("功能已禁用: " + featureId);
            } catch (Exception e) {
                AetherBridge.LOGGER.error("禁用功能失败: " + featureId, e);
            }
        }
    }

    public boolean isFeatureEnabled(String featureId) {
        Feature feature = features.get(featureId);
        return feature != null && feature.isEnabled();
    }

    public void reloadFeatures() {
        AetherBridge.LOGGER.info("正在重新加载功能状态...");
        features.forEach((id, feature) -> {
            if (ModConfig.getInstance().isFeatureEnabled(id)) {
                enableFeature(id);
            } else {
                disableFeature(id);
            }
        });
        // 输出功能状态摘要
        StringBuilder status = new StringBuilder("功能状态:\n");
        features.forEach((id, feature) -> {
            status.append(String.format("- %s: %s\n", id, feature.isEnabled() ? "启用" : "禁用"));
        });
        AetherBridge.LOGGER.info(status.toString());
    }
    
    /**
     * 热重载所有功能
     * 这个方法会先禁用所有功能，然后重新注册并启用它们
     */
    public void hotReload() {
        AetherBridge.LOGGER.info("正在执行热重载...");
        
        // 先禁用所有功能
        shutdown();
        
        // 关闭消息发送线程池
        MessageSender.shutdown();
        
        // 清空功能列表
        features.clear();
        
        // 重新注册功能
        registerFeatures();
        
        // 根据配置重新加载功能
        reloadFeatures();
        
        AetherBridge.LOGGER.info("热重载完成");
    }

    public void shutdown() {
        AetherBridge.LOGGER.info("正在关闭所有功能...");
        features.forEach((id, feature) -> {
            if (feature.isEnabled()) {
                disableFeature(id);
            }
        });
    }
} 