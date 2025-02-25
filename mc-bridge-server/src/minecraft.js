import http from 'http';

export function createMinecraftClient({ host, port }) {
    return {
        async sendMessage(message) {
            return new Promise((resolve, reject) => {
                const data = JSON.stringify({ message });
                
                const options = {
                    hostname: host,
                    port: port,
                    path: '/api/send-to-mc',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${process.env.API_KEY}`,
                        'Content-Length': Buffer.byteLength(data)
                    }
                };

                const req = http.request(options, (res) => {
                    if (res.statusCode !== 200) {
                        reject(new Error(`HTTP Error: ${res.statusCode}`));
                        return;
                    }
                    
                    res.on('data', () => {}); // 消耗响应数据
                    res.on('end', () => resolve());
                });

                req.on('error', reject);
                req.write(data);
                req.end();
            });
        }
    };
} 