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
      
      <!-- 菜单滚动区：logo 固定置顶，菜单超长时内部滚动。
           注意：必须让菜单 ul 自身作为滚动容器（antd 内联子菜单展开动画依赖此结构），
           外包 wrapper 层会导致子菜单展开测量失效被压成 0px -->
      <a-menu
        v-model:selected-keys="selectedKeys"
        :open-keys="openKeys"
        mode="inline"
        theme="light"
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

      <!-- 多标签页导航 -->
      <div class="tabs-bar">
        <div ref="tabsScrollRef" class="tabs-scroll" @wheel.prevent="onTabsWheel">
          <div
            v-for="tab in visitedTabs"
            :key="tab.key"
            :class="['tab-item', { 'tab-item--active': tab.key === activeTabKey }]"
            @click="goTab(tab)"
            @contextmenu="openTabMenu($event, tab)"
          >
            <span class="tab-dot" />
            <span
              v-if="tab.bizType"
              :class="['tab-type', tab.bizType === 'SELF' ? 'tab-type--self' : 'tab-type--ext']"
            >{{ getDeclarationTypeLabel(tab.bizType) }}</span>
            <span v-tab-marquee class="tab-label" :title="tabLabel(tab)">
              <span class="tab-label__inner">{{ tabLabel(tab) }}</span>
            </span>
            <close-outlined
              v-if="!tab.affix"
              class="tab-close"
              @click.stop="closeTab(tab)"
            />
          </div>
        </div>
        <a-dropdown placement="bottomRight" :trigger="['click']">
          <div class="tabs-action">
            <down-outlined />
          </div>
          <template #overlay>
            <a-menu @click="handleTabAction">
              <a-menu-item key="refresh">
                <reload-outlined />
                <span style="margin-left: 8px">刷新当前页</span>
              </a-menu-item>
              <a-menu-item key="closeOthers">
                <close-outlined />
                <span style="margin-left: 8px">关闭其他</span>
              </a-menu-item>
              <a-menu-item key="closeAll">
                <close-circle-outlined />
                <span style="margin-left: 8px">关闭全部</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>

      <!-- 内容区域 -->
      <a-layout-content class="content">
        <router-view v-slot="{ Component }">
          <keep-alive :max="20">
            <component :is="Component" :key="cacheKey" />
          </keep-alive>
        </router-view>
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

  <!-- 关闭标签前的草稿确认：三态可选，“继续填写”（含右上角×与 ESC）会中止关闭并留在该页 -->
  <a-modal
    :open="draftAskVisible"
    :width="460"
    :mask-closable="false"
    :closable="true"
    title="关闭前是否保存草稿？"
    @cancel="answerDraftAsk('cancel')"
  >
    <p class="draft-ask-tip">{{ draftAskTip }}</p>
    <template #footer>
      <a-button @click="answerDraftAsk('cancel')">继续填写</a-button>
      <a-button danger @click="answerDraftAsk('discard')">不保存关闭</a-button>
      <a-button type="primary" @click="answerDraftAsk('save')">保存为草稿</a-button>
    </template>
  </a-modal>

  <!-- 标签右键菜单 -->
  <teleport to="body">
    <div
      v-if="ctxVisible"
      class="tab-ctx-mask"
      @click="closeCtx"
      @contextmenu.prevent="closeCtx"
    >
      <ul class="tab-ctx-menu" :style="{ left: ctxX + 'px', top: ctxY + 'px' }" @click.stop>
        <li @click="onCtx('refresh')"><reload-outlined /><span>刷新</span></li>
        <li :class="{ 'ctx-disabled': ctxTab?.affix }" @click="onCtx('close')"><close-outlined /><span>关闭</span></li>
        <li class="ctx-divider" />
        <li @click="onCtx('closeLeft')"><vertical-right-outlined /><span>关闭左侧</span></li>
        <li @click="onCtx('closeRight')"><vertical-left-outlined /><span>关闭右侧</span></li>
        <li class="ctx-divider" />
        <li @click="onCtx('closeOthers')"><close-outlined /><span>关闭其他</span></li>
        <li @click="onCtx('closeAll')"><close-circle-outlined /><span>关闭全部</span></li>
      </ul>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h, watch, reactive, nextTick, type Directive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserRoutes } from '@/api/system'
