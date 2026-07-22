<template>
  <div class="declaration-form-page">
    <a-card :title="(isMaterialMode ? (isReadonly ? '申报资料查看' : '提交申报资料') : isMaterialAuditMode ? '申报单详情 - 资料审核' : isSupplementMode ? '申报单详情 - 补充资料提交' : isSupplementAuditMode ? '申报单详情 - 补充资料审核' : canSubmitInvoiceAmount ? '申报单详情 - 申请开票金额' : (canAuditInvoiceAmount || isInvoiceAmountAuditMode) ? '申报单详情 - 开票金额审核' : isInvoiceAmountMode ? '申报单详情 - 申请开票金额' : isInvoiceAuditMode ? '申报单详情 - 发票审核' : isInvoiceUploadMode ? '申报单详情 - 上传发票' : '出口申报表单')" >
      <template #extra>
        <a-space>
          <a-button @click="goBack">
            <template #icon><RollbackOutlined /></template>
            返回列表
          </a-button>
          
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
            >
              <template #icon><CheckCircleOutlined /></template>
              {{ getAuditActionText() }}通过
            </a-button>
            <a-button
              danger
              @click="handleReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:initial', 'business:declaration:audit:return']"
            >
              <template #icon><CloseCircleOutlined /></template>
              {{ getAuditActionText() }}驳回
            </a-button>
          </template>

          <!-- 资料提交模式下的按钮：状态 2（待资料提交）时显示 -->
          <template v-else-if="isMaterialMode && !isReadonly">
            <a-button
              v-if="formStatus === 2"
              type="primary"
              @click="handleSubmitMaterial"
              :loading="submitting"
              v-permission="['business:declaration:material:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交资料审核
            </a-button>
          </template>

          <!-- 资料审核模式下的按钮：状态 3（待资料审核）时显示 -->
          <template v-else-if="isMaterialAuditMode && formStatus === 3">
            <a-button
              type="primary"
              @click="handleMaterialAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:material']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleMaterialAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:material']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 补充资料提交：状态 4 时显示（不限 mode，避免从列表进入时漏显） -->
          <template v-else-if="canSubmitSupplement">
            <a-button
              type="primary"
              @click="handleSubmitSupplement"
              :loading="submitting"
              v-permission="['business:declaration:supplement:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交补充资料
            </a-button>
          </template>

          <!-- 补充资料审核：状态 5 时显示 -->
          <template v-else-if="canAuditSupplement">
            <a-button
              type="primary"
              @click="handleSupplementAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleSupplementAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 申请开票金额：状态 6 时显示（不限 mode，避免无发票提交菜单时漏显） -->
          <template v-else-if="canSubmitInvoiceAmount">
            <a-button
              type="primary"
              @click="handleSubmitInvoiceAmount"
              :loading="submitting"
              v-permission="['business:declaration:invoice-amount:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交开票金额申请
            </a-button>
          </template>

          <!-- 开票金额审核：状态 7 时显示 -->
          <template v-else-if="canAuditInvoiceAmount">
            <a-button
              type="primary"
              @click="handleInvoiceAmountAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice-amount']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleInvoiceAmountAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice-amount']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 业务发票审核：状态 9 -->
          <template v-else-if="canAuditInvoice">
            <a-button
              type="primary"
              @click="handleInvoiceAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleInvoiceAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 业务发票提交：状态 8（页头快捷入口，与业务发票区块内按钮一致） -->
          <template v-else-if="canSubmitInvoice">
            <a-button
              type="primary"
              @click="handleSubmitInvoice"
              :loading="submitting"
              v-permission="['business:declaration:invoice:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交发票审核
            </a-button>
          </template>

          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 保存草稿按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="handleSaveDraft" :loading="submitting" v-permission="['business:declaration:create']">
              <template #icon><SaveOutlined /></template>
              保存草稿
            </a-button>
            
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting" v-permission="['business:declaration:submit']">
              <template #icon><SendOutlined /></template>
              提交申报
            </a-button>
          </template>
        </a-space>
      </template>
      
      <!-- 退回原因提示 -->
      <a-alert
        v-if="formStatus === 11 && returnReason"
        message="退回申请原因"
        :description="returnReason"
        type="warning"
        show-icon
        class="mb-4"
      />
      
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
              <a-auto-complete
                v-model:value="formData.consigneeCompany"
                :options="customerOptions"
                placeholder="选择或输入收货人公司名"
                :disabled="isFormReadonly"
                @select="onCustomerSelect"
                :filter-option="filterCustomerOption"
                style="width: 100%"
              >
                <template #option="{ value: val, label }">
                  <span>{{ label || val }}</span>
                </template>
              </a-auto-complete>
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
            <a-form-item label="出发口岸">
              <a-select
                v-model:value="formData.departureCity"
                placeholder="请选择出发口岸"
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
              <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeCarton(index)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 申报资料 (状态 2 及以上显示) -->
      <a-card v-if="formStatus && formStatus >= 2" id="section-material" title="申报资料" size="small" class="section-card">
        <template #extra>
          <a-space>
            <!-- 资料提交按钮：状态 2 且非审核模式 -->
            <a-button
              v-if="formStatus === 2 && isMaterialMode && !isReadonly"
              type="primary"
              size="small"
              @click="handleSubmitMaterial"
              :loading="submitting"
              v-permission="['business:declaration:material:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交资料审核
            </a-button>
            
            <!-- 资料审核按钮：状态 3 且审核模式 -->
            <template v-if="formStatus === 3 && isMaterialAuditMode">
              <a-button
                type="primary"
                size="small"
                @click="handleMaterialAuditApprove"
                :loading="submitting"
                v-permission="['business:declaration:audit:material']"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                danger
                size="small"
                @click="handleMaterialAuditReject"
                :loading="submitting"
                v-permission="['business:declaration:audit:material']"
              >
                <template #icon><CloseCircleOutlined /></template>
                审核驳回
              </a-button>
            </template>
          </a-space>
        </template>
        <a-spin :spinning="materialLoading">
          <!-- 进度卡片 -->
          <div class="progress-card">
            <div class="progress-left">
              <div class="progress-title">
                <FileDoneOutlined class="text-blue-500 mr-2" />
                <span v-if="!isMaterialReadonly">资料上传进度</span>
                <span v-else>资料查看</span>
              </div>
              <div class="progress-desc">
                共 <b>{{ coreMaterialItems.length }}</b> 项资料，必填 <b class="text-red-500">{{ materialRequiredCount }}</b> 项，
                已上传 <b :class="materialUploadedCount === materialRequiredCount ? 'text-green-500' : 'text-blue-500'">{{ materialUploadedCount }}</b> 项
              </div>
            </div>
            <div class="progress-right">
              <a-progress
                type="circle"
                :percent="materialProgressPercent"
                :width="60"
                :stroke-color="materialProgressPercent === 100 ? '#52c41a' : '#1677ff'"
              />
            </div>
          </div>

          <!-- 环节 Tabs -->
          <a-tabs v-model:activeKey="activeStageTab" size="small" class="stage-tabs">
            <a-tab-pane v-for="stage in availableStages" :key="stage.value">
              <template #tab>
                <span>
                  {{ stage.label }}
                  <a-badge
                    v-if="stageStats[stage.value] && stageStats[stage.value].required > 0"
                    :count="stageStats[stage.value].uploaded + '/' + stageStats[stage.value].required"
                    :number-style="{ backgroundColor: stageStats[stage.value].uploaded >= stageStats[stage.value].required ? '#52c41a' : '#1677ff', fontSize: '11px', boxShadow: 'none' }"
                    class="ml-1"
                  />
                  <a-badge
                    v-else-if="stageStats[stage.value]"
                    :count="stageStats[stage.value].total"
                    :number-style="{ backgroundColor: '#8c8c8c', fontSize: '11px', boxShadow: 'none' }"
                    class="ml-1"
                  />
                </span>
              </template>
            </a-tab-pane>
          </a-tabs>

          <div class="toolbar" v-if="isMaterialEditable">
            <a-space>
              <a-button type="primary" size="small" class="material-customize-btn" @click="openAddMaterialRow"
                        v-permission="['business:declaration:material:customize']">
                <template #icon><PlusOutlined /></template>
                新增自定义资料项
              </a-button>
            </a-space>
          </div>

          <a-table
            :dataSource="activeStageItems"
            :columns="materialColumns"
            :pagination="false"
            :rowKey="materialRowKey"
            size="middle"
            class="material-table"
            :expandedRowKeys="materialExpandedKeys"
            :showExpandColumn="false"
          >
            <template #expandedRowRender="{ record }">
              <div class="schema-inline" v-if="parseMaterialSchema(record.formSchema).length">
                <div
                  class="schema-field"
                  v-for="field in parseMaterialSchema(record.formSchema).filter((f: any) => !isInvoiceMaterial(record as MaterialItem) || !MATERIAL_FIXED_KEYS.includes(f.key))"
                  :key="field.key"
                >
                  <label class="schema-label">
                    <span v-if="field.required" class="required-star">*</span>
                    {{ field.label }}
                  </label>
                  <div
                    v-if="field.type === 'number' && field.key === 'amount' && isInvoiceMaterial(record as MaterialItem)"
                    class="schema-input-wrap"
                  >
                    <a-input-number
                      :value="getMaterialFieldValue(record as MaterialItem, field.key)"
                      @update:value="(v: any) => setMaterialFieldValue(record as MaterialItem, field.key, v)"
                      @blur="saveMaterialRowFields(record as MaterialItem)"
                      :disabled="!isMaterialEditable"
                      size="small"
                      class="schema-input"
                      :precision="4"
                    />
                    <div
                      v-if="materialPdfMessages[materialRowKey(record as MaterialItem)]"
                      class="pdf-amount-hint"
                      :class="'pdf-amount-hint-' + materialPdfMessages[materialRowKey(record as MaterialItem)].type"
                    >
                      {{ materialPdfMessages[materialRowKey(record as MaterialItem)].text }}
                    </div>
                  </div>
                  <a-input-number
                    v-else-if="field.type === 'number'"
                    :value="getMaterialFieldValue(record as MaterialItem, field.key)"
                    @update:value="(v: any) => setMaterialFieldValue(record as MaterialItem, field.key, v)"
                    @blur="saveMaterialRowFields(record as MaterialItem)"
                    :disabled="!isMaterialEditable"
                    size="small"
                    class="schema-input"
                    :precision="4"
                  />
                  <a-date-picker
                    v-else-if="field.type === 'date'"
                    :value="getMaterialFieldValue(record as MaterialItem, field.key) || undefined"
                    value-format="YYYY-MM-DD"
                    @update:value="(v: any) => { setMaterialFieldValue(record as MaterialItem, field.key, v); saveMaterialRowFields(record as MaterialItem) }"
                    :disabled="!isMaterialEditable"
                    size="small"
                    class="schema-input"
                  />
                  <a-select
                    v-else-if="field.type === 'select'"
                    :value="getMaterialFieldValue(record as MaterialItem, field.key)"
                    @update:value="(v: any) => { setMaterialFieldValue(record as MaterialItem, field.key, v); saveMaterialRowFields(record as MaterialItem) }"
                    :disabled="!isMaterialEditable"
                    :options="(field.options || []).map((o: string) => ({ label: o, value: o }))"
                    size="small"
                    class="schema-input"
                    allow-clear
                  />
                  <a-input
                    v-else
                    :value="getMaterialFieldValue(record as MaterialItem, field.key)"
                    @update:value="(v: any) => setMaterialFieldValue(record as MaterialItem, field.key, v)"
                    @blur="saveMaterialRowFields(record as MaterialItem)"
                    :disabled="!isMaterialEditable"
                    size="small"
                    class="schema-input"
                    :maxlength="200"
                  />
                </div>
              </div>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <div class="name-main">
                    <span class="name-text">{{ record.name }}</span>
                    <a-tag v-if="record.required === 1" color="red" class="ui-tag">必填</a-tag>
                    <a-tag v-else class="ui-tag">选填</a-tag>
                    <a-tag v-if="record.templateId == null" color="blue" class="ui-tag">自定义</a-tag>
                    <a-tag v-if="parseMaterialSchema(record.formSchema).length" color="purple" class="ui-tag">
                      <FormOutlined /> 需填写字段
                    </a-tag>
                    <!-- 上传按鈕内嵌在名称行 -->
                    <div class="name-upload-actions" v-if="isMaterialEditable">
                      <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record as MaterialItem)">
                        <a-button type="primary" size="small" class="material-upload-btn">
                          <template #icon><UploadOutlined v-if="record.status !== 1" /><PlusOutlined v-else /></template>
                          {{ record.status === 1 ? '追加' : '上传' }}
                        </a-button>
                      </a-upload>
                      <a-dropdown v-if="checkPermission(['business:declaration:material:customize'])" :trigger="['click']">
                        <a-button size="small" type="text"><MoreOutlined /></a-button>
                        <template #overlay>
                          <a-menu>
                            <a-menu-item @click="openEditMaterialRow(record as MaterialItem)"><EditOutlined /> 编辑名称/说明</a-menu-item>
                            <a-menu-item v-if="record.status === 1" @click="confirmClearMaterialFile(record as MaterialItem)"><DeleteOutlined /> <span class="text-red-500">清除附件</span></a-menu-item>
                            <a-menu-item v-if="record.templateId == null" @click="confirmDeleteMaterialRow(record as MaterialItem)"><CloseOutlined /> <span class="text-red-500">删除资料项</span></a-menu-item>
                          </a-menu>
                        </template>
                      </a-dropdown>
                    </div>
                  </div>
                  <div v-if="record.remark" class="name-remark">{{ record.remark }}</div>

                  <!-- 附件列表（在资料项名称下方） -->
                  <template v-if="record.attachments && record.attachments.length > 0">
                    <!-- 发票类：每个附件独立卡片 -->
                    <template v-if="isInvoiceMaterial(record as MaterialItem)">
                      <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                        <!-- 第一行：文件 + 字段 + 删除 -->
                        <div class="att-row-main">
                          <div class="att-file-name">
                            <FileTextOutlined class="file-icon-sm" />
                            <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          </div>
                          <div class="att-divider-v"></div>
                          <template v-if="isMaterialEditable">
                            <div class="att-field-inline">
                              <span class="att-field-label">金额</span>
                              <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                            </div>
                            <div class="att-field-inline">
                              <span class="att-field-label">发票号</span>
                              <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', v)" @blur="() => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', att.invoiceNo)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                            </div>
                            <div class="att-field-inline">
                              <span class="att-field-label">日期</span>
                              <a-date-picker :value="att.invoiceDate || undefined" value-format="YYYY-MM-DD" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceDate', v)" placeholder="-" size="small" style="width: 140px" />
                            </div>
                          </template>
                          <template v-else>
                            <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                            <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                            <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                          </template>
                          <a-popconfirm v-if="isMaterialEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                            <DeleteOutlined class="file-delete-btn" />
                          </a-popconfirm>
                        </div>
                        <!-- 第二行：元数据 -->
                        <div class="att-row-meta">
                          <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                        </div>
                      </div>
                    </template>
                    <!-- 非发票类：统一卡片风格 -->
                    <template v-else>
                      <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                        <div class="att-row-main">
                          <div class="att-file-name">
                            <FileTextOutlined class="file-icon-sm" />
                            <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          </div>
                          <a-popconfirm v-if="isMaterialEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                            <DeleteOutlined class="file-delete-btn" />
                          </a-popconfirm>
                        </div>
                        <div class="att-row-meta">
                          <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                        </div>
                      </div>
                      <div class="file-count-hint" v-if="record.attachments.length > 1">共 {{ record.attachments.length }} 份文件</div>
                    </template>
                  </template>
                  <!-- 兼容旧数据 -->
                  <template v-else-if="record.status === 1 && record.fileUrl">
                    <div class="file-item-row">
                      <FileTextOutlined class="file-icon-sm" />
                      <a @click.prevent="previewFile(record.fileUrl)" class="file-name-sm" style="cursor:pointer">{{ record.fileName || '查看附件' }}</a>
                    </div>
                  </template>
                  <!-- 未上传 -->
                  <template v-else>
                    <div class="file-cell file-empty"><CloudUploadOutlined class="file-icon" /><span>尚未上传</span></div>
                  </template>
                </div>
              </template>
            </template>
          </a-table>
        </a-spin>
      </a-card>

      <!-- 业务发票已合并至申报资料模块的「业务发票」环节标签页 -->

      <!-- 新增/编辑资料项弹窗 -->
      <a-modal
        v-model:open="materialRowModalVisible"
        :title="materialRowEditingId ? '编辑资料项' : '新增自定义资料项'"
        @ok="handleSaveMaterialRow"
        @cancel="materialRowModalVisible = false"
        :confirm-loading="materialRowSaving"
        width="520px"
        destroyOnClose
      >
        <a-form layout="vertical" :model="materialRowForm">
          <a-form-item label="名称" required>
            <a-input v-model:value="materialRowForm.name" placeholder="请输入资料名称" :maxlength="100" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="是否必填">
                <a-radio-group v-model:value="materialRowForm.required" button-style="solid">
                  <a-radio :value="1">必填</a-radio>
                  <a-radio :value="0">选填</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="排序">
                <a-input-number v-model:value="materialRowForm.sort" :min="0" :max="9999" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="说明">
            <a-textarea v-model:value="materialRowForm.remark" :rows="3" :maxlength="500" />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- 补充资料（SUPPLEMENT 环节独立区域） -->
      <a-card v-if="showSupplementSection" id="section-supplement" title="补充资料" size="small" class="section-card">
        <template #extra>
          <a-space>
            <!-- 补充资料提交按钮：状态 4 -->
            <a-button
              v-if="canSubmitSupplement"
              type="primary"
              size="small"
              @click="handleSubmitSupplement"
              :loading="submitting"
              v-permission="['business:declaration:supplement:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交补充资料
            </a-button>
            
            <!-- 补充资料审核按钮：状态 5 -->
            <template v-if="canAuditSupplement">
              <a-button
                type="primary"
                size="small"
                @click="handleSupplementAuditApprove"
                :loading="submitting"
                v-permission="['business:declaration:audit:supplement']"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                danger
                size="small"
                @click="handleSupplementAuditReject"
                :loading="submitting"
                v-permission="['business:declaration:audit:supplement']"
              >
                <template #icon><CloseCircleOutlined /></template>
                审核驳回
              </a-button>
            </template>
          </a-space>
        </template>
        <a-spin :spinning="materialLoading">
          <!-- 进度卡片 -->
          <div class="progress-card">
            <div class="progress-left">
              <div class="progress-title">
                <FileDoneOutlined class="text-blue-500 mr-2" />
                <span v-if="isSupplementEditable">补充资料上传进度</span>
                <span v-else>补充资料查看</span>
              </div>
              <div class="progress-desc">
                共 <b>{{ supplementStats.total }}</b> 项资料，必填 <b class="text-red-500">{{ supplementStats.required }}</b> 项，
                已上传 <b :class="supplementStats.uploaded === supplementStats.required ? 'text-green-500' : 'text-blue-500'">{{ supplementStats.uploaded }}</b> 项
              </div>
            </div>
            <div class="progress-right">
              <a-progress
                type="circle"
                :percent="supplementStats.required === 0 ? (supplementStats.total === 0 ? 0 : 100) : Math.round((supplementStats.uploaded / supplementStats.required) * 100)"
                :width="60"
                :stroke-color="supplementStats.uploaded >= supplementStats.required && supplementStats.required > 0 ? '#52c41a' : '#1677ff'"
              />
            </div>
          </div>

          <a-table :dataSource="supplementItems" :columns="materialColumns" :pagination="false" rowKey="id" size="small" class="material-table" :expandIcon="() => null">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <div class="name-main">
                    <span class="name-text">{{ (record as MaterialItem).name }}</span>
                    <a-tag v-if="(record as MaterialItem).required === 1" color="red" class="ui-tag">必填</a-tag>
                    <a-tag v-else class="ui-tag">选填</a-tag>
                    <div class="name-upload-actions" v-if="isSupplementEditable">
                      <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record as MaterialItem)">
                        <a-button type="primary" size="small" class="material-upload-btn">
                          <template #icon><UploadOutlined v-if="(record as MaterialItem).status !== 1" /><PlusOutlined v-else /></template>
                          {{ (record as MaterialItem).status === 1 ? '追加' : '上传' }}
                        </a-button>
                      </a-upload>
                    </div>
                  </div>
                  <div v-if="(record as MaterialItem).remark" class="name-remark">{{ (record as MaterialItem).remark }}</div>
                  <!-- 附件列表 -->
                  <template v-if="record.attachments && record.attachments.length > 0">
                    <!-- 发票类：每个附件带金额/发票号/日期字段 -->
                    <template v-if="isInvoiceMaterial(record as MaterialItem)">
                      <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                        <div class="att-row-main">
                          <div class="att-file-name">
                            <FileTextOutlined class="file-icon-sm" />
                            <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          </div>
                          <div class="att-divider-v"></div>
                          <template v-if="isSupplementEditable">
                            <div class="att-field-inline">
                              <span class="att-field-label">金额</span>
                              <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                            </div>
                            <div class="att-field-inline">
                              <span class="att-field-label">发票号</span>
                              <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', v)" @blur="() => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', att.invoiceNo)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                            </div>
                            <div class="att-field-inline">
                              <span class="att-field-label">日期</span>
                              <a-date-picker :value="att.invoiceDate || undefined" value-format="YYYY-MM-DD" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceDate', v)" placeholder="-" size="small" style="width: 140px" />
                            </div>
                          </template>
                          <template v-else>
                            <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                            <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                            <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                          </template>
                          <a-popconfirm v-if="isSupplementEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                            <DeleteOutlined class="file-delete-btn" />
                          </a-popconfirm>
                        </div>
                        <div class="att-row-meta">
                          <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                        </div>
                      </div>
                    </template>
                    <!-- 非发票类：简单格式 -->
                    <template v-else>
                      <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                        <div class="att-row-main">
                          <div class="att-file-name">
                            <FileTextOutlined class="file-icon-sm" />
                            <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          </div>
                          <a-popconfirm v-if="isSupplementEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                            <DeleteOutlined class="file-delete-btn" />
                          </a-popconfirm>
                        </div>
                        <div class="att-row-meta">
                          <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                          <span class="att-meta-dot"></span>
                          <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                        </div>
                      </div>
                    </template>
                    <div class="file-count-hint" v-if="record.attachments.length > 1">共 {{ record.attachments.length }} 份文件</div>
                  </template>
                  <template v-else>
                    <div class="file-cell file-empty"><CloudUploadOutlined class="file-icon" /><span>尚未上传</span></div>
                  </template>
                </div>
              </template>
            </template>
          </a-table>
        </a-spin>
      </a-card>

      <!-- 申请开票金额区域 -->
      <a-card v-if="showInvoiceAmountSection" id="section-invoice-amount" title="申请开票金额" size="small" class="section-card">
        <template #extra>
          <a-space>
            <a-button
              v-if="canSubmitInvoiceAmount"
              type="primary"
              size="small"
              @click="handleSubmitInvoiceAmount"
              :loading="submitting"
              v-permission="['business:declaration:invoice-amount:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交开票金额
            </a-button>
            
            <template v-if="canAuditInvoiceAmount">
              <a-button
                type="primary"
                size="small"
                @click="handleInvoiceAmountAuditApprove"
                :loading="submitting"
                v-permission="['business:declaration:audit:invoice-amount']"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                danger
                size="small"
                @click="handleInvoiceAmountAuditReject"
                :loading="submitting"
                v-permission="['business:declaration:audit:invoice-amount']"
              >
                <template #icon><CloseCircleOutlined /></template>
                审核驳回
              </a-button>
            </template>
          </a-space>
        </template>
        <a-spin :spinning="invoiceAmountLoading">
          <div class="progress-card">
            <div class="progress-left">
              <div class="progress-title">
                <CalculatorOutlined class="text-blue-500 mr-2" />
                <span v-if="isInvoiceAmountEditable">开票金额计算详情</span>
                <span v-else>开票金额查看</span>
              </div>
              <div class="progress-desc">
                系统根据收汇、商品退税率、发票资料项自动计算
              </div>
            </div>
            <div class="progress-right">
              <a-button v-if="showInvoiceAmountSection" type="link" @click="loadInvoiceAmountDetail">
                <template #icon><ReloadOutlined /></template>
                {{ isInvoiceAmountEditable ? '刷新计算' : '加载详情' }}
              </a-button>
              <a-button v-if="invoiceAmountCalcDetail" type="link" @click="handleDownloadInvoicePackage">
                <template #icon><DownloadOutlined /></template>
                下载开票文件
              </a-button>
            </div>
          </div>

          <a-alert
            v-if="isInvoiceAmountEditable"
            type="info"
            show-icon
            message="提交开票金额申请前，请确认外汇水单已提交。系统将自动计算开票金额。"
            style="margin-bottom: 12px"
          />

          <!-- 关联水单列表 -->
          <div style="margin-bottom: 16px;">
            <div style="display: flex; align-items: center; margin-bottom: 8px;">
              <LinkOutlined style="margin-right: 6px; color: #1677ff;" />
              <span style="font-weight: 600; font-size: 14px;">关联水单</span>
              <a-tag v-if="invoiceAmountRemittances.length > 0" style="margin-left: 8px;">{{ invoiceAmountRemittances.length }} 笔</a-tag>
            </div>
            <a-table
              v-if="invoiceAmountRemittances.length > 0"
              :dataSource="invoiceAmountRemittances"
              :columns="remittanceColumns"
              :pagination="false"
              size="small"
              rowKey="id"
              :scroll="{ x: 860 }"
              bordered
            />
            <a-empty v-else description="暂无关联水单，请先在水单管理中关联并审核通过" :image-style="{ height: '30px' }" />
          </div>

          <!-- 开票金额计算详情 -->
          <div v-if="invoiceAmountCalcDetail" class="calc-detail-wrap">
            <!-- 收入部分 -->
            <div class="calc-section calc-income">
              <div class="calc-section-title">
                <RiseOutlined style="margin-right: 6px;" /> 收入部分
              </div>
              <div v-for="(rd, idx) in (invoiceAmountCalcDetail.remittanceDetails || [])" :key="'rd-' + idx" style="margin-bottom: 12px; padding: 10px; background: #f9f9f9; border-radius: 4px;">
                <div class="calc-row">
                  <span class="calc-label" style="font-weight: 600;">{{ rd.remittanceName || '水单' }}</span>
                  <span class="calc-value">{{ fmtAmt(rd.amount) }} {{ rd.currency || 'USD' }} × {{ Number(rd.taxRate || 0).toFixed(4) }} = <b>{{ fmtAmt(rd.cnyAmount) }} CNY</b></span>
                </div>
                <div v-if="rd.proportion && rd.proportion < 100" style="font-size: 12px; color: #666; margin-left: 12px; margin-top: 4px;">
                  分配占比: {{ rd.proportion }}% ({{ fmtAmt(rd.relationAmount) }} / {{ fmtAmt(rd.fullAmount) }})
                </div>
                <div v-if="rd.bankFeeCny > 0 || rd.internalBankFee > 0" style="font-size: 12px; margin-left: 12px; margin-top: 4px; padding: 6px; background: #fff3e0; border-radius: 3px;">
                  <div v-if="rd.bankFeeCny > 0" style="color: #e65100;">
                    银行手续费: {{ fmtAmt(rd.bankFeeOriginal) }} {{ rd.currency || 'USD' }} × {{ Number(rd.taxRate || 0).toFixed(4) }}
                    <span v-if="rd.proportion && rd.proportion < 100"> × {{ rd.proportion }}%</span>
                    = {{ fmtAmt(rd.bankFeeCny) }} CNY
                  </div>
                  <div v-if="rd.internalBankFee > 0" style="color: #d84315; margin-top: 2px;">
                    内部操作费: {{ fmtAmt(rd.internalBankFeeOriginal) }} CNY
                    <span v-if="rd.proportion && rd.proportion < 100"> × {{ rd.proportion }}% = </span>
                    <span v-if="rd.proportion && rd.proportion < 100">{{ fmtAmt(rd.internalBankFee) }} CNY</span>
                  </div>
                </div>
              </div>
              <div class="calc-row calc-subtotal">
                <span class="calc-label">收汇合计</span>
                <span class="calc-value text-green-600"><b>{{ fmtAmt(invoiceAmountCalcDetail.totalCny) }} CNY</b></span>
              </div>
              <div class="calc-row" v-if="invoiceAmountCalcDetail.productTaxDetails && invoiceAmountCalcDetail.productTaxDetails.length > 0">
                <span class="calc-label">退税加成明细</span>
                <span class="calc-value">
                  <span v-for="(pd, pdx) in invoiceAmountCalcDetail.productTaxDetails" :key="'pd-'+pdx" style="display: block; font-size: 12px; margin-bottom: 6px;">
                    <div style="color: #666;">{{ pd.productName || pd.hsCode || '商品' + (Number(pdx) + 1) }}</div>
                    <div style="margin-left: 12px;">
                      原币: {{ fmtAmt(pd.amount) }} × 汇率: {{ invoiceAmountCalcDetail.weightedExchangeRate }} = {{ fmtAmt(pd.cnyAmount) }} CNY
                    </div>
                    <div style="margin-left: 12px;">
                      {{ fmtAmt(pd.cnyAmount) }} × (1+{{ pd.taxRefundRate }}%) = <b style="color: #16a34a;">{{ fmtAmt(pd.amountWithTaxRefund) }} CNY</b>
                    </div>
                  </span>
                  <span style="display: block; margin-top: 4px; font-weight: bold; color: #16a34a; border-top: 1px dashed #ddd; padding-top: 4px;">合计: {{ fmtAmt(invoiceAmountCalcDetail.amountWithTaxRefund) }} CNY</span>
                </span>
              </div>
              <div class="calc-row calc-highlight" v-else>
                <span class="calc-label">退税加成</span>
                <span class="calc-value text-gray-400">商品未配置退税率，按 0% 计算</span>
              </div>
            </div>
          
            <!-- 支出部分 -->
            <div class="calc-section calc-expense">
              <div class="calc-section-title">
                <FallOutlined style="margin-right: 6px;" /> 支出部分（扣减项）
              </div>
              <div class="calc-row" v-for="(ded, didx) in (invoiceAmountCalcDetail.invoiceDeductionItems || [])" :key="'ded-'+didx">
                <span class="calc-label">{{ ded.name }}</span>
                <span class="calc-value text-red-500">-{{ fmtAmt(ded.amount) }} CNY</span>
              </div>
              <div class="calc-row" v-if="!invoiceAmountCalcDetail.invoiceDeductionItems || invoiceAmountCalcDetail.invoiceDeductionItems.length === 0">
                <span class="calc-label">发票扣减项</span>
                <span class="calc-value text-gray-400">无</span>
              </div>
              <div class="calc-row" v-if="invoiceAmountCalcDetail.bankFeeAmount > 0">
                <span class="calc-label">银行手续费</span>
                <span class="calc-value text-red-500">-{{ fmtAmt(invoiceAmountCalcDetail.bankFeeAmount) }} CNY</span>
              </div>
              <div class="calc-row" v-if="invoiceAmountCalcDetail.internalBankFee > 0">
                <span class="calc-label">内部操作费</span>
                <span class="calc-value text-red-500">-{{ fmtAmt(invoiceAmountCalcDetail.internalBankFee) }} CNY</span>
              </div>
              <div class="calc-row" v-if="!invoiceAmountCalcDetail.bankFeeAmount && !invoiceAmountCalcDetail.internalBankFee">
                <span class="calc-label">手续费</span>
                <span class="calc-value text-gray-400">无</span>
              </div>
              <div class="calc-row calc-subtotal">
                <span class="calc-label">支出合计</span>
                <span class="calc-value text-red-600"><b>-{{ fmtAmt(calcExpenseTotal) }} CNY</b></span>
              </div>
            </div>
          
            <!-- 开票金额 -->
            <div class="calc-section calc-result">
              <div class="calc-row">
                <span class="calc-label">开票金额</span>
                <span class="calc-value">
                  {{ fmtAmt(invoiceAmountCalcDetail.amountWithTaxRefund) }} - {{ fmtAmt(calcExpenseTotal) }} =
                  <b class="text-blue-600" style="font-size: 18px;">{{ fmtAmt(invoiceAmountCalcDetail.invoiceAmount) }} CNY</b>
                </span>
              </div>
            </div>
          
            <!-- 计算过程 -->
            <a-collapse :bordered="false" size="small" v-if="invoiceAmountCalcDetail.calculationSteps && invoiceAmountCalcDetail.calculationSteps.length > 0">
              <a-collapse-panel key="steps" header="计算过程明细">
                <div class="calc-steps">
                  <div v-for="(step, idx) in invoiceAmountCalcDetail.calculationSteps" :key="idx" class="calc-step-item">
                    <span class="calc-step-no">{{ Number(idx) + 1 }}</span>
                    <span class="calc-step-text">{{ step }}</span>
                  </div>
                </div>
              </a-collapse-panel>
            </a-collapse>
          </div>
          <a-empty v-else :description="isInvoiceAmountEditable ? '点击刷新计算加载开票金额详情' : '暂无开票金额计算数据'" />
        </a-spin>
      </a-card>

      <!-- 业务发票（INVOICE 环节独立区域） -->
      <a-card v-if="showInvoiceSection" id="section-invoice" :title="isInvoiceEditable ? '业务发票 (可编辑)' : '业务发票'" size="small" class="section-card">
        <template #extra>
          <a-space>
            <a-button
              v-if="canSubmitInvoice"
              type="primary"
              size="small"
              @click="handleSubmitInvoice"
              :loading="submitting"
              v-permission="['business:declaration:invoice:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交发票审核
            </a-button>
            <template v-if="canAuditInvoice">
              <a-button
                type="primary"
                size="small"
                @click="handleInvoiceAuditApprove"
                :loading="submitting"
                v-permission="['business:declaration:audit:invoice']"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                danger
                size="small"
                @click="handleInvoiceAuditReject"
                :loading="submitting"
                v-permission="['business:declaration:audit:invoice']"
              >
                <template #icon><CloseCircleOutlined /></template>
                审核驳回
              </a-button>
            </template>
          </a-space>
        </template>
        <a-spin :spinning="materialLoading">
          <!-- 进度卡片 -->
          <div class="progress-card">
            <div class="progress-left">
              <div class="progress-title">
                <FileDoneOutlined class="text-blue-500 mr-2" />
                <span v-if="isInvoiceEditable">业务发票上传进度</span>
                <span v-else>业务发票查看</span>
              </div>
              <div class="progress-desc">
                共 <b>{{ invoiceStats.total }}</b> 项资料，必填 <b class="text-red-500">{{ invoiceStats.required }}</b> 项，
                已上传 <b :class="invoiceStats.uploaded === invoiceStats.required ? 'text-green-500' : 'text-blue-500'">{{ invoiceStats.uploaded }}</b> 项
              </div>
            </div>
            <div class="progress-right">
              <a-progress
                type="circle"
                :percent="invoiceStats.required === 0 ? (invoiceStats.total === 0 ? 0 : 100) : Math.round((invoiceStats.uploaded / invoiceStats.required) * 100)"
                :width="60"
                :stroke-color="invoiceStats.uploaded >= invoiceStats.required && invoiceStats.required > 0 ? '#52c41a' : '#1677ff'"
              />
            </div>
          </div>

          <a-alert v-if="isInvoiceEditable" type="info" show-icon message="请上传业务发票相关附件，支持上传多份发票" style="margin-bottom: 12px" />
          <a-table :dataSource="invoiceStageItems" :columns="materialColumns" :pagination="false" rowKey="id" size="small" class="material-table" :expandIcon="() => null">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <div class="name-main">
                    <span class="name-text">{{ (record as MaterialItem).name }}</span>
                    <a-tag v-if="(record as MaterialItem).required === 1" color="red" class="ui-tag">必填</a-tag>
                    <a-tag v-else class="ui-tag">选填</a-tag>
                    <div class="name-upload-actions" v-if="isInvoiceEditable">
                      <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record as MaterialItem)">
                        <a-button type="primary" size="small" class="material-upload-btn">
                          <template #icon><UploadOutlined v-if="(record as MaterialItem).status !== 1" /><PlusOutlined v-else /></template>
                          {{ (record as MaterialItem).status === 1 ? '追加' : '上传' }}
                        </a-button>
                      </a-upload>
                    </div>
                  </div>
                  <div v-if="(record as MaterialItem).remark" class="name-remark">{{ (record as MaterialItem).remark }}</div>
                  <!-- 附件列表（发票类：带金额/发票号/日期字段） -->
                  <template v-if="record.attachments && record.attachments.length > 0">
                    <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <div class="att-divider-v"></div>
                        <template v-if="isInvoiceEditable">
                          <div class="att-field-inline">
                            <span class="att-field-label">金额</span>
                            <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                          </div>
                          <div class="att-field-inline">
                            <span class="att-field-label">发票号</span>
                            <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', v)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                          </div>
                          <div class="att-field-inline">
                            <span class="att-field-label">日期</span>
                            <a-date-picker :value="att.invoiceDate ? dayjs(att.invoiceDate) : undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceDate', v ? v.format('YYYY-MM-DD') : null)" size="small" style="width: 150px" />
                          </div>
                        </template>
                        <template v-else>
                          <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                        </template>
                        <a-popconfirm v-if="isInvoiceEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                          <DeleteOutlined class="file-delete-btn" />
                        </a-popconfirm>
                      </div>
                      <div class="att-row-meta">
                        <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                      </div>
                    </div>
                    <div class="file-count-hint" v-if="record.attachments.length > 1">共 {{ record.attachments.length }} 份文件</div>
                  </template>
                  <template v-else>
                    <div class="file-cell file-empty"><CloudUploadOutlined class="file-icon" /><span>尚未上传</span></div>
                  </template>
                </div>
              </template>
            </template>
          </a-table>
        </a-spin>
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

    <!-- 文件预览弹窗 -->
    <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />

    <!-- 20%拆分产品设置弹窗 -->
    <InvoiceSplitModal ref="invoiceSplitModalRef" :form-id="formId!" :calc-detail="invoiceAmountCalcDetail" :readonly="!hasFinancePermission" @confirm="handleSplitConfirm" />

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { message, Modal, Textarea } from 'ant-design-vue'
import { checkPermission } from '@/directives/permission'
import type { SelectValue } from 'ant-design-vue/lib/select';
import {
  PlusOutlined,
  UploadOutlined,
  HistoryOutlined,
  EnvironmentOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  CloseOutlined,
  FormOutlined,
  FileTextOutlined,
  FileDoneOutlined,
  UserOutlined,
  ClockCircleOutlined,
  CloudUploadOutlined,
  RollbackOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SaveOutlined,
  SendOutlined,
  CheckOutlined,
  CalculatorOutlined,
  ReloadOutlined,
  LinkOutlined,
  DownloadOutlined,
  RiseOutlined,
  FallOutlined
} from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'

