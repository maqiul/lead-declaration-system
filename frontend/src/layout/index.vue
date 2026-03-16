<template>
  <a-layout class="layout-wrapper">
    <!-- 侧边栏 -->
    <a-layout-sider 
      v-model:collapsed="collapsed" 
      :trigger="null" 
      collapsible
      width="256"
      class="sider"
    >
      <div class="logo">
        <span v-if="!collapsed" class="logo-text">{{ systemConfig['system.name'] || '线索申报系统' }}</span>
        <span v-else class="logo-mini">{{ (systemConfig['system.name'] || '线索').substring(0, 2) }}</span>
      </div>
      
      <a-menu
        v-model:selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        mode="inline"
        theme="dark"
        :items="menuItems"
        @click="handleMenuClick"
        class="modern-menu"
      />
    </a-layout-sider>

    <a-layout :style="{ marginLeft: collapsed ? '80px' : '256px' }">
      <!-- 头部 -->
      <a-layout-header class="header">
        <div class="header-left">
          <menu-unfold-outlined
            v-if="collapsed"
            class="trigger"
            @click="() => (collapsed = !collapsed)"
          />
          <menu-fold-outlined
            v-else
            class="trigger"
            @click="() => (collapsed = !collapsed)"
          />
          
          <!-- 面包屑导航 -->
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>首页</a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>用户管理</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        
        <div class="header-right">
          <a-dropdown>
            <a class="ant-dropdown-link" @click.prevent>
              <a-avatar :src="userStore.avatar" :size="32" />
              <span style="margin-left: 8px">{{ userStore.name }}</span>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <user-outlined />
                  个人中心
                </a-menu-item>
                <a-menu-item key="settings">
                  <setting-outlined />
                  系统设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区域 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>

      <!-- 底部 -->
      <a-layout-footer v-if="systemConfig['ui.footer.show'] !== 'false'" class="footer">
        {{ systemConfig['ui.footer.text'] || '线索申报系统 ©2026 Created by Admin' }}
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserRoutes } from '@/api/system'
import { getSystemBasicInfo, getUiConfig } from '@/api/system/config'
import { 
  MenuUnfoldOutlined, 
  MenuFoldOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  HomeOutlined,
  TeamOutlined,
  ApartmentOutlined,
  MenuOutlined,
  FileDoneOutlined,
  ProfileOutlined,
  CheckCircleOutlined,
  EditOutlined,
  FundViewOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  ApiOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])
const openKeys = ref<string[]>([])
const menuData = ref<any[]>([])
const loading = ref(false)

// 系统配置
const systemConfig = ref<Record<string, string>>({
  'system.name': '线索申报系统',
  'ui.footer.text': '线索申报系统 ©2026 Created by Admin',
  'ui.footer.show': 'true'
})

// 加载系统配置
const loadSystemConfig = async () => {
  try {
    // 加载系统基本信息
    const basicResponse = await getSystemBasicInfo()
    if (basicResponse.data?.code === 200) {
      Object.assign(systemConfig.value, basicResponse.data.data)
      
      // 更新页面标题
      const systemName = systemConfig.value['system.name'] || '线索申报系统'
      const titleElement = document.getElementById('app-title')
      if (titleElement) {
        titleElement.textContent = systemName
      }
      document.title = systemName
    }
    
    // 加载UI配置
    const uiResponse = await getUiConfig()
    if (uiResponse.data?.code === 200) {
      Object.assign(systemConfig.value, uiResponse.data.data)
    }
  } catch (error) {
    console.warn('加载系统配置失败，使用默认配置')
  }
}

// 加载菜单数据
const loadMenuData = async () => {
  try {
    loading.value = true
    const response = await getUserRoutes()
    menuData.value = response.data.data || []
    
    console.log('获取到的菜单数据:', menuData.value)
    console.log('菜单数据详细结构:')
    menuData.value.forEach((menu: any) => {
      console.log(`  ${menu.menuName}: path=${menu.path}, type=${menu.menuType}`)
      if (menu.children) {
        menu.children.forEach((child: any) => {
          console.log(`    └─ ${child.menuName}: path=${child.path}, type=${child.menuType}`)
        })
      }
    })
    
    // 如果后端没有返回菜单数据，使用默认菜单
    if (!menuData.value || menuData.value.length === 0) {
      console.log('使用默认菜单')
      menuData.value = getDefaultMenu()
    }
  } catch (error) {
    console.error('加载菜单失败:', error)
    // 使用默认菜单
    menuData.value = getDefaultMenu()
  } finally {
    loading.value = false
  }
}

