import express from 'express';
import dotenv from 'dotenv';
import winston from 'winston';
import { createMinecraftClient } from './minecraft.js';
import { validateApiKey } from './middleware.js';
import { getFeatureConfig } from './config.js';

// 加载环境变量
dotenv.config();

// 获取功能配置
const features = getFeatureConfig();

// 配置日志
const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.printf(({ level, message, timestamp, ...meta }) => {
            return `${timestamp} [${level.toUpperCase()}] ${message} ${Object.keys(meta).length ? JSON.stringify(meta, null, 2) : ''}`;
        })
    ),
    transports: [
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        }),
        new winston.transports.File({ 
            filename: 'logs/error.log', 
            level: 'error',
            maxsize: parseFileSize(process.env.MAX_LOG_SIZE || '5MB'),
            maxFiles: parseInt(process.env.MAX_LOG_FILES || '5')
        }),
        new winston.transports.File({ 
            filename: 'logs/combined.log',
            maxsize: parseFileSize(process.env.MAX_LOG_SIZE || '5MB'),
            maxFiles: parseInt(process.env.MAX_LOG_FILES || '5')
        })
    ]
});

// 解析文件大小字符串
function parseFileSize(size) {
    const units = {
        'KB': 1024,
        'MB': 1024 * 1024,
        'GB': 1024 * 1024 * 1024
    };
    
    const match = size.match(/^(\d+)(KB|MB|GB)$/i);
    if (match) {
        const [, number, unit] = match;
        return parseInt(number) * units[unit.toUpperCase()];
    }
    return parseInt(size) || 5242880; // 默认5MB
}

// 创建Express应用
const app = express();

// 中间件配置
app.use(express.json());
app.use((req, res, next) => {
    // 请求日志
    logger.info(`${req.method} ${req.path}`, {
        ip: req.ip,
        userAgent: req.get('user-agent')
    });
    next();
});

// 错误处理中间件
app.use((err, req, res, next) => {
    logger.error('服务器错误', { error: err.message, stack: err.stack });
    res.status(500).json({ error: '内部服务器错误' });
});

// Minecraft客户端
const mcClient = createMinecraftClient({
    host: process.env.MC_SERVER_HOST,
    port: process.env.MC_SERVER_PORT
});

// 健康检查端点
app.get('/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        timestamp: new Date().toISOString(),
        features: {
            messageSender: features.messageSender,
            messageReceiver: features.messageReceiver
        }
    });
});

// 接收来自Minecraft的消息
app.post('/api/mc-message', validateApiKey, (req, res) => {
    try {
        const { player, message } = req.body;
        
        if (!player || !message) {
            return res.status(400).json({ error: '缺少必要参数' });
        }
        
        logger.info('收到来自Minecraft的消息', {
            player,
            message,
            timestamp: new Date().toISOString()
        });

        // 这里可以添加消息处理逻辑
        // 例如：转发到Discord、保存到数据库等
        
        // 消息过滤（示例）
        if (message.toLowerCase().includes('admin')) {
            logger.warn('检测到敏感词', { player, message });
            return res.status(403).json({ error: '消息包含敏感词' });
        }

        res.status(200).json({
            status: 'success',
            received: {
                player,
                message,
                time: new Date().toISOString()
            }
        });
    } catch (error) {
        logger.error('处理Minecraft消息时出错', { error: error.message });
        res.status(500).json({ error: '处理消息失败' });
    }
});

// 发送消息到Minecraft
app.post('/api/send', validateApiKey, async (req, res) => {
    try {
        const { message, prefix } = req.body;
        
        if (!message) {
            return res.status(400).json({ error: '消息不能为空' });
        }

        if (message.length > 256) {
            return res.status(400).json({ error: '消息长度超过限制' });
        }

        // 构建完整消息
        const fullMessage = prefix ? `[${prefix}] ${message}` : message;

        await mcClient.sendMessage(fullMessage);
        logger.info('消息已发送到Minecraft', { 
            message,
            prefix,
            time: new Date().toISOString() 
        });
        
        res.status(200).json({ 
            status: 'success',
            sent: {
                message,
                prefix,
                time: new Date().toISOString()
            }
        });
    } catch (error) {
        logger.error('发送消息到Minecraft时出错', { error: error.message });
        res.status(500).json({ error: '发送消息失败' });
    }
});

// 启动服务器
const port = process.env.PORT || 3000;
const server = app.listen(port, () => {
    logger.info(`服务器已启动，监听端口 ${port}`);
});

// 优雅退出
process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);

function gracefulShutdown() {
    logger.info('正在关闭服务器...');
    server.close(() => {
        logger.info('服务器已关闭');
        process.exit(0);
    });

    // 如果10秒内没有完成关闭，强制退出
    setTimeout(() => {
        logger.error('强制关闭服务器');
        process.exit(1);
    }, 10000);
} 