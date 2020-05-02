module.exports = {
    devServer: {
        proxy: {
            '^/streams/': {
                target: 'http://localhost:3000',
                ws: true,
                changeOrigin: true,
                pathRewrite: {"^/" : "/"}          
            },
            '^/api': {
                target: 'http://localhost:8081/',
                ws: true,
                changeOrigin: true,
                pathRewrite: {"^/" : "/"}          
            }
        }
    }
}
