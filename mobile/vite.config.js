import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 移动端自己一个端口（5174），但**不改后端**：
// 浏览器只跟 5174 说话，/api 由 dev server 代理到 8080，所以不存在跨域问题。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
})
