package com.devcl.aetherbridge.network;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MessageSender {
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    
    public static void sendToRemote(String player, String message, String prefix) {
        // 检查功能是否启用
        if (!ModConfig.getInstance().isFeatureEnabled("messageSender")) {
            AetherBridge.LOGGER.warn("尝试发送消息，但消息发送功能已禁用");
            return;
        }

        ChatMessage chatMessage = new ChatMessage(player, message);
        if (prefix != null && !prefix.isEmpty()) {
            chatMessage.setPrefix(prefix);
        }
        String jsonBody = GSON.toJson(chatMessage);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ModConfig.getInstance().getApiUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + ModConfig.getInstance().getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(10))
            .build();
            
        // 异步发送请求（带重试）
        sendWithRetry(request, 0);
    }
    
    private static void sendWithRetry(HttpRequest request, int retryCount) {
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    if (retryCount < MAX_RETRIES && shouldRetry(response.statusCode())) {
                        AetherBridge.LOGGER.warn("发送失败，将在" + RETRY_DELAY.getSeconds() + "秒后重试 (" + (retryCount + 1) + "/" + MAX_RETRIES + ")");
                        try {
                            Thread.sleep(RETRY_DELAY.toMillis());
                            sendWithRetry(request, retryCount + 1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        AetherBridge.LOGGER.error("服务器响应错误: " + response.statusCode());
                    }
                }
            })
            .exceptionally(e -> {
                if (retryCount < MAX_RETRIES) {
                    AetherBridge.LOGGER.warn("发送失败，将在" + RETRY_DELAY.getSeconds() + "秒后重试 (" + (retryCount + 1) + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis());
                        sendWithRetry(request, retryCount + 1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    AetherBridge.LOGGER.error("发送消息到远程服务器失败", e);
                }
                return null;
            });
    }
    
    private static boolean shouldRetry(int statusCode) {
        // 对于这些状态码进行重试
        return statusCode == 429 || // Too Many Requests
               statusCode == 502 || // Bad Gateway
               statusCode == 503 || // Service Unavailable
               statusCode == 504;   // Gateway Timeout
    }
    
    private static class ChatMessage {
        private final String player;
        private final String message;
        private String prefix;
        
        public ChatMessage(String player, String message) {
            this.player = player;
            this.message = message;
        }
        
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
} 