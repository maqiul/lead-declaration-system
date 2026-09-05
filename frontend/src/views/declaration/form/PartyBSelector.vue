<!--
  乙方（销货方）选择器
  维护方式与收货人公司名一致：输入即过滤下拉，名称不在档案列表里时下拉给出「快速新增」，
  弹窗补齐财务资料后自动选中；未命中已有档案时不写入 partyBId，单证销货方信息保持留空。
  档案的修改/删除统一在「乙方配置」菜单完成，申报页不额外挂维护入口。
-->
<template>
  <div class="party-b-selector">
    <a-auto-complete
      v-model:value="partyBText"
      :options="dropdownOptions"
      :filter-option="false"
      :disabled="disabled"
      placeholder="选择或输入乙方（销货方）名称"
      allow-clear
      style="width: 100%"
      @select="handleSelect"
    >
      <template #option="item">
        <span v-if="item.addNew" style="color: var(--color-primary); font-weight: 500">
          <PlusOutlined /> {{ item.label }}
        </span>
        <span v-else>{{ item.label }}</span>
      </template>
    </a-auto-complete>

    <!-- 快速新增乙方：与收货人快速新增客户一致，补齐单证所需的财务信息 -->
    <a-modal
      v-model:open="quickAddVisible"
      title="快速新增乙方"
      :width="560"
      :confirm-loading="quickAddSaving"
      ok-text="保存并选用"
      cancel-text="取消"
      @ok="handleQuickAdd"
    >
      <a-alert
        message="乙方信息将用于合同、发票等单证的销货方填充，请尽量补全财务信息。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form ref="quickAddFormRef" :model="quickAddForm" :rules="quickAddRules" layout="vertical">
        <a-form-item label="乙方名称（销货方）" name="partyBName">
          <a-input v-model:value="quickAddForm.partyBName" placeholder="与单证销货方名称保持一致" :maxlength="200" />
        </a-form-item>
        <a-form-item label="乙方地址（销货方地址）" name="partyBAddress">
          <a-input v-model:value="quickAddForm.partyBAddress" placeholder="请输入乙方地址" :maxlength="200" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="quickAddForm.contactPerson" placeholder="请输入联系人" :maxlength="50" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="quickAddForm.contactPhone" placeholder="请输入联系电话" :maxlength="50" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="纳税人识别号" name="taxId">
          <a-input v-model:value="quickAddForm.taxId" placeholder="请输入纳税人识别号（统一社会信用代码）" :maxlength="50" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开户银行" name="bankName">
              <a-input v-model:value="quickAddForm.bankName" placeholder="请输入开户银行" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="银行账号" name="bankAccount">
              <a-input v-model:value="quickAddForm.bankAccount" placeholder="请输入银行账号" :maxlength="50" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
// AutoComplete 在 vite.config 的组件解析里被 exclude，必须显式引入才能渲染（否则模板找不到组件、下拉框直接空白）
import { message, AutoComplete as AAutoComplete } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { addPartyB, type PartyBConfig } from '@/api/system/partyBConfig'
import { checkPermission } from '@/directives/permission'

type PartyBId = number | string | undefined

/**
 * id 比对口径
 * 后端 Fastjson2 开了 WriteLongAsString，Long 一律以字符串下发；
 * 下拉回填与档案命中必须按字符串比，否则“已选乙方”会被当成未选（输入框空、但摘要有值）
 */
const isSameId = (a: unknown, b: unknown): boolean =>
  a !== undefined && a !== null && b !== undefined && b !== null && String(a) === String(b)

const props = defineProps<{
  /** 已选乙方ID（未选为 undefined） */
  modelValue?: PartyBId
  /** 可选乙方列表（由父组件统一维护，避免多处重复请求） */
  options?: { value?: number | string; label?: string }[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: PartyBId): void
  /** 新增成功后通知父组件刷新乙方列表（payload 带名称，便于自动选中新档案） */
  (e: 'saved', payload: { name: string; isNew: boolean }): void
}>()

