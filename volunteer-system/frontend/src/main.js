import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/base.css'
// 通过 JS 调用的消息类组件，样式需显式引入（模板中的 el-* 由 unplugin 按需加载）
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import '@/styles/main.css'

import App from './App.vue'
import router from './router'
import countUp from './directives/countUp'

const app = createApp(App)

app.directive('count-up', countUp)

app.use(createPinia())
app.use(router)
app.mount('#app')