import {
  getDeclarationDetail, 
  addDeclaration, 
  updateDeclaration, 
  uploadFile, 
  saveDraft, 
  deleteDeclaration,
  submitDeclaration,
  auditDeclaration,
  getActiveTasks,
  auditReturnToDraft,
  getReturnAuditHistory,
  
  exportInvoicePackage,
  getInvoiceSplitItems,
  // 业务发票 API 已废弃，统一使用资料项 INVOICE 环节
} from '@/api/business/declaration'
import {
  getMaterialItems,
  addMaterialItem,
  updateMaterialItem,
  deleteMaterialItem,
  uploadMaterialFile,
  clearMaterialFile,
  deleteMaterialAttachment,
  updateMaterialAttachment,
  ensureMaterialItem,
  submitMaterial,
  submitInvoice,
  auditMaterial,
  auditInvoice,
  parseInvoicePdf,
  submitSupplement,
  auditSupplement,
  submitInvoiceAmount,
  auditInvoiceAmount,
  getInvoiceAmountDetail,
  type MaterialItem,
  type MaterialAttachment
} from '@/api/business/materialItem'
import { getRemittancesByFormId } from '@/api/business/remittance'
import {
  MATERIAL_STAGES,
  type MaterialStage
} from '@/api/system/materialTemplate'
import { getProductTypes } from '@/api/system/product'
import { getEnabledTransportModes } from '@/api/system/transportMode'
import { getEnabledPaymentMethods } from '@/api/system/paymentMethod'
import { getEnabledCountries } from '@/api/system'
import { getEnabledCurrencies } from '@/api/system/currency'
import { getActiveMeasurementUnits, type MeasurementUnit } from '@/api/system/measurement-unit'
import { getCitiesByCountry } from '@/api/system/city-info'
import {  findUnitByCode } from '@/utils/measurement-unit'
import { getEnabledEntityConfigs, type EntityConfig } from '@/api/system/entityConfig'
import { getAllEnabledCustomers, type CustomerConfig } from '@/api/system/customerConfig'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import InvoiceSplitModal from './InvoiceSplitModal.vue'

