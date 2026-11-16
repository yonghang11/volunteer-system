import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // 自动引入 ElMessage / ElMessageBox / ElLoading 等 JS 方法及其所需样式
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      dts: false
    }),
    // 模板中的 el-* 组件按需引入，样式随组件一起加载（首屏体积的关键）
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      directives: true,
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 只把长期不变的框架代码单独抽出来做缓存，其余依赖交给 Rollup
        // 按路由动态导入自动切分，避免把 element-plus 全量打进首屏
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('/vue/') || id.includes('/@vue/') || id.includes('/pinia/')) {
            return 'vue-vendor'
          }
        }
      }
    },
    chunkSizeWarningLimit: 600
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