// 默认菜单配置
const getDefaultMenu = () => [
  {
    id: 1,
    menuName: '首页',
    path: '/dashboard',
    icon: 'HomeOutlined',
    isShow: 1
  },
  {
    id: 2,
    menuName: '系统管理',
    path: '/system',
    icon: 'SettingOutlined',
    isShow: 1,
    children: [
      {
        id: 3,
        menuName: '用户管理',
        path: '/system/user',
        icon: 'UserOutlined',
        isShow: 1
      },
      {
        id: 4,
        menuName: '角色管理',
        path: '/system/role',
        icon: 'TeamOutlined',
        isShow: 1
      },
      {
        id: 5,
        menuName: '组织管理',
        path: '/system/org',
        icon: 'ApartmentOutlined',
        isShow: 1
      },
      {
        id: 6,
        menuName: '菜单管理',
        path: '/system/menu',
        icon: 'MenuOutlined',
        isShow: 1
      }
    ]
  },
  {
    id: 7,
    menuName: '工作流',
    path: '/workflow',
    icon: 'BranchesOutlined',
    isShow: 1,
    children: [
      {
        id: 8,
        menuName: '流程定义',
        path: '/workflow/definition',
        icon: 'FileDoneOutlined',
        isShow: 1
      },
      {
        id: 9,
        menuName: '流程设计',
        path: '/workflow/modeler',
        icon: 'EditOutlined',
        isShow: 1
      },
      {
        id: 10,
        menuName: '流程监控',
        path: '/workflow/monitor',
        icon: 'FundViewOutlined',
        isShow: 1
      },
      {
        id: 11,
        menuName: '流程实例',
        path: '/workflow/instance',
        icon: 'ProfileOutlined',
        isShow: 1
      },
      {
        id: 12,
        menuName: '我的任务',
        path: '/workflow/task',
        icon: 'CheckCircleOutlined',
        isShow: 1
      }
    ]
  }
]

// 菜单项配置
const menuItems = computed(() => {
  console.log('原始menuData值:', menuData.value)
  
  // 显示过滤前的菜单统计
  const allMenus = Array.isArray(menuData.value) ? menuData.value.flat() : [menuData.value]
  console.log('菜单类型统计:', {
    total: allMenus.length,
    directories: allMenus.filter(m => m.menuType === 1).length,
    menus: allMenus.filter(m => m.menuType === 2).length,
    buttons: allMenus.filter(m => m.menuType === 3).length
  })
  
  const convertMenu = (menus: any[]): any[] => {
    return menus
      .filter(menu => menu.status === 1 && menu.menuType !== 3)
      .map(menu => {
        const menuItem: any = {
          key: menu.path || `menu-${menu.id}`,
          label: menu.menuName
        }
        
        // 设置图标
        if (menu.icon) {
          const IconComponent = getIconComponent(menu.icon)
          if (IconComponent) {
            menuItem.icon = () => h(IconComponent)
          }
        }
        
        // 处理子菜单
        if (menu.children && menu.children.length > 0) {
          const filteredChildren = menu.children.filter((child: any) => child.status === 1 && child.menuType !== 3)
          if (filteredChildren.length > 0) {
            menuItem.children = convertMenu(filteredChildren)
          }
        }
        
        return menuItem
      })
  }
  
  return convertMenu(Array.isArray(menuData.value) ? menuData.value : [menuData.value])
})

// 获取图标组件
const getIconComponent = (iconName: string) => {
  const iconMap: Record<string, any> = {
    'HomeOutlined': HomeOutlined,
    'SettingOutlined': SettingOutlined,
    'UserOutlined': UserOutlined,
    'TeamOutlined': TeamOutlined,
    'ApartmentOutlined': ApartmentOutlined,
    'MenuOutlined': MenuOutlined,
    'BranchesOutlined': BranchesOutlined,
    'FileDoneOutlined': FileDoneOutlined,
    'ProfileOutlined': ProfileOutlined,
    'CheckCircleOutlined': CheckCircleOutlined,
    'EditOutlined': EditOutlined,
    'FundViewOutlined': FundViewOutlined,
    'DatabaseOutlined': DatabaseOutlined,
    'ApiOutlined': ApiOutlined
  }
  return iconMap[iconName] || MenuUnfoldOutlined
}

