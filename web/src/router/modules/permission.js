export default [
  {
    path: '/permission',
    name: 'Permission',
    component: () => import('@/layout/defaultRouter.vue'),
    meta: {
      title: 'permission',
      auth: ['[ROLE_admin]', '[ROLE_guest]'],
      icon: 'KeyOutlined',
    },
    redirect: {
      path: '/btnPermission',
    },
    children: [
      {
        path: '/btnPermission',
        name: 'BtnPermission',
        component: () => import('@/views/permission/btnPermission.vue'),
        meta: {
          title: 'btnPermission',
          auth: ['[ROLE_admin]', '[ROLE_guest]'],
        },
      },
      {
        path: '/adminPermission',
        name: 'AdminPermission',
        component: () => import('@/views/permission/adminPermission.vue'),
        meta: {
          title: 'adminPermission',
          auth: ['[ROLE_admin]'],
        },
      },
      {
        path: '/userPermission',
        name: 'UserPermission',
        component: () => import('@/views/permission/userPermission.vue'),
        meta: {
          title: 'userPermission',
          auth: ['[ROLE_admin]','[ROLE_guest]'],
        },
      },
    ],
  },
]