const canAdd = computed(() => checkPermission(['party-b:config:add']))
// 保留档案 id 原样（不做 Number 转换），否则与父组件里的字符串 id 对不上
const partyBOptions = computed(() =>
  (props.options || [])
    .filter(o => o.value !== undefined && o.value !== null)
    .map(o => ({ value: o.value as PartyBId, label: o.label || '' }))
)

/** 用户手输的名称：只在「未命中档案」时承担回显，命中后展示一律以 id 对应的档案为准 */
const typedText = ref('')

/** 按名称精确命中已有档案（大小写不敏感、忽略首尾空格） */
const findOptionId = (name: string): PartyBId => {
  const key = (name || '').trim().toLowerCase()
  if (!key) return undefined
  return partyBOptions.value.find(o => (o.label || '').trim().toLowerCase() === key)?.value
}

const partyBText = computed<string>({
  get: () => {
    const id = props.modelValue
    if (id === undefined || id === null || id === '') return typedText.value
    return partyBOptions.value.find(o => isSameId(o.value, id))?.label ?? ''
  },
  set: value => {
    const text = value || ''
    const id = findOptionId(text)
    // 命中档案时由 id 驱动展示，避免外部清空 partyBId 后残留旧名称
    typedText.value = id != null ? '' : text
    emit('update:modelValue', id)
  },
})

// 下拉选项：先按输入过滤已有档案，未精确命中时追加「快速新增」（与收货人口径一致）
const dropdownOptions = computed(() => {
  const text = partyBText.value || ''
  const q = text.trim().toLowerCase()
  const list = partyBOptions.value
    .filter(o => !q || (o.label || '').toLowerCase().includes(q))
    .map(o => ({ value: o.label, label: o.label, addNew: false }))
  if (q && canAdd.value && !partyBOptions.value.some(o => (o.label || '').trim().toLowerCase() === q)) {
    list.push({ value: text, label: `快速新增：${text}`, addNew: true })
  }
  return list
})

// a-auto-complete 的 @select 入参由组件库控制（与收货人公司名同款写法），不在本项目类型范围内
const handleSelect = (value: any, option: any) => {
  // 已有档案的取值由 partyBText 的 setter 完成，这里只需拦截「快速新增」
  if (option && option.addNew) {
    openQuickAdd(String(value ?? partyBText.value ?? ''))
  }
}

// ========== 快速新增 ==========
const quickAddVisible = ref(false)
const quickAddSaving = ref(false)
const quickAddFormRef = ref<FormInstance>()
const quickAddForm = reactive<Omit<PartyBConfig, 'id'>>({
  partyBName: '',
  partyBAddress: '',
  contactPerson: '',
  contactPhone: '',
  taxId: '',
  bankName: '',
  bankAccount: '',
  status: 1,
})
const quickAddRules = {
  partyBName: [{ required: true, message: '请输入乙方名称' }],
  partyBAddress: [{ required: true, message: '请输入乙方地址' }],
}

const openQuickAdd = (name: string) => {
  Object.assign(quickAddForm, {
    partyBName: name.trim(),
    partyBAddress: '',
    contactPerson: '',
    contactPhone: '',
    taxId: '',
    bankName: '',
    bankAccount: '',
    status: 1,
  })
  quickAddFormRef.value?.clearValidate()
  quickAddVisible.value = true
}

const handleQuickAdd = async () => {
  try {
    await quickAddFormRef.value?.validate()
  } catch {
    return
  }
  try {
    quickAddSaving.value = true
    const name = quickAddForm.partyBName.trim()
    const response = await addPartyB({ ...quickAddForm, partyBName: name })
    if (response.data?.code === 200) {
      message.success('乙方添加成功')
      quickAddVisible.value = false
      // 新增接口不返回 id，交由父组件刷新列表后按名称回查并自动选中
      emit('saved', { name, isNew: true })
    } else {
      message.error(response.data?.message || '乙方添加失败')
    }
  } catch (error: any) {
    console.error('快速新增乙方失败:', error)
    message.error('乙方添加失败，请检查网络或联系管理员')
  } finally {
    quickAddSaving.value = false
  }
}
</script>

<style scoped>
.party-b-selector {
  width: 100%;
}
</style>
