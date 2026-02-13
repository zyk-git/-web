import { createRouter, createWebHistory } from 'vue-router'
import GuestView from '../views/GuestView.vue'
import AdminView from '../views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: GuestView },
    { path: '/admin', component: AdminView }
  ]
})

export default router