import { changePassword } from '@/api/user'
import { getSystemBasicInfo, getUiConfig } from '@/api/system/config'
import { message } from 'ant-design-vue'
import { getTabKey, getFormTabTitle, getFormTabTitleByFullPath, getRouteCacheBase, isFormTabPath, getFormTabDeclarationType, getDeclarationTypeLabel, getNewFormTabKey, isNewFormTabKey, type DeclarationTabType } from '@/utils/tabKey'
import { findTabGuards, findTabGuardByPath, dropTabGuards, registerTabMetaSetter } from '@/composables/useTabGuard'
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
  DollarOutlined,
  FileProtectOutlined,
  FileTextOutlined,
  AccountBookOutlined,
  UnorderedListOutlined,
  FileSearchOutlined,
  PlusOutlined,
  UploadOutlined,
  FileAddOutlined,
  FolderOpenOutlined,
  ContainerOutlined,
  PayCircleOutlined,
  CarOutlined,
  MoneyCollectOutlined,
  DashboardOutlined,
  EnvironmentOutlined,
  ShopOutlined,
  BookOutlined,
  ClockCircleOutlined,
  LinkOutlined,
  AuditOutlined,
  HistoryOutlined,
  BarChartOutlined,
  CloseOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  DownOutlined,
  VerticalRightOutlined,
  VerticalLeftOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])
const openKeys = ref<string[]>([])
const menuData = ref<any[]>([])
const loading = ref(false)
const menuEl = ref<HTMLElement | null>(null)

onMounted(() => {
  // 菜单 ul 是 a-menu 的根元素，拿到它用于超长时滚动定位选中项
  menuEl.value = document.querySelector('.modern-menu') as HTMLElement | null
})

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

// ========== 多标签页 ==========
interface TabItem { key: string; fullPath: string; title: string; affix?: boolean; bizType?: DeclarationTabType; invoiceNo?: string }
const HOME_TAB: TabItem = { key: '/dashboard', fullPath: '/dashboard', title: '首页', affix: true }
const visitedTabs = ref<TabItem[]>([{ ...HOME_TAB }])
const tabsScrollRef = ref<HTMLElement | null>(null)
const activeTabKey = computed(() => getTabKey(route))

// ========== 页面缓存（keep-alive）==========
// 刷新计数：缓存键变化时强制重新挂载
const refreshNonce = ref<Record<string, number>>({})
// 标签代号：同一标签键下的多个入口地址（readonly / scrollTo 变体）共享，关闭标签即整体作废
const tabGeneration = ref<Record<string, number>>({})
const cacheKey = computed(() => {
  // 表单页按 fullPath 区分（每个入口各自保留编辑状态），其余页按 path 区分（忽略 query，保留搜索/分页状态）
  const base = getRouteCacheBase(route)
  const gen = tabGeneration.value[getTabKey(route)] || 0
  const n = refreshNonce.value[base] || 0
  return `${base}#g${gen}${n ? `r${n}` : ''}`
})

// 取当前路由标题（表单页直接给出“新建申报/查看申报”，其余拼接上一级菜单名）
const getRouteTitle = (): string => {
  const formTitle = getFormTabTitle(route)
  if (formTitle) return formTitle
  const titles: string[] = []
  route.matched.forEach(r => {
    if (r.meta?.title && r.path !== '/') {
      titles.push(r.meta.title as string)
    }
  })
  if (titles.length === 0) {
    return (route.meta?.title as string) || '未命名页面'
  }
  // 取最后两级（上一级 + 当前），避免标题过长
  return titles.slice(-2).join(' / ')
}

// 滚动使激活标签可见
const scrollToActive = () => {
  nextTick(() => {
    const el = tabsScrollRef.value?.querySelector('.tab-item--active') as HTMLElement | null
    el?.scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' })
  })
}

// 标签区域鼠标滚轮转横向滚动（标签过多时可滑动）
const onTabsWheel = (e: WheelEvent) => {
  const el = tabsScrollRef.value
  if (!el) return
  const delta = Math.abs(e.deltaY) >= Math.abs(e.deltaX) ? e.deltaY : e.deltaX
  if (delta) el.scrollBy({ left: delta })
}

