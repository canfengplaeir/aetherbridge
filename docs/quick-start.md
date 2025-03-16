# 快速开始

## 环境要求

- Minecraft 1.20.1
- Fabric Loader >= 0.14.21
- Java 17 或更高版本
- Fabric API

## 安装步骤

1. 下载最新版本的mod
2. 放入服务器的mods目录
3. 启动服务器，mod将自动生成配置文件
4. 修改配置文件中的API密钥等设置
5. 使用 `/aetherbridge reload` 重新加载配置

## 基本命令

- `/aetherbridge info` - 显示配置信息
- `/aetherbridge reload` - 重新加载配置
- `/aetherbridge hotreload` - 执行完整热重载（更新mod后使用）
- `/aetherbridge feature list` - 显示功能列表
- `/aetherbridge feature <功能ID> enable` - 启用功能
- `/aetherbridge feature <功能ID> disable` - 禁用功能

## 快速配置

1. 设置API地址和密钥：
   ```json
   {
     "apiUrl": "https://your-api.com/endpoint",
     "apiKey": "your-secret-key"
   }
   ```

2. 配置消息前缀：
   ```json
   {
     "defaultChatPrefix": "MC"
   }
   ```

3. 启用或禁用功能：
   ```json
   {
     "features": {
       "messageSender": true,
       "messageReceiver": true
     }
   }
   ```

## 调试提示

如果遇到问题，可以查看日志获取详细信息：

1. 服务器日志位置：`logs/latest.log`
2. 查找包含 `[AetherBridge]` 的日志条目
3. 可以在配置中调整日志级别获取更详细的信息：
   ```json
   {
     "logging": {
       "level": "DEBUG"
     }
   }
   ```

## 热重载说明

当您更新mod文件后，可以使用热重载功能而不必重启服务器：

1. 替换 `mods` 目录中的 `.jar` 文件
2. 在游戏中执行 `/aetherbridge hotreload` 命令
3. 模组将重新初始化所有功能组件

注意：热重载仅重新初始化现有功能，如果有新增功能或重大结构变更，仍可能需要重启服务器。

更多详细信息，请参考[配置说明](./configuration.md)和[故障排除](./troubleshooting.md)文档。 