<template>
  <div class="role-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="角色名称">
          <a-input v-model:value="searchForm.roleName" placeholder="请输入角色名称" class="ui-input" />
        </a-form-item>
        <a-form-item label="角色编码">
          <a-input v-model:value="searchForm.roleCode" placeholder="请输入角色编码" class="ui-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 140px" placeholder="请选择状态" class="ui-select" allow-clear>
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

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="handleAdd" v-permission="['role:add']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增角色
        </a-button>
        <a-button @click="handleBatchDelete" :disabled="selectedRowKeys.length === 0" v-permission="['role:delete']" class="ui-btn-secondary">
          <template #icon><delete-outlined /></template>
          批量删除
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'" class="ui-tag">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'dataScope'">
            <a-tag v-if="record.dataScope === 1" color="blue">全部数据</a-tag>
            <a-tag v-else-if="record.dataScope === 2" color="cyan">本级数据</a-tag>
            <a-tag v-else-if="record.dataScope === 3" color="orange">本级及下级</a-tag>
            <a-tag v-else color="default">自定义</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record as Role)" v-permission="['role:update']" class="font-medium text-blue-600">编辑</a-button>
              <a-button type="link" size="small" @click="handlePermission(record as Role)" v-permission="['role:menu']" class="font-medium text-blue-600">权限配置</a-button>
              <a-button 
                v-if="(record as Role).roleCode === 'admin' || (record as Role).roleCode === 'SUPER_ADMIN'" 
                type="link" 
                size="small" 
                @click="handleAssignAllPermissions(record as Role)"
                class="font-medium text-blue-600"
              >
                分配全部权限
              </a-button>
              <a-popconfirm
                title="确定要删除这个角色吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['role:delete']" class="font-medium">删除</a-button>
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
      destroyOnClose
    >
      <div class="permission-tree-container px-4 py-4">
        <a-alert
          message="勾选菜单权限会自动关联对应的按钮权限"
          type="info"
          show-icon
          class="mb-4 rounded-md"
        />
        <div class="tree-wrapper border border-slate-200 rounded-lg p-4 bg-slate-50 max-h-[500px] overflow-y-auto">
          <a-tree
            v-model:checked-keys="checkedKeys"
            :tree-data="menuTreeData"
            :field-names="{ title: 'menuName', key: 'id' }"
            checkable
            check-strictly
            default-expand-all
          >
            <template #title="{ menuName, menuType, permission: perm }">
              <span class="flex items-center gap-2">
                <a-tag v-if="menuType === 1" color="blue" class="scale-90 origin-left">目录</a-tag>
                <a-tag v-else-if="menuType === 2" color="green" class="scale-90 origin-left">菜单</a-tag>
                <a-tag v-else-if="menuType === 3" color="orange" class="scale-90 origin-left">按钮</a-tag>
                <span class="font-medium">{{ menuName }}</span>
                <span v-if="perm" class="text-slate-400 text-xs italic">
                  ({{ perm }})
                </span>
              </span>
            </template>
          </a-tree>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import type { TableProps, TreeProps } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { getRoleList, addRole, updateRole, deleteRole, updateRoleMenus, assignAllPermissionsToAdmin, getRoleMenus, getMenuTree } from '@/api/system'

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
  showTotal: (total: number) => `共 ${total} 条`
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
  onChange: (selectedKeys: any[]) => {
    selectedRowKeys.value = selectedKeys as number[]
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
const formRules: Record<string, Rule[]> = {
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
const checkedKeys = ref<(number | string)[] | { checked: (number | string)[]; halfChecked: (number | string)[] }>([])
const currentRoleId = ref<number>()

// 菜单树数据
const menuTreeData = ref<TreeProps['treeData']>([])

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
      // 增加数组安全性检查
      const records = response.data.data?.records
      tableData.value = Array.isArray(records) ? records : []
      pagination.total = Number(response.data.data?.total) || 0
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
  Object.assign(formData, { ...record })
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    const response = await deleteRole(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadData()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const handleBatchDelete = async () => {
  if (selectedRowKeys.value.length === 0) return
  try {
    const response = await deleteRole(selectedRowKeys.value as any)
    if (response.data?.code === 200) {
      message.success('批量删除成功')
      selectedRowKeys.value = []
      loadData()
    }
  } catch (error) {
    message.error('批量删除失败')
  }
}

const handlePermission = async (record: Role) => {
  currentRoleId.value = record.id
  permissionVisible.value = true
  permissionLoading.value = true
  
  try {
    const [menuRes, roleMenuRes] = await Promise.all([
      getMenuTree(),
      getRoleMenus(record.id)
    ])
    
    if (menuRes.data?.code === 200) {
      menuTreeData.value = menuRes.data.data || []
    }
    if (roleMenuRes.data?.code === 200) {
      checkedKeys.value = roleMenuRes.data.data || []
    }
  } catch (error) {
    message.error('加载权限数据失败')
  } finally {
    permissionLoading.value = false
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
    
    let response
    if (modalTitle.value === '新增角色') {
      response = await addRole(formData)
    } else {
      response = await updateRole(formData.id!, formData)
    }
    
    if (response.data?.code === 200) {
      message.success('操作成功')
      modalVisible.value = false
      loadData()
    }
  } catch (error) {
    // 验证失败
  } finally {
    confirmLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const handlePermissionOk = async () => {
  if (!currentRoleId.value) return
  permissionLoading.value = true
  
  try {
    let menuIds: (number | string)[] = []
    if (Array.isArray(checkedKeys.value)) {
      menuIds = checkedKeys.value
    } else if (checkedKeys.value && (checkedKeys.value as any).checked) {
      menuIds = (checkedKeys.value as any).checked
    }
    
    const numericMenuIds = menuIds
      .map(id => typeof id === 'string' ? parseInt(id, 10) : id)
      .filter(id => !isNaN(id as number)) as number[]
    
    const response = await updateRoleMenus({
      roleId: currentRoleId.value,
      menuIds: numericMenuIds
    })
    
    if (response.data?.code === 200) {
      message.success('权限配置已更新')
      permissionVisible.value = false
    }
  } catch (error) {
    message.error('更新失败')
  } finally {
    permissionLoading.value = false
  }
}

const handlePermissionCancel = () => {
  permissionVisible.value = false
}

const handleAssignAllPermissions = async (record: Role) => {
  try {
    const hide = message.loading('正在同步系统权限...', 0)
    const response = await assignAllPermissionsToAdmin(record.id)
    hide()
    if (response.data?.code === 200) {
      message.success('全权限同步完成')
      loadData()
    }
  } catch (error) {
    message.error('同步失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>