// 记录已访问页面：同键标签复用，离开后不再自动关闭
const addTab = () => {
  if (route.path === '/login' || route.path === '/404') return
  const key = getTabKey(route)
  const title = getRouteTitle()
  const bizType = getFormTabDeclarationType(route)
  const existing = visitedTabs.value.find(t => t.key === key)
  if (existing) {
    // 同一单据切换入口（查看/编辑/资料提交）时，标签跟随最近一次访问的地址
    existing.fullPath = route.fullPath
    existing.title = title
    // 路由定得出类型才刷新，定不出时保留页面回写的值，避免徽标闪没
    if (bizType) existing.bizType = bizType
    scrollToActive()
    return
  }
  // 新建标签在首次保存为草稿后原地转为“查看申报”，避免残留一个已失效的新建标签
  // 只转同一表单路径下的新建标签：新建槽位按申报类型独立，同套的 /form 与 /form-v2 不互相顶替
  const draftTab = isFormTabPath(route.path)
    ? visitedTabs.value.find(t => t.key === getNewFormTabKey(route.path) && t.fullPath.split('?')[0] === route.path)
    : undefined
  if (draftTab) {
    draftTab.key = key
    draftTab.fullPath = route.fullPath
    draftTab.title = title
    if (bizType) draftTab.bizType = bizType
  } else {
    visitedTabs.value.push({ key, fullPath: route.fullPath, title, bizType: bizType || undefined })
  }
  scrollToActive()
}

// 表单页装载完成后回写展示元信息：老链接（/declaration/*）推不出类型时纠正徽标，发票号到位后标签改显发票号
registerTabMetaSetter((tabKey, meta) => {
  const tab = visitedTabs.value.find(t => t.key === tabKey)
  if (!tab) return
  if (meta.bizType) tab.bizType = meta.bizType
  if (meta.invoiceNo !== undefined) tab.invoiceNo = meta.invoiceNo || undefined
})

/** 标签展示文案：表单页在页面回写发票号后以发票号替代数据库编号 */
const tabLabel = (tab: TabItem): string => getFormTabTitleByFullPath(tab.fullPath, tab.invoiceNo) || tab.title

/**
 * 标签文字溢出检测（配合 CSS 跑马灯）
 * 纯 CSS 判不出溢出（文本比可视区窄时同一位移动画会反向跑出左边界），必须实测宽度；
 * 溢出量同时写成 CSS 变量，位移与时长跟着内容走，长发票号不会一闪而过
 */
const measureTabOverflow = (el: HTMLElement) => {
  const inner = el.querySelector<HTMLElement>('.tab-label__inner')
  if (!inner) return
  const shift = Math.max(inner.offsetWidth - el.clientWidth, 0)
  el.classList.toggle('tab-label--scroll', shift > 1)
  el.style.setProperty('--tab-marquee-shift', `${shift}px`)
  el.style.setProperty('--tab-marquee-duration', `${Math.min(Math.max(shift / 22, 5), 16)}s`)
}

const tabMarqueeObserver = new WeakMap<HTMLElement, ResizeObserver>()

/** 局部指令：只服务于标签栏，不全局注册 */
const vTabMarquee: Directive<HTMLElement> = {
  mounted(el) {
    measureTabOverflow(el)
    // 发票号是页面异步回写的，文本变长后需重新测量（只观察内层文字，避开自身样式写入引发的循环）
    if (typeof ResizeObserver === 'undefined') return
    const inner = el.querySelector<HTMLElement>('.tab-label__inner')
    if (!inner) return
    const ro = new ResizeObserver(() => measureTabOverflow(el))
    ro.observe(inner)
    tabMarqueeObserver.set(el, ro)
  },
  updated(el) {
    measureTabOverflow(el)
  },
  unmounted(el) {
    tabMarqueeObserver.get(el)?.disconnect()
    tabMarqueeObserver.delete(el)
  }
}

const goTab = (tab: TabItem) => {
  if (tab.key !== activeTabKey.value) {
    router.push(tab.fullPath).catch(() => {})
  }
}

// 关闭含未保存内容的标签前，询问是否先保存为草稿
/** 草稿确认结果：save 存草稿后关闭，discard 不存直接关闭，cancel 中止关闭并留在该页继续填 */
type DraftAskAction = 'save' | 'discard' | 'cancel'

const draftAskVisible = ref(false)
const draftAskTitle = ref('')
let draftAskResolve: ((action: DraftAskAction) => void) | null = null

const draftAskTip = computed(() =>
  `“${draftAskTitle.value}” 存在未保存的内容。可以先存为草稿，之后到申报录入列表里继续填写。`
)

/**
 * 弹出草稿确认
 * 用受控 a-modal 而非 Modal.confirm：confirm 只能给两个按钮，且右上角×与 ESC 等同于取消按钮，
 * 无法单独表达“不关了、我还在填”这个选项
 */
const askSaveDraft = (title: string): Promise<DraftAskAction> => new Promise(resolve => {
  draftAskTitle.value = title
  draftAskVisible.value = true
  draftAskResolve = resolve
})

