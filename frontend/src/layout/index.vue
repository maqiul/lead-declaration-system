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
      <!-- 品牌区域 -->
      <div class="logo">
        <div v-if="!collapsed" class="logo-inner">
          <div class="logo-icon-wrap">
            <thunderbolt-outlined class="logo-icon" />
          </div>
          <span class="logo-text">{{ getConfigValue('system.name', '线索申报系统') }}</span>
        </div>
        <div v-else class="logo-icon-wrap logo-icon-wrap--mini">
          <thunderbolt-outlined class="logo-icon" />
        </div>
      </div>
      
      <a-menu
        v-model:selected-keys="selectedKeys"
        :open-keys="openKeys"
        mode="inline"
        theme="dark"
        :items="menuItems"
        @click="handleMenuClick"
        @openChange="handleOpenChange"
        class="modern-menu"
        :selectable="false"
      />
    </a-layout-sider>

    <a-layout :style="{ marginLeft: collapsed ? '80px' : '256px', transition: 'margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1)' }">
      <!-- 头部 -->
      <a-layout-header class="header">
        <div class="header-left">
          <div class="trigger-wrap" @click="() => (collapsed = !collapsed)">
            <menu-unfold-outlined v-if="collapsed" class="trigger" />
            <menu-fold-outlined v-else class="trigger" />
          </div>
          
          <!-- 面包屑导航 -->
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>
              <home-outlined style="margin-right: 4px" />
              首页
            </a-breadcrumb-item>
            <a-breadcrumb-item v-for="item in breadcrumbItems" :key="item">
              {{ item }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        
        <div class="header-right">
          <a-dropdown placement="bottomRight">
            <a class="ant-dropdown-link user-info" @click.prevent>
              <a-avatar :src="userStore.avatar" :size="32" class="user-avatar">
                <template #icon><user-outlined /></template>
              </a-avatar>
              <span class="user-name">{{ userStore.name || '用户' }}</span>
            </a>
            <template #overlay>
              <a-menu class="user-dropdown-menu" @click="handleUserMenuClick">
                <a-menu-item key="profile">
                  <user-outlined />
                  <span style="margin-left: 8px">个人中心</span>
                </a-menu-item>
                <a-menu-item key="settings">
                  <setting-outlined />
                  <span style="margin-left: 8px">系统设置</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="change-password">
                  <lock-outlined />
                  <span style="margin-left: 8px">修改密码</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" style="color: #F43F5E">
                  <logout-outlined />
                  <span style="margin-left: 8px">退出登录</span>
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
      <a-layout-footer v-if="getConfigValue('ui.footer.show', 'true') !== 'false'" class="footer">
        {{ getConfigValue('ui.footer.text', '线索申报系统 ©2026 Created by Admin') }}
      </a-layout-footer>
    </a-layout>
  </a-layout>

  <!-- 修改密码弹窗 -->
  <a-modal
    v-model:open="changePwdVisible"
    title="修改密码"
    :confirm-loading="changePwdLoading"
    @ok="handleChangePassword"
    @cancel="changePwdVisible = false"
    :destroyOnClose="true"
  >
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px;">
      <a-form-item label="旧密码" required>
        <a-input-password v-model:value="changePwdForm.oldPassword" placeholder="请输入旧密码" />
      </a-form-item>
      <a-form-item label="新密码" required>
        <a-input-password v-model:value="changePwdForm.newPassword" placeholder="请输入新密码（至少6位）" />
      </a-form-item>
      <a-form-item label="确认密码" required>
        <a-input-password v-model:value="changePwdForm.confirmPassword" placeholder="请再次输入新密码" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h, watch, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserRoutes } from '@/api/system'
import { changePassword } from '@/api/user'
import { getSystemBasicInfo, getUiConfig } from '@/api/system/config'
import { message } from 'ant-design-vue'
import { 
  MenuUnfoldOutlined, 
  MenuFoldOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  LockOutlined,
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
  ApiOutlined,
  ThunderboltOutlined,
  BankOutlined,
  GlobalOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])
const openKeys = ref<string[]>([])
const menuData = ref<any[]>([])
const loading = ref(false)

