package com.devcl.aetherbridge;

import com.devcl.aetherbridge.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import com.devcl.aetherbridge.command.ModCommands;
import com.devcl.aetherbridge.feature.FeatureManager;
import com.devcl.aetherbridge.network.MessageSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AetherBridge - Minecraft消息桥接系统
 * 
 * 这个mod提供了一个灵活的消息桥接系统，允许Minecraft服务器与外部服务进行双向实时通信。
 * 主要功能包括：
 * - 消息转发：将游戏内聊天消息转发到外部服务
 * - 消息接收：接收外部消息并在游戏内广播
 * - 功能管理：支持独立开关各项功能
 * - 配置热重载：支持在不重启服务器的情况下重新加载配置
 * 
 * @author DevCL
 * @version 1.0.0
 */
@Environment(EnvType.SERVER)
public class AetherBridge implements ModInitializer {
	public static final String MOD_ID = "aetherbridge";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static FeatureManager featureManager;

	@Override
	public void onInitialize() {
		// 加载配置
		ModConfig.loadConfig();
		
		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
			ModCommands.register(dispatcher)
		);
		
		// 注册服务器生命周期事件
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LOGGER.info("正在启动AetherBridge...");
			featureManager = FeatureManager.getInstance(server);
			featureManager.reloadFeatures();
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("正在关闭AetherBridge...");
			if (featureManager != null) {
				featureManager.shutdown();
			}
			// 关闭消息发送线程池
			MessageSender.shutdown();
		});
		
		LOGGER.info("AetherBridge初始化完成！");
	}
}