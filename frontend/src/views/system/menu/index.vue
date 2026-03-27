<template>
  <div class="menu-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="菜单名称">
          <a-input v-model:value="searchForm.menuName" placeholder="请输入菜单名称" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" style="width: 140px" allow-clear class="ui-select">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" class="ui-btn-primary">
              <template #icon><search-outlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset" class="ui-btn-secondary">
              <template #icon><reload-outlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="handleAdd()" v-permission="['menu:add']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增菜单
        </a-button>
        <a-button @click="toggleExpandAll" class="ui-btn-secondary">
          <template #icon>
            <component :is="isExpandAll ? 'vertical-align-middle-outlined' : 'vertical-align-bottom-outlined'" />
          </template>
          {{ isExpandAll ? '折叠全部' : '展开全部' }}
        </a-button>
        <a-button @click="loadMenuList" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新数据
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="menuList"
        :loading="loading"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        @expand="handleExpand"
        row-key="id"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuName'">
            <span class="flex items-center gap-2">
              <component :is="record.icon" v-if="record.icon" class="text-blue-500" />
              <span class="font-medium">{{ record.menuName }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'menuType'">
            <a-tag v-if="record.menuType === 1" color="blue" class="ui-tag">目录</a-tag>
            <a-tag v-else-if="record.menuType === 2" color="green" class="ui-tag">菜单</a-tag>
            <a-tag v-else-if="record.menuType === 3" color="orange" class="ui-tag">按钮</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'" class="ui-tag">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record as Menu)" v-permission="['menu:update']" class="font-medium text-blue-600">编辑</a-button>
              <a-button type="link" size="small" @click="handleAdd(record.id)" v-permission="['menu:add']" class="font-medium text-blue-600">新增下级</a-button>
              <a-popconfirm
                title="确定要删除该菜单及其所有子菜单吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['menu:delete']" class="font-medium">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      :confirm-loading="confirmLoading"
      width="800px"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="上级菜单" name="parentId">
              <a-tree-select
                v-model:value="formData.parentId"
                :tree-data="menuList"
                :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
                placeholder="请选择上级菜单 (不选则为顶级目录)"
                allow-clear
                tree-default-expand-all
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单类型" name="menuType">
              <a-radio-group v-model:value="formData.menuType" button-style="solid">
                <a-radio-button :value="1">目录</a-radio-button>
                <a-radio-button :value="2">菜单</a-radio-button>
                <a-radio-button :value="3">按钮</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单名称" name="menuName">
              <a-input v-model:value="formData.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单编码 (唯一)" name="menuCode">
              <a-input v-model:value="formData.menuCode" placeholder="请输入唯一编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="权限标识" name="permission">
              <a-input v-model:value="formData.permission" placeholder="如: system:user:add" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" v-if="formData.menuType !== 3">
          <a-col :span="12">
            <a-form-item label="路由地址" name="path">
              <a-input v-model:value="formData.path" placeholder="请输入路由地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组件路径" name="component">
              <a-input v-model:value="formData.component" placeholder="如: system/user/index" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="图标" name="icon">
              <a-input v-model:value="formData.icon" placeholder="请输入图标类名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示排序" name="sort">
              <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

         <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="是否显示" name="isShow">
              <a-switch v-model:checked="formData.isShowBoolean" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="外部链接" name="isExternal">
              <a-switch v-model:checked="formData.isExternalBoolean" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="菜单状态" name="status">
               <a-radio-group v-model:value="formData.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">禁用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  ReloadOutlined, 
  SearchOutlined
} from '@ant-design/icons-vue'
import { getMenuTree, addMenu, updateMenu, deleteMenu } from '@/api/system'
import type { Rule } from 'ant-design-vue/es/form'

// 类型定义
interface Menu {
  id: number
  menuName: string
  menuCode: string
  parentId: number | null
  menuType: number
  path: string
  component: string
  permission: string
  icon: string
  sort: number
  isExternal: number
  isCache: number
  isShow: number
  status: number
  children?: Menu[]
}

// 搜索表单
const searchForm = reactive({
  menuName: '',
  status: undefined as number | undefined
})

// 表格列定义
const columns = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
    width: '20%'
  },
  {
    title: '类型',
    dataIndex: 'menuType',
    key: 'menuType',
    width: 80
  },
  {
    title: '权限标识',
    dataIndex: 'permission',
    key: 'permission'
  },
  {
    title: '路由地址',
    dataIndex: 'path',
    key: 'path'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort',
    width: 80
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 200
  }
]