// 文件预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFile = (url: string) => { if (url) { previewUrl.value = url; previewVisible.value = true } }

// 文件预览 URL 生成函数
const FILE_DOWNLOAD_URL = '/api/v1/files/download'
const getFilePreviewUrl = (id: number | string) => `${FILE_DOWNLOAD_URL}?id=${id}`

const route = useRoute()
const router = useRouter()

// 页面状态
const isAudit = ref(route.query.mode === 'audit')
const isPaymentMode = ref(route.query.mode === 'payment') // 水单提交模式
const isMaterialMode = ref(route.query.mode === 'material') // 资料提交/查看模式
const isMaterialAuditMode = ref(route.query.mode === 'materialAudit') // 资料审核模式
const isInvoiceAuditMode = ref(route.query.mode === 'invoiceAudit') // 发票审核模式
const isInvoiceUploadMode = ref(route.query.mode === 'invoiceUpload') // 发票上传模式
const isSupplementMode = ref(route.query.mode === 'supplement') // 补充资料提交模式
const isSupplementAuditMode = ref(route.query.mode === 'supplementAudit') // 补充资料审核模式
const isInvoiceAmountMode = ref(route.query.mode === 'invoiceAmount') // 申请开票金额提交模式
const isInvoiceAmountAuditMode = ref(route.query.mode === 'invoiceAmountAudit') // 开票金额审核模式
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(route.query.status ? Number(route.query.status) : null)
const submitting = ref(false)

