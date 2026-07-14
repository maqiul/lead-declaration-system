<template>
  <div class="section-wrapper">
          <!-- 基本信息 -->
          <a-card id="section-basic" title="基本信息" size="small" class="section-card">
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
                  <a-select
                    v-model:value="formData.shipperCompany"
                    show-search
                    allow-clear
                    placeholder="请选择或输入发货人公司名"
                    :disabled="isFormReadonly"
                    :filter-option="filterCompanyOption"
                    @change="handleCompanyChange"
                  >
                    <a-select-option v-for="entity in entityList" :key="entity.id" :value="entity.entityName">
                      {{ entity.entityName }}
                      <span v-if="entity.entityNameCn" style="color: #999; font-size: 12px; margin-left: 8px">{{ entity.entityNameCn }}</span>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="发货人地址">
                  <a-input v-model:value="formData.shipperAddress" placeholder="发货人地址" :readonly="isFormReadonly" />
                </a-form-item>
              </a-col>
            </a-row>
            
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="收货人公司名">
                  <a-input v-model:value="formData.consigneeCompany" placeholder="收货人公司名" :readonly="isFormReadonly" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="收货人地址">
                  <a-input v-model:value="formData.consigneeAddress" placeholder="收货人地址" :readonly="isFormReadonly" />
                </a-form-item>
              </a-col>
            </a-row>
            
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="出发城市">
                  <a-select
                    v-model:value="formData.departureCity"
                    placeholder="请选择出发城市"
                    :disabled="isFormReadonly"
                    show-search
                    option-filter-prop="label"
                    style="width: 100%"
                    :options="cityOptions"
                    @change="onDepartureCityChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="目的国">
                  <a-select
                    v-model:value="formData.destinationCountry"
                    :options="countryOptions"
                    placeholder="请输入或选择目的国家 (支持中英文)" 
                    :disabled="isFormReadonly"
                    show-search
                    option-filter-prop="label"
                    style="width: 100%"
                    :filter-option="filterCountrySelectOption"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="贸易国">
                  <a-select
                    v-model:value="formData.tradeCountry"
                    :options="countryOptions"
                    placeholder="请输入或选择贸易国家 (支持中英文)" 
                    :disabled="isFormReadonly"
                    show-search
                    option-filter-prop="label"
                    style="width: 100%"
                    :filter-option="filterCountrySelectOption"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            
            <a-row :gutter="16">
              <a-col :span="4">
                <a-form-item label="运输方式">
                  <a-select 
                    v-model:value="formData.transportMode" 
                    :options="transportModeOptions"
                    placeholder="请选择运输方式" 
                    :disabled="isFormReadonly || transportModeLocked"
                    style="width: 100%"
                  />
                  <div v-if="transportModeLocked" style="font-size: 11px; color: #999; margin-top: 2px;">新建时已选定，不可修改</div>
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="支付方式">
                  <a-select 
                    v-model:value="formData.paymentMethod" 
                    :options="paymentMethodOptions"
                    placeholder="请选择支付方式" 
                    :disabled="isFormReadonly"
                    style="width: 100%"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="发票号">
                  <a-input 
                    v-model:value="formData.invoiceNo" 
                    placeholder="请输入发票号，留空则自动生成(ZIYI-yy-mmdd格式)" 
                    :readonly="isFormReadonly"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="币种">
                  <a-select 
                    v-model:value="formData.currency" 
                    :options="currencyOptions"
                    placeholder="请选择币种" 
                    :disabled="isFormReadonly"
                    show-search
                    option-filter-prop="label"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="申报类型">
                  <a-tag :color="formData.declarationType === 'SELF' ? 'blue' : 'default'" style="font-size: 13px; padding: 4px 12px;">
                    {{ formData.declarationType === 'SELF' ? '梓熠、理德申报' : '集洛申报' }}
                  </a-tag>
                </a-form-item>
              </a-col>
            </a-row>
          </a-card>
          
          <!-- 产品明细 -->
          <a-card id="section-products" title="产品明细" size="small" class="section-card">
            <template #extra>
              <a-button v-if="!isFormReadonly" type="primary" size="small" @click="addProduct">
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
              :scroll="{ x: 1400 }"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'productName'">
                  <a-select
                    v-if="!isFormReadonly"
                    v-model:value="record.productName"
                    :options="productAutoCompleteOptionsWithCustom"
                    placeholder="请输入产品名称(支持中英文)"
                    style="width: 100%"
                    show-search
                    :filter-option="filterProductOption"
                  />
                  <span v-else>{{ record.productName }}</span>
                </template>
                
                <template v-else-if="column.key === 'productChineseName'">
                  <a-input 
                    v-if="!isFormReadonly" 
                    v-model:value="record.productChineseName" 
                    placeholder="产品中文名"
                    @change="() => updateProductName(record)"
                  />
                  <span v-else>{{ record.productChineseName }}</span>
                </template>
                
                <template v-else-if="column.key === 'productEnglishName'">
                  <a-input 
                    v-if="!isFormReadonly" 
                    v-model:value="record.productEnglishName" 
                    placeholder="产品英文名"
                    @change="() => updateProductName(record)"
                  />
                  <span v-else>{{ record.productEnglishName }}</span>
                </template>
                
                <template v-else-if="column.key === 'hsCode'">
                  <a-select 
                    v-if="!isFormReadonly"
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
                    v-if="!isFormReadonly"
                    v-model:value="record.quantity"
                    :min="1"
                    style="width: 100%"
                    @change="handleQuantityOrPriceChange(record)"
                  />
                  <span v-else>{{ record.quantity }}</span>
                </template>
                
                <template v-else-if="column.key === 'unit'">
                  <a-select 
                    v-if="!isFormReadonly"
                    v-model:value="record.unitCode" 
                    style="width: 100%"
                    placeholder="请选择单位"
                    @change="handleUnitChange(record)"
                  >
                    <a-select-option 
                      v-for="unit in measurementUnits" 
                      :key="unit.unitCode"
                      :value="unit.unitCode"
                    >
                      {{ unit.unitName }} ({{ unit.unitNameEn }})
                    </a-select-option>
                  </a-select>
                  <span v-else>{{ findUnitByCode(measurementUnits, record.unitCode)?.unitNameEn || record.unit }}</span>
                </template>
                
                <template v-else-if="column.key === 'unitPrice'">
                  <a-input-number
                    v-if="!isFormReadonly"
                    v-model:value="record.unitPrice"
                    :min="0"
                    :step="0.01"
                    style="width: 100%"
                    @change="handleQuantityOrPriceChange(record)"
                  />
                  <span v-else>{{ record.unitPrice }}</span>
                </template>
                
                <template v-else-if="column.key === 'grossWeight'">
                  <a-input-number 
                    v-if="!isFormReadonly"
                    v-model:value="record.grossWeight" 
                    :min="0"
                    :step="0.001"
                    style="width: 100%"
                  />
                  <span v-else>{{ record.grossWeight }}</span>
                </template>
                
                <template v-else-if="column.key === 'netWeight'">
                  <a-input-number 
                    v-if="!isFormReadonly"
                    v-model:value="record.netWeight" 
                    :min="0"
                    :step="0.001"
                    style="width: 100%"
                  />
                  <span v-else>{{ record.netWeight }}</span>
                </template>
                
                <template v-else-if="column.key === 'cartonInfo'">
                  <!-- 显示关联的箱子信息（只读） -->
                  <div v-if="getProductCartonInfo(record).length > 0">
                    <a-space size="small" wrap>
                      <a-tag v-for="carton in getProductCartonInfo(record)" :key="carton.id" color="orange">
                        {{ carton.cartonNo }}
                      </a-tag>
                    </a-space>
                  </div>
                  <a-tag v-else color="red">未分配箱子</a-tag>
                </template>
                
                <template v-else-if="column.key === 'amount'">
                  <a-input-number
                    v-if="!isFormReadonly"
                    v-model:value="record.amount"
                    :min="0"
                    :step="0.01"
                    style="width: 100%"
                    @change="handleAmountChange(record)"
                  />
                  <span v-else>{{ record.amount }}</span>
                </template>
                
                <template v-else-if="column.key === 'productPhoto'">
                  <!-- 查看模式: 显示图片或"无" -->
                  <div v-if="isFormReadonly" style="text-align: center;">
                    <a-image 
                      v-if="record.productPhoto && record.productPhoto.trim() !== ''"
                      :src="record.productPhoto" 
                      :width="60" 
                      :height="60" 
                      :preview="true" 
                      style="object-fit: cover; border-radius: 4px; cursor: pointer;"
                    />
                    <span v-else style="color: #ccc; font-size: 12px;">无图片</span>
                  </div>
                  <!-- 编辑模式: 上传图片 -->
                  <a-upload
                    v-else
                    :file-list="record.photoFile ? [record.photoFile] : []"
                    :max-count="1"
                    :before-upload="(file) => beforeProductPhotoUpload(file, index)"
                    @remove="() => handleRemoveProductPhoto(index)"
                    accept="image/*"
                    list-type="picture-card"
                    show-upload-list
                  >
                    <div v-if="record.productPhoto || record.photoFile">
                      <a-icon type="plus" />
                    </div>
                    <div v-else style="font-size: 12px; color: #999;">上传</div>
                  </a-upload>
                </template>
                
                <template v-else-if="column.key === 'declarationElements'">
                  <!-- 申报要素按钮触发弹窗 -->
                  <a-button 
                    v-if="record.declarationElements && record.declarationElements.length > 0"
                    type="link"
                    size="small"
                    @click="showElementsModal(record)"
                    style="padding: 0;"
                  >
                    <EnvironmentOutlined />
                    查看申报要素 ({{ record.declarationElements.length }}项)
                  </a-button>
                  <span v-else style="color: #94a3b8; font-size: 13px;">无要素配置</span>
                </template>
                
                <template v-else-if="column.key === 'action'">
                  <!-- 查看模式下隐藏删除按钮 -->
                                <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeProduct(index)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
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
          
          <!-- 产品申报要素弹窗 -->
          <a-modal
            v-model:open="elementsModalVisible"
            :title="`申报要素 - ${currentProductForElements?.productName || ''}`"
            width="800px"
          >
            <a-spin :spinning="elementsLoading">
              <a-form layout="vertical">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="HS编码">
                      <span>{{ currentProductForElements?.hsCode || '' }}</span>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="产品名称">
                      <span>{{ currentProductForElements?.productChineseName || '' }}</span>
                    </a-form-item>
                  </a-col>
                </a-row>
                
                <a-divider orientation="left">申报要素详情</a-divider>
                
                <a-row :gutter="[16, 16]">
                  <a-col 
                    v-for="element in currentElementValues" 
                    :key="element.id || element.label" 
                    :span="element.type === 'textarea' ? 24 : 12"
                  >
                    <a-form-item :label="`${element.label}${element.required ? '*' : ''}`">
                      <!-- 编辑模式: 显示对应的输入组件 -->
                      <template v-if="!isFormReadonly && (!element.editable || element.editable === true)">
                        <a-select
                          v-if="element.type === 'select' && element.options && element.options.length > 0"
                          v-model:value="element.value"
                          placeholder="请选择"
                          style="width: 100%"
                          :options="element.options.map((opt: any) => ({ label: opt, value: opt }))"
                          :disabled="element.editable === false"
                        />
                        <a-textarea
                          v-else-if="element.type === 'textarea'"
                          v-model:value="element.value"
                          placeholder="请输入内容"
                          :auto-size="{ minRows: 2, maxRows: 4 }"
                          style="width: 100%; border-radius: 6px;"
                          :disabled="element.editable === false"
                        />
                        <a-input
                          v-else
                          v-model:value="element.value"
                          placeholder="请输入"
                          style="width: 100%; border-radius: 6px;"
                          :disabled="element.editable === false"
                        />
                      </template>
                      
                      <!-- 只读/查看模式: 显示静态文本 -->
                      <span v-else style="line-height: 1.5;">{{ element.value || '未填写' }}</span>
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-form>
            </a-spin>
            
            <template #footer>
              <a-space>
                <a-button @click="elementsModalVisible = false">
                  <template #icon><CloseOutlined /></template>
                  关闭
                </a-button>
                <a-button 
                  v-if="!isFormReadonly" 
                  type="primary"
                  @click="handleElementsModalConfirm"
                >
                  <template #icon><CheckOutlined /></template>
                  确认
                </a-button>
              </a-space>
            </template>
          </a-modal>
          
          <!-- 箱子信息 -->
          <a-card title="箱子信息" size="small" class="section-card">
            <template #extra>
              <a-button v-if="!isFormReadonly" type="primary" size="small" @click="addCarton">
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
                    v-if="!isFormReadonly" 
                    v-model:value="record.cartonNo" 
                    placeholder="箱号"
                    size="small"
                  />
                  <span v-else class="value-display">{{ record.cartonNo }}</span>
                </template>
                
                <template v-else-if="column.key === 'typeChinese'">
                  <a-select
                    v-if="!isFormReadonly"
                    v-model:value="record.typeChinese"
                    @change="(value) => {
                                    record.typeChinese = value;
                                    if (value === '纸箱') {
                                      record.typeEnglish = 'CARTONS';
                                    } else if (value === '托盘') {
                                      record.typeEnglish = 'PALLETS';
                                    }
                                  }"
                    style="width: 100%"
                    size="small"
                    placeholder="选择类型"
                  >
                    <a-select-option value="纸箱">纸箱</a-select-option>
                    <a-select-option value="托盘">托盘</a-select-option>
                  </a-select>
                  <span v-else class="value-display">{{ record.typeChinese }}</span>
                </template>
                
                <template v-else-if="column.key === 'quantity'">
                  <a-input-number 
                    v-if="!isFormReadonly"
                    v-model:value="record.quantity" 
                    :min="1"
                    size="small"
                    style="width: 100%"
                  />
                  <span v-else class="value-display">{{ record.quantity }}</span>
                </template>
                
                <template v-else-if="column.key === 'volume'">
                  <a-input-number 
                    v-if="!isFormReadonly"
                    v-model:value="record.volume" 
                    :min="0"
                    :step="0.001"
                    size="small"
                    style="width: 100%"
                  />
                  <span v-else class="value-display">{{ record.volume }} <span style="font-size: 12px; color: #999;">CBM</span></span>
                </template>
                
                <template v-else-if="column.key === 'selectedProducts'">
                  <!-- 查看模式: 只读显示产品标签 + 数量/毛重/净重 -->
                  <div v-if="isFormReadonly" class="products-display">
                    <div v-for="detail in (record.productDetails || [])" :key="detail.productId" style="margin-bottom: 4px;">
                      <a-tag color="orange">{{ getProductDisplayById(detail.productId) }}</a-tag>
                      <span style="font-size: 12px; color: #666;">
                        数量:{{ detail.quantity }} | 毛重:{{ detail.grossWeight ?? '-' }} | 净重:{{ detail.netWeight ?? '-' }}
                      </span>
                    </div>
                    <a-space v-if="!record.productDetails?.length" size="small" wrap>
                      <a-tag v-for="pid in record.selectedProducts || []" :key="pid" color="orange">
                        {{ getProductDisplayById(pid) }}
                      </a-tag>
                    </a-space>
                  </div>
                  <!-- 编辑模式: 选择产品 + 每个产品的数量/毛重/净重 -->
                  <div v-else>
                    <a-select
                      v-model:value="record.selectedProducts"
                      mode="multiple"
                      style="width: 100%"
                      placeholder="选择产品"
                      :options="productOptions"
                      size="small"
                      @change="(vals: any) => syncProductDetails(record, vals)"
                    />
                    <!-- 已选产品详情 -->
                    <div v-if="record.productDetails?.length" style="margin-top: 6px;">
                      <div v-for="detail in record.productDetails" :key="detail.productId"
                        style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px; font-size: 12px;">
                        <span style="min-width: 80px; color: #333; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="getProductDisplayById(detail.productId)">
                          {{ getProductDisplayById(detail.productId) }}
                        </span>
                        <a-input-number v-model:value="detail.quantity" :min="0" :max="getMaxQuantity(detail.productId, record)" size="small" placeholder="数量" style="width: 70px;" />
                        <span style="color: #999; font-size: 11px;">/{{ getMaxQuantity(detail.productId, record) }}</span>
                        <a-input-number v-model:value="detail.grossWeight" :min="0" :max="getMaxWeight(detail.productId, 'grossWeight', record)" :step="0.001" size="small" placeholder="毛重" style="width: 80px;" />
                        <span style="color: #999; font-size: 11px;">/{{ getMaxWeight(detail.productId, 'grossWeight', record) }}</span>
                        <a-input-number v-model:value="detail.netWeight" :min="0" :max="getMaxWeight(detail.productId, 'netWeight', record)" :step="0.001" size="small" placeholder="净重" style="width: 80px;" />
                        <span style="color: #999; font-size: 11px;">/{{ getMaxWeight(detail.productId, 'netWeight', record) }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                
                <template v-else-if="column.key === 'action'">
                  <!-- 查看模式下隐藏删除按钮 -->
                  <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeCarton(index)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 基本信息 + 产品明细 + 箱子信息
 * - formData 通过 inject 直接读写（v-model 直接修改响应式对象）
 * - 结构性操作（添加/删除产品、箱子）通过 emit 通知父组件
 * - 纯 UI 状态（申报要素弹窗）保留在组件内部
 */
import { ref, toRefs } from 'vue'
import { useFormState } from '../composables/useDeclarationForm'
import {
  PlusOutlined, DeleteOutlined, EnvironmentOutlined,
  CloseOutlined, CheckOutlined,
} from '@ant-design/icons-vue'
import { findUnitByCode } from '@/utils/measurement-unit'

// emit：结构性操作（父组件处理数据增删 + API）
const emit = defineEmits<{
  'add-product': []
  'remove-product': [index: number]
  'add-carton': []
  'remove-carton': [index: number]
  'upload-product-photo': [file: File, index: number]
  'remove-product-photo': [index: number]
}>()

// inject：共享状态
const state = useFormState()
const {
  formData, isFormReadonly, transportModeLocked,
  entityList, productList, cartonList,
  cityOptions, countryOptions, currencyOptions,
  transportModeOptions, paymentMethodOptions, productOptions,
  productColumns, cartonColumns, productAutoCompleteOptionsWithCustom,
  hsOptions, measurementUnits,
  totals, getProductCartonInfo, getProductDisplayById,
  syncProductDetails, getMaxQuantity, getMaxWeight,
  handleCompanyChange, filterCompanyOption,
  onDepartureCityChange, filterCountrySelectOption,
  handleQuantityOrPriceChange, handleUnitChange, handleAmountChange,
  updateProductName, onHsCodeChange,
} = toRefs(state) as any

// 本地 UI 状态：申报要素弹窗
const elementsModalVisible = ref(false)
const currentProductForElements = ref<any>(null)
const currentElementValues = ref<any[]>([])
const elementsLoading = ref(false)

function showElementsModal(record: any) {
  currentProductForElements.value = record
  currentElementValues.value = record.declarationElements || []
  elementsModalVisible.value = true
}

function handleElementsModalConfirm() {
  if (currentProductForElements.value) {
    currentProductForElements.value.declarationElements = currentElementValues.value
  }
  elementsModalVisible.value = false
}

// 产品照片上传代理
function beforeProductPhotoUpload(file: File, index: number) {
  emit('upload-product-photo', file, index)
  return false // 阻止默认上传，由父组件处理
}

function handleRemoveProductPhoto(index: number) {
  emit('remove-product-photo', index)
}

// 添加/删除产品代理
function addProduct() { emit('add-product') }
function removeProduct(index: number) { emit('remove-product', index) }

// 添加/删除箱子代理
function addCarton() { emit('add-carton') }
function removeCarton(index: number) { emit('remove-carton', index) }

// 过滤函数（纯 UI，本地定义）
const filterProductOption = (input: string, option: any) => {
  const label = option.label || ''
  const lower = input.toLowerCase()
  return String(label).toLowerCase().includes(lower)
}
</script>
