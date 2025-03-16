package com.devcl.aetherbridge.network;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MessageSender {
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    // 创建一个固定大小的线程池用于异步发送消息
    private static final ExecutorService MESSAGE_EXECUTOR = Executors.newFixedThreadPool(3);
    
    public static void sendToRemote(String playerName, String message, String prefix) {
        // 检查功能是否启用
        if (!ModConfig.getInstance().isFeatureEnabled("messageSender")) {
            AetherBridge.LOGGER.warn("尝试发送消息，但消息发送功能已禁用");
            return;
        }

        AetherBridge.LOGGER.debug("准备发送消息 (旧方法): 玩家=" + playerName + ", 消息=" + message);
        // 创建没有UUID的消息（向后兼容）
        sendToRemote(null, playerName, message, prefix);
    }
    
    public static void sendToRemote(UUID playerId, String playerName, String message, String prefix) {
        // 检查功能是否启用
        if (!ModConfig.getInstance().isFeatureEnabled("messageSender")) {
            AetherBridge.LOGGER.warn("尝试发送消息，但消息发送功能已禁用");
            return;
        }

        AetherBridge.LOGGER.info("准备发送消息: 玩家ID=" + (playerId != null ? playerId.toString() : "null") + 
                               ", 玩家名=" + playerName + 
                               ", 前缀=" + prefix);

        // 使用线程池异步执行消息发送
        MESSAGE_EXECUTOR.submit(() -> {
            try {
                ChatMessage chatMessage = new ChatMessage(playerId, playerName, message);
                if (prefix != null && !prefix.isEmpty()) {
                    chatMessage.setPrefix(prefix);
                }
                String jsonBody = GSON.toJson(chatMessage);
                
                AetherBridge.LOGGER.info("发送消息到远程服务器: " + jsonBody);
                
                URI targetUri = URI.create(ModConfig.getInstance().getApiUrl());
                AetherBridge.LOGGER.debug("目标URL: " + targetUri);
                
                HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + ModConfig.getInstance().getApiKey())
                    .header("Accept", "application/json")
                    .POST(bodyPublisher)
                    .timeout(Duration.ofSeconds(10))
                    .build();
                    
                AetherBridge.LOGGER.debug("HTTP请求头: " + request.headers().map().toString());
                
                sendWithRetryAsync(request, 0);
            } catch (Exception e) {
                AetherBridge.LOGGER.error("创建HTTP请求时发生错误", e);
            }
        });
    }
    
    private static void sendWithRetryAsync(HttpRequest request, int retryCount) {
        AetherBridge.LOGGER.debug("开始异步发送HTTP请求" + (retryCount > 0 ? " (重试 #" + retryCount + ")" : ""));
        
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() == 200) {
                    AetherBridge.LOGGER.info("消息发送成功，状态码: 200");
                    try {
                        AetherBridge.LOGGER.debug("服务器响应: " + response.body());
                    } catch (Exception e) {
                        // 忽略响应体解析错误
                    }
                } else {
                    if (retryCount < MAX_RETRIES && shouldRetry(response.statusCode())) {
                        AetherBridge.LOGGER.warn("发送失败，状态码: " + response.statusCode() + 
                                              "，将在" + RETRY_DELAY.getSeconds() + "秒后重试 (" + 
                                              (retryCount + 1) + "/" + MAX_RETRIES + ")");
                        
                        // 使用CompletableFuture.delayedExecutor进行延迟重试
                        CompletableFuture.delayedExecutor(RETRY_DELAY.toMillis(), 
                                                         java.util.concurrent.TimeUnit.MILLISECONDS, 
                                                         MESSAGE_EXECUTOR)
                            .execute(() -> sendWithRetryAsync(request, retryCount + 1));
                    } else {
                        AetherBridge.LOGGER.error("服务器响应错误: " + response.statusCode());
                        try {
                            AetherBridge.LOGGER.error("错误响应内容: " + response.body());
                        } catch (Exception e) {
                            // 忽略响应体解析错误
                        }
                    }
                }
            })
            .exceptionally(e -> {
                if (retryCount < MAX_RETRIES) {
                    AetherBridge.LOGGER.warn("发送失败: " + e.getMessage() + 
                                          "，将在" + RETRY_DELAY.getSeconds() + "秒后重试 (" + 
                                          (retryCount + 1) + "/" + MAX_RETRIES + ")");
                    
                    // 使用CompletableFuture.delayedExecutor进行延迟重试
                    CompletableFuture.delayedExecutor(RETRY_DELAY.toMillis(), 
                                                     java.util.concurrent.TimeUnit.MILLISECONDS, 
                                                     MESSAGE_EXECUTOR)
                        .execute(() -> sendWithRetryAsync(request, retryCount + 1));
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
    
    // 关闭线程池的方法，应在模组关闭时调用
    public static void shutdown() {
        AetherBridge.LOGGER.info("正在关闭消息发送线程池...");
        MESSAGE_EXECUTOR.shutdown();
        try {
            if (!MESSAGE_EXECUTOR.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                AetherBridge.LOGGER.warn("消息发送线程池未能在5秒内完全关闭，将强制关闭");
                MESSAGE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            AetherBridge.LOGGER.error("关闭消息发送线程池时被中断", e);
            Thread.currentThread().interrupt();
            MESSAGE_EXECUTOR.shutdownNow();
        }
    }
    
    private static class ChatMessage {
        private final String playerId;
        private final String playerName;
        private final String message;
        private String prefix;
        
        public ChatMessage(UUID playerId, String playerName, String message) {
            this.playerId = playerId != null ? playerId.toString() : null;
            this.playerName = playerName;
            this.message = message;
        }
        
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
} 