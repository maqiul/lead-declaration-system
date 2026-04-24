<template>
  <div class="org-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="机构名称">
          <a-input v-model:value="searchForm.orgName" placeholder="请输入机构名称" allow-clear class="ui-input" />
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
        <a-button type="primary" @click="handleAdd()" v-permission="['org:create']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增机构
        </a-button>
        <a-button @click="toggleExpandAll" class="ui-btn-secondary">
          <template #icon>
            <component :is="isExpandAll ? 'vertical-align-middle-outlined' : 'vertical-align-bottom-outlined'" />
          </template>
          {{ isExpandAll ? '折叠全部' : '展开全部' }}
        </a-button>
        <a-button @click="loadOrgTree" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="orgTreeData"
        :loading="loading"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        @expand="handleExpand"
        row-key="id"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'" class="ui-tag">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record as Organization)" v-permission="['org:update']" class="font-medium text-blue-600">编辑</a-button>
              <a-button type="link" size="small" @click="handleAdd(record.id)" v-permission="['org:add']" class="font-medium text-blue-600">新增下级</a-button>
              <a-popconfirm
                title="确定要删除该机构及其所有下级机构吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['org:delete']" class="font-medium">删除</a-button>
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
      width="700px"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上级机构" name="parentId">
              <a-tree-select
                v-model:value="formData.parentId"
                :tree-data="orgTreeData"
                :field-names="{ label: 'orgName', value: 'id', children: 'children' }"
                placeholder="请选择上级机构 (不选则为顶级)"
                allow-clear
                tree-default-expand-all
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="机构名称" name="orgName">
              <a-input v-model:value="formData.orgName" placeholder="请输入机构名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="机构编码" name="orgCode">
              <a-input v-model:value="formData.orgCode" placeholder="请输入机构编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="负责人" name="leader">
              <a-input v-model:value="formData.leader" placeholder="请输入负责人姓名" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
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
              <a-radio-group v-model:value="formData.status" button-style="solid">
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  ReloadOutlined, 
  SearchOutlined
} from '@ant-design/icons-vue'
import { getOrgTree, addOrg, updateOrg, deleteOrg } from '@/api/system'
import type { Rule } from 'ant-design-vue/es/form'

// 类型定义
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
  children?: Organization[]
}

// 搜索表单
const searchForm = reactive({
  orgName: '',
  status: undefined as number | undefined
})

// 表格列定义
const columns = [
  {
    title: '机构名称',
    dataIndex: 'orgName',
    key: 'orgName',
    width: '25%'
  },
  {
    title: '机构编码',
    dataIndex: 'orgCode',
    key: 'orgCode'
  },
  {
    title: '负责人',
    dataIndex: 'leader',
    key: 'leader'
  },
  {
    title: '联系电话',
    dataIndex: 'phone',
    key: 'phone'
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
const orgTreeData = ref<Organization[]>([])
const expandedRowKeys = ref<number[]>([])
const isExpandAll = ref(false)

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增机构')
const confirmLoading = ref(false)
const formRef = ref()
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

const formRules = {
  orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur', type: 'string' }],
  orgCode: [{ required: true, message: '请输入机构编码', trigger: 'blur', type: 'string' }],
  leader: [{ required: true, message: '请输入负责人', trigger: 'blur', type: 'string' }]
} as Record<string, Rule[]>

// 方法
const loadOrgTree = async () => {
  loading.value = true
  try {
    const params = {
      orgName: searchForm.orgName,
      status: searchForm.status
    }
    const response = await getOrgTree(params)
    if (response.data?.code === 200) {
      // 增加数组安全性检查
      orgTreeData.value = Array.isArray(response.data.data) ? response.data.data : []
      if (isExpandAll.value) {
        expandedRowKeys.value = getAllKeys(orgTreeData.value)
      }
    }
  } catch (error) {
    message.error('加载组织树失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadOrgTree()
}

const handleReset = () => {
  searchForm.orgName = ''
  searchForm.status = undefined
  handleSearch()
}

const getAllKeys = (data: Organization[]): number[] => {
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
    expandedRowKeys.value = getAllKeys(orgTreeData.value)
  } else {
    expandedRowKeys.value = []
  }
}

const handleExpand = (expanded: boolean, record: Organization) => {
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
  modalTitle.value = '新增机构'
  Object.assign(formData, {
    id: undefined,
    orgName: '',
    orgCode: '',
    parentId: parentId,
    leader: '',
    phone: '',
    email: '',
    sort: 0,
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = (record: Organization) => {
  modalTitle.value = '编辑机构'
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
      loadOrgTree()
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
    if (formData.id) {
      response = await updateOrg(formData.id, formData as any)
    } else {
      response = await addOrg(formData as any)
    }
    
    if (response.data?.code === 200) {
      message.success('保存成功')
      modalVisible.value = false
      loadOrgTree()
    }
  } catch (error) {
    // 验证失败
  } finally {
    confirmLoading.value = false
  }
}

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
:deep(.ant-table-wrapper-tree-expand-icon) {
  color: var(--color-primary);
}
</style>