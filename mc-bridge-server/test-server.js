import express from 'express';
import dotenv from 'dotenv';
import winston from 'winston';

// 加载环境变量
dotenv.config();

// 配置日志
const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json()
    ),
    transports: [
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        })
    ]
});

// 创建测试服务器
const app = express();
app.use(express.json());

// 验证API密钥中间件
const validateApiKey = (req, res, next) => {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        logger.warn('请求缺少Authorization头');
        return res.status(401).json({ error: '未提供认证令牌' });
    }
    
    const token = authHeader.split(' ')[1];
    
    if (token !== process.env.API_KEY) {
        logger.warn('无效的API密钥');
        return res.status(403).json({ error: '无效的认证令牌' });
    }
    
    next();
};

// 接收来自Minecraft的消息
app.post('/api/mc-message', validateApiKey, (req, res) => {
    const { player, message } = req.body;
    
    logger.info('收到来自Minecraft的消息', {
        player,
        message,
        timestamp: new Date().toISOString()
    });
    
    // 这里可以添加消息处理逻辑
    // 例如：转发到其他服务、保存到数据库等
    
    res.status(200).json({
        status: 'success',
        received: {
            player,
            message,
            time: new Date().toISOString()
        }
    });
});

// 启动测试服务器
const port = parseInt(process.env.PORT) || 3000;
app.listen(port, () => {
    logger.info('=== AetherBridge 测试服务器 ===');
    logger.info(`服务器已启动，监听端口: ${port}`);
    logger.info('等待来自Minecraft的消息...');
    logger.info(`API密钥: ${process.env.API_KEY}`);
    logger.info('提示：在Minecraft中发送消息来测试连接');
    logger.info('按 Ctrl+C 停止服务器');
});

// 优雅退出
process.on('SIGINT', () => {
    logger.info('正在关闭服务器...');
    process.exit(0);
}); 