// 响应式数据
const loading = ref(false)
const menuList = ref<Menu[]>([])
const expandedRowKeys = ref<number[]>([])
const isExpandAll = ref(false)

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增菜单')
const confirmLoading = ref(false)
const formRef = ref()

const formData = reactive({
  id: undefined as number | undefined,
  menuName: '',
  menuCode: '',
  parentId: null as number | null,
  menuType: 2,
  path: '',
  component: '',
  permission: '',
  icon: '',
  sort: 0,
  isExternal: 0,
  isCache: 1,
  isShow: 1,
  status: 1,
  // 辅助变量
  isShowBoolean: true,
  isExternalBoolean: false
})

// 监听辅助变量
watch(() => formData.isShowBoolean, (val) => {
  formData.isShow = val ? 1 : 0
})
watch(() => formData.isExternalBoolean, (val) => {
  formData.isExternal = val ? 1 : 0
})

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur', type: 'string' }],
  menuCode: [{ required: true, message: '请输入唯一编码', trigger: 'blur', type: 'string' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change', type: 'number' }]
} as Record<string, Rule[]>

// 方法
const loadMenuList = async () => {
  loading.value = true
  try {
    // 使用树形接口获取完整菜单树结构（/system/menu/tree 返回 List<Menu>）
    const response = await getMenuTree()
    if (response.data?.code === 200) {
      let allMenus = Array.isArray(response.data.data) ? response.data.data : []
      
      // 前端本地过滤搜索条件
      if (searchForm.menuName || searchForm.status !== undefined) {
        allMenus = filterMenuTree(allMenus, searchForm.menuName, searchForm.status)
      }
      
      menuList.value = allMenus
      if (isExpandAll.value) {
        expandedRowKeys.value = getAllKeys(menuList.value)
      }
    }
  } catch (error) {
    message.error('加载菜单列表失败')
  } finally {
    loading.value = false
  }
}

// 递归过滤菜单树
const filterMenuTree = (menus: Menu[], nameKeyword?: string, status?: number): Menu[] => {
  return menus.reduce((acc: Menu[], menu) => {
    const nameMatch = !nameKeyword || menu.menuName?.includes(nameKeyword)
    const statusMatch = status === undefined || menu.status === status
    const children = menu.children ? filterMenuTree(menu.children, nameKeyword, status) : []
    
    if ((nameMatch && statusMatch) || children.length > 0) {
      acc.push({ ...menu, children: children.length > 0 ? children : menu.children })
    }
    return acc
  }, [])
}

const handleSearch = () => {
  loadMenuList()
}

const handleReset = () => {
  searchForm.menuName = ''
  searchForm.status = undefined
  handleSearch()
}

const getAllKeys = (data: Menu[]): number[] => {
  let keys: number[] = []
  data.forEach(item => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      keys = [...keys, ...getAllKeys(item.children)]
    }
  })
  return keys
}

const toggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value
  if (isExpandAll.value) {
    expandedRowKeys.value = getAllKeys(menuList.value)
  } else {
    expandedRowKeys.value = []
  }
}

const handleExpand = (expanded: boolean, record: Menu) => {
  if (expanded) {
    expandedRowKeys.value.push(record.id)
  } else {
    const index = expandedRowKeys.value.indexOf(record.id)
    if (index > -1) {
      expandedRowKeys.value.splice(index, 1)
    }
  }
}

const handleAdd = (parentId: number | null = null) => {
  modalTitle.value = '新增菜单'
  Object.assign(formData, {
    id: undefined,
    menuName: '',
    menuCode: '',
    parentId: parentId,
    menuType: 2,
    path: '',
    component: '',
    permission: '',
    icon: '',
    sort: 0,
    isExternal: 0,
    isCache: 1,
    isShow: 1,
    status: 1,
    isShowBoolean: true,
    isExternalBoolean: false
  })
  modalVisible.value = true
}

const handleEdit = (record: Menu) => {
  modalTitle.value = '编辑菜单'
  Object.assign(formData, {
    ...record,
    isShowBoolean: record.isShow === 1,
    isExternalBoolean: record.isExternal === 1
  })
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteMenu(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadMenuList()
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validateFields()
    confirmLoading.value = true
    
    let response
    const postData = { ...formData }
    delete (postData as any).isShowBoolean
    delete (postData as any).isExternalBoolean

    if (formData.id) {
      response = await updateMenu(formData.id, postData as any)
    } else {
      response = await addMenu(postData as any)
    }
    
    if (response.data?.code === 200) {
      message.success('保存成功')
      modalVisible.value = false
      loadMenuList()
    }
  } catch (error) {
    // 验证失败
  } finally {
    confirmLoading.value = false
  }
}

onMounted(() => {
  loadMenuList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>