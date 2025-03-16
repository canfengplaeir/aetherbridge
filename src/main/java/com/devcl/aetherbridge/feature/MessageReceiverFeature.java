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
            AetherBridge.LOGGER.info("正在启用消息接收功能...");
            startHttpServer();
            enabled = true;
            AetherBridge.LOGGER.info("消息接收功能已启用，监听端口: " + ModConfig.getInstance().getListenPort());
        } else {
            AetherBridge.LOGGER.debug("消息接收功能已经处于启用状态");
        }
    }

    @Override
    public void disable() throws Exception {
        if (enabled) {
            AetherBridge.LOGGER.info("正在禁用消息接收功能...");
            stopHttpServer();
            enabled = false;
            AetherBridge.LOGGER.info("消息接收功能已禁用");
        } else {
            AetherBridge.LOGGER.debug("消息接收功能已经处于禁用状态");
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
                String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
                AetherBridge.LOGGER.debug("收到来自 " + remoteAddr + " 的请求: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
                
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
                String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
                AetherBridge.LOGGER.info("收到来自 " + remoteAddr + " 的消息发送请求: " + 
                                      exchange.getRequestMethod() + " " + exchange.getRequestURI());
                
                if (!enabled) {
                    AetherBridge.LOGGER.warn("功能已禁用，拒绝请求");
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
                    AetherBridge.LOGGER.warn("API密钥验证失败，来自: " + remoteAddr);
                    exchange.sendResponseHeaders(403, 0);
                    exchange.close();
                    return;
                }

                // 读取请求体
                try (InputStream is = exchange.getRequestBody()) {
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    AetherBridge.LOGGER.debug("收到请求体: " + requestBody);
                    
                    JsonObject json = GSON.fromJson(requestBody, JsonObject.class);
                    
                    if (json.has("message")) {
                        String message = json.get("message").getAsString();
                        String prefix = json.has("prefix") ? json.get("prefix").getAsString() : null;
                        
                        AetherBridge.LOGGER.info("收到消息: " + message + (prefix != null ? ", 前缀: " + prefix : ""));
                        
                        // 构建完整消息
                        String fullMessage = prefix != null ? String.format("[%s] %s", prefix, message) : message;
                        
                        // 在主线程中广播消息
                        broadcastMessage(fullMessage);
                        
                        // 返回成功响应
                        String response = "{\"status\":\"success\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                        
                        AetherBridge.LOGGER.debug("消息处理成功");
                    } else {
                        AetherBridge.LOGGER.warn("请求缺少message字段");
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
        
        AetherBridge.LOGGER.info("准备广播消息到服务器: " + message);
        
        // 确保在主线程中执行
        server.execute(() -> {
            try {
                AetherBridge.LOGGER.debug("在主线程中广播消息");
                server.getPlayerManager().broadcast(
                    Text.literal(message),
                    false
                );
                AetherBridge.LOGGER.debug("消息广播成功");
            } catch (Exception e) {
                AetherBridge.LOGGER.error("广播消息时发生错误", e);
            }
        });
    }
} 