package com.devcl.aetherbridge.feature;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class MessageReceiverFeature implements Feature {
    private final MinecraftServer server;
    private boolean enabled = false;
    private HttpServer httpServer;
    private java.util.concurrent.ExecutorService executor;
    private static final Gson GSON = new Gson();

    public MessageReceiverFeature(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void enable() throws Exception {
        if (!enabled) {
            startHttpServer();
            enabled = true;
            AetherBridge.LOGGER.info("消息接收功能已启用");
        }
    }

    @Override
    public void disable() throws Exception {
        if (enabled) {
            stopHttpServer();
            enabled = false;
            AetherBridge.LOGGER.info("消息接收功能已禁用");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getId() {
        return "messageReceiver";
    }

    private void startHttpServer() throws IOException {
        try {
            // 支持IPv6
            InetSocketAddress addr = new InetSocketAddress("::", ModConfig.getInstance().getListenPort());
            httpServer = HttpServer.create(addr, 0);
            AetherBridge.LOGGER.info("正在启动HTTP服务器，端口: " + ModConfig.getInstance().getListenPort());

            // 添加CORS支持
            httpServer.createContext("/", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                
                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                
                exchange.sendResponseHeaders(404, -1);
            });

            httpServer.createContext("/api/send-to-mc", exchange -> {
                if (!enabled) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }

                // 添加CORS头
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                
                // 处理预检请求
                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // 验证token
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ") || 
                    !authHeader.substring(7).equals(ModConfig.getInstance().getApiKey())) {
                    exchange.sendResponseHeaders(403, 0);
                    exchange.close();
                    return;
                }

                // 读取请求体
                try (InputStream is = exchange.getRequestBody()) {
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject json = GSON.fromJson(requestBody, JsonObject.class);
                    
                    if (json.has("message")) {
                        String message = json.get("message").getAsString();
                        String prefix = json.has("prefix") ? json.get("prefix").getAsString() : null;
                        
                        // 构建完整消息
                        String fullMessage = prefix != null ? String.format("[%s] %s", prefix, message) : message;
                        
                        // 在主线程中广播消息
                        broadcastMessage(fullMessage);
                        
                        // 返回成功响应
                        String response = "{\"status\":\"success\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                    } else {
                        String error = "{\"error\":\"missing message field\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, error.length());
                        exchange.getResponseBody().write(error.getBytes());
                    }
                } catch (Exception e) {
                    AetherBridge.LOGGER.error("处理HTTP请求失败", e);
                    String error = "{\"error\":\"internal server error\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, error.length());
                    exchange.getResponseBody().write(error.getBytes());
                } finally {
                    exchange.close();
                }
            });

            // 使用线程池处理请求
            executor = Executors.newFixedThreadPool(4);
            httpServer.setExecutor(executor);
            httpServer.start();

            AetherBridge.LOGGER.info("HTTP服务器已启动，监听所有地址(IPv4/IPv6)，端口: " + ModConfig.getInstance().getListenPort());
        } catch (IOException e) {
            AetherBridge.LOGGER.error("启动HTTP服务器失败: " + e.getMessage());
            if (e.getMessage().contains("Address already in use")) {
                AetherBridge.LOGGER.error("端口 " + ModConfig.getInstance().getListenPort() + " 已被占用，请检查配置或其他程序");
            }
            throw e;
        }
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            AetherBridge.LOGGER.info("正在停止HTTP服务器...");
            httpServer.stop(0);
            // 优雅关闭线程池
            if (executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
            httpServer = null;
            executor = null;
            AetherBridge.LOGGER.info("HTTP服务器已停止");
        }
    }

    private void broadcastMessage(String message) {
        // 安全检查：消息长度限制
        if (message == null || message.length() > 256) {
            AetherBridge.LOGGER.warn("消息长度超出限制或为空");
            return;
        }
        
        // 安全检查：检查服务器是否在运行
        if (server == null || !server.isRunning()) {
            AetherBridge.LOGGER.error("服务器未运行，无法广播消息");
            return;
        }
        
        // 确保在主线程中执行
        server.execute(() -> {
            try {
                server.getPlayerManager().broadcast(
                    Text.literal(message),
                    false
                );
            } catch (Exception e) {
                AetherBridge.LOGGER.error("广播消息时发生错误", e);
            }
        });
    }
} 