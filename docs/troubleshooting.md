# 故障排除指南

## 常见问题

### 1. 网络连接问题

- 症状：无法连接到远程服务器
- 解决方案：
  - 检查API地址配置是否正确
  - 确认网络连接是否正常
  - 验证防火墙设置

### 2. 认证问题

- 症状：API密钥验证失败
- 解决方案：
  - 确认API密钥配置正确
  - 检查密钥格式
  - 验证密钥有效性

### 3. 功能启用失败

- 症状：功能无法启用
- 解决方案：
  - 检查配置文件权限
  - 确认端口未被占用
  - 查看详细错误日志

## 错误信息说明

| 错误代码 | 说明 | 解决方案 |
|---------|------|---------|
| NETWORK_TIMEOUT | 连接超时 | 检查网络连接 |
| INVALID_KEY | API密钥无效 | 验证密钥配置 |
| PORT_IN_USE | 端口被占用 | 修改监听端口 |

## 日志位置

- Minecraft服务端：`logs/latest.log`
- Node.js服务器：`logs/error.log`

## 获取帮助

1. 使用 `/aetherbridge info` 命令查看当前配置
2. 检查服务器日志获取详细错误信息
3. 提交Issue获取支持：[GitHub Issues](https://github.com/DevCL/aetherbridge/issues) 