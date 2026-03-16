import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import Layout from '@/layout/index.vue'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  }
]

// 预加载关键组件
const preloadComponents = [
  () => import('@/views/system/user/index.vue'),
  () => import('@/views/system/role/index.vue')
]

export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeOutlined' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    name: 'System',
    meta: { title: '系统管理', icon: 'SettingOutlined' },
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'UserOutlined' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'TeamOutlined' }
      },
      {
        path: 'org',
        name: 'Organization',
        component: () => import('@/views/system/org/index.vue'),
        meta: { title: '组织管理', icon: 'ApartmentOutlined' }
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'MenuOutlined' }
      },
      {
        path: 'api-test',
        name: 'ApiTest',
        component: () => import('@/views/system/api-test/index.vue'),
        meta: { title: 'API测试', icon: 'ApiOutlined' }
      },
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', icon: 'SettingOutlined' }
      },
      {
        path: 'product',
        name: 'ProductMaintenance',
        component: () => import('@/views/system/product/index.vue'),
        meta: { title: 'HS商品维护', icon: 'DatabaseOutlined' }
      }
    ]
  },
  {
    path: '/workflow',
    component: Layout,
    name: 'Workflow',
    meta: { title: '工作流', icon: 'BranchesOutlined' },
    children: [
      {
        path: 'definition',
        name: 'ProcessDefinition',
        component: () => import('@/views/workflow/definition/index.vue'),
        meta: { title: '流程定义', icon: 'FileDoneOutlined' }
      },
      {
        path: 'modeler',
        name: 'Modeler',
        component: () => import('@/views/workflow/modeler/index.vue'),
        meta: { title: '流程设计', icon: 'EditOutlined' }
      },
      {
        path: 'monitor',
        name: 'WorkflowMonitor',
        component: () => import('@/views/workflow/monitor/index.vue'),
        meta: { title: '流程监控', icon: 'FundViewOutlined' }
      },
      {
        path: 'instance',
        name: 'ProcessInstance',
        component: () => import('@/views/workflow/instance/index.vue'),
        meta: { title: '流程实例', icon: 'ProfileOutlined' }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/workflow/task/index.vue'),
        meta: { title: '我的任务', icon: 'CheckCircleOutlined' }
      }
    ]
  },
  {
    path: '/business',
    component: Layout,
    name: 'Business',
    meta: { title: '业务管理', icon: 'ShoppingOutlined' },
    children: [
      {
        path: 'finance-review',
        name: 'FinanceReview',
        component: () => import('@/views/business/tax-refund/finance-review.vue'),
        meta: { title: '财务审核', icon: 'AuditOutlined' }
      }
    ]
  },
  {
    path: '/demo',
    component: Layout,
    name: 'Demo',
    meta: { title: '演示功能', icon: 'ExperimentOutlined' },
    children: [
      {
        path: 'shipping-list',
        name: 'ShippingListDemo',
        component: () => import('@/views/demo/simple-shipping-demo.vue'),
        meta: { title: '发货清单演示', icon: 'UnorderedListOutlined' }
      },
      {
        path: 'declaration-form',
        name: 'DeclarationFormDemo',
        component: () => import('@/views/demo/declaration-form-demo.vue'),
        meta: { title: '申报表单演示', icon: 'FileTextOutlined' }
      },
      {
        path: 'declaration-history',
        name: 'DeclarationHistory',
        component: () => import('@/views/demo/declaration-history.vue'),
        meta: { title: '申报历史记录', icon: 'HistoryOutlined' }
      }
    ]
  },
  {
    path: '/declaration',
    component: Layout,
    name: 'Declaration',
    meta: { title: '出口申报', icon: 'FileProtectOutlined' },
    children: [
      {
        path: 'manage',
        name: 'DeclarationManage',
        component: () => import('@/views/declaration/manage/index.vue'),
        meta: { title: '申报管理', icon: 'ContainerOutlined' }
      },
      {
        path: 'form',
        name: 'DeclarationForm',
        component: () => import('@/views/declaration/form/index.vue'),
        meta: { title: '申报表单', icon: 'FileTextOutlined' }
      },
      {
        path: 'statistics',
        name: 'DeclarationStatistics',
        component: () => import('@/views/declaration/statistics/index.vue'),
        meta: { title: '申报统计', icon: 'BarChartOutlined' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

// 预加载组件
preloadComponents.forEach(loader => {
  loader().catch(err => console.warn('预加载组件失败:', err))
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  console.log('路由守卫触发:', to.path)
  const userStore = useUserStore()
  
  if (to.path === '/login') {
    next()
  } else {
    if (userStore.token) {
      console.log('用户已登录，routes长度:', userStore.routes.length)
      if (userStore.routes.length === 0) {
        try {
          console.log('开始获取用户信息和生成路由')
          await userStore.getUserInfo()
          await userStore.generateRoutes()
          console.log('路由生成完成，重新导航')
          next({ ...to, replace: true })
        } catch (error) {
          console.error('路由守卫错误:', error)
          await userStore.resetToken()
          next(`/login?redirect=${to.path}`)
        }
      } else {
        console.log('使用已缓存的路由')
        next()
      }
    } else {
      console.log('用户未登录，跳转到登录页')
      next(`/login?redirect=${to.path}`)
    }
  }
})

export default router