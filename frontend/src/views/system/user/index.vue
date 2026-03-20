<template>
  <div class="user-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="用户名">
          <a-input v-model:value="searchForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="searchForm.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 120px" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>
          新增用户
        </a-button>
        <a-button @click="handleBatchDelete" :disabled="selectedRowKeys.length === 0">
          <template #icon><delete-outlined /></template>
          批量删除
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record as any)">编辑</a-button>
              <a-button type="link" size="small" @click="handleResetPwd(record as any)">重置密码</a-button>
              <a-popconfirm
                title="确定要删除这个用户吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 用户编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="confirmLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      :width="modalWidth"
      :style="{ maxWidth: '90vw' }"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="formData.username" placeholder="请输入用户名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="昵称" name="nickname">
              <a-input v-model:value="formData.nickname" placeholder="请输入昵称" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="所属组织" name="orgId">
          <a-tree-select
            v-model:value="formData.orgId"
            :tree-data="orgTreeData"
            placeholder="请选择所属组织"
            tree-default-expand-all
          />
        </a-form-item>
        
        <a-form-item label="角色" name="roleIds">
          <a-select
            v-model:value="formData.roleIds"
            mode="multiple"
            placeholder="请选择角色"
            :options="roleOptions"
          />
        </a-form-item>
        
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
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { getUserList, getUser, addUser, updateUser, deleteUser, resetUserPwd, getOrgTree, getRoleList } from '@/api/system'

// 类型定义
interface User {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  orgId: number
  status: number
  createTime: string
}

interface SearchForm {
  username?: string
  phone?: string
  status?: number
}

// 响应式数据
const loading = ref(false)
const tableData = ref<User[]>([])
const selectedRowKeys = ref<number[]>([])

// 搜索表单
const searchForm = reactive<SearchForm>({
  username: '',
  phone: '',
  status: undefined
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username'
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    key: 'nickname'
  },
  {
    title: '手机号',
    dataIndex: 'phone',
    key: 'phone'
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime'
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 200
  }
]

// 表格行选择配置
const rowSelection: TableProps['rowSelection'] = {
  onChange: (selectedKeys: any[]) => {
    selectedRowKeys.value = selectedKeys as number[]
  }
}

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增用户')
const confirmLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  orgId: 1, // 设置默认值
  roleIds: [] as number[],
  status: 1
})

// 表单验证规则
const formRules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 组织树数据
const orgTreeData = ref<any[]>([])

// 角色选项
const roleOptions = ref<any[]>([])

// 响应式宽度计算
const modalWidth = computed(() => {
  const screenWidth = window.innerWidth
  if (screenWidth < 768) {
    return '95vw'
  } else if (screenWidth < 1200) {
    return '700px'
  } else {
    return '800px'
  }
})

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      username: searchForm.username,
      phone: searchForm.phone,
      status: searchForm.status
    }
    
    const response = await getUserList(params)
    if (response.data?.code === 200) {
      tableData.value = response.data.data.records
      pagination.total = Number(response.data.data.total) || 0
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
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.phone = ''
  searchForm.status = undefined
  handleSearch()
}

const handleAdd = () => {
  modalTitle.value = '新增用户'
  Object.assign(formData, {
    id: undefined,
    username: '',
    nickname: '',
    phone: '',
    email: '',
    orgId: undefined,
    roleIds: [],
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = async (record: User) => {
  modalTitle.value = '编辑用户'
  try {
    // 调用API获取用户详情（包含roleIds）
    const response = await getUser(record.id)
    const userDetail = response.data?.data || response.data
    
    Object.assign(formData, {
      id: userDetail.id,
      username: userDetail.username,
      nickname: userDetail.nickname,
      phone: userDetail.phone,
      email: userDetail.email,
      orgId: userDetail.orgId,
      roleIds: userDetail.roleIds || [],
      status: userDetail.status
    })
  } catch (error) {
    console.error('获取用户详情失败:', error)
    // 降级：使用record中的数据，roleIds默认空
    Object.assign(formData, {
      id: record.id,
      username: record.username,
      nickname: record.nickname,
      phone: record.phone,
      email: record.email,
      orgId: record.orgId,
      roleIds: [],
      status: record.status
    })
  }
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteUser(id)
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

const handleBatchDelete = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的用户')
    return
  }
  
  try {
    const response = await deleteUser(selectedRowKeys.value as any)
    if (response.data?.code === 200) {
      message.success('批量删除成功')
      selectedRowKeys.value = []
      loadData()
    } else {
      message.error(response.data?.message || '批量删除失败')
    }
  } catch (error) {
    message.error('批量删除失败')
  }
}

const handleResetPwd = async (record: User) => {
  try {
    const response = await resetUserPwd(record.id)
    if (response.data?.code === 200) {
      message.success(response.data.message || '密码重置成功')
    } else {
      message.error(response.data?.message || '密码重置失败')
    }
  } catch (error) {
    message.error('密码重置失败')
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validateFields()
    confirmLoading.value = true
    
    if (modalTitle.value === '新增用户') {
      const response = await addUser(formData)
      if (response.data?.code === 200) {
        message.success('新增成功')
        modalVisible.value = false
        loadData()
      } else {
        message.error(response.data?.message || '新增失败')
      }
    } else {
      const response = await updateUser(formData.id!, formData)
      if (response.data?.code === 200) {
        message.success('编辑成功')
        modalVisible.value = false
        loadData()
      } else {
        message.error(response.data?.message || '编辑失败')
      }
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

// 加载组织树数据
const loadOrgTree = async () => {
  try {
    const response = await getOrgTree()
    if (response.data?.code === 200) {
      // 转换数据格式以适应 a-tree-select
      const convertToTreeData = (orgs: any[]): any[] => {
        return orgs.map(org => ({
          title: org.orgName,
          value: org.id,
          children: org.children ? convertToTreeData(org.children) : undefined
        }))
      }
      orgTreeData.value = convertToTreeData(response.data.data)
    } else {
      message.error(response.data?.message || '获取组织树失败')
    }
  } catch (error) {
    message.error('获取组织树失败')
  }
}

// 加载角色列表
const loadRoleList = async () => {
  try {
    const response = await getRoleList({ pageNum: 1, pageSize: 1000 })
    if (response.data?.code === 200) {
      roleOptions.value = response.data.data.records.map((role: any) => ({
        label: role.roleName,
        value: role.id
      }))
    } else {
      message.error(response.data?.message || '获取角色列表失败')
    }
  } catch (error) {
    message.error('获取角色列表失败')
  }
}

// 生命周期
onMounted(() => {
  loadData()
  loadOrgTree()
  loadRoleList()
})
</script>

<style scoped>
.user-management {
  padding: 0;
  height: 100%;
}

.search-card, .operation-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
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

:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
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
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.2);
  border-radius: 8px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
  transform: translateY(-1px);
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input:focus) {
  box-shadow: 0 0 0 2px rgba(30, 64, 175, 0.1);
  border-color: #3b82f6;
}

/* 响应式调整 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
}

</style>