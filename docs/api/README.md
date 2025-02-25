# API 文档

## 认证
所有API请求都需要包含有效的API密钥。

### 请求头
```
Authorization: Bearer your-api-key
```

## 接口

### 1. 发送消息到Minecraft

**POST** `/api/send-to-mc`

#### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 消息内容，最大256字符 |
| prefix | string | 否 | 消息前缀，如"QQ"、"Discord" |

#### 请求体
```json
{
    "message": "消息内容",
    "prefix": "消息前缀"  // 可选
}
```

#### 错误响应
```json
{
    "success": false,
    "error": "错误信息",
    "code": "ERROR_CODE"
}
```

### 2. 接收Minecraft消息

**POST** `/api/mc-message`

#### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 发送消息的玩家名称 |
| message | string | 是 | 聊天消息内容 |

#### 请求体
```json
{
    "player": "玩家名称",
    "message": "聊天消息内容"
}
```

#### 响应说明
```json
{
    "success": true,
    "received": {
        "player": "玩家名称",
        "message": "聊天消息内容",
        "time": "2024-01-01T12:00:00Z"
    }
}
```

## 错误码说明

- 400: 请求格式错误
- 401: 未提供认证令牌
- 403: 无效的认证令牌
- 404: 接口不存在
- 429: 请求过于频繁
- 500: 服务器内部错误
- 503: 功能未启用

## 安全建议

1. API密钥保护
   - 使用足够长的随机密钥
   - 定期更换密钥
   - 避免密钥泄露

2. 网络安全
   - 建议使用HTTPS
   - 配置防火墙规则
   - 限制访问IP

3. 监控和日志
   - 记录异常请求
   - 监控API调用频率
   - 定期检查日志 