/** 应答草稿确认：右上角×与 ESC 由 a-modal 的 cancel 事件进来，同样算“继续填写” */
const answerDraftAsk = (action: DraftAskAction) => {
  draftAskVisible.value = false
  const resolve = draftAskResolve
  draftAskResolve = null
  resolve?.(action)
}

/**
 * 关闭前先把视图切到目标标签（可选切到该标签下真正有未保存内容的那个地址）
 * 导航被守卫中断时再试一程 replace（replace 不改历史，总能落到目标页），
 * 避免“弹窗在另一张页面上弹”的错乱感
 */
const focusTab = async (tab: TabItem, address?: string): Promise<void> => {
  const target = address || tab.fullPath
  if (route.fullPath === target) return
  await router.push(target).catch(() => {})
  await nextTick()
  if (tab.key !== activeTabKey.value) {
    await router.replace(target).catch(() => {})
    await nextTick()
  }
}

/** 逐个标签确认关闭（保存失败时中止，避免已填写内容静默丢失） */
const ensureTabsClosable = async (tabs: TabItem[]): Promise<boolean> => {
  for (const tab of tabs) {
    if (tab.affix) continue
    // 一个标签键下可能活着多个表单实例（重复点新建、/form 与 /form-v2 共用新建槽位），
    // 任意一个有未保存内容就要问；优先用标签当前地址那份，否则用最近一个脏实例并跳到它自己的页面
    let dirtyGuards = findTabGuards(tab.key).filter(guard => guard.isDirty())
    // 兜底：标签键与页面注册的键对不上时（历史快照按旧算法算的）按地址再认一次
    if (dirtyGuards.length === 0) {
      const byPath = findTabGuardByPath(tab.fullPath)
      if (byPath && byPath.isDirty()) dirtyGuards = [byPath]
    }
    if (dirtyGuards.length === 0) continue
    const target = dirtyGuards.find(guard => guard.fullPath === tab.fullPath)
      || dirtyGuards[dirtyGuards.length - 1]
    // 先置顶再弹询问：让用户看清关的是哪张单，保存也发生在可见页面上
    await focusTab(tab, target.fullPath)
    const action = await askSaveDraft(tabLabel(tab))
    // 继续填写：整批关闭到此中止，标签保留，视图已停在用户要填的那张单上
    if (action === 'cancel') return false
    if (action === 'discard') continue
    if (!(await target.save())) return false
  }
  return true
}

const closeTab = async (tab: TabItem) => {
  if (!(await ensureTabsClosable([tab]))) return
  const idx = visitedTabs.value.findIndex(t => t.key === tab.key)
  if (idx === -1) return
  visitedTabs.value.splice(idx, 1)
  purgeTabCache([tab])
  // 关闭的是当前活跃标签时，跳转到相邻标签
  if (tab.key === activeTabKey.value) {
    const next = visitedTabs.value[idx] || visitedTabs.value[idx - 1] || HOME_TAB
    router.push(next.fullPath).catch(() => {})
  }
}

// 关闭指定标签左侧的所有标签（保留 affix）
const closeLeftOf = async (tab: TabItem) => {
  const idx = visitedTabs.value.findIndex(t => t.key === tab.key)
  if (idx <= 0) return
  const removing = visitedTabs.value.filter((t, i) => i < idx && !t.affix)
  if (!(await ensureTabsClosable(removing))) return
  visitedTabs.value = visitedTabs.value.filter((t, i) => t.affix || i >= idx)
  purgeTabCache(removing)
  if (!visitedTabs.value.some(t => t.key === activeTabKey.value)) {
    router.push(tab.fullPath).catch(() => {})
  }
}

// 关闭指定标签右侧的所有标签（保留 affix）
const closeRightOf = async (tab: TabItem) => {
  const idx = visitedTabs.value.findIndex(t => t.key === tab.key)
  if (idx === -1) return
  const removing = visitedTabs.value.filter((t, i) => i > idx && !t.affix)
  if (!(await ensureTabsClosable(removing))) return
  visitedTabs.value = visitedTabs.value.filter((t, i) => t.affix || i <= idx)
  purgeTabCache(removing)
  if (!visitedTabs.value.some(t => t.key === activeTabKey.value)) {
    router.push(tab.fullPath).catch(() => {})
  }
}

// 关闭除指定标签外的其他标签（保留 affix）
const closeOthersOf = async (tab: TabItem) => {
  const removing = visitedTabs.value.filter(t => !t.affix && t.key !== tab.key)
  if (!(await ensureTabsClosable(removing))) return
  visitedTabs.value = visitedTabs.value.filter(t => t.affix || t.key === tab.key)
  purgeTabCache(removing)
  if (activeTabKey.value !== tab.key) {
    router.push(tab.fullPath).catch(() => {})
  }
}