// 主体配置
const entityList = ref<EntityConfig[]>([])
const loadEntityList = async () => {
  try {
    const response = await getEnabledEntityConfigs()
    if (response.data?.code === 200) {
      entityList.value = response.data.data || []
    }
  } catch (error) {
    // ignore
  }
  // 加载完主体列表后，如果 entityId 未设置，根据 shipperCompany 自动匹配
  autoMatchEntity()
}

// 根据发货公司名称自动匹配主体
const autoMatchEntity = () => {
  if (formData.entityId) return // 已有主体ID，不覆盖
  if (!formData.shipperCompany || entityList.value.length === 0) return
  const entity = entityList.value.find(
    e => e.entityName === formData.shipperCompany || e.entityNameCn === formData.shipperCompany
  )
  if (entity) {
    formData.entityId = entity.id
    if (!formData.shipperAddress && entity.entityAddress) {
      formData.shipperAddress = entity.entityAddress
    }
  }
}

// 选择主体后自动填充发货人信息
const handleCompanyChange = (companyName: any) => {
  if (!companyName) {
    formData.entityId = undefined
    formData.shipperAddress = ''
    return
  }
  const entity = entityList.value.find(e => e.entityName === companyName)
  if (entity) {
    formData.entityId = entity.id
    formData.shipperAddress = entity.entityAddress || ''
  } else {
    formData.entityId = undefined
  }
}

// 过滤公司选项（支持英文名和中文名搜索）
const filterCompanyOption = (input: string, option: any) => {
  const label = option.children?.()[0]?.children || ''
  const lowerInput = input.toLowerCase()
  // 通过 value (entityName) 匹配
  if (option.value && String(option.value).toLowerCase().includes(lowerInput)) return true
  // 通过 label 文本匹配
  if (typeof label === 'string' && label.toLowerCase().includes(lowerInput)) return true
  return false
}
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

// 基本信息是否只读（审核模式、查看模式、水单提交模式、资料模式、资料审核模式、发票上传模式都只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value || isMaterialMode.value || isMaterialAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value || isSupplementMode.value || isSupplementAuditMode.value || isInvoiceAmountMode.value || isInvoiceAmountAuditMode.value)

// 当前审核阶段（从 URL taskKey 中获取）

// 获取当前审核阶段文本
const getAuditActionText = () => {
  if (formStatus.value === 11) return '退回'
  return '审核'
}

// 获取业务类型文本
const getBusinessTypeText = (type: string) => {
  const map: Record<string, string> = {
    'DECLARATION_RETURN': '退回草稿',
    'DECLARATION_AUDIT': '申报审核',
    'REMittance_AUDIT': '水单审核',
    'DELIVERY_ORDER_AUDIT': '提货单审核',
    'DECLARATION_SUBMIT': '申报提交',
    'DECLARATION_MATERIAL_AUDIT': '资料审核',
    'DECLARATION_SUPPLEMENT_AUDIT': '补充资料审核',
    'DECLARATION_INVOICE_AMOUNT_AUDIT': '开票金额审核',
    'DECLARATION_INVOICE_AUDIT': '业务发票审核'
  }
  return map[type] || type
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待初审',
    2: '待资料提交',
    3: '待资料审核',
    4: '待补充资料提交',
    5: '待补充资料审核',
    6: '待开票金额提交',
    7: '待开票金额审核',
    8: '待发票提交',
    9: '待发票审核',
    10: '已完成',
    11: '退回待审'
  }
  return statusMap[status] || '未知'
}

// 申报要素弹窗相关变量
const elementsModalVisible = ref(false)
const currentProductForElements = ref<any>(null)
const currentElementValues = ref<any[]>([])
const elementsLoading = ref(false)

