<template>
  <div class="measurement-unit-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input 
            v-model:value="searchForm.keyword" 
            placeholder="单位代码/中文名称/英文名称" 
            allow-clear 
            class="ui-input" 
          />
        </a-form-item>
        <a-form-item label="单位类型">
          <a-select 
            v-model:value="searchForm.unitType" 
            placeholder="请选择类型" 
            allowClear 
            style="width: 140px" 
            class="ui-select"
          >
            <a-select-option value="数量单位">数量单位</a-select-option>
            <a-select-option value="重量单位">重量单位</a-select-option>
            <a-select-option value="体积单位">体积单位</a-select-option>
            <a-select-option value="面积单位">面积单位</a-select-option>
            <a-select-option value="长度单位">长度单位</a-select-option>
            <a-select-option value="容量单位">容量单位</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select 
            v-model:value="searchForm.status" 
            placeholder="请选择状态" 
            allowClear 
            style="width: 140px" 
            class="ui-select"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" class="ui-btn-primary">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset" class="ui-btn-secondary">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:measurement-unit:create']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增单位
        </a-button>
        <a-button @click="loadUnitList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="unitList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
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
              <a-button 
                type="link" 
                size="small" 
                @click="openEditModal(record as MeasurementUnit)" 
                v-permission="['system:measurement-unit:update']" 
                class="font-medium text-blue-600"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要切换状态吗？"
                @confirm="toggleStatus(record as MeasurementUnit)"
              >
                <a-button 
                  type="link" 
                  size="small" 
                  :danger="record.status === 1" 
                  v-permission="['system:measurement-unit:update']" 
                  class="font-medium"
                >
                  <template #icon>
                    <component :is="record.status === 1 ? 'StopOutlined' : 'CheckCircleOutlined'" />
                  </template>
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button 
                  type="link" 
                  size="small" 
                  danger 
                  v-permission="['system:measurement-unit:delete']" 
                  class="font-medium"
                >
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="editingId ? '编辑计量单位' : '新增计量单位'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="单位代码" name="unitCode">
          <a-input 
            v-model:value="formData.unitCode" 
            placeholder="如：01、02" 
            :disabled="!!editingId"
            :maxlength="10"
          />
        </a-form-item>
        
        <a-form-item label="中文名称" name="unitName">
          <a-input 
            v-model:value="formData.unitName" 
            placeholder="如：个、台、千克" 
            :maxlength="50"
          />
        </a-form-item>
        
        <a-form-item label="英文复数名称" name="unitNameEn">
          <a-input 
            v-model:value="formData.unitNameEn" 
            placeholder="如：PCS、SETS、KILOS（大写缩写）" 
            :maxlength="50"
          />
          <div class="text-gray-400 text-xs mt-1">提示：用于数量>1 时显示</div>
        </a-form-item>
        
        <a-form-item label="英文单数名称" name="unitNameEnSingular">
          <a-input 
            v-model:value="formData.unitNameEnSingular" 
            placeholder="如：piece、set、kilo（完整单词）" 
            :maxlength="50"
          />
          <div class="text-gray-400 text-xs mt-1">提示：用于数量=1 时显示</div>
        </a-form-item>
        
        <a-form-item label="单位类型" name="unitType">
          <a-select 
            v-model:value="formData.unitType" 
            placeholder="请选择单位类型"
          >
            <a-select-option value="数量单位">数量单位</a-select-option>
            <a-select-option value="重量单位">重量单位</a-select-option>
            <a-select-option value="体积单位">体积单位</a-select-option>
            <a-select-option value="面积单位">面积单位</a-select-option>
            <a-select-option value="长度单位">长度单位</a-select-option>
            <a-select-option value="容量单位">容量单位</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="描述说明" name="description">
          <a-textarea 
            v-model:value="formData.description" 
            placeholder="单位的详细说明或用途" 
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>
        
        <a-form-item label="排序" name="sort">
          <a-input-number 
            v-model:value="formData.sort" 
            :min="0" 
            :max="9999"
            style="width: 100%"
          />
          <div class="text-gray-400 text-xs mt-1">数字越小越靠前</div>
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
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { SearchOutlined, PlusOutlined, ReloadOutlined, EditOutlined, StopOutlined, CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import { getMeasurementUnitList, addMeasurementUnit, updateMeasurementUnit, deleteMeasurementUnit, toggleMeasurementUnitStatus } from '@/api/system/measurement-unit';
import type { MeasurementUnit } from '@/api/system/measurement-unit';

// 响应式数据
const loading = ref(false);
const saving = ref(false);
const modalVisible = ref(false);
const editingId = ref<number | null>(null);
const unitList = ref<MeasurementUnit[]>([]);
const formRef = ref<any>(null);

// 搜索表单
const searchForm = reactive({
  keyword: '',
  unitType: '',
  status: undefined as number | undefined
});

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
});

