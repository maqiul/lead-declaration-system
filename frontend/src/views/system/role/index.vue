<template>
  <div class="role-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="角色名称">
          <a-input v-model:value="searchForm.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码">
          <a-input v-model:value="searchForm.roleCode" placeholder="请输入角色编码" />
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
          新增角色
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
          <template v-else-if="column.key === 'dataScope'">
            <a-tag v-if="record.dataScope === 1">全部数据</a-tag>
            <a-tag v-else-if="record.dataScope === 2" color="blue">本级数据</a-tag>
            <a-tag v-else-if="record.dataScope === 3" color="orange">本级及下级</a-tag>
            <a-tag v-else color="purple">自定义</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record as Role)">编辑</a-button>
              <a-button type="link" size="small" @click="handlePermission(record as Role)">权限配置</a-button>
              <a-button 
                v-if="(record as Role).roleCode === 'admin'" 
                type="link" 
                size="small" 
                @click="handleAssignAllPermissions(record as Role)"
              >
                分配全部权限
              </a-button>
              <a-popconfirm
                title="确定要删除这个角色吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 角色编辑弹窗 -->
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
            <a-form-item label="角色名称" name="roleName">
              <a-input v-model:value="formData.roleName" placeholder="请输入角色名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色编码" name="roleCode">
              <a-input v-model:value="formData.roleCode" placeholder="请输入角色编码" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="数据权限范围" name="dataScope">
          <a-select v-model:value="formData.dataScope" placeholder="请选择数据权限范围">
            <a-select-option :value="1">全部数据权限</a-select-option>
            <a-select-option :value="2">本级数据权限</a-select-option>
            <a-select-option :value="3">本级及以下数据权限</a-select-option>
            <a-select-option :value="4">自定义数据权限</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="角色描述" name="description">
          <a-textarea v-model:value="formData.description" placeholder="请输入角色描述" :rows="4" />
        </a-form-item>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 权限配置弹窗 -->
    <a-modal
      v-model:open="permissionVisible"
      title="权限配置"
      :confirm-loading="permissionLoading"
      @ok="handlePermissionOk"
      @cancel="handlePermissionCancel"
      width="800px"
    >
      <div class="permission-tree">
        <a-alert
          message="勾选菜单权限会自动关联对应的按钮权限"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-tree
          v-model:checked-keys="checkedKeys"
          :tree-data="menuTreeData"
          :field-names="{ title: 'menuName', key: 'id' }"
          checkable
          check-strictly
          default-expand-all
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import { getRoleList, addRole, updateRole, deleteRole, getRoleMenus, updateRoleMenus, assignAllPermissionsToAdmin } from '@/api/system'

// 类型定义
interface Role {
  id: number
  roleName: string
  roleCode: string
  description: string
  dataScope: number
  status: number
  createTime: string
}

interface SearchForm {
  roleName?: string
  roleCode?: string
  status?: number
}

// 响应式数据
const loading = ref(false)
const tableData = ref<Role[]>([])
const selectedRowKeys = ref<number[]>([])

// 搜索表单
const searchForm = reactive<SearchForm>({
  roleName: '',
  roleCode: '',
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
    title: '角色名称',
    dataIndex: 'roleName',
    key: 'roleName'
  },
  {
    title: '角色编码',
    dataIndex: 'roleCode',
    key: 'roleCode'
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description'
  },
  {
    title: '数据权限',
    dataIndex: 'dataScope',
    key: 'dataScope'
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
  onChange: (selectedKeys: number[]) => {
    selectedRowKeys.value = selectedKeys
  }
}

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增角色')
const confirmLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  roleName: '',
  roleCode: '',
  description: '',
  dataScope: 3,
  status: 1
})

// 表单验证规则
const formRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' }
  ]
}

// 权限配置相关
const permissionVisible = ref(false)
const permissionLoading = ref(false)
const checkedKeys = ref<number[]>([])
const currentRoleId = ref<number>()

