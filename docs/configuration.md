# 配置说明

## 配置文件位置

```
config/aetherbridge/config.json
```

## 配置项说明

### 基本配置
```json
{
    "apiUrl": "http://localhost:3000/api/mc-message",  // API服务器地址
    "apiKey": "your-secret-key",                       // API密钥
    "listenPort": 8080,                                // HTTP服务器监听端口
    "features": {
        "messageSender": true,                         // 消息发送功能开关
        "messageReceiver": true                        // 消息接收功能开关
    }
}
```

### 高级配置示例

```json
{
    "apiUrl": "https://api.example.com/mc-message",
    "apiKey": "your-secret-key",
    "listenPort": 8080,
    "features": {
        "messageSender": true,
        "messageReceiver": true
    },
    "network": {
        "timeout": 10000,           // 连接超时时间（毫秒）
        "retryCount": 3,            // 重试次数
        "retryDelay": 2000          // 重试延迟（毫秒）
    },
    "security": {
        "allowedIps": [             // 允许的IP地址列表
            "127.0.0.1",
            "192.168.1.0/24"
        ]
    }
}
```

## 功能说明

1. messageSender
   - 功能：将游戏内聊天消息转发到外部服务
   - 配置：通过 `features.messageSender` 控制

### 配置示例
```json
{
    "features": {
        "messageSender": {
            "enabled": true,
            "prefix": "MC",
            "excludeCommands": true
        }
    }
}
```

2. messageReceiver
   - 功能：接收外部消息并在游戏内广播
   - 配置：通过 `features.messageReceiver` 控制

### 配置示例
```json
{
    "features": {
        "messageReceiver": {
            "enabled": true,
            "maxLength": 256,
            "allowHtml": false
        }
    }
}
```

## 配置热重载

使用命令 `/aetherbridge reload` 可以在不重启服务器的情况下重新加载配置。

## 配置验证

配置加载时会进行以下验证：
- API URL格式是否正确
- API密钥是否设置
- 端口号是否有效
- 功能配置是否完整 