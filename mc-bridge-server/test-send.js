import dotenv from 'dotenv';
import axios from 'axios';
import readline from 'readline';

// 加载环境变量
dotenv.config();

// 创建命令行接口
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

// 配置
const config = {
    minecraft: {
        host: process.env.MC_SERVER_HOST || 'mcstatus.especial.top',
        port: parseInt(process.env.MC_SERVER_PORT) || 8080,
        apiKey: process.env.API_KEY,
        useIPv6: false  // 默认使用IPv4
    }
};

// 创建axios实例
const axiosInstance = axios.create({
    timeout: 5000,
    validateStatus: function (status) {
        return true; // 允许所有状态码，以便我们可以自己处理
    },
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer your-secret-key-8ae023bc-72b1-4ac5-a13c-8aef25fc7198`  // 使用固定的API密钥
    }
});

// 发送消息到Minecraft
async function sendMessage(message, prefix = null) {
    try {
        const protocol = 'http'; // 或 'https'
        const baseURL = `${protocol}://${config.minecraft.host}:${config.minecraft.port}`;
        const url = '/api/send-to-mc';

        console.log(`发送消息: ${prefix ? `[${prefix}] ` : ''}${message}`);
        console.log(`连接到: ${baseURL}${url}`);

        // 添加调试信息
        console.log('使用API密钥:', axiosInstance.defaults.headers['Authorization']);

        const response = await axiosInstance.post(baseURL + url, {
            message,
            prefix
        });

        // 详细的响应信息
        console.log('响应状态码:', response.status);
        console.log('响应头:', response.headers);
        console.log('响应数据:', response.data);

        if (response.status === 200) {
            console.log('✅ 消息发送成功');
        } else if (response.status === 403) {
            console.error('❌ API密钥验证失败');
        } else {
            console.error(`❌ 服务器返回错误: ${response.status}`);
        }
    } catch (error) {
        console.error('❌ 发送错误:', error.message);
        // 添加更详细的错误信息
        if (error.response) {
            console.error('响应状态:', error.response.status);
            console.error('响应头:', error.response.headers);
            console.error('响应数据:', error.response.data);
        } else if (error.request) {
            console.error('请求发送成功但没有收到响应');
            console.error('请求详情:', error.request);
        }

        if (error.code === 'ECONNREFUSED') {
            console.error('无法连接到服务器，请检查服务器是否运行');
        } else if (error.code === 'ETIMEDOUT') {
            console.error('连接超时，请检查网络连接');
        }

        throw error;
    }
}

// 预设的消息前缀
const prefixes = {
    '1': 'QQ',
    '2': '系统',
    '3': '公告',
    '4': '管理',
    '0': null  // 无前缀
};

// 显示帮助信息
function showHelp() {
    console.log('\n=== 消息发送测试工具 ===');
    console.log('前缀选项:');
    console.log('1: [QQ]');
    console.log('2: [系统]');
    console.log('3: [公告]');
    console.log('4: [管理]');
    console.log('0: 无前缀');
    console.log('\n命令:');
    console.log('help: 显示帮助');
    console.log('exit: 退出程序');
    console.log('clear: 清屏');
    console.log('ipv4: 切换到IPv4');
    console.log('ipv6: 切换到IPv6');
    console.log('====================\n');
}

// 主循环
async function main() {
    showHelp();
    
    while (true) {
        const prefix = await new Promise(resolve => {
            rl.question('选择前缀 (0-4, help, exit, clear, ipv4, ipv6): ', resolve);
        });
        
        // 处理特殊命令
        if (prefix === 'exit') {
            console.log('再见！');
            break;
        } else if (prefix === 'help') {
            showHelp();
            continue;
        } else if (prefix === 'clear') {
            console.clear();
            continue;
        } else if (prefix === 'ipv4') {
            config.minecraft.useIPv6 = false;
            console.log('已切换到IPv4模式');
            continue;
        } else if (prefix === 'ipv6') {
            config.minecraft.useIPv6 = true;
            console.log('已切换到IPv6模式');
            continue;
        }
        
        // 验证前缀选择
        if (!prefixes.hasOwnProperty(prefix)) {
            console.log('❌ 无效的前缀选择');
            continue;
        }
        
        const message = await new Promise(resolve => {
            rl.question('输入消息: ', resolve);
        });
        
        if (!message) {
            console.log('❌ 消息不能为空');
            continue;
        }
        
        try {
            await sendMessage(message, prefixes[prefix]);
        } catch (error) {
            console.error('发送失败:', error.message);
        }
    }
    
    rl.close();
}

// 运行程序
main().catch(console.error); 