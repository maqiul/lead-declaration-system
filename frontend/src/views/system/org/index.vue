<template>
  <div class="org-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="组织名称">
          <a-input v-model:value="searchForm.orgName" placeholder="请输入组织名称">
            <template #prefix>
              <team-outlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 120px" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <template #icon><search-outlined /></template>
            搜索
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">
            <template #icon><reload-outlined /></template>
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card" :bordered="false">
      <a-space>
        <a-button type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>
          新增组织
        </a-button>
        <a-button class="expand-toggle-btn" @click="handleExpandAll">
          <template #icon>
            <template v-if="isAllExpanded">
              <apartment-outlined />
            </template>
            <template v-else>
              <team-outlined />
            </template>
          </template>
          {{ isAllExpanded ? '全部折叠' : '全部展开' }}
        </a-button>
        <a-button @click="loadData">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
        <!-- 调试按钮，仅在开发环境显示 -->
        <a-button @click="debugState" v-if="false">
          调试状态
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card :bordered="false">
      <!-- 状态指示器 -->
      <div class="status-indicator" :class="getStatusClass()">
        <template v-if="expandedRowKeys.length === 0">
          <team-outlined />
          <span>当前状态：全部折叠</span>
        </template>
        <template v-else-if="isAllExpanded">
          <apartment-outlined />
          <span>当前状态：全部展开</span>
        </template>
        <template v-else>
          <apartment-outlined />
          <span>当前状态：部分展开</span>
        </template>
      </div>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        :expand-row-by-click="true"
        row-key="id"
        @expand="handleExpand"
        @expandedRowsChange="handleExpandedRowsChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleAddChild(record as Organization)">新增下级</a-button>
              <a-button type="link" size="small" @click="handleEdit(record as Organization)">编辑</a-button>
              <a-popconfirm
                title="确定要删除这个组织吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 组织编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="confirmLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="组织名称" name="orgName">
              <a-input v-model:value="formData.orgName" placeholder="请输入组织名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组织编码" name="orgCode">
              <a-input v-model:value="formData.orgCode" placeholder="请输入组织编码" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="上级组织" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="orgTreeData"
            placeholder="请选择上级组织"
            tree-default-expand-all
            allow-clear
            :field-names="{ label: 'orgName', value: 'id', children: 'children' }"
          />
        </a-form-item>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="负责人" name="leader">
              <a-input v-model:value="formData.leader" placeholder="请输入负责人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  SearchOutlined, 
  ReloadOutlined, 
  TeamOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { getOrgTree, addOrg, updateOrg, deleteOrg } from '@/api/system'

interface Organization {
  id: number
  orgName: string
  orgCode: string
  parentId: number | null
  leader: string
  phone: string
  email: string
  sort: number
  status: number
  createTime: string
  children?: Organization[]
}

interface SearchForm {
  orgName?: string
  status?: number
}

// 响应式数据
const loading = ref(false)
const tableData = ref<Organization[]>([])
const expandedRowKeys = ref<number[]>([])
const isAllExpanded = ref(true)  // 默认展开状态

// 搜索表单
const searchForm = reactive<SearchForm>({
  orgName: '',
  status: undefined
})

// 表格列配置
const columns = [
  {
    title: '组织名称',
    dataIndex: 'orgName',
    key: 'orgName',
    width: 200
  },
  {
    title: '组织编码',
    dataIndex: 'orgCode',
    key: 'orgCode',
    width: 150
  },
  {
    title: '负责人',
    dataIndex: 'leader',
    key: 'leader',
    width: 120
  },
  {
    title: '联系电话',
    dataIndex: 'phone',
    key: 'phone',
    width: 150
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
    width: 200
  }
]

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增组织')
const confirmLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  orgName: '',
  orgCode: '',
  parentId: null as number | null,
  leader: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1
})

// 表单验证规则
const formRules: Record<string, Rule[]> = {
  orgName: [
    { required: true, message: '请输入组织名称', trigger: 'blur' }
  ],
  orgCode: [
    { required: true, message: '请输入组织编码', trigger: 'blur' }
  ]
}

// 组织树数据
const orgTreeData = ref<Organization[]>([])

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      orgName: searchForm.orgName,
      status: searchForm.status
    }
    
    const response = await getOrgTree(params)
    if (response.data?.code === 200) {
      tableData.value = response.data.data
      orgTreeData.value = response.data.data
      console.log('数据加载完成:', tableData.value)
      
      // 初始化状态：默认展开第一层
      if (tableData.value && tableData.value.length > 0) {
        const firstLevelIds = tableData.value.map(item => item.id)
        expandedRowKeys.value = firstLevelIds
        isAllExpanded.value = true
        console.log('初始化状态完成: expandedRowKeys =', firstLevelIds, ', isAllExpanded = true')
      } else {
        expandedRowKeys.value = []
        isAllExpanded.value = false
        console.log('无数据，初始化为折叠状态')
      }
    } else {
      message.error(response.data?.message || '加载数据失败')
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadData()
}