// 防抖节流变量
let menuClickTimeout: number | null = null

const handleMenuClick = ({ key }: { key: string | number }) => {
  // 防止重复点击
  if (menuClickTimeout) {
    clearTimeout(menuClickTimeout)
  }
  
  menuClickTimeout = setTimeout(() => {
    console.log('点击菜单key:', key)
    console.log('菜单数据:', menuData.value)
    console.log('当前路由:', router.currentRoute.value.path)
    
    // 根据key找到对应的菜单路径
    const findMenuPath = (menus: any[], targetKey: string, parentPath: string = ''): string | null => {
      for (const menu of menus) {
        const menuKey = menu.path || `menu-${menu.id}`
        console.log(`比较菜单: ${menu.menuName}, key: ${menuKey}, target: ${targetKey}`)
        
        if (menuKey === targetKey) {
          // 构造完整路径：父路径 + 当前路径
          const fullPath = parentPath ? `${parentPath}/${menu.path}` : menu.path
          console.log('找到匹配路径:', fullPath)
          return fullPath
        }
        
        // 递归查找子菜单
        if (menu.children) {
          const childPath = findMenuPath(menu.children, targetKey, menu.path)
          if (childPath) return childPath
        }
      }
      return null
    }
    
    const path = findMenuPath(menuData.value, key.toString())
    console.log('最终跳转路径:', path)
    
    if (path) {
      router.push(path).catch(err => {
        console.error('路由跳转失败:', err)
      })
    } else {
      const fallbackPath = `/${key}`
      console.log('使用备选路径:', fallbackPath)
      router.push(fallbackPath).catch(err => {
        console.error('备选路由跳转失败:', err)
      })
    }
  }, 100) // 100ms防抖
}

const handleLogout = async () => {
  await userStore.resetToken()
  router.push('/login')
}

// 组件挂载时加载菜单
onMounted(() => {
  loadSystemConfig()
  loadMenuData()
})
</script>

<style scoped>
.layout-wrapper {
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow-x: hidden;
}

.sider {
  overflow: hidden;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  box-shadow: 2px 0 20px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  margin: 16px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  transition: var(--transition-ease);
  flex-shrink: 0; /* 防止Logo被压缩 */
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 40px rgba(31, 38, 135, 0.25);
  }
}

.logo-text {
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.logo-mini {
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.header {
  background: var(--glass-bg);
  padding: 0 24px;
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  line-height: 64px;
  padding: 0 24px;
  cursor: pointer;
  transition: var(--transition-ease);
  border-radius: 12px;
  margin: 8px 0;
  
  &:hover {
    color: #667eea;
    background: rgba(102, 126, 234, 0.1);
    transform: scale(1.05);
  }
  
  &:active {
    transform: scale(0.95);
  }
}

.breadcrumb {
  margin-left: 16px;
}

.content {
  margin: 24px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  min-height: calc(100vh - 64px - 64px - 48px); /* 只减去头部和margin高度 */
  transition: var(--transition-ease);
  box-sizing: border-box;
  overflow-x: hidden;
  
  &:hover {
    box-shadow: 0 12px 40px rgba(31, 38, 135, 0.2);
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .content {
    margin: 16px;
    padding: 16px;
    border-radius: 16px;
  }
}

.footer {
  text-align: center;
  padding: 24px;
  color: #6b7280;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

/* 现代化菜单样式 */
:deep(.modern-menu) {
  background: transparent !important;
  border: none !important;
  flex: 1;
  overflow: hidden;
}

:deep(.modern-menu .ant-menu-item) {
  margin: 4px 16px !important;
  border-radius: 12px !important;
  transition: var(--transition-ease) !important;
}

:deep(.modern-menu .ant-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
}

:deep(.modern-menu .ant-menu-item-selected) {
  background: var(--primary-gradient) !important;
  color: white !important;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3) !important;
}

:deep(.modern-menu .ant-menu-submenu-title) {
  margin: 4px 16px !important;
  border-radius: 12px !important;
  transition: var(--transition-ease) !important;
}

:deep(.modern-menu .ant-menu-submenu-title:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
}

:deep(.modern-menu .ant-menu-submenu-arrow) {
  color: rgba(255, 255, 255, 0.6) !important;
}
</style>