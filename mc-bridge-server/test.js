import dotenv from 'dotenv';
import http from 'http';

// 加载环境变量
dotenv.config();

// 测试配置
const config = {
    // 发送到Minecraft的测试消息
    testMessages: [
        { message: "Hello Minecraft!", prefix: "测试" },
        { message: "你好，世界！", prefix: "系统" },
        { message: "这是一条很长的测试消息，用于测试消息长度限制...", prefix: "通知" },
    ],
    
    // 测试间隔(毫秒)
    interval: 2000,
    
    // Minecraft服务器配置
    minecraft: {
        host: process.env.MC_SERVER_HOST || 'localhost',
        port: parseInt(process.env.MC_SERVER_PORT) || 8080,
        apiKey: process.env.API_KEY
    }
};

// 发送测试消息到Minecraft
async function sendTestMessage({ message, prefix }) {
    const data = JSON.stringify({ message, prefix });
    
    const options = {
        hostname: config.minecraft.host,
        port: config.minecraft.port,
        path: '/api/send-to-mc',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${config.minecraft.apiKey}`,
            'Content-Length': Buffer.byteLength(data)
        }
    };

    console.log(`正在发送消息: ${prefix ? `[${prefix}] ` : ''}${message}`);
    
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            if (res.statusCode === 200) {
                console.log(`✅ 消息发送成功: ${prefix ? `[${prefix}] ` : ''}${message}`);
                resolve();
            } else {
                console.error(`❌ 发送失败 (HTTP ${res.statusCode})`);
                reject(new Error(`HTTP Error: ${res.statusCode}`));
            }
        });

        req.on('error', (error) => {
            console.error(`❌ 发送错误: ${error.message}`);
            reject(error);
        });

        req.write(data);
        req.end();
    });
}

// 测试无效token
async function testInvalidToken() {
    const data = JSON.stringify({ message: "这条消息不应该发送成功" });
    
    const options = {
        hostname: config.minecraft.host,
        port: config.minecraft.port,
        path: '/api/send-to-mc',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer invalid-token',
            'Content-Length': Buffer.byteLength(data)
        }
    };

    console.log("\n测试无效token...");
    
    return new Promise((resolve) => {
        const req = http.request(options, (res) => {
            if (res.statusCode === 403) {
                console.log("✅ 无效token测试通过：服务器正确拒绝了请求");
            } else {
                console.error(`❌ 无效token测试失败：服务器返回了意外的状态码 ${res.statusCode}`);
            }
            resolve();
        });

        req.on('error', (error) => {
            console.error(`❌ 测试出错: ${error.message}`);
            resolve();
        });

        req.write(data);
        req.end();
    });
}

// 运行所有测试
async function runTests() {
    console.log("=== 开始测试 AetherBridge ===");
    console.log(`Minecraft服务器: ${config.minecraft.host}:${config.minecraft.port}`);
    
    // 测试无效token
    await testInvalidToken();
    
    // 测试发送消息
    console.log("\n测试发送消息...");
    for (const { message, prefix } of config.testMessages) {
        try {
            await sendTestMessage({ message, prefix });
            // 等待一段时间再发送下一条消息
            await new Promise(resolve => setTimeout(resolve, config.interval));
        } catch (error) {
            console.error(`测试消息发送失败: ${error.message}`);
        }
    }
    
    console.log("\n=== 测试完成 ===");
}

// 运行测试
runTests().catch(console.error); 