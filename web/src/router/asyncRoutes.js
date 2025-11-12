import moniter from './modules/moniter'
import nestMenu from './modules/nestMenu'
import permission from './modules/permission'
import product from './modules/product'
export const asyncRoutes = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: {
      title: 'dashboard', // 名称
      auth: ['[ROLE_admin]', '[ROLE_guest]'], // 权限
      icon: 'DashboardOutlined',
      // noHidden: true,  // 是否隐藏
    },
  },
  {
    path: '/setting',
    name: 'Setting',
    component: () => import('@/views/setting/index.vue'),
    meta: {
      title: 'setting',
      auth: ['[ROLE_admin]', '[ROLE_guest]'],
      icon: 'SettingOutlined',
      isHidden: true,
    },
  },
  //...nestMenu,
  ...product,
  ...moniter,
  ...permission,
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/about/index.vue'),
    meta: {
      auth: ['[ROLE_admin]', '[ROLE_guest]'],
      title: 'about',
    },
  },
]