// 菜单树数据（模拟）
const menuTreeData = ref([
  {
    id: 1,
    menuName: '系统管理',
    children: [
      {
        id: 2,
        menuName: '用户管理',
        children: [
          { id: 3, menuName: '用户查询' },
          { id: 4, menuName: '用户新增' },
          { id: 5, menuName: '用户修改' },
          { id: 6, menuName: '用户删除' }
        ]
      },
      {
        id: 7,
        menuName: '角色管理',
        children: [
          { id: 8, menuName: '角色查询' },
          { id: 9, menuName: '角色新增' },
          { id: 10, menuName: '角色修改' },
          { id: 11, menuName: '角色删除' }
        ]
      },
      {
        id: 12,
        menuName: '组织管理',
        children: [
          { id: 13, menuName: '组织查询' },
          { id: 14, menuName: '组织新增' },
          { id: 15, menuName: '组织修改' },
          { id: 16, menuName: '组织删除' }
        ]
      },
      {
        id: 17,
        menuName: '菜单管理',
        children: [
          { id: 18, menuName: '菜单查询' },
          { id: 19, menuName: '菜单新增' },
          { id: 20, menuName: '菜单修改' },
          { id: 21, menuName: '菜单删除' }
        ]
      }
    ]
  }
])

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      roleName: searchForm.roleName,
      roleCode: searchForm.roleCode,
      status: searchForm.status
    }
    
    const response = await getRoleList(params)
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
  searchForm.roleName = ''
  searchForm.roleCode = ''
  searchForm.status = undefined
  handleSearch()
}

const handleAdd = () => {
  modalTitle.value = '新增角色'
  Object.assign(formData, {
    id: undefined,
    roleName: '',
    roleCode: '',
    description: '',
    dataScope: 3,
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = (record: Role) => {
  modalTitle.value = '编辑角色'
  Object.assign(formData, {
    id: record.id,
    roleName: record.roleName,
    roleCode: record.roleCode,
    description: record.description,
    dataScope: record.dataScope,
    status: record.status
  })
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteRole(id)
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
    message.warning('请先选择要删除的角色')
    return
  }
  
  try {
    const response = await deleteRole(selectedRowKeys.value)
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

const handlePermission = (record: Role) => {
  currentRoleId.value = record.id
  // 模拟获取当前角色已有的权限
  checkedKeys.value = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11] // 模拟数据
  permissionVisible.value = true
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
    
    if (modalTitle.value === '新增角色') {
      const response = await addRole(formData)
      if (response.data?.code === 200) {
        message.success('新增成功')
        modalVisible.value = false
        loadData()
      } else {
        message.error(response.data?.message || '新增失败')
      }
    } else {
      const response = await updateRole(formData.id!, formData)
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

const handlePermissionOk = async () => {
  permissionLoading.value = true
  
  try {
    const response = await updateRoleMenus(currentRoleId.value!, checkedKeys.value)
    if (response.data?.code === 200) {
      message.success('权限配置保存成功')
      permissionVisible.value = false
    } else {
      message.error(response.data?.message || '权限配置保存失败')
    }
  } catch (error) {
    message.error('权限配置保存失败')
  } finally {
    permissionLoading.value = false
  }
}

const handlePermissionCancel = () => {
  permissionVisible.value = false
  checkedKeys.value = []
}

const handleAssignAllPermissions = async (record: Role) => {
  try {
    await message.loading('正在分配全部权限...', 0)
    const response = await assignAllPermissionsToAdmin(record.id)
    message.destroy()
    
    if (response.data?.code === 200) {
      message.success('权限分配成功')
      // 重新加载数据以更新显示
      loadData()
    } else {
      message.error(response.data?.message || '权限分配失败')
    }
  } catch (error) {
    message.destroy()
    console.error('分配权限异常:', error)
    message.error('权限分配失败')
  }
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.role-management {
  padding: 0;
  background: transparent;
  min-height: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.operation-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.permission-tree {
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background-color: #fafafa;
}

:deep(.ant-card) {
  border-radius: 8px;
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

:deep(.ant-tree) {
  background: transparent;
}

:deep(.ant-tree-treenode) {
  padding: 4px 0;
}

:deep(.ant-alert-info) {
  border-radius: 6px;
  border: none;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
}
</style>