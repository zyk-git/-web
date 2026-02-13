import { createRouter, createWebHashHistory } from 'vue-router'
import GuestView from '../views/GuestView.vue'
import AdminView from '../views/AdminView.vue'

// 使用 Hash 路由，避免微信内置浏览器/静态部署时 history 刷新 404
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: GuestView },
    { path: '/admin', component: AdminView }
  ]
})

export default router