// 审核通过
const handleApprove = async () => {
  if (!formId.value) return
  
  // 显示审核意见输入框
  const remark = await showRemarkModal('通过', '已核对数据，通过')
  if (!remark) return // 用户取消
  
  submitting.value = true
  try {
    if (formStatus.value === 11) {
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
    if (formStatus.value === 11) {
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


// ========================================
// 申报资料相关逻辑（从 MaterialSubmitModal 迁移）
// ========================================

interface MaterialSchemaField {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'select'
  required?: boolean
  options?: string[]
}

const MATERIAL_FIXED_KEYS = ['amount', 'currency', 'invoiceNo', 'invoiceDate']

const materialItems = ref<MaterialItem[]>([])
const materialLoading = ref(false)
const materialExpandedKeys = ref<(number | string)[]>([])
const materialRowModalVisible = ref(false)
const materialRowSaving = ref(false)
const materialRowEditingId = ref<number | string | null>(null)
const materialRowForm = reactive<Partial<MaterialItem>>({
  name: '',
  required: 1,
  sort: 0,
  remark: ''
})

const materialRowKey = (record: MaterialItem) => (record.id ?? `tpl-${record.templateId}`) as any

// ---------- 发票 PDF 金额解析状态 ----------
// 资料项 key -> 解析提示信息（用于展示失败原因 / 跳过解析的原因）
const materialPdfMessages = reactive<Record<string, { type: 'success' | 'warn' | 'info'; text: string }>>({})
// 已解析过的文件指纹缓存，避免同一文件重复上传解析

/** 资料项是否为发票类（根据数据库 invoiceMode 字段判断） */
const isInvoiceMaterial = (item: MaterialItem): boolean =>
  item.invoiceMode === 1

/** 附件文件名显示：UUID 类文件名截短展示 */
const displayAttFileName = (att: MaterialAttachment): string => {
  return att.fileName || '查看附件'
}

/** 生成文件指纹，用于去重解析 */
const buildFileSignature = (file: File): string => `${file.name}|${file.size}|${file.lastModified}`

interface ParsedCache {
  amount: number | null
  invoiceNo: string | null
  invoiceDate: string | null
  message: { type: 'success' | 'warn' | 'info'; text: string } | null
}
const parsedFileSignatures = new Map<string, ParsedCache>()

/**
 * 尝试解析发票 PDF 的金额，并自动回填到对应附件的结构化字段。
 * 非 PDF / 非发票类资料项，直接跳过。
 */
const tryParseInvoicePdf = async (file: File, record: MaterialItem) => {
  // 非发票类资料项，不解析
  if (!isInvoiceMaterial(record)) return
  // 非 PDF：提示 + 跳过
  if (file.type !== 'application/pdf') {
    const key = materialRowKey(record)
    materialPdfMessages[key] = { type: 'info', text: '图片类发票暂不支持自动识别金额，请手动核对' }
    return
  }
  const signature = buildFileSignature(file)
  let parsedAmt: number | null = null
  let parsedInvoiceNo: string | null = null
  let parsedInvoiceDate: string | null = null
  let parsedMsg: { type: 'success' | 'warn' | 'info'; text: string } | null = null

  const cached = parsedFileSignatures.get(signature)
  if (cached) {
    parsedAmt = cached.amount
    parsedInvoiceNo = cached.invoiceNo
    parsedInvoiceDate = cached.invoiceDate
    parsedMsg = cached.message
  } else {
    try {
      const res: any = await parseInvoicePdf(file)
      const data = res?.data?.data
      if (data?.success && data.amount != null) {
        parsedAmt = Number(data.amount)
        parsedMsg = { type: 'success', text: `PDF 识别金额：¥${parsedAmt.toFixed(2)}` }
      } else {
        const errText = data?.errorMsg || 'PDF 金额识别失败，请确保手动填写金额正确'
        parsedMsg = { type: 'warn', text: errText }
      }
      // 提取发票号和开票日期（无论金额是否成功都尝试）
      if (data?.invoiceNo) parsedInvoiceNo = data.invoiceNo
      if (data?.invoiceDate) parsedInvoiceDate = data.invoiceDate
    } catch (e) {
      parsedMsg = { type: 'warn', text: 'PDF 解析请求失败，请手动核对金额' }
    }
    parsedFileSignatures.set(signature, { amount: parsedAmt, invoiceNo: parsedInvoiceNo, invoiceDate: parsedInvoiceDate, message: parsedMsg })
  }

  // ---------- 自动回填发票号和开票日期（如果空就回填）----------
  if (parsedInvoiceNo || parsedInvoiceDate) {
    const currentRecord2 = materialItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
    if (currentRecord2?.attachments?.length) {
      const latestAtt2 = currentRecord2.attachments[0]
      const patchNoDate: Partial<import('@/api/business/materialItem').MaterialAttachment> = {}
      if (parsedInvoiceNo && !latestAtt2.invoiceNo) {
        latestAtt2.invoiceNo = parsedInvoiceNo
        patchNoDate.invoiceNo = parsedInvoiceNo
      }
      if (parsedInvoiceDate && !latestAtt2.invoiceDate) {
        latestAtt2.invoiceDate = parsedInvoiceDate
        patchNoDate.invoiceDate = parsedInvoiceDate
      }
      if (Object.keys(patchNoDate).length > 0 && currentRecord2.id && latestAtt2.id) {
        updateMaterialAttachment(currentRecord2.id!, latestAtt2.id, patchNoDate).catch(() => {})
      }
    }
  }

  // ---------- 自动回填金额到对应附件 ----------
  if (parsedAmt == null || parsedAmt <= 0) return
  // 重新获取最新资料项（loadMaterialItems 后数组已刷新）
  const currentRecord = materialItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
  if (!currentRecord || !currentRecord.attachments?.length) return
  // 找到最新上传的附件（后端按 createTime DESC 排序，第一个是最新的）
  const latestAtt = currentRecord.attachments[0]
  if (!latestAtt) return

  const currentAttAmt = Number(latestAtt.amount ?? 0)
  if (currentAttAmt <= 0) {
    // 用户尚未填写金额 → 自动回填并持久化
    latestAtt.amount = parsedAmt
    try {
      await updateMaterialAttachment(currentRecord.id!, latestAtt.id, { amount: parsedAmt })
      message.success(`已自动填入 PDF 识别金额 ¥${parsedAmt.toFixed(2)}`)
    } catch (e) {
      // 保存失败时仅保留解析提示
    }
  } else if (Math.abs(currentAttAmt - parsedAmt) > 0.009) {
    // 用户已填且与 PDF 不一致 → 提示
    materialPdfMessages[materialRowKey(record)] = {
      type: 'warn',
      text: `PDF 识别 ¥${parsedAmt.toFixed(2)}，与填写 ¥${currentAttAmt.toFixed(2)} 不一致`
    }
  } else {
    materialPdfMessages[materialRowKey(record)] = {
      type: 'success',
      text: `PDF 识别 ¥${parsedAmt.toFixed(2)}，与填写一致`
    }
  }
  if (parsedMsg) materialPdfMessages[materialRowKey(record)] = parsedMsg
}

const materialColumns = [
  { title: '资料项', key: 'name', dataIndex: 'name' }
]

/** 核心资料项（排除补充资料和发票环节） */
const coreMaterialItems = computed(() =>
  materialItems.value.filter(i => {
    const stage = getItemStage(i)
    return stage !== 'SUPPLEMENT' && stage !== 'INVOICE'
  })
)
const materialRequiredCount = computed(() => coreMaterialItems.value.filter((i) => i.required === 1).length)
const materialUploadedCount = computed(() =>
  coreMaterialItems.value.filter((i) => i.required === 1 && i.status === 1).length
)
const materialProgressPercent = computed(() => {
  if (materialRequiredCount.value === 0) return coreMaterialItems.value.length === 0 ? 0 : 100
  return Math.round((materialUploadedCount.value / materialRequiredCount.value) * 100)
})

// ---------- 按环节（stage）分组资料项 ----------
const DEFAULT_STAGE: MaterialStage = 'MATERIAL_SUBMIT'
const getItemStage = (item: MaterialItem): MaterialStage =>
  (item.stage as MaterialStage) || DEFAULT_STAGE

const getStageItems = (stage: MaterialStage) =>
  materialItems.value.filter((i) => getItemStage(i) === stage)

const activeStageTab = ref<string>(DEFAULT_STAGE)

/** 当前激活环节对应的资料项 */
const activeStageItems = computed(() =>
  getStageItems(activeStageTab.value as MaterialStage)
)

/** 有资料项的环节列表（控制 tab 显示） */
const availableStages = computed(() =>
  MATERIAL_STAGES.filter((s) => s.value !== 'SUPPLEMENT' && s.value !== 'INVOICE' && getStageItems(s.value as MaterialStage).length > 0)
)

/** 每个环节的进度统计 */
const stageStats = computed(() => {
  const map: Record<string, { total: number; required: number; uploaded: number }> = {}
  for (const s of MATERIAL_STAGES) {
    const items = getStageItems(s.value as MaterialStage)
    const req = items.filter((i) => i.required === 1).length
    const upl = items.filter((i) => i.required === 1 && i.status === 1).length
    map[s.value] = { total: items.length, required: req, uploaded: upl }
  }
  return map
})

// 资料模式下只读：URL readonly=true 或 状态大于 2 (资料提交后)
const isMaterialReadonly = computed(() => {
  if (route.query.readonly === 'true') return true
  if (formStatus.value != null && formStatus.value > 2) return true
  return false
})
// 资料模块可编辑条件：
// 1. 资料提交模式 + 状态=2 + 非只读 → MATERIAL_SUBMIT 环节可编辑
// 2. 发票上传模式 + 状态=4 + 非只读 + INVOICE 环节标签页 → 业务发票可编辑
const isMaterialEditable = computed(() => {
  if (isMaterialReadonly.value) return false
  if (isMaterialMode.value && formStatus.value === 2) return true
  return false
})


/** 补充资料环节资料项 */
const supplementItems = computed(() => getStageItems('SUPPLEMENT'))

/** 业务发票环节资料项 */
const invoiceStageItems = computed(() => getStageItems('INVOICE'))

/** 补充资料审核通过之后（业务 status > 5，即进入开票金额及后续环节） */
const isAfterSupplementStage = computed(() => {
  const s = formStatus.value
  return s != null && s > 5
})

/** 已进入补充资料之后的只读查阅模式（开票金额、发票提交等） */
const isPostSupplementReadonlyMode = computed(() =>
  isAfterSupplementStage.value
  || isInvoiceAmountMode.value
  || isInvoiceAmountAuditMode.value
  || isInvoiceUploadMode.value
  || isInvoiceAuditMode.value
)

/** 补充资料区域是否显示（补充资料流程时显示） */
const showSupplementSection = computed(() => {
  if (supplementItems.value.length === 0) return false
  if (isSupplementMode.value && formStatus.value === 4) return true
  if (isSupplementAuditMode.value && formStatus.value === 5) return true
  if (isMaterialMode.value && formStatus.value != null && formStatus.value >= 4) return true
  if (isMaterialAuditMode.value && formStatus.value != null && formStatus.value >= 5) return true
  if (isPostSupplementReadonlyMode.value && formStatus.value != null && formStatus.value >= 4) {
    return true
  }
  return false
})

/** 业务发票区域是否显示（发票环节进行中、已完成查阅均展示） */
const showInvoiceSection = computed(() => {
  if (invoiceStageItems.value.length === 0) return false
  const s = formStatus.value
  if (s == null) return false
  // 发票上传/审核入口
  if (isInvoiceUploadMode.value && (s === 8 || s === 9)) return true
  if (isInvoiceAuditMode.value && s === 9) return true
  // 待提交/待审核（不限 mode，与 canSubmitInvoice / canAuditInvoice 一致）
  if (canSubmitInvoice.value || canAuditInvoice.value) return true
  // 已进入发票环节及之后：只读查阅（含 status=10 已完成）
  if (s >= 8 && isPostSupplementReadonlyMode.value) return true
  if (s >= 8 && route.query.readonly === 'true') return true
  if (s >= 10) return true
  return false
})

/** 补充资料进度统计 */
const supplementStats = computed(() => {
  const items = supplementItems.value
  const required = items.filter(i => i.required === 1).length
  const uploaded = items.filter(i => i.required === 1 && i.status === 1).length
  return { total: items.length, required, uploaded }
})

/** 业务发票进度统计 */
const invoiceStats = computed(() => {
  const items = invoiceStageItems.value
  const required = items.filter(i => i.required === 1).length
  const uploaded = items.filter(i => i.required === 1 && i.status === 1).length
  return { total: items.length, required, uploaded }
})

/** 状态=4 时可提交补充资料（非审核/只读场景） */
const canSubmitSupplement = computed(() => {
  if (formStatus.value !== 4) return false
  if (route.query.readonly === 'true') return false
  if (isSupplementAuditMode.value || isMaterialAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=5 时可审核补充资料 */
const canAuditSupplement = computed(() => {
  if (formStatus.value !== 5) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value) return false
  return true
})

/** 补充资料可编辑条件：与可提交一致 */
const isSupplementEditable = computed(() => canSubmitSupplement.value)

/** 状态=6 时可提交开票金额（不限 mode，与补充资料提交逻辑一致；自用申报跳过） */
const canSubmitInvoiceAmount = computed(() => {
  if (formData.declarationType === 'SELF') return false
  if (formStatus.value !== 6) return false
  if (route.query.readonly === 'true') return false
  if (isInvoiceAmountAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value) return false
  if (isMaterialAuditMode.value || isSupplementAuditMode.value || isAudit.value) return false
  // 仅资料/补充/开票金额等业务入口，不因 URL 上残留的 mode 拦截
  return true
})

/** 状态=7 时可审核开票金额（自用申报跳过） */
const canAuditInvoiceAmount = computed(() => {
  if (formData.declarationType === 'SELF') return false
  if (formStatus.value !== 7) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value || isSupplementMode.value) return false
  return true
})

/** 申请开票金额区域：补充资料审过后（status > 5）一律展示；自用申报隐藏 */
const showInvoiceAmountSection = computed(() => formData.declarationType !== 'SELF' && isAfterSupplementStage.value)

/** 申请开票金额可编辑（刷新计算、提交前确认） */
const isInvoiceAmountEditable = computed(() => canSubmitInvoiceAmount.value)

/** 开票金额计算详情数据 */
const invoiceAmountCalcDetail = ref<Record<string, any> | null>(null)
const invoiceAmountLoading = ref(false)

/** 金额格式化 */
const fmtAmt = (v: any) => {
  if (v == null || v === '') return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
/** 支出合计 */
const calcExpenseTotal = computed(() => {
  const d = invoiceAmountCalcDetail.value
  if (!d) return 0
  return Number(d.totalInvoiceDeduction || 0) + Number(d.bankFeeAmount || 0) + Number(d.internalBankFee || 0)
})
/** 关联水单列表 */
const invoiceAmountRemittances = ref<any[]>([])
const remittanceColumns = [
  { title: '水单编号', dataIndex: 'remittanceNo', key: 'remittanceNo', width: 140 },
  { title: '收汇名称', dataIndex: 'remittanceName', key: 'remittanceName', width: 120, ellipsis: true },
  { title: '收汇金额', key: 'remittanceAmount', width: 130, customRender: ({ record }: any) => {
    const c = record.currency || 'USD'
    const sym = c === 'CNY' ? '¥' : c === 'USD' ? '$' : c
    return `${sym}${Number(record.remittanceAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '关联金额', key: 'relationAmount', width: 130, customRender: ({ record }: any) => {
    const c = record.currency || 'USD'
    const sym = c === 'CNY' ? '¥' : c === 'USD' ? '$' : c
    return `${sym}${Number(record.relationAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '汇率', dataIndex: 'taxRate', key: 'taxRate', width: 80, customRender: ({ text }: any) => text != null ? Number(text).toFixed(4) : '-' },
  { title: '收汇日期', dataIndex: 'remittanceDate', key: 'remittanceDate', width: 110, customRender: ({ text }: any) => text ? String(text).split(' ')[0] : '-' },
  { title: '状态', key: 'status', width: 80, customRender: ({ text }: any) => text === 0 ? '草稿' : text === 1 ? '待审核' : '已审核' }
]

/** 状态=8 时可提交业务发票（不限 mode） */
const canSubmitInvoice = computed(() => {
  if (formStatus.value !== 8) return false
  if (route.query.readonly === 'true') return false
  if (isInvoiceAuditMode.value || isMaterialAuditMode.value || isSupplementAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=9 时可审核业务发票 */
const canAuditInvoice = computed(() => {
  if (formStatus.value !== 9) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value || isSupplementMode.value) return false
  return true
})

/** 业务发票可编辑条件：状态=8 且可提交 */
const isInvoiceEditable = computed(() => canSubmitInvoice.value)

const parseMaterialSchema = (schema?: string | null): MaterialSchemaField[] => {
  if (!schema) return []
  try {
    const arr = JSON.parse(schema)
    return Array.isArray(arr) ? (arr as MaterialSchemaField[]) : []
  } catch {
    return []
  }
}

const getMaterialFieldValue = (record: MaterialItem, key: string): any => {
  if (MATERIAL_FIXED_KEYS.includes(key)) return (record as any)[key]
  if (!record.extraData) return undefined
  try {
    const obj = JSON.parse(record.extraData)
    return obj[key]
  } catch {
    return undefined
  }
}

const setMaterialFieldValue = (record: MaterialItem, key: string, val: any) => {
  if (MATERIAL_FIXED_KEYS.includes(key)) {
    ;(record as any)[key] = val == null || val === '' ? null : val
    return
  }
  let obj: any = {}
  if (record.extraData) {
    try {
      obj = JSON.parse(record.extraData)
    } catch {
      obj = {}
    }
  }
  if (val == null || val === '') delete obj[key]
  else obj[key] = val
  record.extraData = Object.keys(obj).length ? JSON.stringify(obj) : null
}

const loadMaterialItems = async () => {
  if (!formId.value) return
  try {
    materialLoading.value = true
    const res = await getMaterialItems(formId.value)
    if (res.data?.code === 200) {
      materialItems.value = (res.data.data || []).slice().sort(
        (a: MaterialItem, b: MaterialItem) => (a.sort ?? 0) - (b.sort ?? 0)
      )
      materialExpandedKeys.value = materialItems.value
        .filter((i) => parseMaterialSchema(i.formSchema).length > 0)
        .map((i) => (i.id ?? `tpl-${i.templateId}`) as any)
    }
  } catch (e) {
    message.error('加载资料项失败')
  } finally {
    materialLoading.value = false
  }
}

const saveMaterialRowFields = async (record: MaterialItem) => {
  if (!isMaterialEditable.value) return
  try {
    // 虚拟项（id 为空）先升格为真实记录
    const id = await resolveMaterialItemId(record)
    if (!id) return
    const res = await updateMaterialItem({
      id,
      name: record.name,
      required: record.required,
      sort: record.sort,
      remark: record.remark,
      amount: record.amount,
      currency: record.currency,
      invoiceNo: record.invoiceNo,
      invoiceDate: record.invoiceDate,
      extraData: record.extraData
    })
    if (res.data?.code === 200) {
      record.id = id
      message.success('已保存')
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    message.error('保存失败')
  }
}

const beforeMaterialUpload = async (file: File, record: MaterialItem) => {
  try {
    const id = await resolveMaterialItemId(record)
    if (!id) return false
    // 同时传 formId + templateId：后端在 id 找不到实例时会自动按模板 ensure 一条再上传
    const res = await uploadMaterialFile(id, file, {
      formId: formId.value,
      templateId: record.templateId ?? null
    })
    if (res.data?.code === 200) {
      if (res.data.data?.id) record.id = res.data.data.id
      message.success('上传成功')
      await loadMaterialItems()
      // 发票类资料项：触发 PDF 金额解析（非阻塞，后台异步执行）
      if (isInvoiceMaterial(record)) {
        tryParseInvoicePdf(file, record).catch(() => { /* 静默处理，已在 tryParseInvoicePdf 内部展示提示 */ })
      }
    } else {
      message.error(res.data?.message || '上传失败')
    }
  } catch (e) {
    message.error('上传失败')
  }
  return false
}

/**
 * 将虚拟项（id=null, templateId 非空）升格为真实记录。返回真实 id。
 * 对手动新增的项（已有 id）直接返回原 id。
 */
const resolveMaterialItemId = async (record: MaterialItem): Promise<number | string | null> => {
  if (record.id) return record.id
  if (!record.templateId || !formId.value) {
    message.error('无法定位资料项模板')
    return null
  }
  try {
    const res = await ensureMaterialItem(formId.value, record.templateId)
    // 调试：记录 ensure 返回，方便定位“资料项不存在”类问题
    // eslint-disable-next-line no-console
    console.log('[material ensure] response =', res?.data, 'templateId =', record.templateId)
    if (res.data?.code === 200 && res.data.data?.id) {
      record.id = res.data.data.id
      return record.id as number
    }
    message.error(res.data?.message || '创建资料项失败')
    return null
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('[material ensure] error', e)
    message.error('创建资料项失败')
    return null
  }
}

const handleClearMaterialFile = async (record: MaterialItem) => {
  try {
    const res = await clearMaterialFile(record.id!)
    if (res.data?.code === 200) {
      message.success('已清除')
      await loadMaterialItems()
    }
  } catch (e) {
    message.error('操作失败')
  }
}

/** 删除单个附件 */
const handleDeleteAttachment = async (record: MaterialItem, att: MaterialAttachment) => {
  try {
    const res = await deleteMaterialAttachment(record.id!, att.id)
    if (res.data?.code === 200) {
      message.success('已删除')
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

/** 保存附件的结构化字段（金额/发票号/开票日期） */
const saveAttachmentField = async (record: MaterialItem, att: MaterialAttachment, field: string, value: any) => {
  if (!record.id || !att.id) return
  // 先更新本地数据
  ;(att as any)[field] = value ?? null
  try {
    const payload: Partial<MaterialAttachment> = { [field]: value ?? null }
    const res = await updateMaterialAttachment(record.id, att.id, payload)
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    message.error('保存失败')
  }
}

const confirmClearMaterialFile = (record: MaterialItem) => {
  Modal.confirm({
    title: '确定清除此附件吗？',
    content: '清除后需重新上传。',
    okText: '确定清除',
    okButtonProps: { danger: true },
    onOk: () => handleClearMaterialFile(record)
  })
}

const confirmDeleteMaterialRow = (record: MaterialItem) => {
  Modal.confirm({
    title: '确定删除此自定义资料项？',
    content: '删除后不可恢复。',
    okText: '确定删除',
    okButtonProps: { danger: true },
    onOk: () => handleDeleteMaterialRow(record)
  })
}

const handleDeleteMaterialRow = async (record: MaterialItem) => {
  try {
    const res = await deleteMaterialItem(record.id!)
    if (res.data?.code === 200) {
      message.success('删除成功')
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

const resetMaterialRowForm = () => {
  materialRowForm.name = ''
  materialRowForm.required = 1
  materialRowForm.sort = 0
  materialRowForm.remark = ''
}

const openAddMaterialRow = () => {
  materialRowEditingId.value = null
  resetMaterialRowForm()
  materialRowModalVisible.value = true
}

const openEditMaterialRow = async (record: MaterialItem) => {
  // 虚拟项先升格为真实记录，才能在单据内覆盖模板的名称/说明
  let editingId: number | string | null = record.id ?? null
  if (!editingId) {
    const id = await resolveMaterialItemId(record)
    if (!id) return
    editingId = id
  }
  materialRowEditingId.value = editingId
  materialRowForm.name = record.name
  materialRowForm.required = record.required
  materialRowForm.sort = record.sort
  materialRowForm.remark = record.remark
  materialRowModalVisible.value = true
}

const handleSaveMaterialRow = async () => {
  if (!materialRowForm.name?.trim()) {
    message.warning('请输入资料名称')
    return
  }
  try {
    materialRowSaving.value = true
    let res
    if (materialRowEditingId.value) {
      res = await updateMaterialItem({ ...materialRowForm, id: materialRowEditingId.value })
    } else {
      res = await addMaterialItem({ ...materialRowForm, formId: formId.value! })
    }
    if (res.data?.code === 200) {
      message.success('保存成功')
      materialRowModalVisible.value = false
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    message.error('保存失败')
  } finally {
    materialRowSaving.value = false
  }
}

const validateMaterialSchemaFields = (): string | null => {
  for (const item of materialItems.value) {
    // 跳过补充资料和发票阶段
    const stage = getItemStage(item)
    if (stage === 'SUPPLEMENT' || stage === 'INVOICE') continue
    const schema = parseMaterialSchema(item.formSchema)
    if (!schema.length) continue
    const isInvoice = isInvoiceMaterial(item)
    for (const f of schema) {
      if (!f.required) continue
      // 发票类资料项的固定字段已移至附件级别，跳过
      if (isInvoice && MATERIAL_FIXED_KEYS.includes(f.key)) continue
      const v = getMaterialFieldValue(item, f.key)
      if (v == null || v === '') {
        return `资料「${item.name}」的「${f.label}」为必填项`
      }
    }
  }
  return null
}

const handleSubmitMaterial = async () => {
  if (!formId.value) return
  // 只校验资料提交阶段的项，不包含补充资料和发票阶段
  const submitItems = materialItems.value.filter((i) => {
    const stage = getItemStage(i)
    return stage !== 'SUPPLEMENT' && stage !== 'INVOICE'
  })
  const missing = submitItems.filter((i) => i.required === 1 && i.status !== 1)

  const schemaMissing = validateMaterialSchemaFields()
  if (schemaMissing) {
    message.warning(schemaMissing)
    return
  }

  // 检查发票类资料项每个附件是否填写了金额和发票号
  const invoiceFieldMissing: string[] = []
  for (const item of submitItems) {
    if (!isInvoiceMaterial(item)) continue
    if (!item.attachments?.length) continue
    for (let i = 0; i < item.attachments.length; i++) {
      const att = item.attachments[i]
      if (att.amount == null || Number(att.amount) < 0) {
        invoiceFieldMissing.push(`「${item.name}」第${i + 1}份附件未填写金额`)
      }
      if (!att.invoiceNo) {
        invoiceFieldMissing.push(`「${item.name}」第${i + 1}份附件未填写发票号`)
      }
    }
  }
  if (invoiceFieldMissing.length > 0) {
    message.warning(invoiceFieldMissing.join('、'))
    return
  }

  const doSubmit = async (skipRequiredCheck: boolean) => {
    try {
      submitting.value = true
      const res = await submitMaterial(formId.value!, skipRequiredCheck)
      if (res.data?.code === 200) {
        message.success(skipRequiredCheck ? '资料已提交，等待豁免审核' : '资料提交成功，等待审核')
        goBack()
      } else {
        message.error(res.data?.message || '提交失败')
      }
    } catch (e) {
      message.error('提交失败')
    } finally {
      submitting.value = false
    }
  }

  if (missing.length > 0) {
    Modal.confirm({
      title: `还有 ${missing.length} 项必填资料未上传`,
      content: `缺失项：${missing.map((m) => m.name).join('、')}。\n确认提交？系统将创建豁免审批流程，审核通过后主流程继续。`,
      okText: '确认提交',
      cancelText: '取消',
      onOk: () => doSubmit(true)
    })
    return
  }

  Modal.confirm({
    title: '确认提交资料审核？',
    content: '提交后将进入资料审核流程，无法修改。',
    okText: '确认提交',
    onOk: () => doSubmit(false)
  })
}

// 资料审核通过
const handleMaterialAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入补充资料提交阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditMaterial({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('资料审核已通过')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 资料审核驳回
const handleMaterialAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交资料。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditMaterial({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交资料')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 补充资料提交/审核逻辑
// ========================================

const handleSubmitSupplement = async () => {
  if (!formId.value) return
  const missing = supplementItems.value.filter((i) => i.required === 1 && i.status !== 1)
  if (missing.length > 0) {
    message.warning(`还有 ${missing.length} 项必填补充资料未上传：${missing.map((m) => m.name).join('、')}`)
    return
  }
  Modal.confirm({
    title: '确认提交补充资料审核？',
    content: '提交后将进入补充资料审核流程，无法修改。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitSupplement(formId.value!)
        if (res.data?.code === 200) {
          message.success('补充资料提交成功，等待审核')
          goBack()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e) {
        message.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSupplementAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过补充资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入申请开票金额阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditSupplement({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('补充资料审核已通过')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSupplementAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回补充资料？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交补充资料。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditSupplement({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交补充资料')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 申请开票金额提交/审核逻辑
// ========================================

const loadInvoiceAmountDetail = async () => {
  if (!formId.value) return
  invoiceAmountLoading.value = true
  try {
    // 并发加载计算详情和水单列表
    const [calcRes, remRes] = await Promise.all([
      getInvoiceAmountDetail(formId.value!),
      getRemittancesByFormId(formId.value!)
    ])
    if (calcRes.data?.code === 200) {
      invoiceAmountCalcDetail.value = calcRes.data.data
    } else {
      invoiceAmountCalcDetail.value = null
    }
    if (remRes.data?.code === 200) {
      invoiceAmountRemittances.value = remRes.data.data || []
    } else {
      invoiceAmountRemittances.value = []
    }
  } catch (e) {
    message.error('加载开票金额详情失败')
  } finally {
    invoiceAmountLoading.value = false
  }
}

// 20%拆分弹窗
const invoiceSplitModalRef = ref<InstanceType<typeof InvoiceSplitModal> | null>(null)

/** 是否有财务权限（可编辑20%数据） */
const hasFinancePermission = computed(() => {
  return checkPermission(['business:declaration:finance:supplement'])
})

/** 下载开票文件包(80%+20%) */
const handleDownloadInvoicePackage = async () => {
  if (!formId.value) return

  if (hasFinancePermission.value) {
    // 有财务权限：打开弹窗，可编辑+下载
    invoiceSplitModalRef.value?.open()
  } else {
    // 无财务权限：检查是否已配置20%数据，已配置则直接下载
    try {
      const res = await getInvoiceSplitItems(formId.value)
      const savedItems = res.data?.data
      if (!Array.isArray(savedItems) || savedItems.length === 0) {
        message.warning('请先联系财务人员录入20%产品数据')
        return
      }
    } catch {
      message.warning('请先联系财务人员录入20%产品数据')
      return
    }
    // 直接下载
    await doDownloadInvoicePackage([])
  }
}

/** 执行下载开票文件包 */
const doDownloadInvoicePackage = async (splitItems: any[]) => {
  if (!formId.value) return
  try {
    const res = await exportInvoicePackage(formId.value!, splitItems)
    const downloadUrl = res.data?.data
    if (downloadUrl) {
      window.location.href = downloadUrl
      message.success('开票文件包下载中...')
    } else {
      message.warning('暂无计算数据')
    }
  } catch (e: any) {
    message.error('下载失败: ' + (e.message || '未知错误'))
  }
}

/** 20%弹窗确认回调 */
const handleSplitConfirm = async (splitItems: any[]) => {
  await doDownloadInvoicePackage(splitItems)
}

const handleSubmitInvoiceAmount = async () => {
  if (!formId.value) return
  Modal.confirm({
    title: '确认提交开票金额申请？',
    content: '提交后系统将自动计算开票金额并进入审核流程。请确保：1) 外汇水单已关联 2) 商品退税率已在商品配置中维护（未配置按 0% 计算）。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitInvoiceAmount(formId.value!)
        if (res.data?.code === 200) {
          message.success('开票金额申请已提交，等待审核')
          goBack()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e: any) {
        message.error(e?.message || '提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleInvoiceAmountAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过开票金额审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入业务发票提交阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditInvoiceAmount({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('开票金额审核已通过')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleInvoiceAmountAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回开票金额？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交开票金额申请。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditInvoiceAmount({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交开票金额申请')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 业务发票相关逻辑
// ========================================

// 发票审核通过
const handleInvoiceAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过发票审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入已完成阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditInvoice({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('发票审核已通过')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 发票审核驳回
const handleInvoiceAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回发票审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人需重新上传业务发票。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditInvoice({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交发票')
          goBack()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSubmitInvoice = async () => {
  if (!formId.value) return
  // 检查 INVOICE 阶段的资料项是否已上传附件
  const invoiceItems = getStageItems('INVOICE')
  const hasAttachment = invoiceItems.some(item => item.attachments && item.attachments.length > 0)
  if (invoiceItems.length === 0 || !hasAttachment) {
    message.warning('请至少上传一份业务发票附件后再提交')
    return
  }
  Modal.confirm({
    title: '确认提交发票审核？',
    content: '提交后将进入发票审核流程，无法修改。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitInvoice(formId.value!)
        if (res.data?.code === 200) {
          message.success('发票提交成功，等待审核')
          goBack()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e) {
        message.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 业务发票相关函数已废弃，统一使用资料项 INVOICE 环节

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

// 发票上传/删除/提交函数已废弃，统一使用资料项 INVOICE 环节



const formData = reactive({
  formNo: '',
  entityId: undefined as number | undefined,
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
  currency: undefined as string | undefined,
  declarationDate: undefined as Dayjs | undefined,
  declarationType: 'EXTERNAL' as string,
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

// 常用客户选项
const customerList = ref<CustomerConfig[]>([])
const customerOptions = computed(() =>
  customerList.value.map(c => ({
    value: c.customerName,
    label: c.customerName
  }))
)

// 常用客户筛选
const filterCustomerOption = (input: string, option: any) => {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}

// 选择常用客户后自动填充
const onCustomerSelect = (value: string) => {
  const customer = customerList.value.find(c => c.customerName === value)
  if (customer) {
    formData.consigneeCompany = customer.customerName
    formData.consigneeAddress = customer.customerAddress || ''
    if (customer.destinationCountry) {
      formData.destinationCountry = customer.destinationCountry
    }
    if (customer.tradeCountry) {
      formData.tradeCountry = customer.tradeCountry
    }
  }
}

// 加载常用客户
const loadCustomers = async () => {
  try {
    const response = await getAllEnabledCustomers()
    if (response.data?.code === 200) {
      customerList.value = response.data.data || []
    }
  } catch (error) {
    console.warn('加载常用客户失败', error)
  }
}

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
      // 新建表单时，默认使用配置中的第一个币种
      if (!formId.value && currencyOptions.value.length > 0) {
        formData.currency = currencyOptions.value[0].value
      }
      console.log('加载货币数据成功:', currencyOptions.value)
    } else {
      // 如果API失败，使用默认数据
      currencyOptions.value = [
        { label: 'USD - 美元', value: 'USD' },
        { label: 'EUR - 欧元', value: 'EUR' },
        { label: 'CNY - 人民币', value: 'CNY' }
      ]
      if (!formId.value && currencyOptions.value.length > 0) {
        formData.currency = currencyOptions.value[0].value
      }
    }
  } catch (error) {
    console.warn('加载货币数据失败:', error)
    // 使用默认数据作为后备
    currencyOptions.value = [
      { label: 'USD - 美元', value: 'USD' },
      { label: 'EUR - 欧元', value: 'EUR' },
      { label: 'CNY - 人民币', value: 'CNY' }
    ]
    if (!formId.value && currencyOptions.value.length > 0) {
      formData.currency = currencyOptions.value[0].value
    }
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

// 业务发票已合并至资料模块 INVOICE 环节，以下变量已废弃

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
    typeEnglish: 'CARTONS', // 默认类型
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
  // 智能返回：优先返回上一页，无历史时返回申报录入
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(formData.declarationType === 'SELF' ? '/declaration-self/entry' : '/declaration-external/entry')
  }
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
    
    // 构建箱子产品关联数据（使用 productDetails 中的数量/毛重/净重）
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number, grossWeight?: number | null, netWeight?: number | null}> = []
    cartonList.value.forEach(carton => {
      // 优先使用 productDetails，回退到 selectedProducts
      if (carton.productDetails && carton.productDetails.length > 0) {
        carton.productDetails.forEach((detail: any) => {
          cartonProducts.push({
            cartonId: carton.id,
            productId: detail.productId,
            quantity: detail.quantity || 0,
            grossWeight: detail.grossWeight ?? null,
            netWeight: detail.netWeight ?? null
          })
        })
      } else if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id,
              productId: productId,
              quantity: product.quantity,
              grossWeight: product.grossWeight ?? null,
              netWeight: product.netWeight ?? null
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
      message.error('请选择出发口岸')
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
    
    // 校验箱子产品数量分配不超总量
    // (已取消每箱数量分配功能)
    
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
    
    // 构建箱子产品关联数据（使用 productDetails 中的数量/毛重/净重）
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number, grossWeight?: number | null, netWeight?: number | null}> = []
    cartonList.value.forEach(carton => {
      if (carton.productDetails && carton.productDetails.length > 0) {
        carton.productDetails.forEach((detail: any) => {
          cartonProducts.push({
            cartonId: carton.id,
            productId: detail.productId,
            quantity: detail.quantity || 0,
            grossWeight: detail.grossWeight ?? null,
            netWeight: detail.netWeight ?? null
          })
        })
      } else if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id,
              productId: productId,
              quantity: product.quantity,
              grossWeight: product.grossWeight ?? null,
              netWeight: product.netWeight ?? null
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
        if (submittedStatus >= 1 && submittedStatus <= 9 && formId.value) {
          try {
            const taskRes = await getActiveTasks(formId.value)
            activeTasks.value = taskRes.data?.data || []
            console.log('📋 活跃任务:', activeTasks.value)
          } catch (e) {
            console.warn('获取活跃任务失败', e)
            activeTasks.value = []
          }
        } else {
          activeTasks.value = []
        }
        
        // 只读状态判断：
        // 1. 如果 URL 参数 readonly=true，保持只读
        // 2. 如果是审核模式 (isAudit)，保持只读
        // 3. 如果是水单提交模式、资料模式或发票上传模式，由各自区域内部判断
        // 4. 否则根据状态判断：状态 0/2 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value) {
          isReadonly.value = true
          console.log('查看模式或审核模式, 设置为只读')
        } else if (!isPaymentMode.value && !isMaterialMode.value && !isInvoiceUploadMode.value) {
          const editableStatuses = [0, 2]
          if (!editableStatuses.includes(submittedStatus)) {
            isReadonly.value = true
            console.log('申报单状态=' + submittedStatus + ', 设置为只读模式')
          } else {
            isReadonly.value = false
            console.log('申报单状态=' + submittedStatus + ', 可编辑模式')
          }
        } else if (isMaterialMode.value) {
          // 资料模式：申报单基本信息只读，资料区域根据 readonly 和状态判断
          // readonly 保留为 URL 传入值，资料模块通过 isMaterialEditable 控制可编辑性
          console.log('资料模式：申报单基本信息只读，资料区域按状态判定')
        } else if (isInvoiceUploadMode.value) {
          // 发票上传模式：申报单基本信息只读，但发票区域可操作
          isReadonly.value = true
          console.log('发票上传模式：申报单只读，发票区域可操作')
        } else if (isMaterialAuditMode.value || isInvoiceAuditMode.value) {
          // 审核模式：整个表单只读
          isReadonly.value = true
          console.log('审核模式：申报单只读')
        }
        
        // 填充基本表单数据
        formData.formNo = detailData.formNo || ''
        formData.entityId = detailData.entityId || undefined
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
        formData.currency = detailData.currency || currencyOptions.value[0]?.value || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        formData.declarationType = detailData.declarationType || 'EXTERNAL'
        
        // 如果 entityId 为空，根据发货公司名称自动匹配主体
        autoMatchEntity()
        
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
            // 处理产品照片 - imageId 为 0/"0"/null 均视为无图片
            productPhoto: (product.imageId && String(product.imageId) !== '0')
              ? getFilePreviewUrl(product.imageId) 
              : (product.productPhoto || ''),
            photoFile: (product.imageId && String(product.imageId) !== '0') ? {
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
        

        // 加载申报资料（状态 >=2 资料模块即可浏览/操作）
        if (formId.value && submittedStatus >= 2) {
          await loadMaterialItems()
          // 申报资料区仅展示「资料上传」环节；业务发票在下方独立区域编辑
          const stages = availableStages.value
          if (stages.length > 0) {
            activeStageTab.value = stages[0].value
          } else {
            activeStageTab.value = DEFAULT_STAGE
          }
          scrollToQuerySection()
        }

        // 补充资料审过后（status>5）：任意入口进入都加载开票金额详情（自用申报跳过）
        if (formId.value && submittedStatus > 5 && formData.declarationType !== 'SELF') {
          await loadInvoiceAmountDetail()
          if (route.query.scrollTo === 'invoice-amount') {
            scrollToQuerySection()
          }
        }
                
        // 如果是退回待审状态（status=11），加载最新的退回申请原因
        if (formId.value && submittedStatus === 11) {
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
        
        // 出发口岸始终为中国口岸，cityOptions 已在初始化 loadCities() 中加载为中国城市，
        // 不能用目的国重新加载覆盖（否则出发口岸下拉会变空/失配）

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
//
// // 加载活跃任务
// const loadActiveTasks = async () => {
//   if (!formId.value) return
//   try {
//     const res = await getActiveTasks(formId.value)
//     if (res.data && res.data.code === 200) {
//       activeTasks.value = res.data.data || []
//     }
//   } catch (error) {
//     console.warn('加载活跃任务失败:', error)
//     activeTasks.value = []
//   }
// }

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

watch(
  () => [showInvoiceAmountSection.value, formId.value] as const,
  ([show, id]) => {
    if (show && id) {
      loadInvoiceAmountDetail()
    }
  },
  { immediate: true }
)

/** 根据 URL ?scrollTo= 滚动到对应区块（material / supplement / invoice / invoice-amount） */
const scrollToQuerySection = () => {
  const scrollToSection = route.query.scrollTo as string
  if (!scrollToSection) return
  nextTick(() => {
    setTimeout(() => {
      const element = document.getElementById(`section-${scrollToSection}`)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }, 150)
  })
}

onMounted(() => {
  // 新申报单时根据用户组织类型自动设置申报类型
  const userStore = useUserStore()
  if (!formId.value) {
    formData.declarationType = userStore.orgType === 'INTERNAL' ? 'SELF' : 'EXTERNAL'
  }
  loadData()
  loadCountries()
  loadMeasurementUnits()
  loadEntityList()
  loadCustomers()
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

/* ========== 申报资料模块样式 ========== */
.progress-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #fafcff 100%);
  border: 1px solid #dbe9ff;
  border-radius: 8px;
}
.progress-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.progress-desc {
  font-size: 13px;
  color: #6b7280;
}
.progress-right :deep(.ant-progress-text) {
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.material-table :deep(.ant-table-thead > tr > th) {
  background: #fafbfc;
  font-weight: 600;
  color: #374151;
}
.material-table :deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

.name-cell .name-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
}
.name-cell .name-upload-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}
.name-cell .name-text {
  font-weight: 500;
  color: #111827;
}
.name-cell .name-remark {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-cell .file-icon {
  font-size: 18px;
}
.file-cell.file-uploaded .file-icon {
  color: #52c41a;
}
.file-cell.file-empty {
  color: #9ca3af;
}
.file-cell.file-empty .file-icon {
  color: #d1d5db;
}
.file-cell .file-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.file-cell .file-name {
  color: #1677ff;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-cell .file-time {
  font-size: 11px;
  color: #9ca3af;
}

.schema-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  padding: 10px 16px;
  background: #fafbfc;
  border-left: 3px solid #91caff;
}
.schema-field {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 240px;
}
.schema-label {
  font-size: 13px;
  color: #4b5563;
  white-space: nowrap;
}
.required-star {
  color: #ff4d4f;
  margin-right: 2px;
}
.schema-input {
  min-width: 160px;
  flex: 1;
}
/* 发票 PDF 解析提示包裹（纵向布局：输入框 + 提示） */
.schema-input-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 180px;
}
.schema-input-wrap .schema-input {
  width: 100%;
}
.pdf-amount-hint {
  font-size: 12px;
  line-height: 1.4;
  padding: 3px 8px;
  border-radius: 4px;
  border-left: 3px solid #d9d9d9;
}
.pdf-amount-hint-success {
  color: #389e0d;
  background: #f6ffed;
  border-left-color: #52c41a;
}
.pdf-amount-hint-warn {
  color: #d46b08;
  background: #fff7e6;
  border-left-color: #fa8c16;
}
.pdf-amount-hint-info {
  color: #0958d9;
  background: #e6f4ff;
  border-left-color: #1677ff;
}

/* 多附件展示 */
.file-cell-multi {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-item-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}
.file-icon-sm {
  color: #4f6ef7;
  font-size: 15px;
  flex-shrink: 0;
}
.file-name-sm {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #1677ff;
  font-weight: 500;
}
.file-name-sm:hover {
  color: #4096ff;
  text-decoration: underline;
}
.file-delete-btn {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 12px;
  opacity: 0.6;
  transition: opacity 0.2s;
}
.file-delete-btn:hover {
  opacity: 1;
}
.file-count-hint {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* 发票类附件卡片 */
.att-invoice-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 14px 8px;
  margin-bottom: 8px;
}
.att-row-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.att-file-name {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.att-divider-v {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
  flex-shrink: 0;
}
.att-field-inline {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.att-field-label {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
}
.att-val-tag {
  display: inline-block;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
}
.file-delete-btn {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 13px;
  opacity: 0.45;
  transition: opacity 0.15s;
  margin-left: auto;
  flex-shrink: 0;
}
.file-delete-btn:hover {
  opacity: 1;
}
.att-row-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #f0f0f0;
}
.att-row-meta .anticon {
  font-size: 12px;
  margin-right: 2px;
}
.att-meta-dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  background: #d9d9d9;
  border-radius: 50%;
  margin: 0 4px;
}
.file-meta-inline {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #8c8c8c;
  margin: 0 6px;
  flex-shrink: 0;
}
.meta-icon {
  font-size: 11px;
  color: #bfbfbf;
}

/* 开票金额计算详情 */
.calc-detail-wrap {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}
.calc-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.calc-section:last-of-type {
  border-bottom: none;
}
.calc-section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.calc-income .calc-section-title {
  color: #16a34a;
}
.calc-expense .calc-section-title {
  color: #dc2626;
}
.calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 4px 16px;
  font-size: 13px;
  line-height: 24px;
}
.calc-row.calc-subtotal {
  border-top: 1px dashed #e8e8e8;
  margin-top: 6px;
  padding-top: 8px;
  font-weight: 600;
}
.calc-row.calc-highlight {
  background: #fffbe6;
  border-radius: 4px;
  padding: 4px 8px;
  margin: 4px 0;
}
.calc-label {
  color: #4b5563;
  flex-shrink: 0;
  margin-right: 12px;
}
.calc-value {
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #1f2937;
  text-align: right;
}
.calc-result {
  background: linear-gradient(135deg, #f0f7ff 0%, #fafcff 100%);
  padding: 14px 16px;
}
.calc-result .calc-row {
  padding-left: 0;
  font-size: 14px;
  font-weight: 600;
}
.calc-steps {
  padding: 4px 0;
}
.calc-step-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 3px 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 20px;
}
.calc-step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 11px;
  flex-shrink: 0;
}
.calc-step-text {
  font-family: 'SFMono-Regular', Consolas, monospace;
}
</style>
