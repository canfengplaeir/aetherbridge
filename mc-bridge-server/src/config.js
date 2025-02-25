import dotenv from 'dotenv';

// 加载环境变量
dotenv.config();

export function getFeatureConfig() {
    return {
        messageSender: process.env.ENABLE_MESSAGE_SENDER === 'true',
        messageReceiver: process.env.ENABLE_MESSAGE_RECEIVER === 'true'
    };
}

export function isFeatureEnabled(featureId) {
    const features = getFeatureConfig();
    return features[featureId] || false;
} 