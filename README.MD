# AetherBridge - Minecraft 消息桥接系统

## 项目介绍

AetherBridge 是一个用于 Minecraft 服务端的消息桥接系统，它允许 Minecraft 服务器与外部服务进行双向实时通信。该系统由两个主要部分组成：
1. Minecraft Fabric Mod（服务端）
2. Node.js 消息中转服务器

### 主要功能
- 实时转发游戏内聊天消息到外部服务
- 接收外部消息并在游戏内广播
- 支持自定义消息前缀
- 支持 API 密钥验证
- 可配置的消息过滤
- 完整的错误处理和日志记录

## 系统架构

### Minecraft Mod 部分
- 基于 Fabric 1.20.1
- 使用 Java 17
- 支持热重载配置
- 异步网络通信

### Node.js 服务器部分
- Express.js 框架
- Winston 日志系统
- 环境变量配置
- RESTful API 设计

## 安装说明

### Minecraft Mod 安装
1. 确保服务器已安装 Fabric 1.20.1
2. 将 `aetherbridge-1.0.0.jar` 放入服务器的 `mods` 目录
3. 启动服务器，mod 将自动生成配置文件

### Node.js 服务器安装
```bash
cd mc-bridge-server
npm install
npm start
```

## 配置说明

### Minecraft Mod 配置
配置文件位置：`config/aetherbridge/config.json`
```json
{
    "apiUrl": "http://localhost:3000/api/mc-message",
    "apiKey": "your-secret-key",
    "listenPort": 8080
}
```

### Node.js 服务器配置
配置文件位置：`.env`
```env
PORT=3000
API_KEY=your-secret-key
MC_SERVER_HOST=localhost
MC_SERVER_PORT=8080
```

## API 文档

### 1. 发送消息到 Minecraft
- 端点：`POST /api/send`
- 认证：Bearer Token
- 请求体：
```json
{
    "message": "要发送的消息",
    "prefix": "系统"  // 可选，消息将显示为 [系统] 要发送的消息
}
```
- 响应：
```json
{
    "status": "success",
    "sent": {
        "message": "要发送的消息",
        "prefix": "系统",
        "time": "2024-01-20T12:34:56.789Z"
    }
}
```

### 消息格式说明

1. 从外部发送到Minecraft的消息格式：
   - 带前缀：`[前缀] 消息内容`
   - 不带前缀：直接显示`消息内容`

2. 示例：
```json
// 带前缀的消息
{
    "message": "南城：你好啊",
    "prefix": "QQ"
}
// 将在游戏中显示为：[QQ] 南城：你好啊

// 不带前缀的消息
{
    "message": "欢迎来到服务器"
}
// 将在游戏中显示为：欢迎来到服务器
```

3. 使用建议：
   - 用于标识消息来源：`[QQ]`, `[Discord]`, `[微信]`
   - 用于系统通知：`[系统]`, `[公告]`, `[活动]`
   - 用于管理命令：`[管理]`, `[后台]`

### 2. 接收 Minecraft 消息
- 端点：`POST /api/mc-message`
- 认证：Bearer Token
- 请求体：
```json
{
    "player": "玩家名称",
    "message": "聊天消息内容"
}
```
- 响应：
```json
{
    "status": "success",
    "received": {
        "player": "玩家名称",
        "message": "聊天消息内容",
        "time": "2024-01-20T12:34:56.789Z"
    }
}
```

### 3. 健康检查
- 端点：`GET /health`
- 响应：
```json
{
    "status": "ok",
    "timestamp": "2024-01-20T12:34:56.789Z"
}
```

### 消息验证
- 消息长度限制：256 字符
- 敏感词过滤：可配置
- Token 验证：必需

## 游戏内命令

### 重载配置
```
/aetherbridge reload
```
- 权限要求：OP 权限（等级 4）
- 功能：重新加载配置文件

## 错误处理

### HTTP 状态码
- 200：请求成功
- 400：请求参数错误
- 401：未提供认证令牌
- 403：无效的认证令牌
- 500：服务器内部错误

## 测试工具

### 1. 消息发送测试
```bash
npm run test
```
测试内容包括：
- 带不同前缀的消息发送
- 无前缀消息发送
- 无效token测试
- 消息长度限制测试

### 2. 消息接收测试服务器
```bash
npm run test-server
```

## 日志系统

### Node.js 服务器日志
- 错误日志：`logs/error.log`
- 完整日志：`logs/combined.log`
- 控制台输出：彩色格式化

### Minecraft Mod 日志
- 服务器日志文件中查看
- 前缀：`[AetherBridge]`

## 安全建议

1. 修改默认 API 密钥
2. 使用 HTTPS 进行通信
3. 定期更新 API 密钥
4. 监控异常登录尝试
5. 设置适当的消息过滤规则

## 常见问题

1. Q: 配置文件不生效？
   A: 使用 `/aetherbridge reload` 重新加载配置

2. Q: 消息发送失败？
   A: 检查网络连接和 API 密钥配置

3. Q: 日志文件过大？
   A: 日志文件会自动轮转，每个文件最大 5MB

4. Q: 消息前缀显示异常？
   A: 确保前缀不包含特殊字符，长度适中

## 开发计划

- [ ] 添加 WebSocket 支持
- [ ] 增加消息队列
- [ ] 支持更多消息类型
- [ ] 支持自定义前缀样式
- [ ] 添加前缀权限控制
- [ ] 添加管理界面
- [ ] 支持数据库存储