import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import 'ant-design-vue/dist/reset.css'
import './styles/index.less'
import { setupPermissionDirective } from './directives/permission'
import { loadSystemParams, getSystemParam } from './utils/system-params'

const app = createApp(App)

// 加载系统参数并更新页面标题
loadSystemParams().then(() => {
  const systemName = getSystemParam('system.name', '线索申报系统')
  const titleElement = document.getElementById('app-title')
  if (titleElement) {
    titleElement.textContent = systemName
  }
  document.title = systemName
})

app.use(createPinia())
app.use(router)

// 注册权限指令
setupPermissionDirective(app)

app.mount('#app')