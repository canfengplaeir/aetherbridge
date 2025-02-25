package com.devcl.aetherbridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import com.devcl.aetherbridge.AetherBridge;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aetherbridge/config.json");
    
    private String apiUrl = "https://your-api.com/endpoint";
    private String apiKey = "your-secret-key";
    private int listenPort = 8080;
    private String defaultChatPrefix = "游戏";
    private Map<String, Boolean> features = new HashMap<>();
    
    private static ModConfig INSTANCE;
    
    // 创建默认配置
    private static ModConfig createDefaultConfig() {
        ModConfig config = new ModConfig();
        config.apiUrl = "http://localhost:3000/api/mc-message";
        config.apiKey = "your-secret-key-" + java.util.UUID.randomUUID().toString();
        config.listenPort = 8080;
        config.defaultChatPrefix = "游戏";
        
        // 默认启用所有功能
        config.features.put("messageSender", true);
        config.features.put("messageReceiver", true);
        return config;
    }
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            loadConfig();
        }
        return INSTANCE;
    }
    
    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                }
            } else {
                INSTANCE = createDefaultConfig();
                AetherBridge.LOGGER.info("正在创建默认配置文件...");
                saveConfig();
                AetherBridge.LOGGER.info("默认配置文件已创建在: " + CONFIG_PATH);
                AetherBridge.LOGGER.info("请修改配置文件中的API密钥和其他设置！");
            }
            
            // 验证配置
            if (INSTANCE != null) {
                INSTANCE.validate();
            }
        } catch (IOException e) {
            AetherBridge.LOGGER.error("加载配置文件失败", e);
            INSTANCE = createDefaultConfig();
        }
    }
    
    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write("// AetherBridge配置文件\n");
                writer.write("// apiUrl: 远程服务器的API地址\n");
                writer.write("// apiKey: 用于验证的密钥，与远程服务器保持一致\n");
                writer.write("// listenPort: HTTP服务器监听端口\n");
                writer.write("// features: 功能开关配置\n");
                writer.write("//   - messageSender: 消息发送功能\n");
                writer.write("//   - messageReceiver: 消息接收功能\n\n");
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            AetherBridge.LOGGER.error("保存配置文件失败", e);
        }
    }
    
    // Getters
    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public int getListenPort() { return listenPort; }
    public String getDefaultChatPrefix() { return defaultChatPrefix; }
    
    public boolean isFeatureEnabled(String featureId) {
        return features.getOrDefault(featureId, false);
    }
    
    public void setFeatureEnabled(String featureId, boolean enabled) {
        features.put(featureId, enabled);
    }
    
    public void validate() throws IllegalStateException {
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new IllegalStateException("API URL不能为空");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key不能为空");
        }
        if (listenPort <= 0 || listenPort > 65535) {
            throw new IllegalStateException("监听端口必须在1-65535之间");
        }
        if (defaultChatPrefix == null) {
            defaultChatPrefix = "游戏";
        }
        if (features == null) {
            features = new HashMap<>();
        }
        // 确保所有功能都有默认值
        if (!features.containsKey("messageSender")) {
            features.put("messageSender", true);
        }
        if (!features.containsKey("messageReceiver")) {
            features.put("messageReceiver", true);
        }
    }
} 