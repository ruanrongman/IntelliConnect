import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
// import constantRoutes from './constantRoutes'
// import asyncRoutes from './asyncRoutes'
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
  },
  {
    path: '/forgotPassword',
    name: 'forget', 
    component: () => import('@/views/forgotPassword/index.vue'),
  },
  {
    path: '/register',
    name: 'Register', 
    component: () => import('@/views/register/index.vue'),
  },
  
  // {
  //   path: '/404NotFound',
  //   name: 'NotFound',
  //   component: () => import('@/views/error/404NotFound.vue'),
  // },

  // {
  //   path: '/:pathMatch(.*)*',
  //   name: 'NotFound',
  //   component: () => import('@/views/error/404NotFound.vue'),
  //   redirect: '/404NotFound',
  // },
]

const router = createRouter({
  history: createWebHashHistory('/vue3-antd-manage/'),
  routes,
})

export { constantRoutes } from './constantRoutes'
export { asyncRoutes } from './asyncRoutes'

export default router


