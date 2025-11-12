export default [
  {
    path: '/nestMenu',
    name: 'NestMenu',
    component: () => import('@/views/nestMenu/index.vue'),
    meta: {
      title: 'nestMenu',
      icon: 'UnorderedListOutlined',
      auth: ['admin', 'normal'],
      noBreadcrumbLink: true, //面包屑导航，显示为普通文本
    },
    redirect: {
      path: '/page1',
    },
    children: [
      {
        path: '/page1',
        name: 'Page1',
        component: () => import('@/views/nestMenu/page1/index.vue'),
        meta: {
          title: 'page1',
          auth: ['admin', 'normal'],
          // noHidden: false,
        },
      },
      {
        path: '/page2',
        name: 'Page2',
        component: () => import('@/views/nestMenu/page2/index.vue'),
        meta: {
          title: 'page2',
          auth: ['admin', 'normal'],
          // noHidden: false,
          noBreadcrumbLink: true,
        },
        redirect: {
          path: '/page2-1',
        },
        children: [
          {
            path: '/page2-1',
            name: 'Page2-1',
            component: () => import('@/views/nestMenu/page2/page2-1.vue'),
            meta: {
              title: 'page2_1',
              auth: ['admin', 'normal'],
              // noHidden: false,
            },
          },
          {
            path: '/page2-2',
            name: 'Page2-2',
            component: () => import('@/views/nestMenu/page2/page2-2.vue'),
            meta: {
              title: 'page2_2',
              auth: ['admin', 'normal'],
              // noHidden: false,
            },
          },
        ],
      },
      {
        path: '/page3',
        name: 'Page3',
        component: () => import('@/views/nestMenu/page3/index.vue'),
        meta: {
          title: 'page3',
          auth: ['admin', 'normal'],
          // noHidden: false,
        },
        redirect: {
          path: '/page3-1',
        },
        children: [
          {
            path: '/page3-1',
            name: 'Page3-1',
            component: () => import('@/views/nestMenu/page3/page3-1.vue'),
            meta: {
              title: 'page3_1',
              auth: ['admin', 'normal'],
              // noHidden: false,
              noBreadcrumbLink: true,
            },
          },
          {
            path: '/page3-2',
            name: 'Page3-2',
            component: () => import('@/views/nestMenu/page3/page3-2/index.vue'),
            meta: {
              title: 'page3_2',
              auth: ['admin', 'normal'],
              // noHidden: false,
            },
            redirect: {
              path: '/page3-2-1',
            },
            children: [
              {
                path: '/page3-2-1',
                name: 'Page3-2-1',
                component: () => import('@/views/nestMenu/page3/page3-2/page3-2-1.vue'),
                meta: {
                  title: 'page3_2_1',
                  auth: ['admin', 'normal'],
                  // noHidden: false,
                  noBreadcrumbLink: true,
                },
              },
            ],
          },
        ],
      },
    ],
  },
]
