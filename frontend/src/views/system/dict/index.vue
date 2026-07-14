<template>
  <div class="dict-management">
    <!-- 操作按钮区域 -->
    <a-card class="ui-card dict-action-card" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddDictModal" v-permission="['system:dict:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增字典
        </a-button>
        <a-button @click="loadDicts" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 两栏布局 -->
    <div class="dict-columns">
      <!-- 左侧：字典类型列表 -->
      <div class="dict-left-col">
        <a-card :bordered="false" class="ui-card dict-full-card">
          <template #title>
            <span class="dict-title">字典类型</span>
          </template>
          <template #extra>
            <a-tag color="blue">{{ dictList.length }} 个</a-tag>
          </template>
          <a-spin :spinning="loadingDicts">
            <div v-if="dictList.length === 0" class="dict-empty">暂无字典类型</div>
            <div
              v-for="dict in dictList"
              :key="dict.id"
              class="dict-card"
              :class="selectedCode === dict.dictCode ? 'dict-card--active' : ''"
              @click="selectDict(dict)"
            >
              <div class="dict-card-header">
                <div class="dict-card-name">{{ dict.dictName }}</div>
                <a-tag :color="dict.status === 1 ? 'success' : 'error'" size="small">
                  {{ dict.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </div>
              <div class="dict-card-code">编码: {{ dict.dictCode }}</div>
              <div v-if="dict.remark" class="dict-card-remark">{{ dict.remark }}</div>
              <div class="dict-actions" style="display:none">
                <a-button size="small" type="link" @click.stop="openEditDictModal(dict as SysDict)"
                  v-permission="['system:dict:update']" class="text-xs p-0 h-auto text-blue-600">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-popconfirm title="确定删除该字典？将同时删除所有字典项" @confirm="handleDeleteDict((dict as SysDict).id!)"
                  okText="确定" cancelText="取消">
                  <a-button size="small" type="link" v-permission="['system:dict:delete']" @click.stop
                    class="text-xs p-0 h-auto text-red-500">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </a-spin>
        </a-card>
      </div>

      <!-- 右侧：字典项列表 -->
      <div class="dict-right-col">
        <a-card :bordered="false" class="ui-card dict-full-card">
          <template #title>
            <span class="dict-title">字典项</span>
            <a-tag v-if="selectedDict" color="geekblue" style="margin-left: 8px">{{ selectedDict.dictName }}</a-tag>
          </template>
          <template #extra v-if="selectedDict">
            <a-button size="small" @click="openAddItemModal" v-permission="['system:dict:add']">
              <template #icon><PlusOutlined /></template>
              新增项
            </a-button>
          </template>

          <a-spin :spinning="loadingItems">
            <div v-if="!selectedDict" class="dict-empty-lg">
              <BookOutlined style="font-size: 48px; color: #d9d9d9" />
              <div class="dict-empty-lg-text">请从左侧选择一个字典</div>
            </div>
            <a-table v-else :dataSource="itemList" :columns="itemColumns" :pagination="false"
              rowKey="id" size="small" class="ui-table" :scroll="{ x: 680 }">
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'sortOrder'">
                  <div class="sort-order-wrap">
                    <span class="sort-order-num">{{ index + 1 }}</span>
                    <div class="sort-btn-col">
                      <a-button size="small" type="text" class="sort-btn" :disabled="index === 0"
                        @click="moveItem(index, -1)"><UpOutlined /></a-button>
                      <a-button size="small" type="text" class="sort-btn"
                        :disabled="index === itemList.length - 1" @click="moveItem(index, 1)"><DownOutlined /></a-button>
                    </div>
                  </div>
                </template>
                <template v-else-if="column.key === 'itemColor'">
                  <a-tag v-if="record.itemColor" :color="record.itemColor">{{ record.itemColor }}</a-tag>
                  <span v-else class="dict-muted">-</span>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="record.status === 1 ? 'success' : 'error'">
                    {{ record.status === 1 ? '启用' : '禁用' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space :size="2">
                    <a-button type="link" size="small" @click="openEditItemModal(record as SysDictItem)"
                      v-permission="['system:dict:update']"
                      class="font-medium text-blue-600" style="padding: 0 4px">
                      <template #icon><EditOutlined /></template>
                      编辑
                    </a-button>
                    <a-popconfirm title="确定删除该字典项？" @confirm="handleDeleteItem((record as SysDictItem).id!)"
                      okText="确定" cancelText="取消">
                      <a-button type="link" size="small" danger v-permission="['system:dict:delete']" class="font-medium" style="padding: 0 4px">
                        <template #icon><DeleteOutlined /></template>
                        删除
                      </a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-card>
      </div>
    </div>

    <!-- 新增/编辑字典类型弹窗 -->
    <a-modal v-model:open="dictModalVisible" :title="isDictEdit ? '编辑字典' : '新增字典'"
      @ok="handleDictModalSubmit" :confirmLoading="dictModalLoading" :width="480" destroyOnClose>
      <a-form :model="dictModalForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
        <a-form-item label="字典编码" required>
          <a-input v-model:value="dictModalForm.dictCode" placeholder="英文编码，如 process_type"
            :maxlength="60" :disabled="isDictEdit" />
          <div class="dict-hint">创建后不可修改</div>
        </a-form-item>
        <a-form-item label="字典名称" required>
          <a-input v-model:value="dictModalForm.dictName" placeholder="如：流程类型" :maxlength="100" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="dictModalForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dictModalForm.remark" placeholder="可选" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增/编辑字典项弹窗 -->
    <a-modal v-model:open="itemModalVisible" :title="isItemEdit ? '编辑字典项' : '新增字典项'"
      @ok="handleItemModalSubmit" :confirmLoading="itemModalLoading" :width="480" destroyOnClose>
      <a-form :model="itemModalForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
        <a-form-item label="字典项值" required>
          <a-input v-model:value="itemModalForm.itemValue" placeholder="如 declaration" :maxlength="100" />
          <div class="dict-hint">程序中使用的值</div>
        </a-form-item>
        <a-form-item label="显示文本" required>
          <a-input v-model:value="itemModalForm.itemLabel" placeholder="如 申报" :maxlength="100" />
        </a-form-item>
        <a-form-item label="标签颜色">
          <a-select v-model:value="itemModalForm.itemColor" placeholder="选择颜色（可选）" allowClear>
            <a-select-option v-for="c in tagColors" :key="c" :value="c">
              <a-tag :color="c" size="small">{{ c }}</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="itemModalForm.sortOrder" :min="0" :max="999" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="itemModalForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="itemModalForm.remark" placeholder="可选" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, BookOutlined, UpOutlined, DownOutlined,
  EditOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import {
  getDictList, createDict, updateDict, deleteDict,
  getDictItems, createDictItem, updateDictItem, deleteDictItem,
  type SysDict, type SysDictItem
} from '@/api/system/dict'

const tagColors = [
  'blue', 'green', 'orange', 'purple', 'cyan', 'magenta', 'red', 'gold',
  'lime', 'geekblue', 'volcano', 'default'
]

// ============================================================
// 字典类型
// ============================================================
const loadingDicts = ref(false)
const dictList = ref<SysDict[]>([])
const selectedCode = ref<string | null>(null)
const selectedDict = ref<SysDict | null>(null)

async function loadDicts() {
  loadingDicts.value = true
  try {
    const res = await getDictList()
    if (res.data?.code === 200) {
      dictList.value = res.data.data ?? []
      if (selectedCode.value) {
        const still = dictList.value.find(d => d.dictCode === selectedCode.value)
        if (still) {
          selectedDict.value = still
          await loadItems(still.dictCode)
        } else {
          selectedCode.value = null
          selectedDict.value = null
          itemList.value = []
        }
      }
    }
  } finally {
    loadingDicts.value = false
  }
}

function selectDict(dict: SysDict) {
  selectedCode.value = dict.dictCode
  selectedDict.value = dict
  loadItems(dict.dictCode)
}

// ============================================================
// 字典类型弹窗
// ============================================================
const dictModalVisible = ref(false)
const dictModalLoading = ref(false)
const isDictEdit = ref(false)
const dictEditId = ref<number | null>(null)

const dictModalForm = ref({
  dictCode: '', dictName: '', status: 1, remark: '' as string | undefined,
})

function openAddDictModal() {
  isDictEdit.value = false
  dictEditId.value = null
  dictModalForm.value = { dictCode: '', dictName: '', status: 1, remark: undefined }
  dictModalVisible.value = true
}

function openEditDictModal(dict: SysDict) {
  isDictEdit.value = true
  dictEditId.value = dict.id ?? null
  dictModalForm.value = {
    dictCode: dict.dictCode, dictName: dict.dictName,
    status: dict.status, remark: dict.remark,
  }
  dictModalVisible.value = true
}

async function handleDictModalSubmit() {
  if (!dictModalForm.value.dictCode.trim()) { message.warning('请填写字典编码'); return }
  if (!dictModalForm.value.dictName.trim()) { message.warning('请填写字典名称'); return }

  dictModalLoading.value = true
  try {
    let res: any
    if (isDictEdit.value && dictEditId.value) {
      res = await updateDict(dictEditId.value, dictModalForm.value)
    } else {
      res = await createDict(dictModalForm.value as SysDict)
    }
    if (res.data?.code === 200) {
      message.success(isDictEdit.value ? '字典更新成功' : '字典创建成功')
      dictModalVisible.value = false
      await loadDicts()
    } else {
      message.error(res.data?.message ?? '操作失败')
    }
  } finally {
    dictModalLoading.value = false
  }
}

async function handleDeleteDict(id: number) {
  const res = await deleteDict(id)
  if (res.data?.code === 200) {
    message.success('字典删除成功')
    await loadDicts()
  } else {
    message.error(res.data?.message ?? '删除失败')
  }
}

// ============================================================
// 字典项
// ============================================================
const loadingItems = ref(false)
const itemList = ref<SysDictItem[]>([])

const itemColumns = [
  { title: '排序', key: 'sortOrder', width: 70, align: 'center' as const },
  { title: '值', dataIndex: 'itemValue', width: 130 },
  { title: '显示文本', dataIndex: 'itemLabel', width: 160 },
  { title: '颜色', key: 'itemColor', width: 100, align: 'center' as const },
  { title: '状态', key: 'status', width: 70, align: 'center' as const },
  { title: '操作', key: 'action', width: 150, align: 'center' as const, fixed: 'right' as const },
]

async function loadItems(dictCode: string) {
  loadingItems.value = true
  try {
    const res = await getDictItems(dictCode)
    if (res.data?.code === 200) {
      itemList.value = res.data.data ?? []
    }
  } finally {
    loadingItems.value = false
  }
}

function moveItem(index: number, direction: number) {
  const target = index + direction
  if (target < 0 || target >= itemList.value.length) return
  const temp = itemList.value[index]
  itemList.value[index] = itemList.value[target]
  itemList.value[target] = temp
  itemList.value = [...itemList.value]
  // 更新 sortOrder
  itemList.value.forEach((item, i) => { item.sortOrder = i + 1 })
}

// ============================================================
// 字典项弹窗
// ============================================================
const itemModalVisible = ref(false)
const itemModalLoading = ref(false)
const isItemEdit = ref(false)
const itemEditId = ref<number | null>(null)

const itemModalForm = ref({
  itemValue: '', itemLabel: '', itemColor: undefined as string | undefined,
  sortOrder: 0, status: 1, remark: '' as string | undefined,
})

function openAddItemModal() {
  isItemEdit.value = false
  itemEditId.value = null
  itemModalForm.value = {
    itemValue: '', itemLabel: '', itemColor: undefined,
    sortOrder: itemList.value.length + 1, status: 1, remark: undefined,
  }
  itemModalVisible.value = true
}

function openEditItemModal(item: SysDictItem) {
  isItemEdit.value = true
  itemEditId.value = item.id ?? null
  itemModalForm.value = {
    itemValue: item.itemValue, itemLabel: item.itemLabel,
    itemColor: item.itemColor, sortOrder: item.sortOrder,
    status: item.status, remark: item.remark,
  }
  itemModalVisible.value = true
}

async function handleItemModalSubmit() {
  if (!itemModalForm.value.itemValue.trim()) { message.warning('请填写字典项值'); return }
  if (!itemModalForm.value.itemLabel.trim()) { message.warning('请填写显示文本'); return }
  if (!selectedCode.value) return

  itemModalLoading.value = true
  try {
    let res: any
    if (isItemEdit.value && itemEditId.value) {
      res = await updateDictItem(itemEditId.value, itemModalForm.value)
    } else {
      res = await createDictItem(selectedCode.value, itemModalForm.value as SysDictItem)
    }
    if (res.data?.code === 200) {
      message.success(isItemEdit.value ? '字典项更新成功' : '字典项创建成功')
      itemModalVisible.value = false
      await loadItems(selectedCode.value)
    } else {
      message.error(res.data?.message ?? '操作失败')
    }
  } finally {
    itemModalLoading.value = false
  }
}

async function handleDeleteItem(id: number) {
  const res = await deleteDictItem(id)
  if (res.data?.code === 200) {
    message.success('字典项删除成功')
    if (selectedCode.value) await loadItems(selectedCode.value)
  } else {
    message.error(res.data?.message ?? '删除失败')
  }
}

// ============================================================
// 初始化
// ============================================================
onMounted(async () => {
  await loadDicts()
  if (dictList.value.length > 0) {
    selectDict(dictList.value[0])
  }
})
</script>

<style scoped>
/* 操作按钮卡片间距 */
.dict-action-card {
  margin-bottom: 16px;
}

/* 两栏布局 */
.dict-columns {
  display: flex;
  gap: 16px;
}
.dict-left-col {
  width: 320px;
  flex-shrink: 0;
}
.dict-right-col {
  flex: 1;
  min-width: 0;
}
.dict-full-card {
  height: 100%;
}

/* 卡片标题 */
.dict-title {
  font-weight: 500;
}

/* 空状态 */
.dict-empty {
  text-align: center;
  color: rgba(0, 0, 0, 0.25);
  padding: 32px 0;
}
.dict-empty-lg {
  text-align: center;
  color: rgba(0, 0, 0, 0.25);
  padding: 64px 0;
}
.dict-empty-lg-text {
  margin-top: 16px;
}

/* 字典类型卡片项 */
.dict-card {
  cursor: pointer;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s;
  background: #fff;
}
.dict-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}
.dict-card--active {
  border-color: #3b82f6;
  background: #eff6ff;
}
.dict-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}
.dict-card-name {
  font-weight: 500;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
}
.dict-card-code {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
  margin-bottom: 8px;
}
.dict-card-remark {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dict-card:hover .dict-actions {
  display: flex !important;
}
.dict-actions {
  gap: 4px;
  margin-top: 4px;
}

/* 排序列 */
.sort-order-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sort-order-num {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  width: 20px;
  text-align: center;
}
.sort-btn-col {
  display: flex;
  flex-direction: column;
}

/* 辅助 */
.dict-muted {
  color: rgba(0, 0, 0, 0.25);
}
.dict-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
  margin-top: 4px;
}

/* 排序按钮 */
.sort-btn {
  width: 20px !important;
  height: 16px !important;
  font-size: 10px !important;
  padding: 0 !important;
  color: #8c8c8c !important;
}
.sort-btn:not(:disabled):hover {
  color: #1677ff !important;
  background: #e6f4ff !important;
}
</style>
