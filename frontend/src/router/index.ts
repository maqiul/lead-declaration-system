import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import Layout from '@/layout/index.vue'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  }
]

// 预加载关键组件
const preloadComponents = [
  () => import('@/views/system/user/index.vue'),
  () => import('@/views/system/role/index.vue'),
  () => import('@/views/system/bank-account/index.vue'),
  () => import('@/views/system/country/index.vue'),
  () => import('@/views/system/measurement-unit/index.vue')
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
        component: () => import('@/views/dashboard/simple.vue'),
        meta: { title: '首页', icon: 'HomeOutlined', affix: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', hidden: true }
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
      },
      {
        path: 'bank-account',
        name: 'BankAccount',
        component: () => import('@/views/system/bank-account/index.vue'),
        meta: { title: '银行账户', icon: 'BankOutlined' }
      },
      {
        path: 'country',
        name: 'Country',
        component: () => import('@/views/system/country/index.vue'),
        meta: { title: '国家信息', icon: 'GlobalOutlined' }
      },
      {
        path: 'transport-mode',
        name: 'TransportMode',
        component: () => import('@/views/system/transport-mode/index.vue'),
        meta: { title: '运输方式', icon: 'CarOutlined', permission: 'system:transport:view' }
      },
      {
        path: 'payment-method',
        name: 'PaymentMethod',
        component: () => import('@/views/system/payment-method/index.vue'),
        meta: { title: '支付方式', icon: 'MoneyCollectOutlined', permission: 'system:payment:view' }
      },
      {
        path: 'currency',
        name: 'Currency',
        component: () => import('@/views/system/currency/index.vue'),
        meta: { title: '货币管理', icon: 'MoneyCollectOutlined', permission: 'system:currency:view' }
      },
      {
        path: 'measurement-unit',
        name: 'MeasurementUnit',
        component: () => import('@/views/system/measurement-unit/index.vue'),
        meta: { title: '计量单位管理', icon: 'DashboardOutlined', permission: 'system:measurement-unit:list' }
      },
      {
        path: 'city-info',
        name: 'CityInfo',
        component: () => import('@/views/system/city-info/index.vue'),
        meta: { title: '城市管理', icon: 'EnvironmentOutlined', permission: 'system:city-info:list' }
      },
      {
        path: 'material-template',
        name: 'MaterialTemplate',
        component: () => import('@/views/system/material-template/index.vue'),
        meta: { title: '资料项模板', icon: 'FileTextOutlined', permission: 'system:material:template:view' }
      }
    ]
  },
  {
    path: '/tax-refund',
    component: Layout,
    name: 'TaxRefund',
    redirect: '/tax-refund/list',
    meta: { title: '税务退费', icon: 'DollarOutlined' },
    children: [
      {
        path: 'apply',
        name: 'TaxRefundApply',
        component: () => import('@/views/tax-refund/apply/index.vue'),
        meta: { title: '退税申请', icon: 'PlusOutlined' }
      },
      {
        path: 'list',
        name: 'TaxRefundList',
        component: () => import('@/views/tax-refund/list/index.vue'),
        meta: { title: '申请列表', icon: 'UnorderedListOutlined' }
      },
      {
        path: 'detail/:id',
        name: 'TaxRefundDetail',
        component: () => import('@/views/tax-refund/detail/index.vue'),
        meta: { title: '申请详情', icon: 'FileSearchOutlined', hideInMenu: true }
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
    path: '/remittance',
    component: Layout,
    name: 'Remittance',
    meta: { title: '水单管理', icon: 'AccountBookOutlined' },
    children: [
      {
        path: 'list',
        name: 'RemittanceList',
        component: () => import('@/views/remittance/list/index.vue'),
        meta: { title: '水单列表', icon: 'ListOutlined' }
      },
      {
        path: 'audit',
        name: 'RemittanceAudit',
        component: () => import('@/views/remittance/audit/index.vue'),
        meta: { title: '水单审核', icon: 'AuditOutlined', permission: 'business:remittance:audit' }
      }
    ]
  },
  {
    path: '/finance-invoice',
    component: Layout,
    name: 'FinanceInvoice',
    meta: { title: '发票管理', icon: 'FileTextOutlined', permission: 'finance:invoice:view' },
    children: [
      {
        path: 'index',
        name: 'FinanceInvoiceIndex',
        component: () => import('@/views/finance/invoice/index.vue'),
        meta: { title: '发票台账', icon: 'FileTextOutlined' }
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
        path: 'finance',
        name: 'DeclarationFinance',
        component: () => import('@/views/declaration/finance/index.vue'),
        meta: { title: '财务单证', icon: 'PayCircleOutlined', permission: 'business:declaration:financeSupplement' }
      },
      {
        path: 'form',
        name: 'DeclarationForm',
        component: () => import('@/views/declaration/form/index.vue'),
        meta: { title: '申报表单', icon: 'FileTextOutlined' }
      },
      {
        path: 'payment',
        name: 'DeclarationPayment',
        component: () => import('@/views/declaration/payment/index.vue'),
        meta: { title: '支付凭证', icon: 'AccountBookOutlined', hidden: true }
      },
      {
        path: 'statistics',
        name: 'DeclarationStatistics',
        component: () => import('@/views/declaration/statistics/index.vue'),
        meta: { title: '申报统计', icon: 'BarChartOutlined' }
      }
    ]
  },
  {
    path: '/contract',
    component: Layout,
    name: 'Contract',
    meta: { title: '合同管理', icon: 'FileTextOutlined' },
    children: [
      {
        path: 'template',
        name: 'ContractTemplate',
        component: () => import('@/views/contract/template/index.vue'),
        meta: { title: '模板管理', icon: 'FileAddOutlined' }
      },
      {
        path: 'generation',
        name: 'ContractGeneration',
        component: () => import('@/views/contract/generation/index.vue'),
        meta: { title: '合同列表', icon: 'HistoryOutlined' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...constantRoutes, ...asyncRoutes]
})

// 预加载组件
preloadComponents.forEach(loader => {
  loader().catch(() => {})
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  
  if (to.path === '/login') {
    next()
  } else {
    if (userStore.token) {
      if (userStore.routes.length === 0) {
        try {
          await userStore.getUserInfo()
          await userStore.generateRoutes()
          next({ ...to, replace: true })
        } catch (error) {
          await userStore.resetToken()
          next(`/login?redirect=${to.path}`)
        }
      } else {
        next()
      }
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})

export default router