// 面包屑
const breadcrumbItems = computed(() => {
  const matched = route.matched
  const items: string[] = []
  matched.forEach(r => {
    if (r.meta?.title && r.path !== '/') {
      items.push(r.meta.title as string)
    }
  })
  return items.length > 0 ? items : ['系统管理']
})

// 系统配置
const systemConfig = ref<Record<string, string>>({
  'system.name': '线索申报系统',
  'ui.footer.text': '线索申报系统 ©2026 Created by Admin',
  'ui.footer.show': 'true'
})

// 安全获取配置值的函数
const getConfigValue = (key: string, defaultValue: string = ''): string => {
  try {
    return systemConfig.value?.[key] ?? defaultValue
  } catch (error) {
    return defaultValue
  }
}

// 监听路由变化更新选中菜单和展开的菜单
watch(() => route.path, (path) => {
  selectedKeys.value = [path]
  
  // 自动判断并展开父级菜单
  const parentPath = path.substring(0, path.lastIndexOf('/'))
  if (parentPath && parentPath !== '/') {
    // 只有不在已展开的列表中时，我们将其覆盖为主展开项（实现加载时和点击跳转时的手风琴效果）
    if (!openKeys.value.includes(parentPath)) {
      openKeys.value = [parentPath]
    }
  }
}, { immediate: true })

// 加载系统配置
const loadSystemConfig = async () => {
  try {
    const basicResponse = await getSystemBasicInfo()
    if (basicResponse.data?.code === 200) {
      Object.assign(systemConfig.value, basicResponse.data.data)
      
      const systemName = getConfigValue('system.name', '线索申报系统')
      const titleElement = document.getElementById('app-title')
      if (titleElement) {
        titleElement.textContent = systemName
      }
      document.title = systemName
    }
    
    const uiResponse = await getUiConfig()
    if (uiResponse.data?.code === 200) {
      Object.assign(systemConfig.value, uiResponse.data.data)
    }
  } catch (error) {
    // 加载失败时使用默认配置
  }
}

// 加载菜单数据
const loadMenuData = async () => {
  try {
    loading.value = true
    const response = await getUserRoutes()
    menuData.value = (response.data?.data && Array.isArray(response.data.data)) 
      ? response.data.data 
      : getDefaultMenu()
    
    if (!menuData.value || menuData.value.length === 0) {
      menuData.value = getDefaultMenu()
    }
  } catch (error) {
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
      { id: 3, menuName: '用户管理', path: '/system/user', icon: 'UserOutlined', isShow: 1 },
      { id: 4, menuName: '角色管理', path: '/system/role', icon: 'TeamOutlined', isShow: 1 },
      { id: 5, menuName: '组织管理', path: '/system/org', icon: 'ApartmentOutlined', isShow: 1 },
      { id: 6, menuName: '菜单管理', path: '/system/menu', icon: 'MenuOutlined', isShow: 1 },
      { id: 13, menuName: '银行账户', path: '/system/bank-account', icon: 'BankOutlined', isShow: 1 },
      { id: 14, menuName: '国家信息', path: '/system/country', icon: 'GlobalOutlined', isShow: 1 },
      { id: 15, menuName: '税务退费', path: '/tax-refund', icon: 'DollarOutlined', isShow: 1 }
    ]
  },
  {
    id: 7,
    menuName: '工作流',
    path: '/workflow',
    icon: 'BranchesOutlined',
    isShow: 1,
    children: [
      { id: 8, menuName: '流程定义', path: '/workflow/definition', icon: 'FileDoneOutlined', isShow: 1 },
      { id: 9, menuName: '流程设计', path: '/workflow/modeler', icon: 'EditOutlined', isShow: 1 },
      { id: 10, menuName: '流程监控', path: '/workflow/monitor', icon: 'FundViewOutlined', isShow: 1 },
      { id: 11, menuName: '流程实例', path: '/workflow/instance', icon: 'ProfileOutlined', isShow: 1 },
      { id: 12, menuName: '我的任务', path: '/workflow/task', icon: 'CheckCircleOutlined', isShow: 1 }
    ]
  }
]

