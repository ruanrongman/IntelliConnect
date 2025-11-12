export default [
    {
      path: '/deviceData',
      name: 'deviceDataPage',
      component: () => import('@/layout/defaultRouter.vue'),
      meta: {
        title: 'deviceDataPage',
        auth: ['[ROLE_admin]', '[ROLE_guest]'],
        icon: 'DashboardOutlined',
      },
      redirect: {
        path: '/deviceData',
      },
      children: [
        {
          path: '/deviceData',
          name: 'deviceData',
          component: () => import('@/views/deviceMoniter/index.vue'),
          meta: {
            title: 'deviceData',
            auth: ['[ROLE_admin]', '[ROLE_guest]'],
          },
        }
      ],
    },
  ]
  