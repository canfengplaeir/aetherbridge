package com.devcl.aetherbridge.command;

import com.devcl.aetherbridge.AetherBridge;
import com.devcl.aetherbridge.config.ModConfig;
import com.devcl.aetherbridge.feature.FeatureManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.arguments.StringArgumentType;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class ModCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("aetherbridge")
            .requires(source -> source.hasPermissionLevel(4)) // 需要OP权限
            .then(literal("reload")
                .executes(ModCommands::reloadConfig)
            )
            .then(literal("hotreload")
                .executes(ModCommands::hotReload)
            )
            .then(literal("info")
                .executes(ModCommands::showInfo)
            )
            .then(literal("feature")
                .then(argument("featureId", StringArgumentType.word())
                    .then(literal("enable")
                        .executes(ModCommands::enableFeature)
                    )
                    .then(literal("disable")
                        .executes(ModCommands::disableFeature)
                    )
                )
                .then(literal("list")
                    .executes(ModCommands::listFeatures)
                )
            )
        );
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            MinecraftServer server = source.getServer();
            
            // 获取服务器IP地址信息
            List<String> ipAddresses = new ArrayList<>();
            try {
                NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni -> {
                    ni.getInetAddresses().asIterator().forEachRemaining(addr -> {
                        if (!addr.isLoopbackAddress()) {
                            ipAddresses.add(addr.getHostAddress());
                        }
                    });
                });
            } catch (Exception e) {
                AetherBridge.LOGGER.error("获取网络接口信息失败", e);
            }
            
            // 构建配置信息
            MutableText info = Text.literal("")
                .append(Text.literal("§6=== AetherBridge 配置信息 ===§r\n"))
                .append(Text.literal("§2API地址: §f" + ModConfig.getInstance().getApiUrl() + "\n"))
                .append(Text.literal("§2监听端口: §f" + ModConfig.getInstance().getListenPort() + "\n"))
                .append(Text.literal("§2API密钥: §f" + ModConfig.getInstance().getApiKey() + "\n"))
                .append(Text.literal("\n§6=== 功能状态 ===§r\n"))
                .append(Text.literal("§2消息发送: §f" + (ModConfig.getInstance().isFeatureEnabled("messageSender") ? "§a启用" : "§c禁用") + "\n"))
                .append(Text.literal("§2消息接收: §f" + (ModConfig.getInstance().isFeatureEnabled("messageReceiver") ? "§a启用" : "§c禁用") + "\n"))
                .append(Text.literal("\n§6=== 网络信息 ===§r\n"));
            
            // 添加IP地址信息
            if (!ipAddresses.isEmpty()) {
                info.append(Text.literal("§2可用IP地址:§r\n"));
                for (String ip : ipAddresses) {
                    boolean isIPv6 = ip.contains(":");
                    String displayIp = isIPv6 ? "[" + ip + "]" : ip;
                    String url = "http://" + displayIp + ":" + ModConfig.getInstance().getListenPort();
                    info.append(Text.literal("§7- §f" + url + "\n"));
                }
            } else {
                info.append(Text.literal("§c未找到可用IP地址\n"));
            }
            
            // 添加测试命令示例
            info.append(Text.literal("\n§6=== 测试命令 ===§r\n"))
                .append(Text.literal("§2curl测试命令:§r\n"))
                .append(Text.literal("§7curl -X POST -H \"Content-Type: application/json\" "))
                .append(Text.literal("§7-H \"Authorization: Bearer " + ModConfig.getInstance().getApiKey() + "\" "))
                .append(Text.literal("§7-d '{\"message\":\"测试消息\",\"prefix\":\"测试\"}' "))
                .append(Text.literal("§7http://localhost:" + ModConfig.getInstance().getListenPort() + "/api/send-to-mc\n"));
            
            source.sendFeedback(() -> info, false);
            return 1;
        } catch (Exception e) {
            AetherBridge.LOGGER.error("显示配置信息时出错", e);
            context.getSource().sendError(Text.literal("§c显示配置信息时出错: " + e.getMessage()));
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        try {
            ModConfig.loadConfig();
            // 重新加载功能状态
            FeatureManager manager = FeatureManager.getInstance(context.getSource().getServer());
            manager.reloadFeatures();
            context.getSource().sendFeedback(() -> Text.literal("§a配置已重新加载"), true);
            return 1;
        } catch (Exception e) {
            AetherBridge.LOGGER.error("重新加载配置失败", e);
            context.getSource().sendError(Text.literal("§c重新加载配置失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int hotReload(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(() -> Text.literal("§6正在执行热重载，请稍候..."), true);
            
            // 重新加载配置
            ModConfig.loadConfig();
            
            // 执行热重载
            FeatureManager manager = FeatureManager.getInstance(context.getSource().getServer());
            manager.hotReload();
            
            context.getSource().sendFeedback(() -> Text.literal("§a热重载完成！所有功能已重新初始化"), true);
            return 1;
        } catch (Exception e) {
            AetherBridge.LOGGER.error("执行热重载失败", e);
            context.getSource().sendError(Text.literal("§c执行热重载失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int enableFeature(CommandContext<ServerCommandSource> context) {
        String featureId = StringArgumentType.getString(context, "featureId");
        try {
            FeatureManager manager = FeatureManager.getInstance(context.getSource().getServer());
            if (manager.isFeatureEnabled(featureId)) {
                context.getSource().sendError(Text.literal("§c功能已经处于启用状态: " + featureId));
                return 0;
            }
            
            manager.enableFeature(featureId);
            ModConfig.getInstance().setFeatureEnabled(featureId, true);
            ModConfig.saveConfig();
            
            context.getSource().sendFeedback(() -> 
                Text.literal("§a功能已启用: " + featureId), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§c启用功能失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int disableFeature(CommandContext<ServerCommandSource> context) {
        String featureId = StringArgumentType.getString(context, "featureId");
        try {
            FeatureManager manager = FeatureManager.getInstance(context.getSource().getServer());
            if (!manager.isFeatureEnabled(featureId)) {
                context.getSource().sendError(Text.literal("§c功能已经处于禁用状态: " + featureId));
                return 0;
            }
            
            manager.disableFeature(featureId);
            ModConfig.getInstance().setFeatureEnabled(featureId, false);
            ModConfig.saveConfig();
            
            context.getSource().sendFeedback(() -> 
                Text.literal("§c功能已禁用: " + featureId), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§c禁用功能失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int listFeatures(CommandContext<ServerCommandSource> context) {
        try {
            FeatureManager manager = FeatureManager.getInstance(context.getSource().getServer());
            MutableText message = Text.literal("§6=== 功能列表 ===§r\n");
            
            // 添加功能状态信息
            message.append("§2消息发送 (messageSender): §r")
                .append(manager.isFeatureEnabled("messageSender") ? "§a启用\n" : "§c禁用\n")
                .append("§7- 将游戏内聊天消息转发到外部服务\n");
            
            message.append("§2消息接收 (messageReceiver): §r")
                .append(manager.isFeatureEnabled("messageReceiver") ? "§a启用\n" : "§c禁用\n")
                .append("§7- 接收外部消息并在游戏内广播\n");
            
            // 添加使用说明
            message.append("\n§6命令使用说明:§r\n")
                .append("§7/aetherbridge feature <功能ID> enable §r- 启用功能\n")
                .append("§7/aetherbridge feature <功能ID> disable §r- 禁用功能\n")
                .append("\n§6可用功能ID:§r\n")
                .append("§7- messageSender §r- 消息发送功能\n")
                .append("§7- messageReceiver §r- 消息接收功能\n");
            
            // 添加配置提示
            message.append("\n§6提示:§r\n")
                .append("§7- 功能状态会保存到配置文件\n")
                .append("§7- 使用 /aetherbridge reload 重新加载配置\n")
                .append("§7- 使用 /aetherbridge hotreload 执行完整热重载\n")
                .append("§7- 使用 /aetherbridge info 查看详细配置\n");
            
            context.getSource().sendFeedback(() -> message, false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§c获取功能列表失败: " + e.getMessage()));
            return 0;
        }
    }
} 