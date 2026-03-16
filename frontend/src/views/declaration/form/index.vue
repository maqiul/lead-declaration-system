<template>
  <div class="declaration-form-page">
    <a-card title="出口申报表单">
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          
          <!-- 审核模式下的按钮 -->
          <template v-if="isAudit">
            <a-button type="primary" @click="handleApprove" :loading="submitting">{{ getAuditActionText() }}通过</a-button>
            <a-button danger @click="handleReject" :loading="submitting">{{ getAuditActionText() }}驳回</a-button>
          </template>
          
          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting">提交申报</a-button>
            
            <!-- 定金提交按钮: 待付定金(2)状态 -->
            <a-button v-if="formStatus === 2" type="primary" @click="handleSubmitAudit('deposit')" :loading="submitting">提交定金审核</a-button>
            
            <!-- 尾款提交按钮: 待付尾款(4)状态 -->
            <a-button v-if="formStatus === 4" type="primary" @click="handleSubmitAudit('balance')" :loading="submitting">提交尾款审核</a-button>
          </template>
        </a-space>
      </template>
      
      <!-- 基本信息 -->
      <a-card title="基本信息" size="small" class="section-card">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="申报单号">
              <a-input v-model:value="formData.formNo" placeholder="系统自动生成" readonly />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="申报日期">
              <a-date-picker v-model:value="formData.declarationDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发货人公司名">
              <a-input v-model:value="formData.shipperCompany" placeholder="发货人公司名" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发货人地址">
              <a-input v-model:value="formData.shipperAddress" placeholder="发货人地址" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="收货人公司名">
              <a-input v-model:value="formData.consigneeCompany" placeholder="收货人公司名" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收货人地址">
              <a-input v-model:value="formData.consigneeAddress" placeholder="收货人地址" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="出发城市">
              <a-input v-model:value="formData.departureCity" placeholder="例如：SHANGHAI, CHINA" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="到达地区">
              <a-input v-model:value="formData.destinationRegion" placeholder="例如：NEW YORK, USA" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="运输方式">
              <a-select 
                v-model:value="formData.transportMode" 
                :options="transportModeOptions"
                placeholder="请选择运输方式" 
                :disabled="isReadonly"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发票号">
              <a-input 
                v-model:value="formData.invoiceNo" 
                placeholder="请输入发票号，留空则自动生成(ZIYI-yy-mmdd格式)" 
                :readonly="isReadonly"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="币种">
              <a-select v-model:value="formData.currency" style="width: 100%" :disabled="isReadonly">
                <a-select-option value="USD">USD</a-select-option>
                <a-select-option value="EUR">EUR</a-select-option>
                <a-select-option value="CNY">CNY</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>
      
      <!-- 产品明细 -->
      <a-card title="产品明细" size="small" class="section-card">
        <template #extra>
          <a-button v-if="!isReadonly" type="primary" size="small" @click="addProduct">
            <template #icon><PlusOutlined /></template>
            添加产品
          </a-button>
        </template>
        
        <a-table 
          :dataSource="productList" 
          :columns="productColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              <a-input 
                v-if="!isReadonly" 
                v-model:value="record.productName" 
                placeholder="产品名称"
              />
              <span v-else>{{ record.productName }}</span>
            </template>
            
            <template v-else-if="column.key === 'hsCode'">
              <a-select 
                v-if="!isReadonly"
                v-model:value="record.hsCode" 
                style="width: 100%"
                placeholder="选择HS编码"
                :options="hsOptions"
                show-search
                option-filter-prop="label"
                                                @change="(value) => {
                                                  if (value && typeof value === 'string') {
                                                    onHsCodeChange(index, value)
                                                  }
                                                }"
              />
              <span v-else>{{ record.hsCode }}</span>
            </template>
            
            <template v-else-if="column.key === 'quantity'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.quantity" 
                :min="1"
                @change="() => calculateAmount(record)"
                style="width: 100%"
              />
              <span v-else>{{ record.quantity }}</span>
            </template>
            
            <template v-else-if="column.key === 'unit'">
              <a-select 
                v-if="!isReadonly"
                v-model:value="record.unit" 
                style="width: 100%"
                placeholder="单位"
              >
                <a-select-option value="PCS">PCS</a-select-option>
                <a-select-option value="KGS">KGS</a-select-option>
                <a-select-option value="CBM">CBM</a-select-option>
              </a-select>
              <span v-else>{{ record.unit }}</span>
            </template>
            
            <template v-else-if="column.key === 'unitPrice'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.unitPrice" 
                :min="0"
                :step="0.01"
                @change="() => calculateAmount(record)"
                style="width: 100%"
              />
              <span v-else>{{ record.unitPrice }}</span>
            </template>
            
            <template v-else-if="column.key === 'grossWeight'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.grossWeight" 
                :min="0"
                :step="0.001"
                style="width: 100%"
              />
              <span v-else>{{ record.grossWeight }}</span>
            </template>
            
            <template v-else-if="column.key === 'netWeight'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.netWeight" 
                :min="0"
                :step="0.001"
                style="width: 100%"
              />
              <span v-else>{{ record.netWeight }}</span>
            </template>
            
            <template v-else-if="column.key === 'amount'">
              <span>{{ calculateAmount(record) }}</span>
            </template>
            
            <template v-else-if="column.key === 'productPhoto'">
              <!-- 查看模式: 显示图片或“无” -->
              <div v-if="isReadonly" style="text-align: center;">
                <a-image 
                  v-if="record.productPhoto"
                  :src="record.productPhoto" 
                  :width="60" 
                  :height="60" 
                  :preview="true" 
                  style="object-fit: cover; border-radius: 4px;"
                />
                <span v-else style="color: #ccc;">无</span>
              </div>
              <!-- 编辑模式: 上传图片 -->
              <a-upload
                v-else
                v-model:file-list="record.photoFileList"
                :max-count="1"
                :before-upload="(file) => beforeProductPhotoUpload(file, index)"
                @remove="() => handleRemoveProductPhoto(index)"
                accept="image/*"
                list-type="picture-card"
                show-upload-list
              >
                <div v-if="record.productPhoto || (record.photoFileList && record.photoFileList.length > 0)">
                  <a-icon type="plus" />
                </div>
                <div v-else style="font-size: 12px; color: #999;">上传</div>
              </a-upload>
            </template>
            
            <template v-else-if="column.key === 'declarationElements'">
              <!-- 申报要素容器 -->
              <div v-if="record.declarationElements && record.declarationElements.length > 0">
                <a-collapse :ghost="true" size="small" :bordered="false">
                  <a-collapse-panel key="1" :header="`申报要素 (${record.declarationElements.length}项)`">
                    <a-row :gutter="[12, 12]">
                      <a-col 
                        v-for="element in record.declarationElements" 
                        :key="element.id || element.label" 
                        :span="element.type === 'textarea' ? 24 : 12"
                      >
                        <div style="font-size: 12px; margin-bottom: 4px; color: #64748b; font-weight: 600;">
                          {{ element.label }}{{ element.required ? '*' : '' }}
                        </div>
                        
                        <!-- 编辑模式: 显示对应的输入组件 -->
                        <template v-if="!isReadonly">
                          <a-select
                            v-if="element.type === 'select' && element.options && element.options.length > 0"
                            v-model:value="element.value"
                            placeholder="请选择"
                            size="small"
                            style="width: 100%"
                            :options="element.options.map((opt: any) => ({ label: opt, value: opt }))"
                          />
                          <a-textarea
                            v-else-if="element.type === 'textarea'"
                            v-model:value="element.value"
                            placeholder="请输入内容"
                            size="small"
                            :auto-size="{ minRows: 2, maxRows: 4 }"
                            style="width: 100%; border-radius: 6px;"
                          />
                          <a-input
                            v-else
                            v-model:value="element.value"
                            placeholder="请输入"
                            size="small"
                            style="width: 100%; border-radius: 6px;"
                          />
                        </template>
                        
                        <!-- 只读/查看模式: 显示静态文本 -->
                        <div 
                          v-else
                          class="readonly-element-value"
                        >
                          <span v-if="element.value">{{ element.value }}</span>
                          <span v-else style="color: #cbd5e1; font-style: italic;">未填写</span>
                        </div>
                      </a-col>
                    </a-row>
                  </a-collapse-panel>
                </a-collapse>
              </div>
              <span v-else style="color: #94a3b8; font-size: 13px;">无要素配置</span>
            </template>
            
            <template v-else-if="column.key === 'action'">
              <!-- 查看模式下隐藏删除按钮 -->
              <a-button v-if="!isReadonly && record.id > 0" type="link" danger @click="removeProduct(index)">删除</a-button>
            </template>
          </template>
        </a-table>
        
        <!-- 总计信息 -->
        <div class="totals-section">
          <a-row :gutter="16">
            <a-col :span="4">
              <div class="total-item">
                <span class="total-label">总数量:</span>
                <span class="total-value">{{ totals.totalQuantity }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总毛重(KGS):</span>
                <span class="total-value">{{ totals.totalGrossWeight.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总净重(KGS):</span>
                <span class="total-value">{{ totals.totalNetWeight.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总体积(CBM):</span>
                <span class="total-value">{{ totals.totalVolume.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总金额({{ formData.currency }}):</span>
                <span class="total-value">{{ totals.totalAmount.toFixed(2) }}</span>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-card>
      
      <!-- 箱子信息 -->
      <a-card title="箱子信息" size="small" class="section-card">
        <template #extra>
          <a-button v-if="!isReadonly" type="primary" size="small" @click="addCarton">
            <template #icon><PlusOutlined /></template>
            添加箱子
          </a-button>
        </template>
        
        <a-table 
          :dataSource="cartonList" 
          :columns="cartonColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
          class="carton-table"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'cartonNo'">
              <a-input 
                v-if="!isReadonly" 
                v-model:value="record.cartonNo" 
                placeholder="箱号"
                size="small"
              />
              <span v-else class="value-display">{{ record.cartonNo }}</span>
            </template>
            
            <template v-else-if="column.key === 'quantity'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.quantity" 
                :min="1"
                size="small"
                style="width: 100%"
              />
              <span v-else class="value-display">{{ record.quantity }}</span>
            </template>
            
            <template v-else-if="column.key === 'volume'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.volume" 
                :min="0"
                :step="0.001"
                size="small"
                style="width: 100%"
              />
              <span v-else class="value-display">{{ record.volume }} <span style="font-size: 12px; color: #999;">CBM</span></span>
            </template>
            
            <template v-else-if="column.key === 'selectedProducts'">
              <!-- 查看模式: 只读显示产品标签 -->
              <div v-if="isReadonly" class="products-display">
                <a-space size="small">
                  <a-tag v-for="pid in record.selectedProducts || []" :key="pid" color="blue">
                    {{ getProductNameById(pid) }}
                  </a-tag>
                </a-space>
              </div>
              <!-- 编辑模式: 选择产品 -->
              <a-select 
                v-else
                v-model:value="record.selectedProducts" 
                mode="multiple"
                style="width: 100%"
                placeholder="选择产品"
                :options="productOptions"
                size="small"
              />
            </template>
            
            <template v-else-if="column.key === 'action'">
              <!-- 查看模式下隐藏删除按钮 -->
              <a-button v-if="!isReadonly && record.id > 0" type="link" danger @click="removeCarton(index)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 水单信息 (初审通过后显示) -->
      <a-card v-if="formStatus && formStatus >= 2" title="水单信息" size="small" class="section-card">
        <template #extra>
          <a-space v-if="!isAudit && !isReadonly">
            <!-- 只有在待付定金(2)或定金审核中(3)或定金通过(4)等状态下，控制新增按钮 -->
            <a-button v-if="formStatus === 2 || formStatus === 3" type="primary" size="small" @click="handleAddRemittance(1)">
              <template #icon><PlusOutlined /></template>
              添定金水单
            </a-button>
            <a-button v-if="formStatus === 4 || formStatus === 5" type="primary" size="small" @click="handleAddRemittance(2)">
              <template #icon><PlusOutlined /></template>
              添尾款水单
            </a-button>
          </a-space>
        </template>

        <a-table 
          :dataSource="remittanceList" 
          :columns="remittanceColumns" 
          :pagination="false"
          rowKey="tempId"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'remittanceType'">
              <a-tag :color="record.remittanceType === 1 ? 'orange' : 'green'">
                {{ record.remittanceType === 1 ? '定金' : '尾款' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'remittanceName'">
              <a-input v-model:value="record.remittanceName" size="small" placeholder="名称" />
            </template>
            <template v-else-if="column.key === 'remittanceDate'">
              <a-date-picker v-model:value="record.remittanceDate" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'remittanceAmount'">
              <a-input-number v-model:value="record.remittanceAmount" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'exchangeRate'">
              <a-input-number v-model:value="record.exchangeRate" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'bankFee'">
              <a-input-number v-model:value="record.bankFee" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'creditedAmount'">
              <a-input-number v-model:value="record.creditedAmount" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'photoUrl'">
              <div v-if="record.id || isReadonly" style="text-align: center;">
                <a-image 
                  v-if="record.photoUrl"
                  :src="record.photoUrl" 
                  :width="40" 
                  :height="40" 
                  :preview="true" 
                />
                <span v-else style="color: #ccc;">无</span>
              </div>
              <a-upload
                v-else
                v-model:file-list="record.photoFileList"
                :max-count="1"
                :before-upload="(file) => beforeRemittancePhotoUpload(file, index)"
                @remove="() => handleRemoveRemittancePhoto(index)"
                accept="image/*"
              >
                <a-button size="small">
                  <template #icon><UploadOutlined /></template>
                  上传
                </a-button>
              </a-upload>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space v-if="!record.id && !isReadonly">
                <a-button type="link" size="small" @click="handleSaveRemittance(record)">保存</a-button>
                <a-button type="link" size="small" danger @click="removeRemittance(index)">删除</a-button>
              </a-space>
              <span v-else-if="record.id" style="color: #52c41a;">已保存</span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { getDeclarationDetail, addDeclaration, updateDeclaration } from '@/api/business/declaration'
import { getProductTypes } from '@/api/system/product'
import { getTransportModes } from '@/api/system/config'

const route = useRoute()
const router = useRouter()

// 页面状态
const isAudit = ref(route.query.mode === 'audit')
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(null)
const submitting = ref(false)

// 获取当前审核阶段文本
const getAuditActionText = () => {
  if (formStatus.value === 1) return '初审'
  if (formStatus.value === 3) return '定金审核'
  if (formStatus.value === 5) return '尾款审核'
  return '审批'
}

// 提交审核（定金/尾款）
const handleSubmitAudit = async (type: 'deposit' | 'balance') => {
  if (!formId.value) return
  submitting.value = true
  try {
    const { submitForAudit } = await import('@/api/business/declaration')
    await submitForAudit(formId.value, type)
    message.success(type === 'deposit' ? '定金信息已提交审核' : '尾款信息已提交审核')
    goBack()
  } catch (error) {
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 审核通过
const handleApprove = async () => {
  if (!formId.value) return
  submitting.value = true
  try {
    const { auditDeclaration } = await import('@/api/business/declaration')
    await auditDeclaration(formId.value, 1) // 1 是通过
    message.success(`${getAuditActionText()}已通过`)
    if (formStatus.value === 1) {
      message.info('全套单证已自动生成')
    }
    goBack()
  } catch (error) {
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}

// 驳回
const handleReject = async () => {
  if (!formId.value) return
  submitting.value = true
  try {
    const { auditDeclaration } = await import('@/api/business/declaration')
    await auditDeclaration(formId.value, 2) // 2 是驳回
    message.success(`${getAuditActionText()}已驳回`)
    goBack()
  } catch (error) {
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}

// 保存水单
const handleSaveRemittance = async (record: any) => {
  if (!formId.value) return
  submitting.value = true
  try {
    const { saveRemittance } = await import('@/api/business/declaration')
    const data = {
      ...record,
      remittanceDate: record.remittanceDate.format('YYYY-MM-DD'),
    }
    const res = await saveRemittance(formId.value, data)
    if (res.data && res.data.code === 200) {
      record.id = res.data.data?.id || record.id
    }
    message.success('水单信息保存成功，已生成对应单证')
  } catch (error) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const beforeRemittancePhotoUpload = (file: any, index: number) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    remittanceList.value[index].photoUrl = e.target?.result as string
  }
  reader.readAsDataURL(file)
  return false
}

const handleRemoveRemittancePhoto = (index: number) => {
  remittanceList.value[index].photoUrl = ''
  remittanceList.value[index].photoFileList = []
}

const removeRemittance = (index: number) => {
  remittanceList.value.splice(index, 1)
}

const remittanceList = ref<any[]>([])
const remittanceColumns = [
  { title: '类型', key: 'remittanceType', width: 80 },
  { title: '名称', key: 'remittanceName', width: 120 },
  { title: '日期', key: 'remittanceDate', width: 130 },
  { title: '金额', key: 'remittanceAmount', width: 100 },
  { title: '汇率', key: 'exchangeRate', width: 80 },
  { title: '手续费', key: 'bankFee', width: 80 },
  { title: '入账金额', key: 'creditedAmount', width: 100 },
  { title: '附件', key: 'photoUrl', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

const handleAddRemittance = (type: number) => {
  remittanceList.value.push({
    tempId: Date.now(),
    remittanceType: type,
    remittanceName: type === 1 ? '定金水单' : '尾款水单',
    remittanceDate: dayjs(),
    remittanceAmount: 0,
    exchangeRate: 7.2,
    bankFee: 30,
    creditedAmount: 0,
    photoUrl: '',
    photoFileList: []
  })
}


const formData = reactive({
  formNo: '',
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: '',
  transportMode: undefined as string | undefined,
  departureCity: 'SHANGHAI, CHINA',
  destinationRegion: '',
  currency: 'USD',
  declarationDate: undefined as Dayjs | undefined
})

// 产品列表
const productList = ref<any[]>([])

// 箱子列表
const cartonList = ref<any[]>([])

// 图片上传限制
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png']

// HS编码选项
const hsOptions = ref<any[]>([])

// 运输方式选项
const transportModeOptions = ref<any[]>([])

// 加载运输方式选项
const loadTransportModes = async () => {
  try {
    const response = await getTransportModes()
    if (response.data.code === 200 && response.data.data.length > 0) {
      transportModeOptions.value = response.data.data.map((item: any) => ({
        label: item.label,
        value: item.value
      }))
      console.log('加载运输方式成功:', transportModeOptions.value)
    }
  } catch (error) {
    console.warn('加载运输方式失败:', error)
    // 使用默认运输方式
    transportModeOptions.value = [
      { label: '海运', value: 'SEA' },
      { label: '空运', value: 'AIR' },
      { label: '陆运', value: 'LAND' },
      { label: '快递', value: 'EXPRESS' }
    ]
  }
}

// 加载HS商品类型数据
const loadProductTypes = async () => {
  try {
    const response = await getProductTypes()
    console.log('加载HS商品类型数据:', response.data)
    if (response.data.code === 200 && response.data.data.length > 0) {
      hsOptions.value = response.data.data.map((item: any) => ({
        label: `${item.hsCode} - ${item.englishName}`,
        value: item.hsCode
      }))
      console.log('加载HS商品类型成功:', hsOptions.value)
    } else {
      // 如果API失败，使用默认数据
      hsOptions.value = [
        { label: '39269090 - PLASTIC SEAL', value: '39269090' },
        { label: '85235210 - RFID STICKER', value: '85235210' },
        { label: '48239090 - HANGTAG', value: '48239090' },
        { label: '39232900 - Polybag', value: '39232900' }
      ]
    }
  } catch (error) {
    console.warn('加载HS商品类型失败，使用默认数据:', error)
    // 使用默认数据
    hsOptions.value = [
      { label: '39269090 - PLASTIC SEAL', value: '39269090' },
      { label: '85235210 - RFID STICKER', value: '85235210' },
      { label: '48239090 - HANGTAG', value: '48239090' },
      { label: '39232900 - Polybag', value: '39232900' }
    ]
  }
}

// 产品选项
const productOptions = computed(() => {
  return productList.value.map(item => ({
    label: item.productName,
    value: item.id
  }))
})

// 产品表格列配置
const productColumns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 150 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 120 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '产品照片', key: 'productPhoto', width: 120 },
  { title: '申报要素', key: 'declarationElements', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

// 箱子表格列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '总体积(CBM)', dataIndex: 'volume', key: 'volume', width: 150 },
  { title: '产品选择', key: 'selectedProducts', width: 200 },
  { title: '操作', key: 'action', width: 80 }
]

// 计算总计
const totals = computed(() => {
  let totalQuantity = 0
  let totalGrossWeight = 0
  let totalNetWeight = 0
  let totalVolume = 0
  let totalAmount = 0
  
  productList.value.forEach(item => {
    totalQuantity += item.quantity || 0
    totalGrossWeight += (item.grossWeight || 0) 
    totalNetWeight += (item.netWeight || 0) 
    totalAmount += (item.unitPrice || 0) * (item.quantity || 0)
  })
  
  // 箱子总体积直接累加（因为输入的就是总体积）
  cartonList.value.forEach(carton => {
    totalVolume += (carton.volume || 0)
  })
  
  return {
    totalQuantity,
    totalGrossWeight,
    totalNetWeight,
    totalVolume,
    totalAmount
  }
})

// 计算单项金额
const calculateAmount = (record: any) => {
  const amount = (record.unitPrice || 0) * (record.quantity || 0)
  // 同时更新record中的amount字段
  record.amount = amount.toFixed(2)
  return record.amount
}

// HS编码变更处理
const onHsCodeChange = async (index: number, value: string | number) => {
  const stringValue = typeof value === 'string' ? value : String(value)
  const option = hsOptions.value.find(opt => opt.value === stringValue)
  
  if (option) {
    // 设置产品名称
    const productName = option.label.split(' - ')[1]
    productList.value[index].productName = productName
    
    // 加载对应的申报要素
    try {
      const response = await getProductTypes()
      console.log('API响应数据:', response)
      
      // 检查响应数据结构
      let productTypes = []
      if (response.data && Array.isArray(response.data)) {
        productTypes = response.data
      } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
        productTypes = response.data.data
      } else if (response.data && response.data.records && Array.isArray(response.data.records)) {
        productTypes = response.data.records
      }
      
      console.log('处理后的商品类型数据:', productTypes)
      
      const productType = productTypes.find((item: any) => item.hsCode === stringValue)
      console.log('匹配的商品类型:', productType)
      
      if (productType) {
        // 解析申报要素配置
        let elements = []
        if (productType.elements && productType.elements.length > 0) {
          elements = productType.elements
        } else if (productType.elementsConfig) {
          try {
            // 解析elementsConfig JSON字符串
            elements = JSON.parse(productType.elementsConfig)
            console.log('解析elementsConfig:', elements)
          } catch (parseError) {
            console.error('解析elementsConfig失败:', parseError)
            elements = []
          }
        }
        
        if (elements.length > 0) {
          // 初始化申报要素
          productList.value[index].declarationElements = elements.map((element: any) => ({
            ...element,
            value: element.defaultValue || element.value || ''
          }))
          console.log('设置申报要素:', productList.value[index].declarationElements)
          
          // 测试数据结构
          testDataStructure(productList.value[index].declarationElements)
        } else {
          productList.value[index].declarationElements = []
          console.log('该商品无申报要素配置')
        }
      } else {
        productList.value[index].declarationElements = []
        console.log('未找到匹配的商品类型')
      }
    } catch (error) {
      console.error('加载申报要素失败:', error)
      productList.value[index].declarationElements = []
    }
  }
}

// 获取产品名称(通过ID)
const getProductNameById = (id: number) => {
  const product = productList.value.find(p => p.id === id)
  return product ? product.productName : '未知产品'
}

// 产品照片上传前验证
const beforeProductPhotoUpload = (file: any, productIndex: number) => {
  const isJPGorPNG = ALLOWED_PHOTO_TYPES.includes(file.type)
  if (!isJPGorPNG) {
    message.error('只能上传 JPG/JPEG/PNG 格式的图片!')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('上传图片大小不能超过 2MB!')
    return false
  }
  
  // 上传成功后设置图片URL
  const reader = new FileReader()
  reader.onload = (e) => {
    productList.value[productIndex].productPhoto = e.target?.result as string
  }
  reader.readAsDataURL(file)
  
  // 返回false阻止自动上传
  return false
}

// 移除产品照片
const handleRemoveProductPhoto = (productIndex: number) => {
  productList.value[productIndex].productPhoto = ''
  productList.value[productIndex].photoFileList = []
}

// 添加产品
const addProduct = () => {
  const newId = Math.max(...productList.value.map(p => p.id)) + 1
  productList.value.push({
    id: newId,
    productName: '',
    hsCode: '',
    quantity: 1,
    unit: 'PCS',
    unitPrice: 0,
    amount: '0.00', // 初始化金额字段
    grossWeight: 0,
    netWeight: 0,
    productPhoto: '', // 产品照片URL
    photoFileList: [], // 上传文件列表
    declarationElements: [] // 添加申报要素字段
  })
}

// 删除产品
const removeProduct = (index: number) => {
  productList.value.splice(index, 1)
}

// 添加箱子
const addCarton = () => {
  // 获取当前最大的箱子ID，确保编号连续
  let maxId = 0
  if (cartonList.value.length > 0) {
    maxId = Math.max(...cartonList.value.map(c => c.id))
  }
  const newId = maxId + 1
  
  cartonList.value.push({
    id: newId,
    cartonNo: `CTN${String(newId).padStart(3, '0')}`,
    quantity: 1,
    volume: 0,
    selectedProducts: []
  })
  
  console.log('添加箱子:', { newId, cartonNo: `CTN${String(newId).padStart(3, '0')}`, totalCartons: cartonList.value.length })
}

// 删除箱子
const removeCarton = (index: number) => {
  const removedCarton = cartonList.value[index]
  cartonList.value.splice(index, 1)
  console.log('删除箱子:', { removedId: removedCarton.id, removedCartonNo: removedCarton.cartonNo, remainingCartons: cartonList.value.length })
}

// 返回列表
const goBack = () => {
  router.push('/declaration/manage')
}

// 提交申报
const handleSubmit = async () => {
  try {
    // 验证是否为只读模式(已提交的申报单)
    if (isReadonly.value) {
      message.warning('该申报单已提交，无法编辑')
      return
    }
    
    // 验证必填字段
    if (!formData.consigneeCompany) {
      message.error('请填写收货人公司名')
      return
    }
    
    if (!formData.consigneeAddress) {
      message.error('请填写收货人地址')
      return
    }
    
    if (productList.value.length === 0) {
      message.error('请至少添加一个产品')
      return
    }
    
    // 确保所有产品的金额都已计算
    productList.value.forEach(product => {
      if (product.unitPrice !== undefined && product.quantity !== undefined) {
        product.amount = (product.unitPrice * product.quantity).toFixed(2)
      } else {
        product.amount = '0.00'
      }
    })
    
    // 构建箱子产品关联数据
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number}> = []
    cartonList.value.forEach(carton => {
      if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          // 找到对应的产品
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id, // 注意：这里暂时用临时ID，后端需要处理
              productId: productId,
              quantity: product.quantity // 使用产品数量
            })
          }
        })
      }
    })
    
    console.log('箱子产品关联数据:', cartonProducts)
    
    // 构造提交数据
    const submitData = {
      ...formData,
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      status: 1, // 已提交状态
      products: productList.value.map((product: any) => ({
        ...product,
        // 将 declarationElements 转换为 elementValues (后端需要的格式)
        elementValues: (product.declarationElements || [])
          .filter((elem: any) => elem.value && elem.value.trim())
          .map((elem: any) => ({
            elementName: elem.label,
            elementValue: elem.value
          })),
        // 移除 declarationElements (后端不需要)
        declarationElements: undefined
      })),
      cartons: cartonList.value,
      cartonProducts: cartonProducts // 添加箱子产品关联数据
    }
    
    console.log('提交的数据:', submitData)
    
    if (formId.value) {
      // 更新
      await updateDeclaration(formId.value, submitData)
      message.success('申报单更新成功')
    } else {
      // 新增
      await addDeclaration(submitData)
      message.success('申报单提交成功')
    }
    
    goBack()
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败')
  }
}

// 加载数据
const loadData = async () => {
  // 并行加载配置数据
  await Promise.all([
    loadProductTypes(),
    loadTransportModes()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value)
      console.log('=== 申报单详情API响应 ===', response)
      console.log('response.data:', response.data)
      
      // 处理返回的数据
      if (response.data && response.data.code === 200 && response.data.data) {
        const detailData = response.data.data
        console.log('申报单数据:', detailData)
        console.log('产品列表:', detailData.products)
        console.log('箱子列表:', detailData.cartons)
        
        // 更新只读状态：如果申报单已提交(status >= 1)，则设为只读
        const submittedStatus = detailData.status || 0
        if (submittedStatus >= 1) {
          isReadonly.value = true
          formStatus.value = submittedStatus
          console.log('声明: 该申报单已提交(status=' + submittedStatus + '), 设置为只读模式')
          
          // 即使用户通过URL进入编辑页,也弹出警告
          message.warning('该申报单已提交(status=' + submittedStatus + '), 已自动切换为只读模式')
        }
        
        // 填充基本表单数据
        formData.formNo = detailData.formNo || ''
        formData.shipperCompany = detailData.shipperCompany || 'NINGBO ZIYI TECHNOLOGY CO.,LTD'
        formData.shipperAddress = detailData.shipperAddress || 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA'
        formData.consigneeCompany = detailData.consigneeCompany || ''
        formData.consigneeAddress = detailData.consigneeAddress || ''
        formData.invoiceNo = detailData.invoiceNo || ''
        formData.transportMode = detailData.transportMode
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
        formData.destinationRegion = detailData.destinationRegion || ''
        formData.currency = detailData.currency || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        
        // 填充产品列表
        if (detailData.products && Array.isArray(detailData.products)) {
          productList.value = detailData.products.map((product: any) => ({
            ...product,
            // 处理申报要素值 - 注意后端字段名是 elementName 和 elementValue
            declarationElements: (product.elementValues || []).map((ev: any) => ({
              id: ev.id,
              productId: ev.productId,
              elementId: ev.elementId || ev.elementName,  // 兼容两种字段名
              label: ev.elementName || ev.label,  // 优先使用elementName
              value: ev.elementValue || ev.value,  // 优先使用elementValue
              type: ev.type || 'text',  // 默认为text类型
              required: ev.required || false,
              options: ev.options || [],
              editable: true
            })),
            // 添加体积字段
            volume: product.volume || 0
          }))
          console.log('✅ 加载产品列表成功:', productList.value.length + ' 个产品')
          
          // 打印产品详情用于调试
          productList.value.forEach((p: any, idx: number) => {
            console.log(`产品 ${idx + 1}:`, {
              名称: p.productName,
              HS编码: p.hsCode,
              数量: p.quantity,
              单价: p.unitPrice,
              金额: p.amount,
              体积: p.volume,
              申报要素数量: p.declarationElements?.length || 0
            })
            if (p.declarationElements && p.declarationElements.length > 0) {
              console.log('  申报要素:', p.declarationElements)
            }
          })
        } else {
          console.warn('产品列表为空或不是数组:', detailData.products)
        }
        
        // 填充箱子列表
        if (detailData.cartons && Array.isArray(detailData.cartons)) {
          cartonList.value = detailData.cartons.map((carton: any) => ({
            ...carton,
            // 添加体积字段(如果后端没返回)
            volume: carton.volume || 0,
            // 从 cartonProducts 关联中提取选中的产品
            selectedProducts: (detailData.cartonProducts || [])
              .filter((cp: any) => cp.cartonId === carton.id)
              .map((cp: any) => cp.productId)
          }))
          console.log('✅ 加载箱子列表成功:', cartonList.value.length + ' 个箱子')
          
          // 打印箱子详情用于调试
          cartonList.value.forEach((c: any, idx: number) => {
            console.log(`箱子 ${idx + 1}:`, {
              箱号: c.cartonNo,
              数量: c.quantity,
              体积: c.volume,
              关联产品: c.selectedProducts
            })
          })
        } else {
          console.warn('箱子列表为空或不是数组:', detailData.cartons)
        }
        
        // 填充水单列表
        if (detailData.remittances && Array.isArray(detailData.remittances)) {
          remittanceList.value = detailData.remittances.map((rem: any) => ({
            ...rem,
            remittanceDate: dayjs(rem.remittanceDate),
            tempId: rem.id,
            photoFileList: rem.photoUrl ? [{
              uid: '-1',
              name: 'waterbill.jpg',
              status: 'done',
              url: rem.photoUrl,
            }] : []
          }))
          console.log('✅ 加载水单列表成功:', remittanceList.value.length + ' 条记录')
        }
        
        message.success('数据加载成功')
      } else {
        console.error('API返回异常:', response.data)
        message.error('获取申报单详情失败')
      }
    } catch (error: any) {
      console.error('❌ 加载申报单详情失败:', error)
      message.error('加载数据失败: ' + (error.message || '未知错误'))
    }
  }
}

// 移除不再使用的变量和方法，解决 lint 警告
// const MAX_PHOTO_SIZE = 2 * 1024 * 1024 (已删除)
// const getSelectedProductNames = ... (删除)
// const removeProductFromCarton = ... (删除)
// const getSelectOptions = ... (删除)
// const toggleElements = ... (删除)



// 测试申报要素数据结构
const testDataStructure = (elements: any[]) => {
  console.log('=== 申报要素数据结构测试 ===')
  elements.forEach((element, index) => {
    console.log(`要素 ${index + 1}:`, {
      label: element.label,
      type: element.type,
      editable: element.editable,
      required: element.required,
      hasOptions: !!element.options,
      optionsLength: element.options?.length || 0,
      hasOptionsStr: !!element.optionsStr,
      defaultValue: element.defaultValue,
      currentValue: element.value
    })
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 统一UI风格 - 与系统管理页面完全一致 */
:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
  font-size: 16px;
}

/* 表格样式 */
:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f8fafc !important;
  font-weight: 600;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.ant-table-cell) {
  border-bottom: 1px solid #f1f5f9;
}

/* 主按钮样式 */
:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.25);
  border-radius: 10px;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.35);
  transform: translateY(-1px);
}

/* 产品表格特定样式 */
:deep(.product-table .ant-table-thead > tr > th) {
  background: #f8fafc !important;
}

/* 箱子表格特定样式 */
:deep(.carton-table .ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
}

/* 数值显示样式 */
.value-display {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

/* 箱子产品选择显示样式 */
.products-display {
  padding: 4px 0;
}

.readonly-element-value {
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  min-height: 32px;
  display: flex;
  align-items: center;
  color: #1e293b;
  font-weight: 500;
}

/* 响应式布局 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
}

.declaration-form-page {
  height: 100%;
  overflow-x: hidden;
}

.section-card {
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.ant-card-head) {
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  min-height: 48px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.totals-section {
  margin-top: 24px;
  padding: 24px;
  background: #f8fafc;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.total-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.total-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.total-value {
  font-weight: 800;
  color: #1e40af;
  font-size: 18px;
  letter-spacing: -0.5px;
}
</style>