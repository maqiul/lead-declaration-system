<template>
  <div class="menu-management">
    <!-- 操作按钮区域 -->
    <a-card class="operation-card" :bordered="false">
      <a-space>
        <a-button type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>
          新增菜单
        </a-button>
        <a-button class="expand-toggle-btn" @click="handleExpandAll">
          <template #icon>
            <template v-if="isAllExpanded">
              <folder-open-outlined />
            </template>
            <template v-else>
              <folder-outlined />
            </template>
          </template>
          {{ isAllExpanded ? '全部折叠' : '展开一级' }}
        </a-button>
        <a-button @click="loadData">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card :bordered="false">
      <!-- 状态指示器 -->
      <div class="status-indicator" :class="getStatusClass()">
        <template v-if="expandedKeys.length === 0">
          <folder-outlined />
          <span>当前状态：全部折叠</span>
        </template>
        <template v-else-if="isAllExpanded">
          <folder-open-outlined />
          <span>当前状态：全部展开</span>
        </template>
        <template v-else>
          <folder-open-outlined />
          <span>当前状态：部分展开</span>
        </template>
      </div>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="false"
        :expanded-row-keys="expandedKeys"
        :expand-row-by-click="true"
        row-key="id"
        @expand="handleExpand"
        @expandedRowsChange="handleExpandedRowsChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <a-tag v-if="record.menuType === 1" color="blue">目录</a-tag>
            <a-tag v-else-if="record.menuType === 2" color="green">菜单</a-tag>
            <a-tag v-else color="orange">按钮</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'icon'">
            <component :is="record.icon" v-if="record.icon" />
            <span v-else>--</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleAddChild(record as Menu)">新增</a-button>
              <a-button type="link" size="small" @click="handleEdit(record as Menu)">修改</a-button>
              <a-popconfirm
                title="确定要删除这个菜单吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 菜单编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="confirmLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单类型" name="menuType">
              <a-radio-group v-model:value="formData.menuType" @change="handleMenuTypeChange">
                <a-radio :value="1">目录</a-radio>
                <a-radio :value="2">菜单</a-radio>
                <a-radio :value="3">按钮</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上级菜单" name="parentId">
              <a-tree-select
                v-model:value="formData.parentId"
                :tree-data="menuTreeData"
                placeholder="请选择上级菜单"
                tree-default-expand-all
                allow-clear
                :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
              />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单名称" name="menuName">
              <a-input v-model:value="formData.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单编码" name="menuCode">
              <a-input v-model:value="formData.menuCode" placeholder="请输入菜单编码" />
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
              <a-input v-model:value="formData.component" placeholder="请输入组件路径" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16" v-if="formData.menuType === 2">
          <a-col :span="12">
            <a-form-item label="权限标识" name="permission">
              <a-input v-model:value="formData.permission" placeholder="请输入权限标识" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图标" name="icon">
              <a-input v-model:value="formData.icon" placeholder="请输入图标名称" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="显示排序" name="sort">
              <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="formData.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">禁用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16" v-if="formData.menuType !== 3">
          <a-col :span="8">
            <a-form-item label="是否外链">
              <a-switch v-model:checked="formData.isExternal" checked-children="是" un-checked-children="否" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否缓存">
              <a-switch v-model:checked="formData.isCache" checked-children="是" un-checked-children="否" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否显示">
              <a-switch v-model:checked="formData.isShow" checked-children="是" un-checked-children="否" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  ReloadOutlined,
  FolderOutlined,
  FolderOpenOutlined
} from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { getMenuTree, addMenu, updateMenu, deleteMenu } from '@/api/system'

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
  createTime: string
  children?: Menu[]
}

// 响应式数据
const loading = ref(false)
const tableData = ref<Menu[]>([])
const expandedKeys = ref<(number | string)[]>([])
const isAllExpanded = ref(true)

// 表格列配置
const columns = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
    width: 180
  },
  {
    title: '菜单编码',
    dataIndex: 'menuCode',
    key: 'menuCode',
    width: 150
  },
  {
    title: '菜单类型',
    dataIndex: 'menuType',
    key: 'menuType',
    width: 100
  },
  {
    title: '路由地址',
    dataIndex: 'path',
    key: 'path',
    width: 150
  },
  {
    title: '组件路径',
    dataIndex: 'component',
    key: 'component',
    width: 180
  },
  {
    title: '权限标识',
    dataIndex: 'permission',
    key: 'permission',
    width: 150
  },
  {
    title: '图标',
    dataIndex: 'icon',
    key: 'icon',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 180
  }
]

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增菜单')
const confirmLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  menuName: '',
  menuCode: '',
  parentId: null as number | null,
  menuType: 1,
  path: '',
  component: '',
  permission: '',
  icon: '',
  sort: 0,
  isExternal: 0,
  isCache: 0,
  isShow: 1,
  status: 1
})