// 关闭全部（保留 affix，回到首页）
const closeAllTabs = async () => {
  const removing = visitedTabs.value.filter(t => !t.affix)
  if (!(await ensureTabsClosable(removing))) return
  visitedTabs.value = visitedTabs.value.filter(t => t.affix)
  purgeTabCache(removing)
  if (activeTabKey.value !== HOME_TAB.key) {
    router.push(HOME_TAB.fullPath).catch(() => {})
  }
}

// 刷新当前页（通过变更 cacheKey 强制重新挂载，不影响其他标签缓存）
const refreshCurrent = () => {
  const base = getRouteCacheBase(route)
  refreshNonce.value = { ...refreshNonce.value, [base]: (refreshNonce.value[base] || 0) + 1 }
}

/**
 * 关闭标签后作废其缓存实例与草稿守卫
 * keep-alive 实例不随标签移除而销毁，不作废则下次打开同一张单会复活旧实例，
 * 表现为“新建申报带着上一张单的数据”；按标签键轮换可同时覆盖同一单的所有入口地址
 */
const purgeTabCache = (tabs: TabItem[]) => {
  if (tabs.length === 0) return
  const next = { ...tabGeneration.value }
  tabs.forEach(tab => {
    next[tab.key] = (next[tab.key] || 0) + 1
    // 标签已关闭，其关闭守卫一并作废；实例日后被重新激活时会自行重新注册
    dropTabGuards(tab.key)
  })
  tabGeneration.value = next
}

// 右上角下拉菜单（作用于当前活跃标签）
const handleTabAction = ({ key }: { key: string | number }) => {
  const active = visitedTabs.value.find(t => t.key === activeTabKey.value) || HOME_TAB
  const k = String(key)
  if (k === 'refresh') {
    refreshCurrent()
  } else if (k === 'closeOthers') {
    closeOthersOf(active)
  } else if (k === 'closeAll') {
    closeAllTabs()
  }
}

// ========== 右键菜单 ==========
const ctxVisible = ref(false)
const ctxX = ref(0)
const ctxY = ref(0)
const ctxTab = ref<TabItem | null>(null)

const openTabMenu = (e: MouseEvent, tab: TabItem) => {
  e.preventDefault()
  ctxTab.value = tab
  ctxX.value = e.clientX
  ctxY.value = e.clientY
  ctxVisible.value = true
}

const closeCtx = () => {
  ctxVisible.value = false
}

const onCtx = (action: string) => {
  const tab = ctxTab.value
  if (!tab) return
  switch (action) {
    case 'refresh':
      if (tab.key !== activeTabKey.value) {
        router.push(tab.fullPath).then(() => refreshCurrent()).catch(() => {})
      } else {
        refreshCurrent()
      }
      break
    case 'close':
      if (!tab.affix) closeTab(tab)
      break
    case 'closeLeft':
      closeLeftOf(tab)
      break
    case 'closeRight':
      closeRightOf(tab)
      break
    case 'closeOthers':
      closeOthersOf(tab)
      break
    case 'closeAll':
      closeAllTabs()
      break
  }
  closeCtx()
}

// ========== 标签持久化 ==========
const TABS_STORAGE_KEY = 'app_visited_tabs_v2'

const restoreTabs = () => {
  try {
    const raw = localStorage.getItem(TABS_STORAGE_KEY)
    if (!raw) return
    const arr = JSON.parse(raw) as Array<Partial<TabItem> & { path?: string }>
    if (!Array.isArray(arr) || arr.length === 0) return
    // 兼容旧格式（仅有 path 字段）：path 既当标签键也当跳转地址
    const seenKeys = new Set<string>()
    const list = arr
      .map(item => {
        const fullPath = item.fullPath || item.path || ''
        const rawKey = item.key || item.path || item.fullPath || ''
        // 新建标签按“每套申报一个”计算键，旧快照里的全局“#new”与“路径#new”统一重建，避免刷新后挂出重复新建标签
        const formPathOnly = fullPath.split('?')[0]
        const key = isNewFormTabKey(rawKey) && isFormTabPath(formPathOnly) ? getNewFormTabKey(formPathOnly) : rawKey
        return {
          key,
          fullPath,
          title: item.title || '未命名页面',
          affix: !!item.affix,
          // 旧快照没存类型时，按地址里的路径前缀兜底，刷新后徽标不丢
          bizType: item.bizType || getFormTabDeclarationType({ path: fullPath.split('?')[0] }) || undefined,
          invoiceNo: item.invoiceNo || undefined
        }
      })
      .filter(item => {
        if (!item.key || !item.fullPath || seenKeys.has(item.key)) return false
        seenKeys.add(item.key)
        return true
      })
    if (list.length > 0) {
      const hasHome = list.some(t => t.key === HOME_TAB.key)
      visitedTabs.value = hasHome ? list : [{ ...HOME_TAB }, ...list]
    }
  } catch {
    // 解析失败时保留默认
  }
}

