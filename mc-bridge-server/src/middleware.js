export function validateApiKey(req, res, next) {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: '未提供认证令牌' });
    }
    
    const token = authHeader.split(' ')[1];
    
    if (token !== process.env.API_KEY) {
        return res.status(403).json({ error: '无效的认证令牌' });
    }
    
    next();
}

export function checkFeatureEnabled(featureId) {
    return (req, res, next) => {
        if (!isFeatureEnabled(featureId)) {
            return res.status(503).json({ 
                error: '功能未启用',
                feature: featureId
            });
        }
        next();
    };
} 