<template>
  <div class="declaration-form-page">
    <a-card :title="(isPickupMode ? '提货单提交' : isInvoiceUploadMode ? '申报单详情 - 上传发票' : '出口申报表单')" >
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          
          <!-- 审核详情按钮 - 所有状态都显示 -->
          <a-button
            @click="showAuditHistory"
            v-permission="['business:declaration:view']"
          >
            <template #icon><HistoryOutlined /></template>
            审核详情
          </a-button>
          
          <!-- 审核模式下的按钮 -->
          <template v-if="isAudit">
            <a-button
              type="primary"
              @click="handleApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:initial', 'business:declaration:audit:return']"
            >{{ getAuditActionText() }}通过</a-button>
            <a-button
              danger
              @click="handleReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:initial', 'business:declaration:audit:return']"
            >{{ getAuditActionText() }}驳回</a-button>
          </template>

          <!-- 提货单提交模式下的按钮：状态6（待上传提货单）或状态7（提货单待审核）时显示 -->
          <template v-else-if="isPickupMode">
            <a-button 
              v-if="(formStatus === 1 || formStatus === 2)" 
              type="primary" 
              @click="handleSubmitAudit('pickup')" 
              :loading="submitting" 
              :disabled="deliveryOrderList.length === 0 || !deliveryOrderList.some(order => order.status === 0)"
              v-permission="['business:declaration:finance:supplement']"
            >
              提交提货单审核
            </a-button>
            
          </template>
          
          
          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 保存草稿按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="handleSaveDraft" :loading="submitting" v-permission="['business:declaration:create']">保存草稿</a-button>
            
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting" v-permission="['business:declaration:submit']">提交申报</a-button>
          </template>
        </a-space>
      </template>
      
      <!-- 退回原因提示 -->
      <a-alert
        v-if="formStatus === 9 && returnReason"
        message="退回申请原因"
        :description="returnReason"
        type="warning"
        show-icon
        class="mb-4"
      />
      
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
              <a-input v-model:value="formData.shipperCompany" placeholder="发货人公司名" :readonly="isFormReadonly" />
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
                :disabled="isFormReadonly"
                style="width: 100%"
              />
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
        </a-row>
      </a-card>
      
      <!-- 产品明细 -->
      <a-card title="产品明细" size="small" class="section-card">
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
                            <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeProduct(index)">删除</a-button>
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
            <a-button @click="elementsModalVisible = false">关闭</a-button>
            <a-button 
              v-if="!isFormReadonly" 
              type="primary"
              @click="handleElementsModalConfirm"
            >
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
              <!-- 查看模式: 只读显示产品标签 -->
              <div v-if="isFormReadonly" class="products-display">
                <a-space size="small" wrap>
                  <a-tag 
                    v-for="pid in record.selectedProducts || []" 
                    :key="pid" 
                    color="orange"
                  >
                    {{ getProductDisplayById(pid) }}
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
              <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeCarton(index)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 业务发票 (状态1及以上显示) -->
      <a-card v-if="formStatus && formStatus >= 1" title="业务发票" size="small" class="section-card" style="margin-top: 16px;">
        <template #extra>
          <a-button v-if="!isFormReadonly || isInvoiceUploadMode" type="primary" size="small" @click="showInvoiceModal" v-permission="['business:declaration:update']">
            <PlusOutlined /> 上传发票
          </a-button>
        </template>

        <a-table
          :dataSource="businessInvoiceList"
          :columns="businessInvoiceColumns"
          :pagination="false"
          rowKey="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'fileName'">
              <a v-if="record.fileUrl" :href="getInvoiceFileUrl(record.fileUrl)" target="_blank">
                <DownloadOutlined /> {{ record.fileName || '查看' }}
              </a>
              <span v-else style="color: #999;">无附件</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确定删除该发票记录吗？" @confirm="handleDeleteInvoice(record.id)">
                <a-button v-if="!isFormReadonly || isInvoiceUploadMode" type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 提货单 (状态3及以上显示) -->
      <a-card v-if="formStatus && formStatus >= 1" title="提货单" size="small" class="section-card">
        <template #extra>
          <a-button
            v-if="isDeliveryOrderEditable"
            type="primary"
            size="small"
            @click="showDeliveryOrderModal"
            v-permission="['business:declaration:delivery:create']"
          >
            <template #icon><PlusOutlined /></template>
            新增提货单
          </a-button>
        </template>
        
        <a-table 
          v-if="deliveryOrderList.length > 0"
          :dataSource="deliveryOrderList" 
          :columns="deliveryOrderColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
          :loading="loadingDeliveryOrders"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'deliveryDate'">
              {{ record.deliveryDate ? dayjs(record.deliveryDate).format('YYYY-MM-DD') : '' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getDeliveryOrderStatusColor(record.status)">
                {{ getDeliveryOrderStatusText(record.status) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'fileName'">
              <a v-if="record.fileUrl" :href="record.fileUrl" target="_blank">{{ record.fileName || '查看附件' }}</a>
              <span v-else style="color: #999;">无附件</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button 
                  v-if="record.fileUrl" 
                  type="link" 
                  size="small" 
                  @click="handleDownloadDeliveryOrder(record)"
                >
                  下载
                </a-button>
                <a-button
                  v-if="isDeliveryOrderEditable && record.status === 0"
                  type="link"
                  size="small"
                  @click="handleEditDeliveryOrder(record)"
                  v-permission="['business:declaration:delivery:update']"
                >
                  编辑
                </a-button>
                <a-popconfirm
                  v-if="isDeliveryOrderEditable && record.status === 0"
                  title="确定要删除这条提货单吗？"
                  @confirm="handleDeleteDeliveryOrder(record)"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                    v-permission="['business:declaration:delivery:delete']"
                  >
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
        <a-empty v-if="deliveryOrderList.length === 0 && !loadingDeliveryOrders" description="暂无提货单，请点击上方按钮添加" />
      </a-card>
      
      <!-- 新增/编辑提货单弹窗 -->
      <a-modal
        v-model:open="deliveryOrderModalVisible"
        :title="editingDeliveryOrder ? '编辑提货单' : '新增提货单'"
        @ok="handleSaveDeliveryOrder"
        :confirmLoading="savingDeliveryOrder"
        width="520px"
      >
        <a-form :model="deliveryOrderForm" layout="vertical">
          <a-form-item label="提货日期" required>
            <a-date-picker 
              v-model:value="deliveryOrderForm.deliveryDate" 
              style="width: 100%" 
              placeholder="选择提货日期"
            />
          </a-form-item>
          <a-form-item label="备注说明">
            <a-textarea 
              v-model:value="deliveryOrderForm.remark" 
              :rows="3" 
              placeholder="请输入备注说明（可选）"
            />
          </a-form-item>
          <a-form-item label="上传提货单文件">
            <a-upload
              :file-list="deliveryOrderForm.fileList"
              :max-count="1"
              :before-upload="beforeDeliveryOrderUpload"
              @remove="handleRemoveDeliveryOrderFile"
              accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
            >
              <a-button>
                <template #icon><UploadOutlined /></template>
                选择文件
              </a-button>
            </a-upload>
            <div style="margin-top: 8px; color: #999; font-size: 12px;">
              支持PDF、Word文档、图片格式，单个文件不超过10MB
            </div>
          </a-form-item>
        </a-form>
      </a-modal>
      
      <!-- 财务补充单证 -->
      <a-card v-if="financeSupplement?.id || isFinanceUploadMode" :title="isFinanceUploadMode ? '财务补充单证 (可编辑)' : '财务补充单证 (已上传详情)'" size="small" class="section-card">
        <template #extra v-if="isFinanceUploadMode">
          <a-button type="primary" size="small" @click="handleSaveFinanceSupplement" :loading="submittingSupplement" v-permission="['business:declaration:finance:supplement']">保存内容</a-button>
        </template>
        <a-form layout="vertical" :model="financeSupplement">
          <a-row :gutter="16">
             <a-col :span="24" style="margin-bottom: 16px;">
               <a-form-item label="币种" style="margin-bottom: 0;">
                 <a-select v-model:value="financeSupplement.currency" :disabled="!isFinanceUploadMode" style="width: 200px">
                   <a-select-option value="CNY">人民币 (CNY)</a-select-option>
                   <a-select-option value="USD">美元 (USD)</a-select-option>
                 </a-select>
               </a-form-item>
             </a-col>
             
             <!-- 货代发票 -->
             <a-col :span="8">
               <a-card title="货代发票" size="small">
                 <a-form-item label="发票号">
                   <a-input v-model:value="financeSupplement.freightInvoiceNo" :disabled="!isFinanceUploadMode" />
                 </a-form-item>
                 <a-form-item label="金额">
                   <a-input-number v-model:value="financeSupplement.freightAmount" style="width: 100%" :disabled="!isFinanceUploadMode" />
                 </a-form-item>
                 <a-form-item label="附件">
                   <div v-if="!isFinanceUploadMode && financeSupplement.freightFileName">
                     <a :href="financeSupplement.freightFileUrl" target="_blank">{{ financeSupplement.freightFileName }}</a>
                   </div>
                   <a-upload
                     v-else-if="isFinanceUploadMode"
                     :max-count="1"
                     :before-upload="(file) => beforeSupplementPhotoUpload(file, 'freight')"
                     @remove="() => handleRemoveSupplementPhoto('freight')"
                     :file-list="financeSupplement.freightFileList || (financeSupplement.freightFileName ? [{uid: '-1', name: financeSupplement.freightFileName, status: 'done', url: financeSupplement.freightFileUrl}] : [])"
                   >
                     <a-button><UploadOutlined /> {{ financeSupplement.freightFileName ? '替换附件' : '上传附件' }}</a-button>
                   </a-upload>
                   <span v-else style="color: #999">无附件</span>
                 </a-form-item>
               </a-card>
             </a-col>

             <!-- 报关发票 -->
             <a-col :span="8">
               <a-card title="报关发票" size="small">
                 <a-form-item label="发票号">
                   <a-input v-model:value="financeSupplement.customsInvoiceNo" :disabled="!isFinanceUploadMode" />
                 </a-form-item>
                 <a-form-item label="金额">
                   <a-input-number v-model:value="financeSupplement.customsAmount" style="width: 100%" :disabled="!isFinanceUploadMode" />
                 </a-form-item>
                 <a-form-item label="附件">
                   <div v-if="!isFinanceUploadMode && financeSupplement.customsFileName">
                     <a :href="financeSupplement.customsFileUrl" target="_blank">{{ financeSupplement.customsFileName }}</a>
                   </div>
                   <a-upload
                     v-else-if="isFinanceUploadMode"
                     :max-count="1"
                     :before-upload="(file) => beforeSupplementPhotoUpload(file, 'customs')"
                     @remove="() => handleRemoveSupplementPhoto('customs')"
                     :file-list="financeSupplement.customsFileList || (financeSupplement.customsFileName ? [{uid: '-2', name: financeSupplement.customsFileName, status: 'done', url: financeSupplement.customsFileUrl}] : [])"
                   >
                     <a-button><UploadOutlined /> {{ financeSupplement.customsFileName ? '替换附件' : '上传附件' }}</a-button>
                   </a-upload>
                   <span v-else style="color: #999">无附件</span>
                 </a-form-item>
               </a-card>
             </a-col>

             <!-- 海关回执 -->
             <a-col :span="8">
               <a-card title="海关回执" size="small">
                 <a-form-item label="附件">
                   <div v-if="!isFinanceUploadMode && financeSupplement.customsReceiptFileName">
                     <a :href="financeSupplement.customsReceiptFileUrl" target="_blank">{{ financeSupplement.customsReceiptFileName }}</a>
                   </div>
                   <a-upload
                     v-else-if="isFinanceUploadMode"
                     :max-count="1"
                     :before-upload="(file) => beforeSupplementPhotoUpload(file, 'customsReceipt')"
                     @remove="() => handleRemoveSupplementPhoto('customsReceipt')"
                     :file-list="financeSupplement.customsReceiptFileList || (financeSupplement.customsReceiptFileName ? [{uid: '-4', name: financeSupplement.customsReceiptFileName, status: 'done', url: financeSupplement.customsReceiptFileUrl}] : [])"
                   >
                     <a-button><UploadOutlined /> {{ financeSupplement.customsReceiptFileName ? '替换附件' : '上传附件' }}</a-button>
                   </a-upload>
                   <span v-else style="color: #999">无附件</span>
                 </a-form-item>
               </a-card>
             </a-col>

             <a-col :span="24" style="margin-top: 16px;"></a-col>

             <!-- 外汇明细 -->
             <a-col :span="24" style="margin-top: 16px;">
               <a-card title="外汇明细" size="small">
                 <template #extra>
                   <a-tag v-if="remittanceList.length > 0" color="success">
                     已关联 {{ remittanceList.length }} 条水单
                   </a-tag>
                   <a-tag v-else color="default">暂无水单</a-tag>
                 </template>

                 <!-- 水单列表展示 -->
                 <a-table
                   v-if="remittanceList.length > 0"
                   :dataSource="remittanceList"
                   :columns="remittanceSummaryColumns"
                   :pagination="false"
                   rowKey="id"
                   size="small"
                 >
                   <template #bodyCell="{ column, record }">
                     <template v-if="column.key === 'remittanceName'">
                       <a-tag :color="record.status === 2 ? 'green' : 'blue'">
                         {{ record.remittanceName }}
                       </a-tag>
                     </template>
                     <template v-else-if="column.key === 'bankAccount'">
                       {{ record.bankAccountName || '未设置' }}
                     </template>
                     <template v-else-if="column.key === 'amount'">
                       <span style="font-weight: 500;">{{ record.remittanceAmount?.toFixed(2) || '0.00' }}</span>
                       <span style="font-size: 12px; color: #888; margin-left: 4px;">{{ record.currency }}</span>
                     </template>
                     <template v-else-if="column.key === 'relationAmount'">
                       <span style="font-weight: 500; color: #1890ff;">{{ record.totalRelatedAmount?.toFixed(2) || record.remittanceAmount?.toFixed(2) || '0.00' }}</span>
                       <div style="font-size: 11px; color: #888;">关联到本申报单</div>
                     </template>
                     <template v-else-if="column.key === 'bankFeeRate'">
                       {{ ((record.bankFeeRate || 0) * 100).toFixed(2) }}%
                     </template>
                     <template v-else-if="column.key === 'bankFee'">
                       {{ record.bankFee?.toFixed(2) || '0.00' }}
                     </template>
                     <template v-else-if="column.key === 'date'">
                       {{ record.remittanceDate ? dayjs(record.remittanceDate).format('YYYY-MM-DD') : '-' }}
                     </template>
                     <template v-else-if="column.key === 'status'">
                       <a-tag :color="getStatusColor(record.status)">
                         {{ getRemittanceStatusText(record.status) }}
                       </a-tag>
                     </template>
                   </template>
                 </a-table>

                 <a-empty v-else description="暂无水单数据" :image="simpleImage" />

                 <!-- 汇总统计 -->
                 <a-divider style="margin: 16px 0 12px;" />
                 <a-row :gutter="16">
                   <a-col :span="8">
                     <a-statistic title="收汇总计" :value="totalRemittanceAmount" :precision="2" :value-style="{ color: '#3f8600' }">
                       <template #suffix>{{ formData.currency }}</template>
                     </a-statistic>
                   </a-col>
                   <a-col :span="8">
                     <a-statistic title="手续费合计" :value="totalBankFeeAmount" :precision="2" :value-style="{ color: '#cf1322' }">
                       <template #suffix>{{ formData.currency }}</template>
                     </a-statistic>
                   </a-col>
                   <a-col :span="8">
                     <a-statistic title="净收汇金额" :value="netRemittanceAmount" :precision="2" :value-style="{ color: '#1890ff' }">
                       <template #suffix>{{ formData.currency }}</template>
                     </a-statistic>
                   </a-col>
                 </a-row>
               </a-card>
             </a-col>

          </a-row>

          <!-- 开票明细附件 -->
          <a-row :gutter="16" style="margin-top: 16px;">
            <a-col :span="24">
              <a-card title="开票明细附件" size="small">
                <template #extra v-if="isFinanceUploadMode">
                  <a-button type="primary" size="small" @click="handleGenerateFinanceDetails" :loading="generatingDetails" v-permission="['business:declaration:finance:supplement']">
                    <template #icon><DownloadOutlined /></template>
                    生成并下载开票明细
                  </a-button>
                </template>

                <div v-if="!isFinanceUploadMode && financeSupplement.detailsFileName">
                  <a :href="financeSupplement.detailsFileUrl" target="_blank" style="font-size: 14px;">
                    <DownloadOutlined /> {{ financeSupplement.detailsFileName }}
                  </a>
                </div>
                <a-upload
                  v-else-if="isFinanceUploadMode"
                  :max-count="1"
                  :before-upload="(file) => beforeSupplementPhotoUpload(file, 'details')"
                  @remove="() => handleRemoveSupplementPhoto('details')"
                  :file-list="financeSupplement.detailsFileList || (financeSupplement.detailsFileName ? [{uid: '-3', name: financeSupplement.detailsFileName, status: 'done', url: financeSupplement.detailsFileUrl}] : [])"
                >
                  <a-button>
                    <UploadOutlined /> {{ financeSupplement.detailsFileName ? '替换附件' : '上传附件' }}
                  </a-button>
                </a-upload>
                <span v-else style="color: #999">无附件</span>
              </a-card>
            </a-col>
          </a-row>

          <!-- 财务汇总信息 (只读，与财务补充弹窗样式一致) -->
          <a-row :gutter="16" style="margin-top: 16px;" v-if="!isFinanceUploadMode">
            <a-col :span="24">
              <a-card size="small">
                <template #title>
                  <span>财务汇总信息</span>
                  <a-tag v-if="parsedCalculationDetail" color="success" style="margin-left: 8px;">已生成明细</a-tag>
                  <a-tag v-else color="warning" style="margin-left: 8px;">待生成明细</a-tag>
                </template>
                <a-row :gutter="24">
                  <a-col :span="10">
                    <a-card title="退税参数" size="small">
                      <a-form layout="vertical">
                        <a-form-item label="退税点 (%)">
                          <a-input-number :value="financeSupplement.taxRefundRate" style="width: 100%" disabled />
                        </a-form-item>
                        <a-form-item label="货代发票金额">
                          <a-input-number :value="financeSupplement.freightAmount" style="width: 100%" disabled />
                        </a-form-item>
                        <a-form-item label="报关代理发票金额">
                          <a-input-number :value="financeSupplement.customsAmount" style="width: 100%" disabled />
                        </a-form-item>
                      </a-form>
                    </a-card>
                  </a-col>
                  <a-col :span="14">
                    <a-card title="开票明细计算" size="small">
                      <template v-if="parsedCalculationDetail">
                        <div class="calculation-box">
                          <!-- 收汇明细 -->
                          <div class="calc-section">
                            <div class="calc-title" style="color: #1890ff;">收汇明细</div>
                            <template v-if="parsedCalculationDetail.remittanceDetails && parsedCalculationDetail.remittanceDetails.length > 0">
                              <div v-for="(item, index) in parsedCalculationDetail.remittanceDetails" :key="'r'+index">
                                <div class="calc-row">
                                  <span class="calc-label">{{ item.remittanceName || '收汇' }}:</span>
                                  <span class="calc-value">
                                    {{ formatMoney(item.amount) }} {{ item.currency || 'USD' }}
                                    <span v-if="item.relationAmount && item.fullAmount && item.relationAmount !== item.fullAmount" style="color: #999; font-size: 11px;">(水单全额: {{ formatMoney(item.fullAmount) }})</span>
                                    × {{ item.taxRate }} = {{ formatMoney(item.cnyAmount) }} CNY
                                  </span>
                                </div>
                                <div class="calc-row" style="font-size: 12px; color: #666; margin-left: 20px;">
                                  <span>银行: {{ item.bankAccountName || '-' }} | 手续费率: {{ ((item.bankFeeRate || 0) * 100).toFixed(2) }}% | 手续费: {{ formatMoney(item.bankFee || 0) }} {{ item.currency || 'USD' }} = {{ formatMoney(item.bankFeeCny || 0) }} CNY</span>
                                </div>
                              </div>
                            </template>
                            <div class="calc-row total" style="margin-top: 8px;">
                              <span class="calc-label">收汇合计:</span>
                              <span class="calc-value highlight">{{ formatMoney(parsedCalculationDetail.totalGoodsAmount) }} CNY</span>
                              <span v-if="parsedCalculationDetail.weightedExchangeRate" style="margin-left: 12px; color: #999; font-size: 12px;">加权平均汇率: {{ parsedCalculationDetail.weightedExchangeRate }}</span>
                            </div>
                          </div>
                          
                          <a-divider />
                          
                          <!-- 总货物金额 -->
                          <div class="calc-section">
                            <div class="calc-row">
                              <span class="calc-label">总货物金额:</span>
                              <span class="calc-value highlight">{{ formatMoney(parsedCalculationDetail.totalGoodsAmount) }} CNY</span>
                            </div>
                          </div>
                          
                          <a-divider />
                          
                          <!-- 开票金额计算 -->
                          <div class="calc-section">
                            <div class="calc-title">开票金额计算</div>
                            <div class="calc-row">
                              <span class="calc-label">货款金额:</span>
                              <span class="calc-value">{{ formatMoney(parsedCalculationDetail.totalGoodsAmount) }} CNY</span>
                            </div>
                            <div class="calc-row">
                              <span class="calc-label">退税金额 ({{ formatMoney(parsedCalculationDetail.totalGoodsAmount) }} × {{ parsedCalculationDetail.taxRefundRate }}%):</span>
                              <span class="calc-value" style="color: #52c41a;">+{{ formatMoney(parsedCalculationDetail.amountWithTaxRefund - parsedCalculationDetail.totalGoodsAmount) }} CNY</span>
                            </div>
                            <div class="calc-row total-with-tax">
                              <span class="calc-label">含税总金额 (货款+退税):</span>
                              <span class="calc-value highlight">{{ formatMoney(parsedCalculationDetail.amountWithTaxRefund) }} CNY</span>
                            </div>
                            <div class="calc-row deduct">
                              <span class="calc-label">- 货代发票金额:</span>
                              <span class="calc-value">{{ formatMoney(parsedCalculationDetail.freightInvoiceAmount || 0) }} CNY</span>
                            </div>
                            <div class="calc-row deduct">
                              <span class="calc-label">- 海关代理发票金额:</span>
                              <span class="calc-value">{{ formatMoney(parsedCalculationDetail.customsInvoiceAmount || 0) }} CNY</span>
                            </div>
                            <div class="calc-row deduct">
                              <span class="calc-label">- 银行手续费合计:</span>
                              <span class="calc-value">{{ formatMoney(parsedCalculationDetail.bankFeeAmount) }} CNY <span style="font-size: 11px; color: #999;">（综合费率≈{{ parsedCalculationDetail.bankFeeRate }}%）</span></span>
                            </div>
                          </div>
                          
                          <a-divider />
                          
                          <div class="calc-section">
                            <div class="calc-row final">
                              <span class="calc-label">开票金额:</span>
                              <span class="calc-value final-value">{{ formatMoney(parsedCalculationDetail.invoiceAmount) }} CNY</span>
                            </div>
                            <div class="calc-row" v-if="financeSupplement.taxRefundAmount">
                              <span class="calc-label">退税金额:</span>
                              <span class="calc-value" style="color: #52c41a;">+{{ formatMoney(financeSupplement.taxRefundAmount) }} CNY</span>
                            </div>
                          </div>
                        </div>
                      </template>
                      <template v-else>
                        <div style="text-align: center; color: #999; padding: 40px 0;">
                          <p>请先在财务补充中设置退税参数，然后点击"生成开票明细"按钮计算开票金额</p>
                        </div>
                      </template>
                    </a-card>
                  </a-col>
                </a-row>
              </a-card>
            </a-col>
          </a-row>
        </a-form>
      </a-card>

    </a-card>
    <a-modal
  v-model:open="auditHistoryVisible"
  title="审核历史详情"
  width="1200px"
  :footer="null"
>
  <a-table
    :dataSource="auditHistoryList"
    :columns="auditHistoryColumns"
    :loading="auditHistoryLoading"
    rowKey="id"
    size="small"
    :scroll="{ x: 1100 }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'auditStatus'">
        <a-tag :color="record.auditStatus === 1 ? 'success' : record.auditStatus === 2 ? 'error' : 'processing'">
          {{ record.auditStatus === 1 ? '通过' : record.auditStatus === 2 ? '驳回' : '待审核' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'businessType'">
        <a-tag color="blue">{{ getBusinessTypeText(record.businessType) }}</a-tag>
      </template>
      <template v-else-if="column.key === 'preStatus'">
        <a-tag>{{ getStatusText(record.preStatus) }}</a-tag>
      </template>
    </template>
  </a-table>
</a-modal>

<!-- 业务发票上传弹窗 -->
<a-modal
  v-model:open="invoiceModalVisible"
  title="上传业务发票"
  @ok="handleInvoiceSubmit"
  width="600px"
>
  <a-form :model="invoiceForm" layout="vertical">
    <a-form-item label="发票名称">
      <a-input v-model:value="invoiceForm.invoiceName" />
    </a-form-item>
    <a-form-item label="发票号码" required>
      <a-input v-model:value="invoiceForm.invoiceNo" />
    </a-form-item>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="金额 (不含税)">
          <a-input-number v-model:value="invoiceForm.amount" style="width: 100%" :precision="2" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="税额">
          <a-input-number v-model:value="invoiceForm.taxAmount" style="width: 100%" :precision="2" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-form-item label="发票文件" required>
      <a-upload
        :before-upload="beforeInvoiceUpload"
        :file-list="invoiceFileList"
        :max-count="1"
        accept=".pdf,.jpg,.png"
      >
        <a-button><UploadOutlined /> 选择文件</a-button>
      </a-upload>
    </a-form-item>
  </a-form>
</a-modal>

<!-- 审核意见弹窗 -->
<a-modal
  v-model:open="remarkModalVisible"
  :title="`${remarkAction}审核`"
  width="500px"
  :confirm-loading="remarkSubmitting"
  @ok="handleRemarkSubmit"
>
  <a-form layout="vertical">
    <a-form-item label="审核意见" required>
      <a-textarea
        v-model:value="remarkValue"
        placeholder="请输入审核意见"
        :rows="4"
        :auto-size="{ minRows: 4, maxRows: 8 }"
      />
    </a-form-item>
  </a-form>
</a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import type { SelectValue } from 'ant-design-vue/lib/select';
import { PlusOutlined, UploadOutlined, DownloadOutlined, HistoryOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'

// Empty组件的simpleImage
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
import {
  getDeclarationDetail, 
  addDeclaration, 
  updateDeclaration, 
  uploadFile, 
  saveDraft, 
  deleteDeclaration,
  submitDeclaration,
  submitForAudit,
  auditDeclaration,
  getActiveTasks,
  getPickupAttachments,
  getFinancialSupplement,
  createFinancialSupplement,
  updateFinancialSupplement,
  saveDeliveryOrder,
  getDeliveryOrders,
  updateDeliveryOrder,
  deleteDeliveryOrder,
  auditReturnToDraft,
  getReturnAuditHistory,
  getBusinessInvoices,
  uploadBusinessInvoice,
  deleteBusinessInvoice
} from '@/api/business/declaration'
import { getProductTypes } from '@/api/system/product'
import { getEnabledTransportModes } from '@/api/system/transportMode'
import { getEnabledPaymentMethods } from '@/api/system/paymentMethod'
import { getEnabledCountries } from '@/api/system'
import { getEnabledCurrencies } from '@/api/system/currency'
import { getActiveMeasurementUnits, type MeasurementUnit } from '@/api/system/measurement-unit'
import {  findUnitByCode } from '@/utils/measurement-unit'

// 文件预览 URL 生成函数
const FILE_DOWNLOAD_URL = '/api/v1/files/download'
const getFilePreviewUrl = (id: number | string) => `${FILE_DOWNLOAD_URL}?id=${id}`

const route = useRoute()
const router = useRouter()

// 页面状态
const isAudit = ref(route.query.mode === 'audit')
const isFinanceUploadMode = ref(route.query.mode === 'financeUpload') // 财务补充模式
const isPaymentMode = ref(route.query.mode === 'payment') // 水单提交模式
const isPickupMode = ref(route.query.mode === 'pickup' || route.query.type === 'pickup') // 提货单模式
const isInvoiceUploadMode = ref(route.query.mode === 'invoiceUpload') // 发票上传模式
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const isDeliveryOrderEditable = computed(() => !isInvoiceUploadMode.value && isPickupMode.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(route.query.status ? Number(route.query.status) : null)
const submitting = ref(false)
const returnReason = ref('')
const auditHistoryVisible = ref(false)
const auditHistoryList = ref<any[]>([])
const auditHistoryLoading = ref(false)

// 审核意见相关
const remarkModalVisible = ref(false)
const remarkAction = ref('') // '通过' 或 '驳回'
const remarkValue = ref('')
const remarkSubmitting = ref(false)
let handleRemarkSubmit = () => {} // 占位函数，会被 showRemarkModal 动态替换
const auditHistoryColumns = [
  { title: '状态', key: 'auditStatus', width: 70 },
  { title: '业务类型', key: 'businessType', width: 120 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 90 },
  { title: '原因', dataIndex: 'applyReason', key: 'applyReason', ellipsis: true, minWidth: 150 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 },
  { title: '审核人', dataIndex: 'auditorName', key: 'auditorName', width: 90 },
  { title: '备注', dataIndex: 'auditRemark', key: 'auditRemark', ellipsis: true, minWidth: 150 },
  { title: '审核时间', dataIndex: 'auditTime', key: 'auditTime', width: 160 },
  { title: '原状态', key: 'preStatus', width: 70 }
]

// 活跃任务状态（用于任务驱动的 UI 判断）
const activeTasks = ref<any[]>([])

// 计量单位列表
const measurementUnits = ref<MeasurementUnit[]>([])

// 基本信息是否只读（审核模式、查看模式、水单提交模式、财务补充模式、提货单模式都只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value || isFinanceUploadMode.value || isPickupMode.value || isInvoiceUploadMode.value)

// 当前审核阶段（从 URL taskKey 中获取）

// 获取当前审核阶段文本
const getAuditActionText = () => {
  if (formStatus.value === 9) return '退回'
  return '审核'
}

// 获取业务类型文本
const getBusinessTypeText = (type: string) => {
  const map: Record<string, string> = {
    'DECLARATION_RETURN': '退回草稿',
    'DECLARATION_AUDIT': '申报审核',
    'REMittance_AUDIT': '水单审核',
    'DELIVERY_ORDER_AUDIT': '提货单审核',
    'DECLARATION_SUBMIT': '申报提交'
  }
  return map[type] || type
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已完成'
  }
  return statusMap[status] || '未知'
}

// 财务补充模式逻辑
const financeSupplement = ref<any>({
  id: null,
  freightAmount: null,
  freightInvoiceNo: '',
  freightFileName: '',
  freightFileUrl: '',
  customsAmount: null,
  customsInvoiceNo: '',
  customsFileName: '',
  customsFileUrl: '',
  taxRefundAmount: null,
  detailsAmount: null,
  detailsInvoiceNo: '',
  detailsFileName: '',
  detailsFileUrl: '',
  currency: 'CNY',
  // 银行账户相关字段
  foreignExchangeBank: '', // 外汇银行名称（对应下拉框选择的银行账户ID）
  bankFeeRate: null, // 银行手续费率
  taxRefundRate: null, // 退税点
  selectedBankAccount: null // 选中的银行账户对象
})

const submittingSupplement = ref(false)

// 申报要素弹窗相关变量
const elementsModalVisible = ref(false)
const currentProductForElements = ref<any>(null)
const currentElementValues = ref<any[]>([])
const elementsLoading = ref(false)

const loadFinancialSupplement = async () => {
  if (!formId.value) return
  try {
    console.log('加载财务补充记录, formId:', formId.value)
    
    // 先重置财务补充对象，避免旧数据残留
    financeSupplement.value = {
      id: null,
      freightAmount: null,
      freightInvoiceNo: '',
      freightFileName: '',
      freightFileUrl: '',
      customsAmount: null,
      customsInvoiceNo: '',
      customsFileName: '',
      customsFileUrl: '',
      taxRefundAmount: null,
      detailsAmount: null,
      detailsInvoiceNo: '',
      detailsFileName: '',
      detailsFileUrl: '',
      currency: 'CNY',
      foreignExchangeBank: '',
      bankFeeRate: null,
      taxRefundRate: null,
      selectedBankAccount: null
    }
    
    const res = await getFinancialSupplement(formId.value)
    console.log('财务补充记录响应:', res)
    if (res.data && res.data.code === 200 && res.data.data) {
      Object.assign(financeSupplement.value, res.data.data)
      console.log('财务补充数据加载成功:', financeSupplement.value)

      financeSupplement.value.freightFileList = financeSupplement.value.freightFileName ? [{uid: '-1', name: financeSupplement.value.freightFileName, status: 'done', url: financeSupplement.value.freightFileUrl}] : []
      financeSupplement.value.customsFileList = financeSupplement.value.customsFileName ? [{uid: '-2', name: financeSupplement.value.customsFileName, status: 'done', url: financeSupplement.value.customsFileUrl}] : []
      financeSupplement.value.detailsFileList = financeSupplement.value.detailsFileName ? [{uid: '-3', name: financeSupplement.value.detailsFileName, status: 'done', url: financeSupplement.value.detailsFileUrl}] : []
      financeSupplement.value.customsReceiptFileList = financeSupplement.value.customsReceiptFileName ? [{uid: '-4', name: financeSupplement.value.customsReceiptFileName, status: 'done', url: financeSupplement.value.customsReceiptFileUrl}] : []
    } else {
      console.log('未找到财务补充记录，使用默认空数据')
    }
  } catch (error) {
    console.error('获取财务补充记录失败', error)
    message.error('加载财务补充记录失败')
  }
}

const handleSaveFinanceSupplement = async () => {
  if (!formId.value) return
  
  submittingSupplement.value = true
  try {
    // 确保设置formId
    financeSupplement.value.formId = formId.value
    
    if (financeSupplement.value.id) {
      // 更新现有记录
      await updateFinancialSupplement(financeSupplement.value.id, financeSupplement.value)
      message.success('财务补充单证更新成功')
    } else {
      // 创建新记录
      console.log('创建新的财务补充记录:', financeSupplement.value)
      const res = await createFinancialSupplement(financeSupplement.value)
      if (res.data && res.data.code === 200 && res.data.data) {
        // 更新本地数据的ID
        financeSupplement.value.id = res.data.data.id
        message.success('财务补充单证创建成功')
      } else {
        throw new Error('创建失败')
      }
    }
  } catch (error) {
    console.error('财务补充保存失败:', error)
    message.error('保存失败: ' + (error as Error).message)
  } finally {
    submittingSupplement.value = false
  }
}

const beforeSupplementPhotoUpload = async (file: any, type: 'freight' | 'customs' | 'details' | 'customsReceipt') => {
  try {
    const res = await uploadFile(file, 'finance_supplement')
    if (res.data && res.data.code === 200) {
      const attachment = res.data.data
      const fileUrl = getFilePreviewUrl(attachment.id)
      
      if (type === 'freight') {
        financeSupplement.value.freightFileUrl = fileUrl
        financeSupplement.value.freightFileName = file.name
        financeSupplement.value.freightFileList = [{uid: String(attachment.id), name: file.name, status: 'done', url: fileUrl}]
      } else if (type === 'customs') {
        financeSupplement.value.customsFileUrl = fileUrl
        financeSupplement.value.customsFileName = file.name
        financeSupplement.value.customsFileList = [{uid: String(attachment.id), name: file.name, status: 'done', url: fileUrl}]
      } else if (type === 'customsReceipt') {
        financeSupplement.value.customsReceiptFileUrl = fileUrl
        financeSupplement.value.customsReceiptFileName = file.name
        financeSupplement.value.customsReceiptFileList = [{uid: String(attachment.id), name: file.name, status: 'done', url: fileUrl}]
      } else if (type === 'details') {
        financeSupplement.value.detailsFileUrl = fileUrl
        financeSupplement.value.detailsFileName = file.name
        financeSupplement.value.detailsFileList = [{uid: String(attachment.id), name: file.name, status: 'done', url: fileUrl}]
      }
      message.success('附件上传成功')
    }
  } catch (error) {
    message.error('附件上传失败')
  }
  return false
}

const handleRemoveSupplementPhoto = (type: 'freight' | 'customs' | 'details' | 'customsReceipt') => {
  if (type === 'freight') {
    financeSupplement.value.freightFileUrl = ''
    financeSupplement.value.freightFileName = ''
    financeSupplement.value.freightFileList = []
  } else if (type === 'customs') {
    financeSupplement.value.customsFileUrl = ''
    financeSupplement.value.customsFileName = ''
    financeSupplement.value.customsFileList = []
  } else if (type === 'customsReceipt') {
    financeSupplement.value.customsReceiptFileUrl = ''
    financeSupplement.value.customsReceiptFileName = ''
    financeSupplement.value.customsReceiptFileList = []
  } else if (type === 'details') {
    financeSupplement.value.detailsFileUrl = ''
    financeSupplement.value.detailsFileName = ''
    financeSupplement.value.detailsFileList = []
  }
}


// 提交审核（定金/尾款/提货单）
const handleSubmitAudit = async (type: 'deposit' | 'balance' | 'pickup') => {
  if (!formId.value) return
  submitting.value = true
  try {
    await submitForAudit(formId.value, type)
    
    let msg = '已提交审核'
    if (type === 'deposit') msg = '定金信息已提交审核'
    else if (type === 'balance') msg = '尾款信息已提交审核'
    else if (type === 'pickup') msg = '提货单已提交审核'
    
    message.success(msg)
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
  
  // 显示审核意见输入框
  const remark = await showRemarkModal('通过', '已核对数据，通过')
  if (!remark) return // 用户取消
  
  submitting.value = true
  try {
    if (formStatus.value === 9) {
      // 退回申请审核通过
      console.log('执行退回申请审核通过:', { formId: formId.value, remark })
      await auditReturnToDraft(formId.value, { approved: true, remark })
      message.success('退回审核已通过，单据已重置为草稿')
    } else {
      // 普通业务审核通过
      const taskKey = route.query.taskKey as string
      console.log('执行审核通过操作:', { formId: formId.value, taskKey, result: 1, remark })
      await auditDeclaration(formId.value, 1, remark, taskKey)
      message.success(`${getAuditActionText()}已通过`)
      if (formStatus.value === 1) {
        message.info('全套单证已自动生成')
      }
    }
    // 直接跳转到列表页，无需刷新当前页面数据
    goBack()
  } catch (error) {
    console.error('审核操作失败:', error)
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}

// 驳回
const handleReject = async () => {
  if (!formId.value) return
  
  // 显示审核意见输入框
  const remark = await showRemarkModal('驳回', '数据填写错误')
  if (!remark) return // 用户取消
  
  submitting.value = true
  try {
    if (formStatus.value === 9) {
      // 退回申请审核驳回
      console.log('执行退回申请审核驳回:', { formId: formId.value, remark })
      await auditReturnToDraft(formId.value, { approved: false, remark })
      message.success('退回审核已驳回，单据恢复原状态')
    } else {
      // 普通业务审核驳回
      const taskKey = route.query.taskKey as string
      console.log('执行驳回操作:', { formId: formId.value, taskKey, result: 2, remark })
      await auditDeclaration(formId.value, 2, remark, taskKey)
      message.success(`${getAuditActionText()}已驳回`)
    }
    // 直接跳转到列表页，无需刷新当前页面数据
    goBack()
  } catch (error) {
    console.error('驳回操作失败:', error)
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}


const remittanceList = ref<any[]>([])

// 从水单列表汇总金额（用于财务补充）- 使用关联到申报单的金额
const totalRemittanceAmount = computed(() => {
  return Number(remittanceList.value.reduce((sum, r) => {
    // 优先使用关联金额，如果没有则使用水单全额
    const amount = r.totalRelatedAmount || r.remittanceAmount || 0
    return sum + amount
  }, 0).toFixed(2))
})

// 从水单列表汇总手续费
const totalBankFeeAmount = computed(() => {
  return Number(remittanceList.value.reduce((sum, r) => sum + (r.bankFee || 0), 0).toFixed(2))
})

// 净收汇金额 = 收汇总计 - 手续费合计
const netRemittanceAmount = computed(() => {
  return Number((totalRemittanceAmount.value - totalBankFeeAmount.value).toFixed(2))
})

import { watch } from 'vue'

const computedDetailsAmount = computed(() => {
  // 使用从水单获取的数据
  const receipt = totalRemittanceAmount.value
  const taxRate = financeSupplement.value.taxRefundRate || 0
  const freight = financeSupplement.value.freightAmount || 0
  const customs = financeSupplement.value.customsAmount || 0
  const bankFee = totalBankFeeAmount.value

  // 开票金额 = 总金额 * (1 + 退税点%) - 货代 - 报关 - 手续费
  return Number((receipt * (1 + taxRate / 100) - freight - customs - bankFee).toFixed(2))
})

watch(computedDetailsAmount, (newVal) => {
  financeSupplement.value.detailsAmount = newVal
}, { immediate: true })

import { exportFinanceCalculation } from '@/api/business/declaration'
import { getCitiesByCountry } from '@/api/system/city-info'

const generatingDetails = ref(false)
const handleGenerateFinanceDetails = async () => {
  if (!formId.value) {
    message.error('申报单ID不存在')
    return
  }

  // 验证必填字段
  if (!financeSupplement.value.taxRefundRate && financeSupplement.value.taxRefundRate !== 0) {
    message.warning('请先设置退税点')
    return
  }

  generatingDetails.value = true
  try {
    // 第一步：保存财务补充信息到数据库
    message.loading({ content: '正在保存财务补充信息...', key: 'saveFinance', duration: 0 })
    
    // 构建完整的保存数据
    const saveData = {
      formId: formId.value,
      freightAmount: financeSupplement.value.freightAmount,
      freightInvoiceNo: financeSupplement.value.freightInvoiceNo,
      freightFileName: financeSupplement.value.freightFileName,
      freightFileUrl: financeSupplement.value.freightFileUrl,
      customsAmount: financeSupplement.value.customsAmount,
      customsInvoiceNo: financeSupplement.value.customsInvoiceNo,
      customsFileName: financeSupplement.value.customsFileName,
      customsFileUrl: financeSupplement.value.customsFileUrl,
      customsReceiptFileName: financeSupplement.value.customsReceiptFileName,
      customsReceiptFileUrl: financeSupplement.value.customsReceiptFileUrl,
      detailsFileName: financeSupplement.value.detailsFileName,
      detailsFileUrl: financeSupplement.value.detailsFileUrl,
      taxRefundRate: financeSupplement.value.taxRefundRate,
      currency: financeSupplement.value.currency,
      foreignExchangeBank: financeSupplement.value.foreignExchangeBank,
      bankFeeRate: financeSupplement.value.bankFeeRate
    }

    console.log('保存财务补充数据:', saveData)

    if (financeSupplement.value.id) {
      // 更新现有记录
      await updateFinancialSupplement(financeSupplement.value.id, saveData)
      message.success({ content: '财务补充信息保存成功', key: 'saveFinance', duration: 2 })
    } else {
      // 创建新记录
      const res = await createFinancialSupplement(saveData)
      if (res.data && res.data.code === 200 && res.data.data) {
        financeSupplement.value.id = res.data.data.id
        message.success({ content: '财务补充信息创建成功', key: 'saveFinance', duration: 2 })
      } else {
        throw new Error('创建失败')
      }
    }

    // 第二步：生成并下载Excel
    message.loading({ content: '正在生成开票明细...', key: 'generate', duration: 0 })
    
    const res = await exportFinanceCalculation(formId.value) as any
    
    // 后端返回的是下载URL，直接使用URL下载
    const downloadUrl = res.data?.data || res.data
    if (downloadUrl) {
      // 使用隐藏的a标签触发下载
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `开票明细单_${formId.value}_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      message.success({ content: '开票明细生成成功，正在下载...', key: 'generate', duration: 3 })
    } else {
      throw new Error('未获取到下载链接')
    }

    // 第三步：重新加载财务补充数据（更新calculationDetail）
    await loadFinancialSupplement()
    
  } catch (err: any) {
    console.error('生成明细失败:', err)
    message.error({ content: '生成明细失败: ' + (err.message || '未知错误'), key: 'generate', duration: 3 })
  } finally {
    generatingDetails.value = false
  }
}

// 格式化金额显示
const formatMoney = (amount: number | undefined) => {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toFixed(2)
}

// 解析计算详情JSON
const parsedCalculationDetail = computed(() => {
  if (!financeSupplement.value.calculationDetail) return null
  try {
    return JSON.parse(financeSupplement.value.calculationDetail)
  } catch (e) {
    console.error('解析计算详情JSON失败:', e)
    return null
  }
})

// // 计算退税金额
// const getTaxRefundAmount = () => {
//   if (!parsedCalculationDetail.value) return 0
//   const goodsAmount = parsedCalculationDetail.value.totalGoodsAmount || 0
//   const taxRate = parsedCalculationDetail.value.taxRefundRate || 0
//   return goodsAmount * (taxRate / 100)
// }


// 提货单附件列表
const pickupAttachments = ref<any[]>([])

// ========== 提货单相关 ==========
const deliveryOrderList = ref<any[]>([])
const loadingDeliveryOrders = ref(false)
const deliveryOrderModalVisible = ref(false)
const editingDeliveryOrder = ref<any>(null)
const savingDeliveryOrder = ref(false)

const deliveryOrderForm = reactive({
  deliveryDate: undefined as Dayjs | undefined,
  remark: '',
  fileUrl: '',
  fileName: '',
  fileList: [] as any[]
})

// 提货单列表表格列配置
const deliveryOrderColumns = [
  { title: '提货日期', key: 'deliveryDate', width: 120 },
  { title: '备注说明', dataIndex: 'remark', key: 'remark' },
  { title: '附件', key: 'fileName', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 180 }
]

// 获取提货单状态颜色
const getDeliveryOrderStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'orange',    // 待审核
    1: 'green',     // 已通过
    2: 'red'        // 已驳回
  }
  return colorMap[status] || 'default'
}

// 获取提货单状态文本
const getDeliveryOrderStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已驳回'
  }
  return textMap[status] || '未知'
}

// 加载提货单列表
const loadDeliveryOrders = async () => {
  if (!formId.value) return
  loadingDeliveryOrders.value = true
  try {
    const res = await getDeliveryOrders(formId.value)
    if (res.data && res.data.code === 200) {
      // 增加数组安全性检查
      deliveryOrderList.value = Array.isArray(res.data.data) ? res.data.data : []
    }
  } catch (error) {
    console.warn('加载提货单列表失败:', error)
    deliveryOrderList.value = []
  } finally {
    loadingDeliveryOrders.value = false
  }
}

// 显示新增/编辑提货单弹窗
const showDeliveryOrderModal = (record?: any) => {
  if (record) {
    editingDeliveryOrder.value = record
    deliveryOrderForm.deliveryDate = record.deliveryDate ? dayjs(record.deliveryDate) : undefined
    deliveryOrderForm.remark = record.remark || ''
    deliveryOrderForm.fileUrl = record.fileUrl || ''
    deliveryOrderForm.fileName = record.fileName || ''
    deliveryOrderForm.fileList = record.fileUrl ? [{
      uid: '-1',
      name: record.fileName || '附件',
      status: 'done',
      url: record.fileUrl
    }] : []
  } else {
    editingDeliveryOrder.value = null
    deliveryOrderForm.deliveryDate = dayjs()
    deliveryOrderForm.remark = ''
    deliveryOrderForm.fileUrl = ''
    deliveryOrderForm.fileName = ''
    deliveryOrderForm.fileList = []
  }
  deliveryOrderModalVisible.value = true
}

// 提货单文件上传前处理
const beforeDeliveryOrderUpload = async (file: any) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过10MB!')
    return false
  }
  
  try {
    const res = await uploadFile(file, 'DeliveryOrder')
    if (res.data && res.data.code === 200) {
      const attachment = res.data.data
      deliveryOrderForm.fileUrl = attachment.fileUrl || getFilePreviewUrl(attachment.id)
      deliveryOrderForm.fileName = file.name
      deliveryOrderForm.fileList = [{
        uid: String(attachment.id),
        name: file.name,
        status: 'done',
        url: deliveryOrderForm.fileUrl
      }]
      message.success('文件上传成功')
    } else {
      message.error('文件上传失败')
    }
  } catch (error) {
    message.error('文件上传失败')
  }
  return false
}

// 移除提货单文件
const handleRemoveDeliveryOrderFile = () => {
  deliveryOrderForm.fileUrl = ''
  deliveryOrderForm.fileName = ''
  deliveryOrderForm.fileList = []
}

// 保存提货单
const handleSaveDeliveryOrder = async () => {
  if (!formId.value) return
  if (!deliveryOrderForm.deliveryDate) {
    message.warning('请选择提货日期')
    return
  }
  
  savingDeliveryOrder.value = true
  try {
    const data = {
      deliveryDate: deliveryOrderForm.deliveryDate.format('YYYY-MM-DD'),
      remark: deliveryOrderForm.remark,
      fileUrl: deliveryOrderForm.fileUrl,
      fileName: deliveryOrderForm.fileName
    }
    
    if (editingDeliveryOrder.value && editingDeliveryOrder.value.id) {
      // 更新 - 确保 id 存在
      await updateDeliveryOrder(editingDeliveryOrder.value.id, data)
      message.success('提货单更新成功')
    } else {
      // 新增
      await saveDeliveryOrder(formId.value, data)
      message.success('提货单添加成功')
    }
    
    deliveryOrderModalVisible.value = false
    await loadDeliveryOrders()
  } catch (error) {
    message.error('保存提货单失败')
  } finally {
    savingDeliveryOrder.value = false
  }
}

// 编辑提货单
const handleEditDeliveryOrder = (record: any) => {
  showDeliveryOrderModal(record)
}

// 下载提货单
const handleDownloadDeliveryOrder = (record: any) => {
  if (record.fileUrl) {
    window.open(record.fileUrl, '_blank')
  }
}

// 删除提货单
const handleDeleteDeliveryOrder = async (record: any) => {
  try {
    await deleteDeliveryOrder(record.id)
    message.success('提货单删除成功')
    await loadDeliveryOrders()
  } catch (error) {
    message.error('删除提货单失败')
  }
}

// ========================================
// 业务发票相关逻辑
// ========================================

const loadBusinessInvoices = async () => {
  if (!formId.value) return
  try {
    const res = await getBusinessInvoices(formId.value)
    if (res.data && res.data.code === 200) {
      businessInvoiceList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载业务发票失败:', error)
  }
}

const showInvoiceModal = () => {
  invoiceModalVisible.value = true
  invoiceForm.invoiceType = 1
  invoiceForm.invoiceName = ''
  invoiceForm.invoiceNo = ''
  invoiceForm.amount = undefined
  invoiceForm.taxAmount = undefined
  invoiceFileList.value = []
  tempInvoiceFile.value = null
}

// 金额手动输入后，标记为用户修改
const handleAmountChange = (record: any) => {
  record.amountUserModified = true
}

// 数量或单价变化时，如果金额未锁定（未被用户手动修改过），自动计算金额
const handleQuantityOrPriceChange = (record: any) => {
  // 如果金额已被用户手动修改过（无论保存与否），不再自动计算
  if (!record.amountUserModified && !record.amountLocked) {
    const quantity = record.quantity || 0
    const unitPrice = record.unitPrice || 0
    record.amount = quantity * unitPrice
  }
}

// 获取发票文件URL
const getInvoiceFileUrl = (fileUrl: string) => {
  // fileUrl 已经是完整的下载链接，直接返回
  return fileUrl || ''
}

const beforeInvoiceUpload = (file: any) => {
  tempInvoiceFile.value = file
  invoiceFileList.value = [{ uid: '-1', name: file.name, status: 'done' }]
  return false
}

const handleInvoiceSubmit = async () => {
  if (!tempInvoiceFile.value) return message.warning('请选择发票文件')
  if (!invoiceForm.invoiceNo) return message.warning('请填写发票号码')

  const formData = new FormData()
  formData.append('file', tempInvoiceFile.value)
  formData.append('invoiceType', '1') // 业务发票默认进项
  formData.append('invoiceName', invoiceForm.invoiceName)
  formData.append('invoiceNo', invoiceForm.invoiceNo)
  if (invoiceForm.amount) formData.append('amount', String(invoiceForm.amount))
  if (invoiceForm.taxAmount) formData.append('taxAmount', String(invoiceForm.taxAmount))

  try {
    if (!formId.value) return message.warning('申报单ID不存在')
    await uploadBusinessInvoice(formId.value, formData)
    message.success('发票上传成功')
    invoiceModalVisible.value = false
    loadBusinessInvoices()
  } catch (error) {
    message.error('发票上传失败')
  }
}

const handleDeleteInvoice = async (id: number) => {
  try {
    await deleteBusinessInvoice(id)
    message.success('发票删除成功')
    loadBusinessInvoices()
  } catch (error) {
    message.error('发票删除失败')
  }
}

// 财务补充-水单汇总表格列
const remittanceSummaryColumns = [
  { title: '水单名称', key: 'remittanceName', width: 120 },
  { title: '银行账户', key: 'bankAccount', width: 160 },
  { title: '水单金额', key: 'amount', width: 110 },
  { title: '申报单关联金额', key: 'relationAmount', width: 130 },
  { title: '手续费率', key: 'bankFeeRate', width: 100 },
  { title: '手续费', key: 'bankFee', width: 100 },
  { title: '水单日期', key: 'date', width: 110 },
  { title: '状态', key: 'status', width: 90 }
]

// 获取水单状态颜色
const getStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'blue',
    2: 'green',
    3: 'red'
  }
  return colorMap[status] || 'default'
}

// 获取水单状态文本
const getRemittanceStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已审核',
    3: '已驳回'
  }
  return textMap[status] || '未知'
}


const formData = reactive({
  formNo: '',
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: '',
  transportMode: undefined as string | undefined,
  paymentMethod: undefined as string | undefined,
  departureCity: '',
  departureCityChinese: '',
  departureCityEnglish: '',
  destinationCountry: undefined as string | undefined,
  tradeCountry: undefined as string | undefined,
  currency: 'USD',
  declarationDate: undefined as Dayjs | undefined,
  orgId: undefined as number | undefined
})

// 产品列表
const productList = ref<any[]>([])

// 箱子列表
const cartonList = ref<any[]>([])

// 图片上传限制
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png']

// HS编码选项
const hsOptions = ref<any[]>([])

// 支付方式选项
const paymentMethodOptions = ref<any[]>([])

// 运输方式选项
const transportModeOptions = ref<any[]>([])

// 国家选项
const countryOptions = ref<any[]>([])

// // 国家自动完成选项
// const countryAutoCompleteOptions = computed(() => {
//   return countryOptions.value.map(option => ({
//     label: option.label,
//     value: option.value
//   }))
// })

// // 包含自定义选项的国家选项
// const countryOptionsWithCustom = computed(() => {
//   // 如果当前值不在标准选项中，添加为自定义选项
//   if (formData.destinationCountry && !countryOptions.value.some(opt => opt.value === formData.destinationCountry)) {
//     return [
//       {
//         label: formData.destinationCountry,
//         value: formData.destinationCountry,
//         isCustom: true
//       },
//       ...countryOptions.value
//     ];
//   }
//   return countryOptions.value;
// });

// 国家选择过滤函数
const filterCountrySelectOption = (input: string, option: any) => {
  if (!input) return true;
  const lowerInput = input.toLowerCase();
  return (
    (option.label && option.label.toLowerCase().includes(lowerInput)) ||
    (option.value && option.value.toLowerCase().includes(lowerInput))
  );
};


// 货币选项
// 城市选项
const cityOptions = ref<any[]>([])

// 加载城市信息
const loadCities = async (countryName?: string) => {
  if (!countryName) {
    // 如果没有指定国家，默认加载中国城市
    countryName = '中国'
  }
  
  try {
    const response = await getCitiesByCountry(countryName)
    if (response.data && response.data.code === 200) {
      cityOptions.value = response.data.data.map((city: any) => ({
        label: `${city.cityChineseName || city.cityName} (${city.cityEnglishName}), ${city.countryEnglishName}`,
        value: city.cityName || city.cityChineseName, // 使用中文城市名作为值
        cityChineseName: city.cityName || city.cityChineseName,
        cityEnglishName: city.cityEnglishName,
        countryName: city.countryName
      }))
      console.log('城市信息加载成功:', cityOptions.value.length + '个城市')
    } else {
      console.warn('城市信息加载失败:', response.data?.message || '未知错误')
      cityOptions.value = []
    }
  } catch (error) {
    console.error('加载城市信息失败:', error)
    cityOptions.value = []
  }
}

const currencyOptions = ref<any[]>([])

// 加载运输方式选项
const loadTransportModes = async () => {
  try {
    const response = await getEnabledTransportModes()
    if (response.data.code === 200 && response.data.data.length > 0) {
      transportModeOptions.value = response.data.data.map((item: any) => ({
        label: item.chineseName || item.name,
        value: item.name || item.name
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

// 加载支付方式选项
const loadPaymentMethods = async () => {
  try {
    const response = await getEnabledPaymentMethods()
    if (response.data.code === 200 && response.data.data.length > 0) {
      paymentMethodOptions.value = response.data.data.map((item: any) => ({
        label: item.code ? `${item.code} (${item.chineseName || item.name})` : (item.chineseName || item.name),
        value: item.code || item.name
      }))
      console.log('加载支付方式成功:', paymentMethodOptions.value)
    }
  } catch (error) {
    console.warn('加载支付方式失败:', error)
    // 使用默认支付方式
    paymentMethodOptions.value = [
      { label: 'T/T (电汇)', value: 'T/T' },
      { label: 'L/C (信用证)', value: 'L/C' },
      { label: 'D/P (付款交单)', value: 'D/P' },
      { label: 'D/A (承兑交单)', value: 'D/A' }
    ]
  }
}

// 加载货币选项
const loadCurrencies = async () => {
  try {
    const response = await getEnabledCurrencies()
    if (response.data.code === 200 && response.data.data.length > 0) {
      currencyOptions.value = response.data.data.map((item: any) => ({
        label: `${item.currencyCode} - ${item.chineseName || item.currencyName}`,
        value: item.currencyCode
      }))
      console.log('加载货币数据成功:', currencyOptions.value)
    } else {
      // 如果API失败，使用默认数据
      currencyOptions.value = [
        { label: 'USD - 美元', value: 'USD' },
        { label: 'EUR - 欧元', value: 'EUR' },
        { label: 'CNY - 人民币', value: 'CNY' }
      ]
    }
  } catch (error) {
    console.warn('加载货币数据失败:', error)
    // 使用默认数据作为后备
    currencyOptions.value = [
      { label: 'USD - 美元', value: 'USD' },
      { label: 'EUR - 欧元', value: 'EUR' },
      { label: 'CNY - 人民币', value: 'CNY' }
    ]
  }
}

// 加载国家选项
const loadCountries = async () => {
  try {
    const response = await getEnabledCountries()
    if (response.data.code === 200 && response.data.data.length > 0) {
      countryOptions.value = response.data.data.map((item: any) => ({
        label: `${item.chineseName} / ${item.englishName}`,  // 显示中英文名称
        value: item.countryCode,   // 使用国家代码作为值
        englishName: item.englishName,  // 保存英文全名用于提交时转换
        chineseName: item.chineseName   // 保存中文名称
      }))
      console.log('加载国家数据成功:', countryOptions.value)
    } else {
      // 如果 API 失败，使用默认数据
      countryOptions.value = [
        { label: 'China', value: 'CHN', englishName: 'China' },
        { label: 'United States', value: 'USA', englishName: 'United States' },
        { label: 'United Kingdom', value: 'GBR', englishName: 'United Kingdom' },
        { label: 'Germany', value: 'DEU', englishName: 'Germany' },
        { label: 'France', value: 'FRA', englishName: 'France' },
        { label: 'Japan', value: 'JPN', englishName: 'Japan' },
        { label: 'South Korea', value: 'KOR', englishName: 'South Korea' }
      ]
    }
  } catch (error) {
    console.error('加载国家数据失败:', error)
    // 使用默认数据作为后备
  }
}

// 加载计量单位列表
const loadMeasurementUnits = async () => {
  try {
    const response = await getActiveMeasurementUnits()
    if (response.data && response.data.code === 200) {
      measurementUnits.value = response.data.data || []
      console.log('加载计量单位成功:', measurementUnits.value.length + ' 个单位')
    }
  } catch (error) {
    console.error('加载计量单位失败:', error)
  }
}


// // 监听国家变化，动态加载对应的城市
// watch(() => formData.destinationCountry, async (newCountryCode) => {
//   if (newCountryCode && newCountryCode.trim() !== '') {
//     // 尝试找到国家的英文名称
//     const selectedCountry = countryOptions.value.find(country => country.value === newCountryCode);
//     if (selectedCountry) {
//       await loadCities(selectedCountry.englishName || selectedCountry.chineseName);
//     } else {
//       // 如果找不到对应的国家选项，尝试直接使用国家代码
//       await loadCities(newCountryCode);
//     }
//   } else {
//     // 清空城市选项
//     cityOptions.value = [];
//   }
// });

// // 国家输入变化处理函数
// const handleCountryInputChange = (value: string) => {
//   // 如果输入的值匹配某个国家的中文名或英文名，自动选择对应的国家
//   if (value) {
//     const matchedCountry = countryOptions.value.find(option => 
//       option.chineseName.toLowerCase().includes(value.toLowerCase()) ||
//       option.englishName.toLowerCase().includes(value.toLowerCase()) ||
//       option.label.toLowerCase().includes(value.toLowerCase())
//     );
    
//     if (matchedCountry) {
//       formData.destinationCountry = matchedCountry.value;
//     } else {
//       // 如果没有匹配项，可以考虑让用户输入自定义值
//       // 但在这里我们只接受有效的国家代码
//       // formData.destinationCountry = value; // 不推荐直接赋值用户输入
//     }
//   }
// };

// 根据国家代码获取英文全名
const getCountryEnglishName = (countryCode: string): string => {
  const country = countryOptions.value.find(item => item.value === countryCode);
  return country ? country.englishName : countryCode;
}

// 根据国家代码获取中文全名
// const getCountryChineseName = (countryCode: string): string => {
//   const country = countryOptions.value.find(item => item.value === countryCode);
//   return country ? country.chineseName : countryCode;
// }

// 加载HS商品类型数据
const loadProductTypes = async () => {
  try {
    const response = await getProductTypes()
    console.log('加载HS商品类型数据:', response.data)
    if (response.data.code === 200 && response.data.data.length > 0) {
      hsOptions.value = response.data.data.map((item: any) => ({
        label: `${item.hsCode}`,
        value: item.hsCode,
        chineseName: item.chineseName,
        englishName: item.englishName
      }))
      console.log('加载HS商品类型成功:', hsOptions.value)
    } else {
      // 如果API失败，使用默认数据
      hsOptions.value = [
      ]
    }
  } catch (error) {
    console.warn('加载HS商品类型失败，使用默认数据:', error)
    // 使用默认数据
    hsOptions.value = [
    ]
  }
}

// 产品选项
const productOptions = computed(() => {
  return productList.value.map((item, index) => ({
    label: `${index + 1}. ${item.productName} (HS: ${item.hsCode || '未设置'})`,
    value: item.id
  }))
})

// 产品自动完成选项
const productAutoCompleteOptions = computed(() => {
  // 从HS选项中提取产品信息，用于自动完成功能
  const options: { label: string; value: string }[] = [];
  
  hsOptions.value.forEach(option => {
    // 如果有中文和英文名称，分别添加选项
    if (option.chineseName && option.englishName) {
      options.push(
        { label: `${option.chineseName}`, value: option.chineseName },
        { label: `${option.englishName}`, value: option.englishName },
        { label: `${option.chineseName} / ${option.englishName}`, value: `${option.chineseName} / ${option.englishName}` }
      );
    } else if (option.englishName) {
      options.push({ label: `${option.englishName}`, value: option.englishName });
    } else {
      // 备选方案：从label中解析
      const labelParts = option.label.split(' - ');
      if (labelParts.length >= 2) {
        const namePart = labelParts.slice(1).join(' - ');
        options.push({ label: namePart, value: namePart });
      }
    }
  });
  
  return options;
});

// 包含自定义选项的产品自动完成选项
const productAutoCompleteOptionsWithCustom = computed(() => {
  const baseOptions = [...productAutoCompleteOptions.value];
  
  // 如果当前产品名称不在基础选项中，添加为自定义选项
  if (productList.value) {
    productList.value.forEach(product => {
      if (product.productName && 
          !baseOptions.some(opt => opt.value === product.productName)) {
        baseOptions.unshift({
          label: product.productName,
          value: product.productName,
        });
      }
    });
  }
  
  return baseOptions;
});

// 产品选项过滤函数
const filterProductOption = (input: string, option: any) => {
  if (!input) return true;
  const lowerInput = input.toLowerCase();
  return (
    (option.label && option.label.toLowerCase().includes(lowerInput)) ||
    (option.value && option.value.toLowerCase().includes(lowerInput))
  );
};

// 产品表格列配置
const productColumns = [
  { title: '产品中文名', dataIndex: 'productChineseName', key: 'productChineseName', width: 120 },
  { title: '产品英文名', dataIndex: 'productEnglishName', key: 'productEnglishName', width: 120 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 80 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 100 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 100 },
  { title: '关联箱号', key: 'cartonInfo', width: 120 },
  { title: '金额', key: 'amount', width: 100 },
  { title: '产品照片', key: 'productPhoto', width: 100 },
  { title: '申报要素', key: 'declarationElements', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

// 箱子表格列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 120 },
  { title: '类型', dataIndex: 'typeChinese', key: 'typeChinese', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '总体积(CBM)', dataIndex: 'volume', key: 'volume', width: 150 },
  { title: '产品选择', key: 'selectedProducts', width: 200 },
  { title: '操作', key: 'action', width: 80 }
]

// 业务发票表格列配置
const businessInvoiceColumns = [
  { title: '发票名称', dataIndex: 'invoiceName', key: 'invoiceName', width: 150 },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 150 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '附件', key: 'fileName', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

// 业务发票数据
const businessInvoiceList = ref<any[]>([])

// 发票上传相关
const invoiceModalVisible = ref(false)
const invoiceFileList = ref<any[]>([])
const tempInvoiceFile = ref<any>(null)
const invoiceForm = reactive({
  invoiceType: 1,
  invoiceName: '',
  invoiceNo: '',
  amount: undefined as number | undefined,
  taxAmount: undefined as number | undefined
})

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
    totalAmount += parseFloat(item.amount) || 0
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

// 单价和金额均由用户手动填写，不自动计算

// 处理单位变更
const handleUnitChange = (record: any) => {
  console.log('单位变更:', record.unitCode, record);
  // 根据 unitCode 查找对应的单位名称，同时设置 unit 和 unitCode
  const selectedUnit = findUnitByCode(measurementUnits.value, record.unitCode);
  if (selectedUnit) {
    record.unit = selectedUnit.unitNameEn;  // 设置英文名称
    // record.unitCode 已经由 v-model 自动设置了
  }
};

// 处理金额变更


// HS 编码变更处理
const onHsCodeChange = async (index: number, value: string | number) => {
  const stringValue = typeof value === 'string' ? value : String(value)
  const option = hsOptions.value.find(opt => opt.value === stringValue)
  
  if (option) {
    // 设置产品名称（英文名）
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
        // 设置HS产品中文名
        if (productType.chineseName) {
          console.log('设置产品中文名:', productType.chineseName)
          productList.value[index].productChineseName = productType.chineseName
        } else {
          // 从选项标签中提取中文名（如果有）
          const chineseMatch = option.label.match(/^([^\s-]+.*?)\s*-\s*(.+)$/)
          if (chineseMatch && chineseMatch[1]) {
            productList.value[index].productChineseName = chineseMatch[1].trim()
          }
        }
        if (productType.englishName) {
          console.log('设置产品英文名:', productType.englishName)
          productList.value[index].productEnglishName = productType.englishName
        } else {
          // 从选项标签中提取英文名（如果有）
          const englishMatch = option.label.match(/-?\s*([^\s-]+?)\s*$/)
          if (englishMatch && englishMatch[1]) {
            productList.value[index].productEnglishName = englishMatch[1].trim()
          }
        }
        
        // 更新产品名称
        updateProductName(productList.value[index]);
        
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
          // 初始化申报要素，空值自动填充为"无"
          productList.value[index].declarationElements = elements.map((element: any) => ({
            ...element,
            value: element.defaultValue || element.value || '无'
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

// // 获取产品名称(通过ID)
// const getProductNameById = (id: number) => {
//   const product = productList.value.find(p => p.id === id)
//   return product ? product.productName : '未知产品'
// }

// 获取产品显示名称(带序号,用于查看模式)
const getProductDisplayById = (id: number) => {
  const index = productList.value.findIndex(p => p.id === id)
  const product = productList.value.find(p => p.id === id)
  if (!product) return '未知产品'
  const productIndex = index >= 0 ? index + 1 : '?'
  return `${productIndex}. ${product.productName}`
}

// 显示申报要素弹窗
const showElementsModal = (record: any) => {
  currentProductForElements.value = record
  // 使用产品的declarationElements，但创建一个副本以避免直接修改原始数据
  currentElementValues.value = (record.declarationElements || []).map((elem: any) => ({
    ...elem,
    value: elem.value || ''
  }))
  elementsLoading.value = false
  elementsModalVisible.value = true
}

// 处理申报要素弹窗确认
const handleElementsModalConfirm = () => {
  if (currentProductForElements.value && currentElementValues.value) {
    // 将修改后的值保存回产品，空值自动填充为"无"
    currentProductForElements.value.declarationElements = currentElementValues.value.map((elem: any) => ({
      ...elem,
      value: elem.value && elem.value.trim() ? elem.value : '无'
    }))
    message.success('申报要素已更新')
  }
  elementsModalVisible.value = false
}

// 获取产品关联的箱子信息
const getProductCartonInfo = (product: any) => {
  // 遍历所有箱子，找到关联了该产品的箱子
  return cartonList.value.filter(carton => 
    carton.selectedProducts && carton.selectedProducts.includes(product.id)
  )
}

// 产品照片上传前验证
const beforeProductPhotoUpload = async (file: any, productIndex: number) => {
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
  
  try {
    const res = await uploadFile(file, 'ProductPhoto')
    if (res.data && res.data.code === 200) {
      const attachment = res.data.data
      productList.value[productIndex].imageId = attachment.id
      productList.value[productIndex].productPhoto = attachment.fileUrl
      // 设置单个文件对象
      productList.value[productIndex].photoFile = {
        uid: '-1',
        name: file.name,
        status: 'done',
        url: attachment.fileUrl
      }
      message.success('产品图片上传成功')
    }
  } catch (error) {
    message.error('产品图片上传失败')
  }
  
  // 返回false阻止自动上传 (因为我们已经手动上传了)
  return false
}

// 移除产品照片
const handleRemoveProductPhoto = (productIndex: number) => {
  productList.value[productIndex].productPhoto = ''
  productList.value[productIndex].imageId = null
  productList.value[productIndex].photoFile = null
}

// 添加产品
const addProduct = () => {
  const newId = productList.value.length > 0 ? Math.max(...productList.value.map(p => p.id)) + 1 : 1
  productList.value.push({
    id: newId,
    productName: '',
    productChineseName: '',
    productEnglishName: '',
    hsCode: '',
    quantity: 1,
    unitCode: '01',  // 默认单位代码（个）
    unitPrice: 0,
    amount: 0, // 金额由用户手动填写
    amountLocked: 0, // 0-未锁定, 1-锁定
    amountUserModified: false, // 前端标记：用户是否手动修改过
    grossWeight: 0,
    netWeight: 0,
    cartons: 1,    // 默认 1 箱
    volume: 0,     // 默认 0
    imageId: null, // 产品图片 ID
    productPhoto: '', // 产品照片 URL
    photoFile: null, // 上传文件对象
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
    typeChinese: '纸箱', // 默认类型
    typeEnglish: 'CARTRONS', // 默认类型
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

// 更新产品名称，将其设置为中文名和英文名的组合
const updateProductName = (record: any) => {
  const chineseName = record.productChineseName || '';
  const englishName = record.productEnglishName || '';
  
  if (chineseName && englishName) {
    record.productName = `${chineseName} / ${englishName}`;
  } else if (chineseName) {
    record.productName = chineseName;
  } else if (englishName) {
    record.productName = englishName;
  }
}

// 出发城市选择变化
const onDepartureCityChange = (value: SelectValue) => {
  // 查找对应的城市信息并更新中文名和英文名字段
  if (value !== undefined && value !== null && typeof value === 'string') { // 确保值存在且为字符串类型再处理
    const selectedCity = cityOptions.value.find((city: any) => city.value === value);
    if (selectedCity) {
      formData.departureCity = value; // 更新英文格式的城市名称
      formData.departureCityChinese = selectedCity.cityChineseName;
      formData.departureCityEnglish = selectedCity.cityEnglishName;
    }
  } else {
    // 当选择被清空时，重置相关字段
    formData.departureCity = '';
    formData.departureCityChinese = '';
    formData.departureCityEnglish = '';
  }
}

// // 箱子类型选择变化
// const onCartonTypeChange = (record: any, value: string) => {
//   // 根据中文类型设置英文类型
//   if (value === '箱子') {
//     record.typeEnglish = 'Carton';
//   } else if (value === '托盘') {
//     record.typeEnglish = 'Pallet';
//   }
// }

// 返回列表
const goBack = () => {
  router.push('/declaration/manage')
}

// 保存草稿
const handleSaveDraft = async () => {
  submitting.value = true
  try {
    // 将关联箱子的 cartons 和 volume 赋值到产品中，并确保单位完整
    productList.value.forEach(product => {
      // 保证单位信息完整
      if (!product.unit && product.unitCode) {
        const selectedUnit = findUnitByCode(measurementUnits.value, product.unitCode)
        if (selectedUnit) {
          product.unit = selectedUnit.unitNameEn || 'PCS'
        } else {
          product.unit = 'PCS'
        }
      }
      
      const relatedCarton = cartonList.value.find(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
      if (relatedCarton) {
        product.cartons = relatedCarton.quantity || 1
        product.volume = relatedCarton.volume || 0
      }
    })
    
    // 构建箱子产品关联数据
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number}> = []
    cartonList.value.forEach(carton => {
      if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id, 
              productId: productId,
              quantity: product.quantity 
            })
          }
        })
      }
    })
    
    // 确保所有产品的金额都已计算，但保留手动输入的金额
    // 保存时：如果用户手动修改过金额，则永久锁定，下次进入不再自动计算
    productList.value.forEach(product => {
      product.amount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00'
      // 转换为后端格式: true -> 1, false -> 0
      product.amountLocked = (product.amountUserModified || product.amountLocked) ? 1 : 0
    })

    // 构建保存数据
    const draftData = {
      ...formData,
      id: formId.value,
      status: 0,
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      products: productList.value.map((product: any) => ({
        ...product,
        id: product.id ? Number(product.id) : undefined,  // 确保 ID 是数字类型
        imageId: product.imageId ? Number(product.imageId) : null,  // 确保 imageId 是数字类型
        productPhoto: product.productPhoto, // 显式包含图片 URL
        elementValues: (product.declarationElements || []).map((elem: any) => ({
          elementName: elem.label,
          elementValue: elem.value && elem.value.trim() ? elem.value : '无'
        }))
      })),
      cartons: cartonList.value.map(carton => ({
        ...carton,
        id: carton.id ? Number(carton.id) : undefined,  // 确保ID是数字类型
        formId: formId.value ? Number(formId.value) : undefined
      })),
      cartonProducts: cartonProducts // 添加箱子产品关联数据
    }
    
    console.log('保存草稿数据:', draftData)
    const response = await saveDraft(draftData as any)
    
    if (response.data && response.data.code === 200) {
      const newDraftId = response.data.data
      message.success('草稿保存成功')
      
      console.log('新草稿ID:', newDraftId.formId)
      console.log('当前草稿ID:', formId.value)
      // 如果是新草稿，更新ID和URL，避免重复创建
      if (!formId.value) {
        console.log('更新草稿ID:', newDraftId.formId)
        formId.value = newDraftId.formId
        console.log('当前草稿ID:', formId.value)
        formData.formNo = newDraftId.formNo
        formStatus.value = 0
        router.replace({
          path: route.path,
          query: { ...route.query, id: newDraftId, status: 0 }
        })
      }
    } else {
      message.error(response.data.message || '保存草稿失败')
    }
  } catch (error: any) {
    console.error('保存草稿失败:', error)
    message.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
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
    if (!formData.destinationCountry) {
      message.error('请选择目的地国家')
      return
    }
    if (!formData.tradeCountry) {
      message.error('请选择贸易国家')
      return
    }
    if (!formData.transportMode) {
      message.error('请选择运输方式')
      return
    }
    if (!formData.departureCity) {
      message.error('请选择出发城市')
      return
    }
    if(!formData.currency){
      message.error('请选择货币')
      return
    }

    if (productList.value.length === 0) {
      message.error('请至少添加一个产品')
      return
    }    
    // 检查所有产品是否都关联了箱子
    const unassignedProducts = productList.value.filter(product => {
      // 检查这个产品是否在任何箱子的产品列表中
      return !cartonList.value.some(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
    })
    
    if(!formData.transportMode){
      message.error('请填写运输方式')
      return
    }
    if (unassignedProducts.length > 0) {
      const productNames = unassignedProducts.map(p => p.productName || '未命名产品').join('、')
      message.error(`以下产品未分配箱子: ${productNames}，请在箱子信息中选择关联产品`)
      return
    }
    
    submitting.value = true
    
    // 确保所有产品的金额都已计算，但保留手动输入的金额
    productList.value.forEach(product => {
      // 直接使用用户填写的金额
      product.amount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00'
    })
    
    // 将关联箱子的 cartons 和 volume 赋值到产品中，并确保单位完整
    productList.value.forEach(product => {
      // 保证单位信息完整
      if (!product.unit && product.unitCode) {
        const selectedUnit = findUnitByCode(measurementUnits.value, product.unitCode)
        if (selectedUnit) {
          product.unit = selectedUnit.unitNameEn || 'PCS'
        } else {
          product.unit = 'PCS'
        }
      }
      
      const relatedCarton = cartonList.value.find(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
      if (relatedCarton) {
        product.cartons = relatedCarton.quantity || 1
        product.volume = relatedCarton.volume || 0
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
              cartonId: carton.id, 
              productId: productId,
              quantity: product.quantity 
            })
          }
        })
      }
    })
    
    // 构造提交数据
    const submitData = {
      ...formData,
      // 关键修复：将国家代码转换为英文全名
      destinationCountry: formData.destinationCountry ? getCountryEnglishName(formData.destinationCountry) : '',
      tradeCountry: formData.tradeCountry ? getCountryEnglishName(formData.tradeCountry) : '',
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      status: 0, // 初始保存为草稿状态，由后续 /submit 启动流程并改为1
      products: productList.value.map((product: any) => {
        let finalAmount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00';
        return {
          ...product,
          amount: finalAmount,
          elementValues: (product.declarationElements || []).map((elem: any) => ({
            elementName: elem.label,
            elementValue: elem.value && elem.value.trim() ? elem.value : '无'
          }))
        };
      }),
      cartons: cartonList.value,
      cartonProducts: cartonProducts 
    }
    
    console.log('提交的数据:', submitData)
    console.log('表单ID:', formId.value)
    console.log('表单状态:', formStatus.value)
    
    // 如果是从草稿提交，我们需要告诉后端这个表单原本在草稿表
    // 或者后端可以在 submit 逻辑中自动处理
    
    let finalId = formId.value
    if (formId.value && formStatus.value == 0) {
      // 更新正式表单
      await updateDeclaration(formId.value, submitData as any)
      message.success('申报单更新成功')
    } else {
      // 新增正式表单 (包括从草稿提交)
      const res = await addDeclaration(submitData as any)
      if (res.data && res.data.code === 200) {
        finalId = res.data.data
        // 如果是从草稿提交成功，手动删除草稿
        if (formId.value && formStatus.value === 0) {
            try {
                await deleteDeclaration(formId.value, 0)
            } catch (e) {
                console.error('删除旧草稿失败:', e)
            }
        }
        message.success('申报单保存成功')
      } else {
        throw new Error(res.data.message || '保存失败')
      }
    }

    // 关键修复：显式调用提交接口启动 Flowable 流程
    if (finalId) {
      console.log('正在启动流程, ID:', finalId)
      const submitRes = await submitDeclaration(finalId)
      if (submitRes.data && submitRes.data.code === 200) {
        message.success('流程启动完成，已进入部门初审阶段')
      } else {
        message.warning('表单已保存，但流程启动失败: ' + (submitRes.data?.message || '未知错误'))
      }
    }
    
    goBack()
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 加载数据
const loadData = async () => {
  // 并行加载配置数据
  await Promise.all([
    loadProductTypes(),
    loadTransportModes(),
    loadPaymentMethods(),
    loadCountries(),
    loadCurrencies(),
    loadCities()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value, formStatus.value ?? undefined)
      console.log('=== 申报单详情 API 响应 ===', response)
      console.log('response.data:', response.data)
      
      // 处理返回的数据
      if (response.data && response.data.code === 200 && response.data.data) {
        const detailData = response.data.data
        console.log('申报单数据:', detailData)
        console.log('产品列表:', detailData.products)
        console.log('箱子列表:', detailData.cartons)
        
        // 更新状态和只读模式
        const submittedStatus = detailData.status || 0
        formStatus.value = submittedStatus
        console.log('🔄 更新 formStatus 为:', submittedStatus)
        
        // 如果申报单已提交（status >= 1），查询活跃任务
        if (submittedStatus >= 1 && formId.value) {
          try {
            const taskRes = await getActiveTasks(formId.value)
            activeTasks.value = taskRes.data || []
            console.log('📋 活跃任务:', activeTasks.value)
          } catch (e) {
            console.warn('获取活跃任务失败', e)
            activeTasks.value = []
          }
        }
        
        // 只读状态判断：
        // 1. 如果 URL 参数 readonly=true，保持只读
        // 2. 如果是审核模式 (isAudit)，保持只读
        // 3. 如果是水单提交模式 (isPaymentMode) 或提货单模式 (isPickupMode)，不改变 isReadonly (保持false,让各自区域自行判断)
        // 4. 如果是发票上传模式 (isInvoiceUploadMode)，不改变 isReadonly (保持false,让发票区域可操作)
        // 5. 否则根据状态判断：状态 0/2 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value) {
          isReadonly.value = true
          console.log('查看模式或审核模式, 设置为只读')
        } else if (!isPaymentMode.value && !isPickupMode.value && !isInvoiceUploadMode.value) {
          const editableStatuses = [0, 2]
          if (!editableStatuses.includes(submittedStatus)) {
            isReadonly.value = true
            console.log('申报单状态=' + submittedStatus + ', 设置为只读模式')
          } else {
            isReadonly.value = false
            console.log('申报单状态=' + submittedStatus + ', 可编辑模式')
          }
        } else if (isPickupMode.value) {
          // 提货单模式：申报单基本信息只读，但提货单区域可操作
          isReadonly.value = true
          console.log('提货单模式：申报单只读，提货单区域可操作')
        } else if (isInvoiceUploadMode.value) {
          // 发票上传模式：申报单基本信息只读，但发票区域可操作
          isReadonly.value = true
          console.log('发票上传模式：申报单只读，发票区域可操作')
        }
        
        // 填充基本表单数据
        formData.formNo = detailData.formNo || ''
        formData.shipperCompany = detailData.shipperCompany || 'NINGBO ZIYI TECHNOLOGY CO.,LTD'
        formData.shipperAddress = detailData.shipperAddress || 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA'
        formData.consigneeCompany = detailData.consigneeCompany || ''
        formData.consigneeAddress = detailData.consigneeAddress || ''
        formData.invoiceNo = detailData.invoiceNo || ''
        formData.transportMode = detailData.transportMode
        formData.paymentMethod = detailData.paymentMethod
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
                formData.departureCityChinese = detailData.departureCityChinese || '上海'
                formData.departureCityEnglish = detailData.departureCityEnglish || 'SHANGHAI, CHINA'
        formData.destinationCountry = detailData.destinationCountry || ''
        formData.tradeCountry = detailData.tradeCountry || ''
        formData.currency = detailData.currency || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        
        // 填充产品列表
        const productsRaw = detailData.products
        if (Array.isArray(productsRaw)) {
          productList.value = productsRaw.map((product: any) => ({
            ...product,
            amountLocked: product.amountLocked === 1, // 后端 1/0 转前端 true/false
            amountUserModified: product.amountLocked === 1, // 如果已锁定，说明之前是用户手动修改的
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
            // 处理产品照片 - 根据 imageId 构建预览 URL
            productPhoto: product.imageId 
              ? getFilePreviewUrl(product.imageId) 
              : (product.productPhoto || ''),
            photoFile: product.imageId ? {
              uid: String(product.imageId),
              name: 'product.jpg',
              status: 'done',
              url: getFilePreviewUrl(product.imageId)
            } : null,
            // 添加体积字段
            volume: product.volume || 0
          }))
          console.log('加载产品列表成功:', productList.value.length + ' 个产品')
          
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
        const cartonsRaw = detailData.cartons
        if (Array.isArray(cartonsRaw)) {
          cartonList.value = cartonsRaw.map((carton: any) => ({
            ...carton,
            // 添加体积字段(如果后端没返回)
            volume: carton.volume || 0,
            // 从 cartonProducts 关联中提取选中的产品
            selectedProducts: (detailData.cartonProducts || [])
              .filter((cp: any) => cp.cartonId === carton.id)
              .map((cp: any) => cp.productId)
          }))
          console.log('加载箱子列表成功:', cartonList.value.length + ' 个箱子')
          
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
        const remittancesRaw = detailData.remittances
        console.log('💰 水单原始数据:', remittancesRaw)
        console.log('💰 水单数据类型:', typeof remittancesRaw, Array.isArray(remittancesRaw))
        
        if (Array.isArray(remittancesRaw)) {
          remittanceList.value = remittancesRaw.map((rem: any) => ({
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
          console.log('💰 水单列表详情:', remittanceList.value)
        } else {
          console.warn('⚠️ 水单数据不是数组格式:', remittancesRaw)
          remittanceList.value = []
        }
        // 填充提货单附件(若需要)，如果是状态 >=2 开始加载
        const fStatus = formStatus.value || 0
        if (fStatus >= 2 && formId.value) {
          try {
            console.log('开始加载提货单附件，申报单ID:', formId.value)
            const pickupRes = await getPickupAttachments(formId.value)
            console.log('提货单附件API响应:', pickupRes)
            if (pickupRes.data && pickupRes.data.code === 200) {
              pickupAttachments.value = pickupRes.data.data.map((att: any) => ({
                id: att.id,
                fileName: att.fileName,
                fileUrl: att.fileUrl || `/api/v1/files/download?id=${att.id}`,
                fileType: 'PickupList',
                createTime: att.createTime
              }))
              console.log('提货单附件加载成功，数量:', pickupAttachments.value.length)
            } else {
              console.warn('提货单附件加载失败:', pickupRes.data?.message || '未知错误')
              pickupAttachments.value = [] // 确保数组为空而不是undefined
            }
          } catch (e) {
            console.warn('加载提货单附件失败:', e)
            pickupAttachments.value = [] // 确保数组为空而不是undefined
          }
        }

        // 加载财务补充记录
        if (formId.value && (submittedStatus > 1 || isFinanceUploadMode.value)) {
          loadFinancialSupplement()
        }
        
        // 加载业务发票
        loadBusinessInvoices()
        
        // 加载提货单列表（状态>=2 可创建和查看提货单，或者在审核模式下也需要加载）
        if (formId.value && (submittedStatus >= 2 || isAudit.value)) {
          loadDeliveryOrders()
        }
                
        // 如果是退回待审状态（status=9），加载最新的退回申请原因
        if (formId.value && submittedStatus === 9) {
          try {
            const historyRes = await getReturnAuditHistory(formId.value)
            if (historyRes.data && historyRes.data.code === 200) {
              const historyList = historyRes.data.data || []
              // 获取最新的待审核记录（auditStatus=0）或最后一条记录
              const latestRecord = historyList.find((r: any) => r.auditStatus === 0) || historyList[0]
              if (latestRecord) {
                returnReason.value = latestRecord.applyReason || '未填写原因'
                console.log('加载退回原因成功:', returnReason.value)
              }
            }
          } catch (e) {
            console.warn('加载退回原因失败:', e)
          }
        }
        
        // 根据已加载的国家信息加载对应的城市
        if (formData.destinationCountry) {
          const selectedCountry = countryOptions.value.find(country => country.value === formData.destinationCountry);
          if (selectedCountry) {
            loadCities(selectedCountry.englishName || selectedCountry.chineseName);
          }
        }
        
        message.success('数据加载成功')
      } else {
        console.error('API返回异常:', response.data)
        message.error('获取申报单详情失败')
      }
    } catch (error: any) {
      console.error('加载申报单详情失败:', error)
      message.error('加载数据失败：' + (error.message || '未知错误'))
    }
  }
}

// 显示审核历史
const showAuditHistory = async () => {
  if (!formId.value) {
    message.warning('请先选择申报单')
    return
  }
  
  auditHistoryVisible.value = true
  auditHistoryLoading.value = true
  try {
    const res = await getReturnAuditHistory(formId.value)
    if (res.data && res.data.code === 200) {
      auditHistoryList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载审核历史失败:', error)
    message.error('加载审核历史失败')
  } finally {
    auditHistoryLoading.value = false
  }
}

// 显示审核意见弹窗
const showRemarkModal = (action: string, defaultRemark: string): Promise<string> => {
  return new Promise((resolve) => {
    remarkAction.value = action
    remarkValue.value = defaultRemark
    remarkModalVisible.value = true
    
    // 临时覆盖 handleRemarkSubmit 函数，用于当前 promise 的 resolve
    const originalHandleRemarkSubmit = handleRemarkSubmit
    handleRemarkSubmit = () => {
      if (!remarkValue.value.trim()) {
        message.warning('请输入审核意见')
        return
      }
      resolve(remarkValue.value.trim())
      remarkModalVisible.value = false
      remarkValue.value = ''
      // 恢复原始函数
      handleRemarkSubmit = originalHandleRemarkSubmit
    }
  })
}

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
  loadCountries()
  loadMeasurementUnits()
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
  background-color: #FAFBFC !important;
  font-weight: 600;
  color: #FA8C16;
  font-size: 13px;
  text-transform: none;
  letter-spacing: normal;
  border-bottom: 2px solid #F0F0F0 !important;
}

:deep(.ant-table-cell) {
  border-bottom: 1px solid #f1f5f9;
}

/* 主按钮样式已通过全局CSS优化，这里保持基础覆盖以确保一致性 */
:deep(.ant-btn-primary) {
  background: #FA8C16 !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 6px -1px rgba(250, 140, 22, 0.2) !important;
}

:deep(.ant-btn-primary:hover) {
  background: #D46B08 !important;
  box-shadow: 0 10px 15px -3px rgba(250, 140, 22, 0.3) !important;
  transform: translateY(-1px);
}

/* 产品表格特定样式 */
:deep(.product-table .ant-table-thead > tr > th) {
  background: #FAFBFC !important;
}

/* 箱子表格特定样式 */
:deep(.carton-table .ant-table-thead > tr > th) {
  background: #FAFBFC !important;
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
  overflow-x: auto;
  min-width: 900px;
}

.section-card {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
  background: white;
}

:deep(.ant-card-head) {
  background: #FAFBFC;
  border-bottom: 1px solid #E2E8F0;
  min-height: 48px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 700;
  color: #FA8C16;
}

.totals-section {
  margin-top: 24px;
  padding: 24px;
  background: #FAFBFC;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
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
  color: #FA8C16;
  font-size: 18px;
  letter-spacing: -0.5px;
}

/* 水单图片上传样式 */
.remittance-photo-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.remittance-photo {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
}

.no-photo {
  color: #ccc;
  font-size: 12px;
}

.photo-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.photo-wrapper .remittance-photo {
  width: 100%;
  height: 100%;
  display: block;
}

.photo-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.photo-wrapper:hover .photo-overlay {
  opacity: 1;
}

.upload-placeholder {
  width: 48px;
  height: 48px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}

.upload-placeholder:hover {
  border-color: #FA8C16;
  background: #FFF7E6;
}

/* 计算详情样式 */
.calculation-box {
  background: #f7f7f7;
  padding: 20px;
  border-radius: 8px;
}

.calc-section {
  margin-bottom: 20px;
}

.calc-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ddd;
}

.calc-row:last-child {
  border-bottom: none;
}

.calc-label {
  color: #666;
}

.calc-value {
  font-weight: 500;
}

.calc-value.highlight {
  color: #FA8C16;
  font-size: 16px;
  font-weight: bold;
}

.calc-value.final-value {
  color: #D46B08;
  font-size: 18px;
  font-weight: bold;
}

.calc-row.deduct .calc-value {
  color: #ff4d4f;
}

.calc-row.total-with-tax .calc-value {
  color: #fa8c16;
  font-size: 16px;
  font-weight: bold;
}

.calc-row.final .calc-label {
  font-weight: bold;
}

.calc-row.final .calc-value {
  font-size: 20px;
  font-weight: bold;
}

/* 财务汇总信息增强样式 */
.finance-summary-card {
  background-color: #fafbfc;
}

.summary-stat-item {
  background: #fff;
  padding: 12px 16px;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

.summary-stat-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

.summary-stat-final {
  background: linear-gradient(135deg, #FFF7E6, #fff);
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  line-height: 1.3;
}

.stat-unit {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
