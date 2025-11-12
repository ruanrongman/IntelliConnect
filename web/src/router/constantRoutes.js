export const constantRoutes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    meta: {
      title: 'Layout',
    },
    children: [],
  },
  {
    path:'/404NotFound',
    name:'NotFound',
    component:()=>import('@/views/error/404NotFound.vue')
  },
  { path: '/:pathMatch(.*)*', redirect: '/404NotFound' },
]