// 表格列配置
const columns: any[] = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '单位代码', dataIndex: 'unitCode', width: 100 },
  { title: '中文名称', dataIndex: 'unitName', width: 100 },
  { title: '英文 (复数)', dataIndex: 'unitNameEn', width: 120 },
  { title: '英文 (单数)', dataIndex: 'unitNameEnSingular', width: 120 },
  { title: '单位类型', dataIndex: 'unitType', width: 100 },
  { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
];

// 表单数据
const formData = reactive<MeasurementUnit>({
  id: undefined,
  unitCode: '',
  unitName: '',
  unitNameEn: '',
  unitNameEnSingular: '',
  unitType: '',
  description: '',
  status: 1,
  sort: 0
});

// 表单验证规则
const formRules: any = {
  unitCode: [
    { required: true, message: '请输入单位代码', trigger: 'blur' },
    { max: 10, message: '单位代码不能超过 10 个字符', trigger: 'blur' }
  ],
  unitName: [
    { required: true, message: '请输入中文名称', trigger: 'blur' },
    { max: 50, message: '中文名称不能超过 50 个字符', trigger: 'blur' }
  ],
  unitNameEn: [
    { required: true, message: '请输入英文复数名称', trigger: 'blur' },
    { max: 50, message: '英文名称不能超过 50 个字符', trigger: 'blur' }
  ],
  unitNameEnSingular: [
    { required: true, message: '请输入英文单数名称', trigger: 'blur' },
    { max: 50, message: '英文名称不能超过 50 个字符', trigger: 'blur' }
  ],
  unitType: [
    { required: true, message: '请选择单位类型', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
};

// 加载单位列表
const loadUnitList = async () => {
  loading.value = true;
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    };
    
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword;
    }
    if (searchForm.unitType) {
      params.unitType = searchForm.unitType;
    }
    if (searchForm.status !== undefined) {
      params.status = searchForm.status;
    }
    
    const res = await getMeasurementUnitList(params);
    if (res.data && res.data.code === 200) {
      unitList.value = res.data.data?.records || [];
      pagination.total = res.data.data?.total || 0;
    }
  } catch (error) {
    console.error('加载单位列表失败:', error);
    message.error('加载失败');
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.current = 1;
  loadUnitList();
};

// 重置
const handleReset = () => {
  searchForm.keyword = '';
  searchForm.unitType = '';
  searchForm.status = undefined;
  handleSearch();
};

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadUnitList();
};

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null;
  Object.assign(formData, {
    id: undefined,
    unitCode: '',
    unitName: '',
    unitNameEn: '',
    unitNameEnSingular: '',
    unitType: '',
    description: '',
    status: 1,
    sort: 0
  });
  modalVisible.value = true;
};

// 打开编辑弹窗
const openEditModal = (record: MeasurementUnit) => {
  editingId.value = record.id ?? null;
  Object.assign(formData, { ...record });
  modalVisible.value = true;
};

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false;
  formRef.value?.resetFields();
};

// 保存
const handleSave = async () => {
  try {
    await formRef.value?.validate();
    saving.value = true;
    
    const success = editingId.value 
      ? await updateMeasurementUnit(formData)
      : await addMeasurementUnit(formData);
    
    if (success) {
      message.success(editingId.value ? '更新成功' : '新增成功');
      closeModal();
      loadUnitList();
    } else {
      message.error(editingId.value ? '更新失败' : '新增失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
  } finally {
    saving.value = false;
  }
};

// 切换状态
const toggleStatus = async (record: MeasurementUnit) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1;
    const success = await toggleMeasurementUnitStatus(record.id!, newStatus);
    
    if (success) {
      message.success(newStatus === 1 ? '启用成功' : '禁用成功');
      loadUnitList();
    } else {
      message.error('操作失败');
    }
  } catch (error) {
    console.error('切换状态失败:', error);
    message.error('操作失败');
  }
};

// 删除
const handleDelete = async (id: number) => {
  try {
    const success = await deleteMeasurementUnit(id);
    
    if (success) {
      message.success('删除成功');
      loadUnitList();
    } else {
      message.error('删除失败');
    }
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
};

// 初始化
onMounted(() => {
  loadUnitList();
});
</script>

<style scoped>
.measurement-unit-management {
  background-color: #f5f5f5;
}
</style>