// 菜单项配置
const menuItems = computed(() => {
  const convertMenu = (menus: any[]): any[] => {
    // 添加更严格的空值检查
    if (!menus || !Array.isArray(menus)) {
      return []
    }
    
    return menus
      .filter(menu => {
        if (!menu) return false
        // 检查菜单是否应该显示：status=1 且 isShow/is_show 不为 0
        const isVisible = menu?.isShow !== 0 && menu?.is_show !== 0
        return menu?.status === 1 && menu?.menuType !== 3 && isVisible
      })
      .map(menu => {
        if (!menu) return null
        
        const menuItem: any = {
          key: menu?.path || `menu-${menu?.id || Math.random()}`,
          label: menu?.menuName || '未知菜单'
        }
        
        // 安全地处理图标
        if (menu?.icon) {
          try {
            const IconComponent = getIconComponent(menu.icon)
            if (IconComponent) {
              menuItem.icon = () => h(IconComponent)
            }
          } catch (error) {
            // 图标组件创建失败
          }
        }
        
        // 安全地处理子菜单
        if (menu?.children && Array.isArray(menu.children) && menu.children.length > 0) {
          try {
            const filteredChildren = menu.children.filter((child: any) => {
              if (!child) return false
              // 检查子菜单是否应该显示：status=1 且 isShow/is_show 不为 0
              const isChildVisible = child?.isShow !== 0 && child?.is_show !== 0
              return child?.status === 1 && child?.menuType !== 3 && isChildVisible
            })
            if (filteredChildren.length > 0) {
              menuItem.children = convertMenu(filteredChildren)
            }
          } catch (error) {
            // 子菜单处理失败
          }
        }
        
        return menuItem
      })
      .filter(item => item !== null) // 过滤掉null项
  }
  
  return convertMenu(Array.isArray(menuData.value) ? menuData.value : [])
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
    'ApiOutlined': ApiOutlined,
    'BankOutlined': BankOutlined,
    'GlobalOutlined': GlobalOutlined,
    'DollarOutlined': DollarOutlined
  }
  return iconMap[iconName] || MenuUnfoldOutlined
}

// 防抖
let menuClickTimeout: any = null

const handleMenuClick = ({ key }: { key: string | number }) => {
  // 添加安全检查
  if (!key) {
    return
  }
  
  if (menuClickTimeout) {
    clearTimeout(menuClickTimeout)
  }
  
  menuClickTimeout = setTimeout(() => {
    try {
      const findMenuPath = (menus: any[], targetKey: string, parentPath: string = ''): string | null => {
        if (!Array.isArray(menus)) return null
        
        for (const menu of menus) {
          if (!menu) continue
          
          const menuKey = menu.path || `menu-${menu.id}`
          
          if (menuKey === targetKey) {
            const fullPath = parentPath ? `${parentPath}/${menu.path}` : menu.path
            return fullPath
          }
          
          if (menu.children && Array.isArray(menu.children)) {
            const childPath = findMenuPath(menu.children, targetKey, menu.path)
            if (childPath) return childPath
          }
        }
        return null
      }
      
      const path = findMenuPath(menuData.value, key.toString())
      
      if (path) {
        router.push(path).catch(() => {})
      } else {
        router.push(`/${key}`).catch(() => {})
      }
    } catch (error) {
      // 菜单点击处理异常
    } finally {
      menuClickTimeout = null
    }
  }, 100)
}

// 菜单展开/关闭处理（手风琴模式）
const handleOpenChange = (keys: (string | number)[]) => {
  const latestOpenKey = keys.find(key => openKeys.value.indexOf(String(key)) === -1)
  if (!latestOpenKey) {
    openKeys.value = keys.map(String)
    return
  }
  
  // 从真正用于渲染的 menuItems 提炼一级菜单的 key
  const rootSubmenuKeys = menuItems.value.map((item: any) => String(item.key))
  
  if (rootSubmenuKeys.includes(String(latestOpenKey))) {
    openKeys.value = [String(latestOpenKey)]
  } else {
    openKeys.value = keys.map(String)
  }
}

const handleLogout = async () => {
  await userStore.resetToken()
  router.push('/login')
}

// 用户下拉菜单点击处理
const handleUserMenuClick = ({ key }: { key: string | number }) => {
  const keyStr = String(key)
  switch (keyStr) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/system/config')
      break
    case 'change-password':
      changePwdVisible.value = true
      changePwdForm.oldPassword = ''
      changePwdForm.newPassword = ''
      changePwdForm.confirmPassword = ''
      break
    case 'logout':
      handleLogout()
      break
  }
}