const handleReset = () => {
  searchForm.orgName = ''
  searchForm.status = undefined
  loadData()
}

const handleAdd = () => {
  modalTitle.value = '新增组织'
  Object.assign(formData, {
    id: undefined,
    orgName: '',
    orgCode: '',
    parentId: null,
    leader: '',
    phone: '',
    email: '',
    sort: 0,
    status: 1
  })
  modalVisible.value = true
}

const handleAddChild = (record: Organization) => {
  modalTitle.value = '新增下级组织'
  Object.assign(formData, {
    id: undefined,
    orgName: '',
    orgCode: '',
    parentId: record.id,
    leader: '',
    phone: '',
    email: '',
    sort: 0,
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = (record: Organization) => {
  modalTitle.value = '编辑组织'
  Object.assign(formData, {
    id: record.id,
    orgName: record.orgName,
    orgCode: record.orgCode,
    parentId: record.parentId,
    leader: record.leader,
    phone: record.phone,
    email: record.email,
    sort: record.sort,
    status: record.status
  })
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteOrg(id)
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

const handleModalOk = async () => {
  try {
    await formRef.value?.validateFields()
    confirmLoading.value = true
    
    let response
    if (formData.id) {
      response = await updateOrg(formData.id!, formData)
    } else {
      response = await addOrg(formData)
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

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 处理全部展开/折叠
const handleExpandAll = () => {
  console.log('=== handleExpandAll 调用 ===')
  console.log('当前 isAllExpanded 状态:', isAllExpanded.value)
  console.log('当前 tableData:', tableData.value)
  console.log('当前 expandedRowKeys:', expandedRowKeys.value)
  
  if (isAllExpanded.value) {
    // 当前是展开状态，执行折叠
    console.log('执行折叠操作')
    expandedRowKeys.value = []
    isAllExpanded.value = false
  } else {
    // 当前是折叠状态，执行展开
    console.log('执行展开操作')
    
    // 确保有数据再执行展开
    if (!tableData.value || tableData.value.length === 0) {
      console.log('没有数据，无法展开')
      return
    }
    
    // 展开所有层级
    const getAllIds = (data: Organization[]): number[] => {
      let ids: number[] = []
      const traverse = (items: Organization[]) => {
        items.forEach(item => {
          ids.push(item.id)
          console.log(`添加节点ID: ${item.id}, 名称: ${item.orgName}`)
          if (item.children && item.children.length > 0) {
            console.log(`节点 ${item.orgName} 有 ${item.children.length} 个子节点`)
            traverse(item.children)
          }
        })
      }
      traverse(data)
      return ids
    }
    
    try {
      const allIds = getAllIds(tableData.value)
      console.log('获取到的所有节点ID:', allIds)
      expandedRowKeys.value = allIds
      isAllExpanded.value = true
      console.log('展开所有层级完成')
    } catch (error) {
      console.error('展开操作出错:', error)
    }
  }
  
  console.log('操作后 expandedRowKeys:', expandedRowKeys.value)
  console.log('操作后 isAllExpanded:', isAllExpanded.value)
  console.log('========================')
}

// 处理单个节点展开/折叠
const handleExpand = (expanded: boolean, record: Organization) => {
  const recordId = record.id
  console.log(`=== handleExpand 调用 ===`)
  console.log(`节点: ${record.orgName}(${recordId})`)
  console.log(`操作类型: ${expanded ? '展开' : '折叠'}`)
  console.log(`操作前 expandedRowKeys:`, expandedRowKeys.value)
  console.log(`操作前 isAllExpanded:`, isAllExpanded.value)
  
  if (expanded) {
    // 展开节点
    if (!expandedRowKeys.value.includes(recordId)) {
      expandedRowKeys.value.push(recordId)
      console.log(`添加展开节点: ${recordId}`)
    }
    // 同时展开所有子节点
    if (record.children && record.children.length > 0) {
      const addChildKeys = (items: Organization[]) => {
        items.forEach(child => {
          if (!expandedRowKeys.value.includes(child.id)) {
            expandedRowKeys.value.push(child.id)
            console.log(`添加子节点展开: ${child.id} (${child.orgName})`)
          }
          if (child.children && child.children.length > 0) {
            addChildKeys(child.children)
          }
        })
      }
      addChildKeys(record.children)
    }
  } else {
    // 折叠节点
    const index = expandedRowKeys.value.indexOf(recordId)
    if (index > -1) {
      expandedRowKeys.value.splice(index, 1)
      console.log(`移除展开节点: ${recordId}`)
    }
    // 同时折叠所有子节点
    if (record.children && record.children.length > 0) {
      const removeChildKeys = (items: Organization[]) => {
        items.forEach(child => {
          const childIndex = expandedRowKeys.value.indexOf(child.id)
          if (childIndex > -1) {
            expandedRowKeys.value.splice(childIndex, 1)
            console.log(`移除子节点展开: ${child.id} (${child.orgName})`)
          }
          if (child.children && child.children.length > 0) {
            removeChildKeys(child.children)
          }
        })
      }
      removeChildKeys(record.children)
    }
  }
  
  console.log(`操作后 expandedRowKeys:`, expandedRowKeys.value)
  console.log(`========================`)
}

// 表格展开行变化处理（主要用于状态同步）
const handleExpandedRowsChange = (expandedKeys: (string | number)[]) => {
  console.log('=== handleExpandedRowsChange 状态同步 ===')
  console.log('Ant Design Vue 传入的 expandedKeys:', expandedKeys)
  console.log('当前组件的 expandedRowKeys:', expandedRowKeys.value)
  console.log('当前 isAllExpanded 状态:', isAllExpanded.value)
  
  // 将传入的keys转换为数字数组
  const newExpanded = expandedKeys.map(key => Number(key))
  
  // 智能合并策略：保留我们通过handleExpand添加的子节点
  // 只有当传入的keys与我们的状态完全不同时才考虑更新
  const arraysEqual = (a: number[], b: number[]): boolean => {
    if (a.length !== b.length) return false
    const sortedA = [...a].sort()
    const sortedB = [...b].sort()
    return sortedA.every((val, index) => val === sortedB[index])
  }
  
  // 检查是否需要更新
  const shouldUpdate = !arraysEqual(expandedRowKeys.value, newExpanded)
  
  if (shouldUpdate) {
    console.log('⚠️ 检测到状态差异，执行智能合并')
    console.log('Ant Design Vue 状态:', newExpanded)
    console.log('组件当前状态:', expandedRowKeys.value)
    
    // 智能合并：保留子节点，补充缺失的父节点
    const mergedExpanded = [...newExpanded]
    
    // 添加通过handleExpand展开的子节点（如果它们不在newExpanded中）
    expandedRowKeys.value.forEach(key => {
      if (!mergedExpanded.includes(key)) {
        mergedExpanded.push(key)
        console.log(`智能合并: 添加缺失的节点 ${key}`)
      }
    })
    
    console.log('合并后状态:', mergedExpanded)
    expandedRowKeys.value = mergedExpanded
  } else {
    console.log('✅ 状态一致，无需同步')
  }
  
  // 更新全展开状态
  if (expandedRowKeys.value.length === 0) {
    isAllExpanded.value = false
    console.log('设置 isAllExpanded = false (全部折叠)')
  } else if (tableData.value && tableData.value.length > 0) {
    const allPossibleKeys = getAllNodeKeys(tableData.value)
    const isFullyExpanded = allPossibleKeys.every(key => expandedRowKeys.value.includes(key))
    isAllExpanded.value = isFullyExpanded
    console.log('所有可能节点:', allPossibleKeys)
    console.log('当前展开节点:', expandedRowKeys.value)
    console.log(`设置 isAllExpanded = ${isFullyExpanded} (${isFullyExpanded ? '全部展开' : '部分展开'})`)
  } else {
    isAllExpanded.value = expandedRowKeys.value.length > 0
    console.log(`设置 isAllExpanded = ${isAllExpanded.value} (基于节点数量)`)  
  }
  
  console.log('最终 expandedRowKeys:', expandedRowKeys.value)
  console.log('最终 isAllExpanded:', isAllExpanded.value)
  console.log('=====================================')
}

// 获取所有节点的key（用于状态判断）
const getAllNodeKeys = (data: Organization[]): number[] => {
  const keys: number[] = []
  const traverse = (items: Organization[]) => {
    items.forEach(item => {
      keys.push(item.id)
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(data)
  return keys
}

// 获取所有节点数量
const getAllNodeCount = (data: Organization[]): number => {
  let count = 0
  const traverse = (items: Organization[]) => {
    items.forEach(item => {
      count++
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(data)
  return count
}

// 调试方法 - 显示当前状态
const debugState = () => {
  console.log('=== 组织管理页面状态调试 ===')
  console.log('isAllExpanded:', isAllExpanded.value)
  console.log('expandedRowKeys:', expandedRowKeys.value)
  console.log('tableData length:', tableData.value.length)
  console.log('total nodes:', getAllNodeCount(tableData.value))
  console.log('=========================')
}

// 获取状态指示器的CSS类名
const getStatusClass = () => {
  if (expandedRowKeys.value.length === 0) {
    return 'collapsed'
  } else if (isAllExpanded.value) {
    return 'expanded'
  } else {
    return 'partial'
  }
}

// 生命周期
onMounted(() => {
  loadData()
  // 开发环境下可以调用debugState()查看状态
})
</script>

<style scoped>
.org-management {
  padding: 0;
  height: 100%;
}

.search-card, .operation-card {
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

.status-indicator.partial {
  color: #1d4ed8;
  background-color: #eff6ff;
  border-color: #3b82f6;
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

:deep(.ant-card-body) {
  padding: 24px;
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