// 先恢复持久化标签，再监听路由新增当前页
restoreTabs()

watch(visitedTabs, (val) => {
  localStorage.setItem(TABS_STORAGE_KEY, JSON.stringify(val))
}, { deep: true })

// 监听路由变化自动新增标签（表单页离开后标签保留，可随时切回继续编辑）
watch(() => route.fullPath, () => {
  addTab()
}, { immediate: true })

// 系统配置
const systemConfig = ref<Record<string, string>>({
  'system.name': '海关申报系统',
  'ui.footer.text': '海关申报系统 ©2026 宁波梓熠科技有限公司',
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
watch(() => route.fullPath, (path) => {
  selectedKeys.value = [path]
  
  // 自动判断并展开父级菜单
  const pathOnly = path.split('?')[0]
  const parentPath = pathOnly.substring(0, pathOnly.lastIndexOf('/'))
  if (parentPath && parentPath !== '/') {
    if (!openKeys.value.includes(parentPath)) {
      openKeys.value = [parentPath]
    }
  }
  // 菜单超长时滚动到选中项可见
  nextTick(() => {
    const el = menuEl.value?.querySelector('.ant-menu-item-selected') as HTMLElement | null
    el?.scrollIntoView({ block: 'nearest' })
  })
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
    console.log('=== 加载菜单数据 ===')
    const response = await getUserRoutes()
    console.log('后端返回的菜单数据:', response.data)
    console.log('菜单数据详情:', JSON.stringify(response.data?.data, null, 2))
    
    menuData.value = (response.data?.data && Array.isArray(response.data.data))
      ? response.data.data
      : getDefaultMenu()

    console.log('处理后的 menuData.value:', JSON.stringify(menuData.value, null, 2))
    
    // 检查是否包含水单管理菜单
    const hasRemittanceMenu = menuData.value?.some((m: any) => 
      m.path === '/remittance' || m.menuName === '水单管理' || 
      m.children?.some((c: any) => c.path === '/remittance' || c.menuName === '水单管理')
    )
    console.log('是否包含水单管理菜单:', hasRemittanceMenu)

    if (!menuData.value || menuData.value.length === 0) {
      console.log('菜单数据为空，使用默认菜单')
      menuData.value = getDefaultMenu()
    }
  } catch (error) {
    console.error('加载菜单数据失败:', error)
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
      { id: 14, menuName: '国家信息', path: '/system/country', icon: 'GlobalOutlined', isShow: 1 }
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
  const convertMenu = (menus: any[], parentPath: string = ''): any[] => {
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
      .flatMap(menu => {
        if (!menu) return []

        // 计算完整绝对路径，作为唯一的 Key
        let fullPath = menu.path
        if (!fullPath) {
            fullPath = `menu-${menu.id}`
        } else {
            // 如果是相对路径，拼接到父路径后面
            if (!fullPath.startsWith('/')) {
                const base = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
                fullPath = `${base}/${fullPath}`
            }
            // 如果是根节点且没有 /，加上 /
            if (!fullPath.startsWith('/') && !parentPath) {
                fullPath = '/' + fullPath
            }
        }

        const menuName = menu?.menuName || '未知菜单'

        const menuItem: any = {
          key: fullPath,
          label: menuName
        }

        // 安全地处理图标
        if (menu?.icon) {
          try {
            const IconComponent = getIconComponent(menu.icon)
            if (IconComponent) {
              menuItem.icon = () => h(IconComponent)
            }
          } catch (error) {
            // 忽略图标错误
          }
        }

        // 安全地处理子菜单
        if (menu?.children && Array.isArray(menu.children) && menu.children.length > 0) {
          try {
            const filteredChildren = menu.children.filter((child: any) => {
              if (!child) return false
              const isChildVisible = child?.isShow !== 0 && child?.is_show !== 0
              return child?.status === 1 && child?.menuType !== 3 && isChildVisible
            })
            if (filteredChildren.length > 0) {
              // 递归时传入当前的 fullPath 作为 parentPath
              menuItem.children = convertMenu(filteredChildren, fullPath)
            }
          } catch (error) {
            // 忽略子菜单错误
          }
        }

        return [menuItem]
      })
      .filter(item => item !== null)
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
    'DollarOutlined': DollarOutlined,
    'FileProtectOutlined': FileProtectOutlined,
    'FileTextOutlined': FileTextOutlined,
    'AccountBookOutlined': AccountBookOutlined,
    'UnorderedListOutlined': UnorderedListOutlined,
    'FileSearchOutlined': FileSearchOutlined,
    'PlusOutlined': PlusOutlined,
    'UploadOutlined': UploadOutlined,
    'FileAddOutlined': FileAddOutlined,
    'FolderOpenOutlined': FolderOpenOutlined,
    'ContainerOutlined': ContainerOutlined,
    'PayCircleOutlined': PayCircleOutlined,
    'CarOutlined': CarOutlined,
    'MoneyCollectOutlined': MoneyCollectOutlined,
    'DashboardOutlined': DashboardOutlined,
    'EnvironmentOutlined': EnvironmentOutlined,
    'ShopOutlined': ShopOutlined,
    'BookOutlined': BookOutlined,
    'ClockCircleOutlined': ClockCircleOutlined,
    'LinkOutlined': LinkOutlined,
    'AuditOutlined': AuditOutlined,
    'HistoryOutlined': HistoryOutlined,
    'BarChartOutlined': BarChartOutlined
  }
  return iconMap[iconName] || MenuUnfoldOutlined
}

// 防抖
let menuClickTimeout: any = null

const handleMenuClick = ({ key }: { key: string | number }) => {
  if (!key) return

  if (menuClickTimeout) {
    clearTimeout(menuClickTimeout)
  }

  menuClickTimeout = setTimeout(() => {
    try {
      // Key 现在是完整的绝对路径 (例如 /remittance/list)
      const path = key.toString()
      console.log('菜单点击，跳转路径:', path)
      
      router.push(path).catch(() => {
        // 忽略跳转失败（例如点击了当前页）
      })
    } catch (error) {
      console.error('菜单点击处理异常:', error)
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
  // 走 store.logout：先请服务端销毁 token，否则同账号会话数会堆积并触发顶号
  await userStore.logout()
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
  background: #F0F2F5;
  overflow-x: hidden;
}

.sider {
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  background: #FFFFFF !important;
  box-shadow: 1px 0 0 #E2E8F0;
  border-right: 1px solid #E2E8F0;
  overflow: hidden;
}

/* antd 会在 sider 内包一层 .ant-layout-sider-children（普通块级元素），
   sider 上的 flex/overflow 传不到菜单。把包裹层变成纵向 flex 容器，
   菜单作为 flex 项占满剩余高度并内部滚动。
   注意：不能用 position:absolute 钉菜单——antd v4 内联子菜单展开测量依赖
   父级 flex 布局，脱离流后子菜单会被压成 0px */
:deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 品牌区域 */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px;
  border-radius: 16px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  cursor: pointer;
}

.logo:hover {
  background: #EFF6FF;
  border-color: #BFDBFE;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
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
  background: linear-gradient(135deg, #FA8C16, #FA541C);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(250, 140, 22, 0.3);
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
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #FA8C16;
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
  border-bottom: 1px solid #F0F0F0;
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

/* 多标签页导航 */
.tabs-bar {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 12px;
  background: #FFFFFF;
  border-bottom: 1px solid #F0F0F0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
}

.tabs-scroll {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  height: 100%;
  padding: 6px 0;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  font-size: 13px;
  color: #475569;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  user-select: none;
}

.tab-item:hover {
  color: #FA8C16;
  border-color: #FFD591;
}

.tab-item--active {
  color: #FA8C16;
  background: #FFF7E6;
  border-color: #FFD591;
  font-weight: 600;
}

.tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0;
  transition: opacity 0.2s;
}

.tab-item--active .tab-dot {
  opacity: 1;
}

.tab-label {
  line-height: 1;
  position: relative;
  /* 发票号长度不可控：限宽后溢出部分滚动展示，不用省略号截断 */
  max-width: 200px;
  overflow: hidden;
}

.tab-label__inner {
  display: inline-block;
  /* 保持文本全宽（否则会被父容器压缩到可视宽，位移量算不出来） */
  width: max-content;
  white-space: nowrap;
}

/* 只在当前标签自动滚、其余悬停时滚：多张标签同时动会把整行变成干扰项 */
.tab-item--active .tab-label--scroll .tab-label__inner,
.tab-item:hover .tab-label--scroll .tab-label__inner {
  animation: tab-label-marquee var(--tab-marquee-duration, 8s) ease-in-out infinite;
}

/* 滚到末尾停一拍再回到起点，比无缝循环更容易读完 */
@keyframes tab-label-marquee {
  0%, 10% { transform: translateX(0); }
  90%, 100% { transform: translateX(calc(-1 * var(--tab-marquee-shift, 0px))); }
}

@media (prefers-reduced-motion: reduce) {
  .tab-label__inner {
    animation: none !important;
  }
}

/* 申报类型徽标：在标签栏直接区分梓熠/理德与集洛 */
.tab-type {
  flex-shrink: 0;
  padding: 1px 5px;
  font-size: 11px;
  font-weight: 400;
  line-height: 16px;
  border-radius: 3px;
}

.tab-type--self {
  color: #1890FF;
  background: #E6F7FF;
}

.tab-type--ext {
  color: #722ED1;
  background: #F9F0FF;
}

/* 草稿确认弹窗正文：与底部三个按钮拉开间距 */
.draft-ask-tip {
  margin: 8px 0 4px;
  line-height: 22px;
  color: rgba(0, 0, 0, 0.72);
}

.tab-close {
  font-size: 11px;
  color: #94A3B8;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}

.tab-close:hover {
  color: #fff;
  background: #FA8C16;
}

.tabs-action {
  flex-shrink: 0;
  width: 32px;
  height: 28px;
  margin-left: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s;
}

.tabs-action:hover {
  color: #FA8C16;
  background: #FFF7E6;
}

/* 标签右键菜单 */
.tab-ctx-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
}

.tab-ctx-menu {
  position: fixed;
  min-width: 140px;
  margin: 0;
  padding: 4px;
  list-style: none;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid #F0F0F0;
}

.tab-ctx-menu li {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
  color: #475569;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-ctx-menu li:hover {
  color: #FA8C16;
  background: #FFF7E6;
}

.tab-ctx-menu li.ctx-disabled {
  color: #CBD5E1;
  cursor: not-allowed;
  pointer-events: none;
}

.tab-ctx-menu li.ctx-divider {
  height: 1px;
  margin: 4px 6px;
  padding: 0;
  background: #F0F0F0;
  cursor: default;
  pointer-events: none;
}

/* 内容区域 */
.content {
  margin: 24px;
  padding: 24px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #E2E8F0;
  min-height: calc(100vh - 64px - 40px - 64px - 48px);
  box-sizing: border-box;
  overflow-x: auto;
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

/* 菜单样式：作为 .ant-layout-sider-children 的 flex 项占满剩余高度并内部滚动（logo 固定置顶） */
:deep(.modern-menu) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  padding-bottom: 16px;
  background: transparent !important;
  border: none !important;
}

:deep(.modern-menu::-webkit-scrollbar) {
  width: 6px;
}

:deep(.modern-menu::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.modern-menu::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

:deep(.modern-menu::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.3);
}

:deep(.modern-menu .ant-menu-item) {
  margin: 2px 0 !important;
  width: 100% !important;
  border-radius: 0 !important;
  transition: all 0.3s !important;
  height: 36px !important;
  line-height: 36px !important;
  color: rgba(0, 0, 0, 0.65) !important;
}

:deep(.modern-menu .ant-menu-item:hover) {
  color: #FA8C16 !important;
}

:deep(.modern-menu .ant-menu-item-selected) {
  background: #FFF7E6 !important;
  color: #FA8C16 !important;
  font-weight: 600 !important;
  box-shadow: none !important;
}

:deep(.modern-menu .ant-menu-item-selected::after) {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  border-right: 3px solid #FA8C16;
}

:deep(.modern-menu .ant-menu-submenu-title) {
  margin: 2px 0 !important;
  width: 100% !important;
  border-radius: 0 !important;
  transition: all 0.3s !important;
  height: 36px !important;
  line-height: 36px !important;
  color: rgba(0, 0, 0, 0.65) !important;
}

:deep(.modern-menu .ant-menu-submenu-title:hover) {
  color: #FA8C16 !important;
}

:deep(.modern-menu .ant-menu-submenu-arrow) {
  color: rgba(0, 0, 0, 0.45) !important;
}

:deep(.modern-menu .ant-menu-sub) {
  background: #FAFAFA !important;
}
</style>