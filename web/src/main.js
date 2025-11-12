import 'virtual:svg-icons-register' // 注册雪碧图插件

import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router/index.js'
import store from '@/store'
import { setupI18n } from '@/i18n'
import { setupAntd } from '@/plugins/antd'
import { setupComponent } from '@/components'
import { setupDirective } from '@/directives'

import nProgress from '@/plugins/nProgress'
// 全局样式
import '@/assets/global.scss'
import '@/assets/common.scss'

router.beforeEach((to, from, next) => {
  nProgress.start()
  const token = store.getters['auth/token']
  //console.log(token)
  // next()
  if (to.path === '/login'||to.path === '/register'||to.path == '/forgotPassword') {
    // 如果要跳转搭login
    next()
  } else {
    // 存在token
    if (token) {
      const menuList = store.state.auth.menuList
      //  正常跳转 ，已生成路由
      if (menuList.length) {
        next()
      } else {
        //   生成路由再跳转
        const auth = token // 解析token 或者请求后台回去登陆角色
        store.commit('auth/GENERATE_ROUTES', auth)
        next({
          path: to.path,
          replace: true,
        })
        // next()
      }
    } else {
      //  不存在token
      next('/login')
    }
  }
})
router.afterEach((to, from) => {
  nProgress.done()
})
const app = createApp(App)
// 注册 ant-design-vue 组件和图标
setupAntd(app)
// 注册全局组件
setupComponent(app)
// 注册自定义指令
setupDirective(app)

async function initApp(app) {
  await setupI18n(app)
  app.use(store).use(router).mount('#app')
}
initApp(app)