// ========== 修改密码 ==========
const changePwdVisible = ref(false)
const changePwdLoading = ref(false)
const changePwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleChangePassword = async () => {
  if (!changePwdForm.oldPassword) {
    message.warning('请输入旧密码')
    return
  }
  if (!changePwdForm.newPassword || changePwdForm.newPassword.length < 6) {
    message.warning('新密码不能为空且至少6位')
    return
  }
  if (changePwdForm.newPassword !== changePwdForm.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }

  changePwdLoading.value = true
  try {
    const res: any = await changePassword({
      oldPassword: changePwdForm.oldPassword,
      newPassword: changePwdForm.newPassword
    })
    if (res.data.code === 200) {
      message.success('密码修改成功，请重新登录')
      changePwdVisible.value = false
      await userStore.resetToken()
      router.push('/login')
    } else {
      message.error(res.data.message || '密码修改失败')
    }
  } catch (e: any) {
    message.error(e?.message || '密码修改失败')
  } finally {
    changePwdLoading.value = false
  }
}

onMounted(() => {
  loadSystemConfig()
  loadMenuData()
})
</script>

<style scoped>
.layout-wrapper {
  height: 100vh;
  background: linear-gradient(145deg, #F8FAFC 0%, #EEF2FF 50%, #E0E7FF 100%);
  overflow-x: hidden;
}

.sider {
  overflow: hidden;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%) !important;
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.15);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
}

/* 品牌区域 */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  cursor: pointer;
}

.logo:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.2);
}

.logo-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.logo-icon-wrap--mini {
  width: 38px;
  height: 38px;
  border-radius: 12px;
}

.logo-icon {
  color: white;
  font-size: 18px;
}

.logo-text {
  color: white;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #E0E7FF, #C7D2FE, #A5B4FC);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 顶栏 */
.header {
  background: rgba(255, 255, 255, 0.88) !important;
  padding: 0 24px !important;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04), 0 1px 6px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px !important;
  z-index: 100 !important;
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger-wrap {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.trigger-wrap:hover {
  background: rgba(30, 64, 175, 0.06);
}

.trigger {
  font-size: 18px;
  color: #475569;
  transition: color 0.2s;
}

.trigger-wrap:hover .trigger {
  color: #1E40AF;
}

.breadcrumb {
  margin-left: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
  position: relative;
  z-index: 101;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 16px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  text-decoration: none;
  background: transparent;
  border: 1px solid transparent;
  position: relative;
  z-index: 10;
}

.user-info:hover {
  background: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
  transform: translateY(-1px);
}

.user-avatar {
  border: 1.5px solid #fff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

.user-name {
  font-weight: 500;
  color: #1E293B;
  font-size: 14px;
}

/* 内容区域 */
.content {
  margin: 24px;
  padding: 24px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.5);
  min-height: calc(100vh - 64px - 64px - 48px);
  box-sizing: border-box;
  overflow-x: hidden;
}

@media (max-width: 768px) {
  .content {
    margin: 16px;
    padding: 16px;
    border-radius: 16px;
  }
}

/* 底栏 */
.footer {
  text-align: center;
  padding: 20px 24px !important;
  color: #94A3B8 !important;
  font-size: 13px;
  background: transparent !important;
  letter-spacing: 0.3px;
}

/* 菜单样式 */
:deep(.modern-menu) {
  background: transparent !important;
  border: none !important;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 16px;
}

:deep(.modern-menu .ant-menu-item) {
  margin: 3px 12px !important;
  border-radius: 12px !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  height: 42px !important;
  line-height: 42px !important;
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.modern-menu .ant-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.modern-menu .ant-menu-item-selected) {
  background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 100%) !important;
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.3) !important;
  font-weight: 600 !important;
}

:deep(.modern-menu .ant-menu-submenu-title) {
  margin: 3px 12px !important;
  border-radius: 12px !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  height: 42px !important;
  line-height: 42px !important;
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.modern-menu .ant-menu-submenu-title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.modern-menu .ant-menu-submenu-arrow) {
  color: rgba(255, 255, 255, 0.35) !important;
}

:deep(.modern-menu .ant-menu-sub) {
  background: transparent !important;
}
</style>