// 表单验证规则
const formRules: Record<string, Rule[]> = {
  menuName: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' }
  ],
  menuCode: [
    { required: true, message: '请输入菜单编码', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '请输入路由地址', trigger: 'blur' }
  ],
  component: [
    { required: true, message: '请输入组件路径', trigger: 'blur' }
  ]
}

// 菜单树数据
const menuTreeData = ref<Menu[]>([])

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const response = await getMenuTree()
    if (response.data?.code === 200) {
      tableData.value = response.data.data
      menuTreeData.value = [{ id: 0, menuName: '顶级菜单', children: response.data.data } as Menu]
      
      // 默认折叠状态
      expandedKeys.value = []
      isAllExpanded.value = false
    } else {
      message.error(response.data?.message || '加载数据失败')
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  modalTitle.value = '新增菜单'
  Object.assign(formData, {
    id: undefined,
    menuName: '',
    menuCode: '',
    parentId: null,
    menuType: 1,
    path: '',
    component: '',
    permission: '',
    icon: '',
    sort: 0,
    isExternal: 0,
    isCache: 0,
    isShow: 1,
    status: 1
  })
  modalVisible.value = true
}

const handleAddChild = (record: Menu) => {
  modalTitle.value = '新增子菜单'
  Object.assign(formData, {
    id: undefined,
    menuName: '',
    menuCode: '',
    parentId: record.id,
    menuType: record.menuType < 3 ? record.menuType + 1 : 3,
    path: '',
    component: '',
    permission: '',
    icon: '',
    sort: 0,
    isExternal: 0,
    isCache: 0,
    isShow: 1,
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = (record: Menu) => {
  modalTitle.value = '编辑菜单'
  Object.assign(formData, {
    id: record.id,
    menuName: record.menuName,
    menuCode: record.menuCode,
    parentId: record.parentId,
    menuType: record.menuType,
    path: record.path,
    component: record.component,
    permission: record.permission,
    icon: record.icon,
    sort: record.sort,
    isExternal: record.isExternal,
    isCache: record.isCache,
    isShow: record.isShow,
    status: record.status
  })
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteMenu(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadData()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 生命周期
onMounted(() => {
  loadData()
})

// 处理全部展开/折叠
const handleExpandAll = () => {
  if (isAllExpanded.value) {
    // 当前是展开状态，执行折叠
    expandedKeys.value = []
    isAllExpanded.value = false
  } else {
    // 当前是折叠状态，执行展开第一层
    if (!tableData.value || tableData.value.length === 0) {
      return
    }
    
    // 只展开第一层节点
    const firstLevelIds = tableData.value.map(item => item.id)
    expandedKeys.value = firstLevelIds
    isAllExpanded.value = firstLevelIds.length > 0
  }
}

// 处理单个节点展开/折叠
const handleExpand = (expanded: boolean, record: Menu) => {
  const recordId = record.id as number | string
  if (expanded) {
    if (!expandedKeys.value.includes(recordId)) {
      expandedKeys.value.push(recordId)
    }
  } else {
    const index = expandedKeys.value.indexOf(recordId)
    if (index > -1) {
      expandedKeys.value.splice(index, 1)
    }
  }
}

// 处理批量展开/折叠
const handleExpandedRowsChange = (expandedKeysValue: (number | string)[]) => {
  expandedKeys.value = expandedKeysValue
}

const handleMenuTypeChange = () => {
  // 菜单类型改变时的处理逻辑
}

// 获取状态指示器的CSS类名
const getStatusClass = () => {
  if (expandedKeys.value.length === 0) {
    return 'collapsed'
  } else if (isAllExpanded.value) {
    return 'expanded'
  } else {
    return 'partial'
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validateFields()
    confirmLoading.value = true
    
    let response
    if (formData.id) {
      response = await updateMenu(formData.id, formData)
    } else {
      response = await addMenu(formData)
    }
    
    if (response.data?.code === 200) {
      message.success(modalTitle.value.includes('新增') ? '新增成功' : '编辑成功')
      modalVisible.value = false
      loadData()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    confirmLoading.value = false
  }
}
</script>

<style scoped>
.menu-management {
  padding: 0;
  height: 100%;
}

.operation-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.expand-toggle-btn {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%) !important;
  border: none !important;
  color: white !important;
  border-radius: 8px !important;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.2);
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 20px;
  padding: 6px 12px;
  border-radius: 8px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
}

.status-indicator.expanded {
  color: #059669;
  background-color: #ecfdf5;
  border-color: #10b981;
}

.status-indicator.collapsed {
  color: #d97706;
  background-color: #fffbeb;
  border-color: #f59e0b;
}

:deep(.ant-table-row-expand-icon) {
  background: #1e40af;
  border: 2px solid white;
  width: 22px;
  height: 22px;
  box-shadow: 0 2px 6px rgba(30, 64, 175, 0.25);
}

:deep(.ant-table-row-expand-icon:hover) {
  background: #3b82f6;
  transform: scale(1.1);
}

:deep(.ant-card) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
  margin-bottom: 20px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f8fafc !important;
  font-weight: 600;
  color: #475569;
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  border: none;
  border-radius: 8px;
}

</style>