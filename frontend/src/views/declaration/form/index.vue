<template>
  <div class="declaration-form-page">
    <a-card :title="(isPickupMode ? '提货单提交' : (isPaymentMode ? (route.query.type === 'balance' ? '尾款水单提交' : '定金水单提交') : '出口申报表单')) ">
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          
          <!-- 审核详情按钮 - 所有状态都显示 -->
          <a-button 
            @click="showAuditHistory" 
            v-permission="['business:declaration:query']"
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
              v-permission="['business:declaration:audit', 'business:declaration:return-audit']"
            >{{ getAuditActionText() }}通过</a-button>
            <a-button 
              danger 
              @click="handleReject" 
              :loading="submitting"
              v-permission="['business:declaration:audit', 'business:declaration:return-audit']"
            >{{ getAuditActionText() }}驳回</a-button>
          </template>
          
          <!-- 水单提交模式下的按钮（非提货单模式才显示水单按钮） -->
          <template v-else-if="isPaymentMode && !isPickupMode">
            <!-- 定金水单提交按钮 -->
            <a-button 
              v-if="(formStatus === 2 || formStatus === 3) && route.query.type == 'deposit'" 
              type="primary" 
              @click="handleSubmitAudit('deposit')" 
              :loading="submitting" 
              v-permission="['business:declaration:payment']"
            >
              提交定金审核
            </a-button>
            
            <!-- 尾款水单提交按钮：只有定金审核通过后才能提交 -->
            <a-button 
              v-if="(formStatus === 4 || formStatus === 5) && route.query.type === 'balance'" 
              type="primary" 
              @click="handleSubmitAudit('balance')" 
              :loading="submitting" 
              v-permission="['business:declaration:payment']"
            >
              提交尾款审核
            </a-button>
          </template>
          
          <!-- 提货单提交模式下的按钮：状态6（待上传提货单）或状态7（提货单待审核）时显示 -->
          <template v-else-if="isPaymentMode && isPickupMode">
            <a-button 
              v-if="(formStatus === 6 || formStatus === 7)" 
              type="primary" 
              @click="handleSubmitAudit('pickup')" 
              :loading="submitting" 
              :disabled="deliveryOrderList.length === 0 || !deliveryOrderList.some(order => order.status === 0)" 
              v-permission="['business:declaration:payment']"
            >
              提交提货单审核
            </a-button>
            
          </template>
          
          
          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 保存草稿按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="handleSaveDraft" :loading="submitting" v-permission="['business:declaration:add']">保存草稿</a-button>
            
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
                @change="() => handleUnitPriceOrQuantityChange(record)"
                style="width: 100%"
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
                @change="() => handleUnitPriceOrQuantityChange(record)"
                style="width: 100%"
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
                  <a-tag v-for="carton in getProductCartonInfo(record)" :key="carton.id" color="blue">
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
                @change="handleAmountChange(record)"
                style="width: 100%"
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
                        <template v-if="!isFormReadonly">
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
                                if (value === '箱子') {
                                  record.typeEnglish = 'CARTONS';
                                } else if (value === '托盘') {
                                  record.typeEnglish = 'PALLETS';
                                }
                              }"
                style="width: 100%"
                size="small"
                placeholder="选择类型"
              >
                <a-select-option value="箱子">箱子</a-select-option>
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
              <a-button v-if="!isFormReadonly && record.id > 0" type="link" danger @click="removeCarton(index)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 定金水单 (初审通过后显示，提货单模式下只读展示) -->
      <a-card v-if="formStatus && formStatus >= 2" title="定金水单" size="small" class="section-card">
        <template #extra>
          <a-button v-if="isDepositEditable" type="primary" size="small" @click="handleAddRemittance(1)">
            <template #icon><PlusOutlined /></template>
            添加定金水单
          </a-button>
        </template>

        <a-table 
          v-if="depositRemittanceList.length > 0"
          :dataSource="depositRemittanceList" 
          :columns="remittanceColumnsNoType" 
          :pagination="false"
          rowKey="tempId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'remittanceName'">
              <a-input v-if="isDepositEditable" v-model:value="record.remittanceName" size="small" placeholder="名称" />
              <span v-else>{{ record.remittanceName }}</span>
            </template>
            <template v-else-if="column.key === 'remittanceDate'">
              <a-date-picker v-if="isDepositEditable" v-model:value="record.remittanceDate" size="small" style="width: 100%" />
              <span v-else>{{ record.remittanceDate?.format?.('YYYY-MM-DD') || record.remittanceDate }}</span>
            </template>
            <template v-else-if="column.key === 'remittanceAmount'">
              <a-input-number v-if="isDepositEditable" v-model:value="record.remittanceAmount" size="small" style="width: 100%" />
              <span v-else>{{ record.remittanceAmount }}</span>
            </template>
            <template v-else-if="column.key === 'exchangeRate'">
              <a-input-number v-if="isDepositEditable" v-model:value="record.exchangeRate" size="small" style="width: 100%" />
              <span v-else>{{ record.exchangeRate }}</span>
            </template>
            <template v-else-if="column.key === 'bankFee'">
              <a-input-number v-if="isDepositEditable" v-model:value="record.bankFee" size="small" style="width: 100%" />
              <span v-else>{{ record.bankFee }}</span>
            </template>
            <template v-else-if="column.key === 'creditedAmount'">
              <a-input-number v-if="isDepositEditable" v-model:value="record.creditedAmount" size="small" style="width: 100%" />
              <span v-else>{{ record.creditedAmount }}</span>
            </template>
            <template v-else-if="column.key === 'photoUrl'">
              <!-- 只读模式 -->
              <div v-if="!isDepositEditable" class="remittance-photo-cell">
                <template v-if="record.photoUrl">
                  <a-image :src="record.photoUrl" :width="48" :height="48" :preview="true" class="remittance-photo" />
                  <a-button type="link" size="small" @click="handleDownloadRemittancePhoto(record)" style="padding: 0; margin-left: 4px;">
                    <DownloadOutlined />
                  </a-button>
                </template>
                <span v-else class="no-photo">无</span>
              </div>
              <!-- 可编辑模式 -->
              <div v-else class="remittance-photo-cell">
                <a-upload
                  v-model:file-list="record.photoFileList"
                  :max-count="1"
                  :before-upload="(file) => beforeRemittancePhotoUploadByRecord(file, record)"
                  @remove="() => handleRemoveRemittancePhotoByRecord(record)"
                  accept="image/*"
                  :show-upload-list="false"
                >
                  <div v-if="record.photoUrl" class="photo-wrapper">
                    <img :src="record.photoUrl" class="remittance-photo" />
                    <div class="photo-overlay">
                      <CameraOutlined style="font-size: 16px;" />
                    </div>
                  </div>
                  <div v-else class="upload-placeholder">
                    <PlusOutlined style="font-size: 14px; color: #999;" />
                  </div>
                </a-upload>
              </div>
            </template>

            <template v-else-if="column.key === 'action'">
              <a-space v-if="isDepositEditable">
                <a-button type="link" size="small" :loading="submitting" @click="handleSaveRemittance(record)">{{ record.id ? '更新' : '保存' }}</a-button>
                <a-popconfirm title="确定要删除这条水单吗？" @confirm="handleDeleteRemittance(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
              <span v-else-if="record.id" style="color: #52c41a;">已保存</span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
        <a-empty v-if="depositRemittanceList.length === 0" description="暂无定金水单" />
      </a-card>

      <!-- 尾款水单 (定金水单审核通过后显示，提货单模式下只读展示，水单提交模式下也显示，审核模式下也显示但定金审核/初审阶段隐藏) -->
      <a-card v-if="formStatus && formStatus !== 1 && !isDepositAuditStage && (formStatus >= 4 || (isPaymentOnlyMode && route.query.type === 'balance') || isAudit)" title="尾款水单" size="small" class="section-card">
        <template #extra>
          <a-button v-if="isBalanceEditable" type="primary" size="small" @click="handleAddRemittance(2)">
            <template #icon><PlusOutlined /></template>
            添加尾款水单
          </a-button>
        </template>

        <a-table 
          v-if="balanceRemittanceList.length > 0"
          :dataSource="balanceRemittanceList" 
          :columns="remittanceColumnsNoType" 
          :pagination="false"
          rowKey="tempId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'remittanceName'">
              <a-input v-if="isBalanceEditable" v-model:value="record.remittanceName" size="small" placeholder="名称" />
              <span v-else>{{ record.remittanceName }}</span>
            </template>
            <template v-else-if="column.key === 'remittanceDate'">
              <a-date-picker v-if="isBalanceEditable" v-model:value="record.remittanceDate" size="small" style="width: 100%" />
              <span v-else>{{ record.remittanceDate?.format?.('YYYY-MM-DD') || record.remittanceDate }}</span>
            </template>
            <template v-else-if="column.key === 'remittanceAmount'">
              <a-input-number v-if="isBalanceEditable" v-model:value="record.remittanceAmount" size="small" style="width: 100%" />
              <span v-else>{{ record.remittanceAmount }}</span>
            </template>
            <template v-else-if="column.key === 'exchangeRate'">
              <a-input-number v-if="isBalanceEditable" v-model:value="record.exchangeRate" size="small" style="width: 100%" />
              <span v-else>{{ record.exchangeRate }}</span>
            </template>
            <template v-else-if="column.key === 'bankFee'">
              <a-input-number v-if="isBalanceEditable" v-model:value="record.bankFee" size="small" style="width: 100%" />
              <span v-else>{{ record.bankFee }}</span>
            </template>
            <template v-else-if="column.key === 'creditedAmount'">
              <a-input-number v-if="isBalanceEditable" v-model:value="record.creditedAmount" size="small" style="width: 100%" />
              <span v-else>{{ record.creditedAmount }}</span>
            </template>
            <template v-else-if="column.key === 'photoUrl'">
              <!-- 只读模式 -->
              <div v-if="!isBalanceEditable" class="remittance-photo-cell">
                <template v-if="record.photoUrl">
                  <a-image :src="record.photoUrl" :width="48" :height="48" :preview="true" class="remittance-photo" />
                  <a-button type="link" size="small" @click="handleDownloadRemittancePhoto(record)" style="padding: 0; margin-left: 4px;">
                    <DownloadOutlined />
                  </a-button>
                </template>
                <span v-else class="no-photo">无</span>
              </div>
              <!-- 可编辑模式 -->
              <div v-else class="remittance-photo-cell">
                <a-upload
                  v-model:file-list="record.photoFileList"
                  :max-count="1"
                  :before-upload="(file) => beforeRemittancePhotoUploadByRecord(file, record)"
                  @remove="() => handleRemoveRemittancePhotoByRecord(record)"
                  accept="image/*"
                  :show-upload-list="false"
                >
                  <div v-if="record.photoUrl" class="photo-wrapper">
                    <img :src="record.photoUrl" class="remittance-photo" />
                    <div class="photo-overlay">
                      <CameraOutlined style="font-size: 16px;" />
                    </div>
                  </div>
                  <div v-else class="upload-placeholder">
                    <PlusOutlined style="font-size: 14px; color: #999;" />
                  </div>
                </a-upload>
              </div>
            </template>

            <template v-else-if="column.key === 'action'">
              <a-space v-if="isBalanceEditable">
                <a-button type="link" size="small" :loading="submitting" @click="handleSaveRemittance(record)">{{ record.id ? '更新' : '保存' }}</a-button>
                <a-popconfirm title="确定要删除这条水单吗？" @confirm="handleDeleteRemittance(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
              <span v-else-if="record.id" style="color: #52c41a;">已保存</span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
        <a-empty v-if="balanceRemittanceList.length === 0" description="暂无尾款水单" />
      </a-card>

      <!-- 提货单 (尾款审核通过后显示，在提货单上传模式下也显示，在审核模式下也显示但定金/尾款审核阶段隐藏) -->
      <a-card v-if="formStatus !== 1 && !isDepositAuditStage && !isBalanceAuditStage && ((formStatus && formStatus >= 2 && balanceApproved && !isPaymentOnlyMode) || isPickupMode || isAudit)" title="提货单" size="small" class="section-card">
        <template #extra>
          <a-button 
            v-if="(formStatus === 2 || isPickupMode) && !isReadonly" 
            type="primary" 
            size="small" 
            @click="showDeliveryOrderModal"
            v-permission="['business:declaration:payment']"
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
                  v-if="(formStatus === 2 || isPickupMode) && !isReadonly && record.status === 0" 
                  type="link" 
                  size="small" 
                  @click="handleEditDeliveryOrder(record)"
                  v-permission="['business:declaration:payment']"
                >
                  编辑
                </a-button>
                <a-popconfirm
                  v-if="(formStatus === 2 || isPickupMode) && !isReadonly && record.status === 0"
                  title="确定要删除这条提货单吗？"
                  @confirm="handleDeleteDeliveryOrder(record)"
                >
                  <a-button 
                    type="link" 
                    size="small" 
                    danger
                    v-permission="['business:declaration:pickup-delete']"
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
          <a-button type="primary" size="small" @click="handleSaveFinanceSupplement" :loading="submittingSupplement" v-permission="['business:declaration:financeSupplement']">保存内容</a-button>
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

             <!-- 退税与外汇 -->
             <a-col :span="12">
               <a-card title="外汇与退税明细" size="small">
                 <a-row :gutter="16">
                     <a-col :span="12">
                         <a-form-item label="收汇总计(自动汇总)">
                           <a-input-number :value="totalReceiptCNY" style="width: 100%" disabled />
                           <div style="font-size: 12px; color: #888; margin-top: 4px;">已转成人民币(CNY)</div>
                         </a-form-item>
                     </a-col>
                     <a-col :span="12">
                         <a-form-item label="外汇银行">
                           <template v-if="isFinanceUploadMode">
                             <!-- 编辑模式：下拉选择 -->
                             <a-select 
                               v-model:value="financeSupplement.foreignExchangeBank" 
                               style="width: 100%" 
                               :disabled="!isFinanceUploadMode"
                               :loading="loadingBankAccounts"
                               @change="handleBankAccountChange"
                               placeholder="请选择银行账户"
                               allowClear
                             >
                               <a-select-option 
                                 v-for="bank in bankAccountList" 
                                 :key="bank.id" 
                                 :value="bank.id"
                               >
                                 {{ bank.accountName }} - {{ bank.bankName }} ({{ bank.currency }})
                               </a-select-option>
                             </a-select>
                           </template>
                           <template v-else>
                             <!-- 查看模式：只读文本 -->
                             <a-input 
                               :value="financeSupplement.foreignExchangeBank || '未设置'" 
                               style="width: 100%" 
                               disabled 
                             />
                           </template>
                         </a-form-item>
                     </a-col>
                     <a-col :span="12">
                         <a-form-item label="外汇银行手续费率(%)">
                           <a-input-number v-model:value="financeSupplement.bankFeeRate" style="width: 100%" :disabled="!isFinanceUploadMode" />
                         </a-form-item>
                     </a-col>
                     <a-col :span="12">
                         <a-form-item label="退税点(%)">
                           <a-input-number v-model:value="financeSupplement.taxRefundRate" style="width: 100%" :disabled="!isFinanceUploadMode" />
                         </a-form-item>
                     </a-col>
                     <a-col :span="12">
                         <a-form-item label="银行手续费金额">
                           <a-input-number :value="bankFeeAmount" style="width: 100%" disabled />
                           <div style="font-size: 12px; color: #888; margin-top: 4px;">收汇总计 × 手续费率</div>
                         </a-form-item>
                     </a-col>
                     <a-col :span="12">
                         <a-form-item label="退税金额">
                           <a-input-number :value="taxRefundAmount" style="width: 100%" disabled />
                           <div style="font-size: 12px; color: #888; margin-top: 4px;">收汇总计 × 退税点%</div>
                         </a-form-item>
                     </a-col>
                 </a-row>
               </a-card>
             </a-col>

             <!-- 开票明细 -->
             <a-col :span="12">
               <a-card title="开票明细(生成部分)" size="small">
                 <a-form-item label="金额(收汇*汇率*(1+退税%) - 货代 - 报关 -手续费扣款)">
                   <a-input-number v-model:value="computedDetailsAmount" style="width: 100%" disabled />
                 </a-form-item>
                 <a-form-item label="操作">
                   <a-space style="margin-bottom: 8px;" v-if="isFinanceUploadMode">
                     <a-button type="primary" size="small" @click="handleGenerateFinanceDetails" :loading="generatingDetails" v-permission="['business:declaration:financeSupplement']">生成开票明细单</a-button>
                   </a-space>
                   <br/>
                   <div v-if="!isFinanceUploadMode && financeSupplement.detailsFileName">
                     <a :href="financeSupplement.detailsFileUrl" target="_blank">{{ financeSupplement.detailsFileName }}</a>
                   </div>
                   <a-upload
                     v-else-if="isFinanceUploadMode"
                     :max-count="1"
                     :before-upload="(file) => beforeSupplementPhotoUpload(file, 'details')"
                     @remove="() => handleRemoveSupplementPhoto('details')"
                     :file-list="financeSupplement.detailsFileList || (financeSupplement.detailsFileName ? [{uid: '-3', name: financeSupplement.detailsFileName, status: 'done', url: financeSupplement.detailsFileUrl}] : [])"
                   >
                     <a-button><UploadOutlined /> {{ financeSupplement.detailsFileName ? '手动替换附件' : '手动上传附件' }}</a-button>
                   </a-upload>
                   <span v-else style="color: #999">无附件</span>
                 </a-form-item>
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
                        <a-form-item label="外汇银行">
                          <a-input :value="parsedCalculationDetail && parsedCalculationDetail.foreignExchangeBank ? parsedCalculationDetail.foreignExchangeBank : '未设置'" style="width: 100%" disabled />
                        </a-form-item>
                        <a-form-item label="银行手续费率 (%)">
                          <a-input-number :value="financeSupplement.bankFeeRate" style="width: 100%" disabled />
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
                          <!-- 定金明细 -->
                          <div class="calc-section">
                            <div class="calc-title" style="color: #1890ff;">定金收汇明细</div>
                            <template v-if="parsedCalculationDetail.depositDetails && parsedCalculationDetail.depositDetails.length > 0">
                              <div class="calc-row" v-for="(item, index) in parsedCalculationDetail.depositDetails" :key="'d'+index">
                                <span class="calc-label">{{ item.remittanceName ? String(item.remittanceName) : '定金' }}:</span>
                                <span class="calc-value">{{ formatMoney(item.amount) }} {{ item.currency || 'USD' }} × {{ item.exchangeRate }} = {{ formatMoney(item.cny) }} CNY</span>
                              </div>
                            </template>
                            <div class="calc-row total">
                              <span class="calc-label">定金合计:</span>
                              <span class="calc-value highlight">{{ formatMoney(parsedCalculationDetail.depositCny) }} CNY</span>
                            </div>
                          </div>
                          
                          <a-divider />
                          
                          <!-- 尾款明细 -->
                          <div class="calc-section">
                            <div class="calc-title" style="color: #fa8c16;">尾款收汇明细</div>
                            <template v-if="parsedCalculationDetail.balanceDetails && parsedCalculationDetail.balanceDetails.length > 0">
                              <div class="calc-row" v-for="(item, index) in parsedCalculationDetail.balanceDetails" :key="'b'+index">
                                <span class="calc-label">{{ item.remittanceName ? String(item.remittanceName) : '尾款' }}:</span>
                                <span class="calc-value">{{ formatMoney(item.amount) }} {{ item.currency || 'USD' }} × {{ item.exchangeRate }} = {{ formatMoney(item.cny) }} CNY</span>
                              </div>
                            </template>
                            <div class="calc-row total">
                              <span class="calc-label">尾款合计:</span>
                              <span class="calc-value highlight">{{ formatMoney(parsedCalculationDetail.balanceCny) }} CNY</span>
                            </div>
                          </div>
                          
                          <a-divider />
                          
                          <!-- 总货物金额 -->
                          <div class="calc-section">
                            <div class="calc-title">汇总</div>
                            <div class="calc-row">
                              <span class="calc-label">总货物金额 (定金+尾款):</span>
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
                              <span class="calc-label">- 银行手续费 ({{ formatMoney(parsedCalculationDetail.totalGoodsAmount) }} × {{ parsedCalculationDetail.bankFeeRate }}%):</span>
                              <span class="calc-value">{{ formatMoney(parsedCalculationDetail.bankFeeAmount) }} CNY</span>
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
                            <div class="calc-row" v-if="parsedCalculationDetail.foreignExchangeBank">
                              <span class="calc-label">外汇银行:</span>
                              <span class="calc-value">{{ parsedCalculationDetail.foreignExchangeBank }}</span>
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
import { message, Modal } from 'ant-design-vue'
import type { SelectValue } from 'ant-design-vue/lib/select';
import { PlusOutlined, UploadOutlined, CameraOutlined, DownloadOutlined, HistoryOutlined } from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'
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
  saveRemittance,
  updateRemittance,
  deleteRemittance,
  getPickupAttachments,
  getFinancialSupplement,
  createFinancialSupplement,
  updateFinancialSupplement,
  saveDeliveryOrder,
  getDeliveryOrders,
  updateDeliveryOrder,
  deleteDeliveryOrder,
  getEnabledBankAccounts,
  auditReturnToDraft,
  getReturnAuditHistory
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
const isPickupMode = ref(route.query.type === 'pickup') // 提货单模式（从mode=payment&type=pickup进入）
const isPaymentOnlyMode = computed(() => isPaymentMode.value && !isPickupMode.value) // 纯水单模式（隐藏提货单区域）
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
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
const activeTaskKeys = computed(() => activeTasks.value.map((t: any) => t.taskKey))

// 计量单位列表
const measurementUnits = ref<MeasurementUnit[]>([])

// 基本信息是否只读（审核模式、查看模式、水单提交模式、财务补充模式都只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value || isFinanceUploadMode.value)

// 定金水单是否可编辑（付定金时可编辑，付尾款时只读，财务模式不可编辑，水单提交模式下根据type判断，提货单模式下只读）
const isDepositEditable = computed(() => 
  !isReadonly.value && 
  !isAudit.value && 
  !isFinanceUploadMode.value && 
  formStatus.value === 2 && 
  !(isPaymentMode.value && route.query.type === 'balance') && // 尾款水单提交模式下定金水单只读
  !isPickupMode.value // 提货单模式下定金水单只读
)

// 尾款水单是否可编辑（status=4时可编辑，财务模式不可编辑，提货单模式下只读）
const isBalanceEditable = computed(() => 
  !isReadonly.value && 
  !isAudit.value && 
  !isFinanceUploadMode.value && 
  formStatus.value === 4 &&
  !isPickupMode.value // 提货单模式下尾款水单只读
)

// 当前审核阶段（从 URL taskKey 中获取）
const currentAuditTaskKey = computed(() => (route.query.taskKey as string) || '')
// 定金审核阶段：不应该看到尾款水单和提货单
const isDepositAuditStage = computed(() => isAudit.value && (currentAuditTaskKey.value === 'depositAudit' || currentAuditTaskKey.value === 'deptAudit'))
// 尾款审核阶段：不应该看到提货单
const isBalanceAuditStage = computed(() => isAudit.value && currentAuditTaskKey.value === 'balanceAudit')

// 获取当前审核阶段文本
const getAuditActionText = () => {
  // 优先从 URL 参数获取 taskKey
  const taskKey = route.query.taskKey as string
  if (taskKey) {
    const map: Record<string, string> = {
      deptAudit: '初审',
      depositAudit: '定金审核',
      balanceAudit: '尾款审核',
      pickupListAudit: '提货单审核'
    }
    return map[taskKey] || '审批'
  }
  // 兼容：如果没有 taskKey 参数，从活跃任务中推断
  if (formStatus.value === 1) return '初审'
  if (formStatus.value === 9) return '退回'
  if (activeTaskKeys.value.includes('depositAudit')) return '定金审核'
  if (activeTaskKeys.value.includes('balanceAudit')) return '尾款审核'
  if (activeTaskKeys.value.includes('pickupListAudit')) return '提货单审核'
  return '审批'
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
    1: '待初审',
    2: '待上传定金水单',
    3: '定金待审核',
    4: '待上传尾款水单',
    5: '尾款待审核',
    6: '待上传提货单',
    7: '提货单待审核',
    8: '已完成',
    9: '退回待审'
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

// 银行账户相关数据
const bankAccountList = ref<any[]>([])
const loadingBankAccounts = ref(false)

// 加载启用的银行账户列表
const loadBankAccounts = async (currency?: string) => {
  loadingBankAccounts.value = true
  try {
    const res = await getEnabledBankAccounts(currency)
    if (res.data?.code === 200) {
      bankAccountList.value = res.data.data || []
      console.log('加载银行账户列表成功:', bankAccountList.value.length + ' 条记录')
    } else {
      console.error('加载银行账户列表失败:', res.data?.message)
      bankAccountList.value = []
    }
  } catch (error) {
    console.error('加载银行账户列表异常:', error)
    bankAccountList.value = []
  } finally {
    loadingBankAccounts.value = false
  }
}

// 监听财务补充模式变化
watch(isFinanceUploadMode, (newVal) => {
  if (newVal) {
    // 进入财务补充模式时加载银行账户
    loadBankAccounts(financeSupplement.value.currency)
  }
})

// 监听币种变化，重新加载对应的银行账户
watch(() => financeSupplement.value.currency, (newCurrency) => {
  if (isFinanceUploadMode.value) {
    loadBankAccounts(newCurrency)
  }
})

// 银行账户选择变化处理
const handleBankAccountChange = (value: SelectValue) => {
  console.log('银行账户选择变化:', value)
  console.log('银行账户列表:', bankAccountList.value)
  
  if (!value) {
    financeSupplement.value.selectedBankAccount = null
    financeSupplement.value.foreignExchangeBank = '' // 清空绑定值
    return
  }
  
  const bankAccountId = typeof value === 'string' || typeof value === 'number' ? value : (value as any)?.value
  const selectedBank = bankAccountList.value.find(bank => bank.id === Number(bankAccountId))
  console.log('找到的银行:', selectedBank)
  
  if (selectedBank) {
    financeSupplement.value.selectedBankAccount = selectedBank
    // 正确设置银行ID和名称
    financeSupplement.value.foreignExchangeBank = selectedBank.id // 存储银行ID
    // 自动填充银行手续费率（如果银行账户配置中有）
    console.log('银行手续费率:', selectedBank.serviceFeeRate, '类型:', typeof selectedBank.serviceFeeRate)
    if (selectedBank.serviceFeeRate !== undefined && selectedBank.serviceFeeRate !== null) {
      financeSupplement.value.bankFeeRate = selectedBank.serviceFeeRate * 100 // 转换为百分比显示
      console.log('设置手续费率:', selectedBank.serviceFeeRate * 100)
    } else {
      console.log('银行手续费率为undefined或null')
    }
  } else {
    financeSupplement.value.selectedBankAccount = null
    console.log('未找到对应银行')
  }
}

const loadFinancialSupplement = async () => {
  if (!formId.value) return
  try {
    console.log('加载财务补充记录, formId:', formId.value)
    const res = await getFinancialSupplement(formId.value)
    console.log('财务补充记录响应:', res)
    if (res.data && res.data.code === 200 && res.data.data) {
      Object.assign(financeSupplement.value, res.data.data)
      console.log('财务补充数据加载成功:', financeSupplement.value)
      
      // 如果有银行ID，尝试转换为银行名称显示
      if (financeSupplement.value.foreignExchangeBank && bankAccountList.value.length > 0) {
        const bank = bankAccountList.value.find(b => b.id === Number(financeSupplement.value.foreignExchangeBank))
        if (bank) {
          console.log('找到银行信息:', bank)
          // 在查看模式下显示银行名称，在编辑模式下保持ID用于下拉框
          if (!isFinanceUploadMode.value) {
            financeSupplement.value.foreignExchangeBank = bank.accountName
          }
        } else {
          console.log('未找到对应银行，银行ID:', financeSupplement.value.foreignExchangeBank)
        }
      }
      
      financeSupplement.value.freightFileList = financeSupplement.value.freightFileName ? [{uid: '-1', name: financeSupplement.value.freightFileName, status: 'done', url: financeSupplement.value.freightFileUrl}] : []
      financeSupplement.value.customsFileList = financeSupplement.value.customsFileName ? [{uid: '-2', name: financeSupplement.value.customsFileName, status: 'done', url: financeSupplement.value.customsFileUrl}] : []
      financeSupplement.value.detailsFileList = financeSupplement.value.detailsFileName ? [{uid: '-3', name: financeSupplement.value.detailsFileName, status: 'done', url: financeSupplement.value.detailsFileUrl}] : []
      financeSupplement.value.customsReceiptFileList = financeSupplement.value.customsReceiptFileName ? [{uid: '-4', name: financeSupplement.value.customsReceiptFileName, status: 'done', url: financeSupplement.value.customsReceiptFileUrl}] : []
    } else {
      console.log('未找到财务补充记录，可能需要初始化')
      // 如果没有找到记录，在财务补充模式下可以提示用户
      if (isFinanceUploadMode.value) {
        message.info('财务补充记录正在初始化中，请稍后重试')
      }
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


// 保存水单
const handleSaveRemittance = async (record: any) => {
  if (!formId.value) return
  if (record.saving) return // 防止连续点击重复提交
  
  record.saving = true
  submitting.value = true
  try {
    const data = {
      ...record,
      remittanceDate: record.remittanceDate.format('YYYY-MM-DD'),
    }
    
    if (record.id) {
      // 更新现有水单
      await updateRemittance(record.id, data)
      message.success('水单信息更新成功')
    } else {
      // 新增水单
      const res = await saveRemittance(formId.value, data)
      if (res.data && res.data.code === 200) {
        record.id = res.data.data?.id || record.id
      }
      message.success('水单信息保存成功，已生成对应单证')
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    submitting.value = false
    record.saving = false // 释放锁
  }
}

const beforeRemittancePhotoUpload = async (file: any, index: number) => {
  try {
    const res = await uploadFile(file, 'remittance')
    if (res.data && res.data.code === 200) {
      const attachment = res.data.data
      // 使用 getFilePreviewUrl 构建预览 URL
      remittanceList.value[index].photoUrl = getFilePreviewUrl(attachment.id)
      remittanceList.value[index].photoId = attachment.id
      remittanceList.value[index].photoFileList = [{
        uid: String(attachment.id),
        name: file.name,
        status: 'done',
        url: getFilePreviewUrl(attachment.id)
      }]
      message.success('水单图片上传成功')
    }
  } catch (error) {
    message.error('水单图片上传失败')
  }
  return false
}

const handleRemoveRemittancePhoto = (index: number) => {
  remittanceList.value[index].photoUrl = ''
  remittanceList.value[index].photoFileList = []
}

const removeRemittance = (index: number) => {
  remittanceList.value.splice(index, 1)
}

// 通过 record 操作的函数
const beforeRemittancePhotoUploadByRecord = async (file: any, record: any) => {
  const index = remittanceList.value.findIndex(r => r.tempId === record.tempId)
  if (index >= 0) {
    return beforeRemittancePhotoUpload(file, index)
  }
  return false
}

const handleRemoveRemittancePhotoByRecord = (record: any) => {
  const index = remittanceList.value.findIndex(r => r.tempId === record.tempId)
  if (index >= 0) {
    handleRemoveRemittancePhoto(index)
  }
}

const removeRemittanceByRecord = (record: any) => {
  const index = remittanceList.value.findIndex(r => r.tempId === record.tempId)
  if (index >= 0) {
    removeRemittance(index)
  }
}

// 删除水单（支持已保存和未保存的）
const handleDeleteRemittance = async (record: any) => {
  if (record.id) {
    // 已保存的水单需要调用后端删除
    try {
      await deleteRemittance(record.id)
      message.success('水单删除成功')
    } catch (error) {
      message.error('删除失败')
      return
    }
  }
  // 从本地列表中移除
  removeRemittanceByRecord(record)
}

const remittanceList = ref<any[]>([])

// 定金水单列表
const depositRemittanceList = computed(() => 
  remittanceList.value.filter(r => r.remittanceType === 1)
)

// // 定金水单是否有已审核通过的记录（用于控制尾款水单区域显示）
// const depositApproved = computed(() => depositRemittanceList.value.some(r => r.auditStatus === 1))

// 尾款水单是否有已审核通过的记录（用于控制提货单区域显示）
const balanceApproved = computed(() => balanceRemittanceList.value.some(r => r.auditStatus === 1))

// 尾款水单列表
const balanceRemittanceList = computed(() => 
  remittanceList.value.filter(r => r.remittanceType === 2)
)

const totalReceiptCNY = computed(() => {
  let total = 0
  remittanceList.value.forEach(r => total += (r.remittanceAmount || 0) * (r.exchangeRate || 1))
  return Number(total.toFixed(2))
})

// 银行手续费金额计算
const bankFeeAmount = computed(() => {
  const receipt = totalReceiptCNY.value
  const feeRate = financeSupplement.value.bankFeeRate || 0
  return Number((receipt * (feeRate / 100)).toFixed(2))
})

// 退税金额计算
const taxRefundAmount = computed(() => {
  const receipt = totalReceiptCNY.value
  const taxRate = financeSupplement.value.taxRefundRate || 0
  return Number((receipt * (taxRate / 100)).toFixed(2))
})


import { watch } from 'vue'

const computedDetailsAmount = computed(() => {
  const receipt = totalReceiptCNY.value
  const taxRate = financeSupplement.value.taxRefundRate || 0
  const freight = financeSupplement.value.freightAmount || 0
  const customs = financeSupplement.value.customsAmount || 0
  const feeRate = financeSupplement.value.bankFeeRate || 0
  
  // 开票金额 = 总金额 * (1 + 退税点%) - 货代 - 报关 - 总金额 * 外汇银行手续费率%
  const bankFee = receipt * (feeRate / 100)
  return Number((receipt * (1 + taxRate / 100) - freight - customs - bankFee).toFixed(2))
})

watch(computedDetailsAmount, (newVal) => {
  financeSupplement.value.detailsAmount = newVal
}, { immediate: true })

import { exportFinanceCalculation } from '@/api/business/declaration'
import { getCitiesByCountry } from '@/api/system/city-info'

const generatingDetails = ref(false)
const handleGenerateFinanceDetails = async () => {
  if (!formId.value) return
  generatingDetails.value = true
  try {
    // 确保最新数据已经保存
    await handleSaveFinanceSupplement()
    
    // 调用后端生成接口
    const res = await exportFinanceCalculation(formId.value) as any
    // 处理下载
    const blob = new Blob([res.data || res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `开票明细单_${financeSupplement.value.detailsInvoiceNo || formId.value}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.success('明细单下载成功')
  } catch (err) {
    message.error('生成明细单失败')
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

// 计算退税金额
const getTaxRefundAmount = () => {
  if (!parsedCalculationDetail.value) return 0
  const goodsAmount = parsedCalculationDetail.value.totalGoodsAmount || 0
  const taxRate = parsedCalculationDetail.value.taxRefundRate || 0
  return goodsAmount * (taxRate / 100)
}


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

// 下载水单附件
const handleDownloadRemittancePhoto = (record: any) => {
  if (record.photoUrl) {
    window.open(record.photoUrl, '_blank')
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

// 不包含类型列的列定义
const remittanceColumnsNoType = [
  { title: '名称', key: 'remittanceName', width: 120 },
  { title: '日期', key: 'remittanceDate', width: 130 },
  { title: '金额', key: 'remittanceAmount', width: 100 },
  { title: '汇率', key: 'exchangeRate', width: 80 },
  { title: '手续费', key: 'bankFee', width: 80 },
  { title: '入账金额', key: 'creditedAmount', width: 100 },
  { title: '附件', key: 'photoUrl', width: 100 },
  { title: '操作', key: 'action', width: 120 }
]

const handleAddRemittance = (type: number) => {
  remittanceList.value.push({
    tempId: Date.now(),
    remittanceType: type,
    remittanceName: type === 1 ? '定金水单' : '尾款水单',
    remittanceDate: dayjs(),
    remittanceAmount: 0,
    exchangeRate: 0,
    bankFee: 0,
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
  return productList.value.map(item => ({
    label: item.productName,
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
  { title: '产品中文名', dataIndex: 'productChineseName', key: 'productChineseName', width: 150 },
  { title: '产品英文名', dataIndex: 'productEnglishName', key: 'productEnglishName', width: 150 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 150 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 120 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 120 },
  { title: '关联箱号', key: 'cartonInfo', width: 150 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '产品照片', key: 'productPhoto', width: 120 },
  { title: '申报要素', key: 'declarationElements', width: 120 },
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
    // 如果金额被手动设置，则使用手动输入的值，否则使用计算值
    totalAmount += item._amountManuallySet ? parseFloat(item.amount) || 0 : (item.unitPrice || 0) * (item.quantity || 0)
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
  // 只有在用户未明确输入金额或者金额为0时才自动计算
  // 如果用户已输入金额，保留用户的输入
  if (record._amountManuallySet) {
    return record.amount;
  }
  const amount = (record.unitPrice || 0) * (record.quantity || 0)
  // 同时更新record中的amount字段
  record.amount = amount.toFixed(2)
  return record.amount
};

// 处理单价或数量变更
const handleUnitPriceOrQuantityChange = (record: any) => {
  // 如果金额是手动设置的，询问用户是否要重新计算
  if (record._amountManuallySet && record.amount !== 0) {
    Modal.confirm({
      title: '重新计算金额？',
      content: '检测到单价或数量已更改，是否根据新的单价和数量重新计算金额？',
      okText: '重新计算',
      cancelText: '保持手动输入',
      onOk: () => {
        record._amountManuallySet = false;
        calculateAmount(record);
        record._amountManuallySet = true;
      },
      onCancel: () => {}
    });
  } else {
    calculateAmount(record);
  }
};

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
const handleAmountChange = (record: any) => {
  record._amountManuallySet = true;
};

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
    amount: 0, // 初始化金额字段
    _amountManuallySet: false, // 标记金额是否手动设置
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
    typeChinese: '箱子', // 默认类型
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
    productList.value.forEach(product => {
      // 如果金额是手动设置的，保留手动输入的值
      if (product._amountManuallySet && product.amount !== undefined) {
        // 保持手动输入的金额
        product.amount = parseFloat(product.amount).toFixed(2)
      } else {
        // 否则按单价*数量计算
        if (product.unitPrice !== undefined && product.quantity !== undefined) {
          product.amount = (product.unitPrice * product.quantity).toFixed(2)
        } else {
          product.amount = '0.00'
        }
      }
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
        elementValues: (product.declarationElements || [])
          .filter((elem: any) => elem.value && elem.value.trim())
          .map((elem: any) => ({
            elementName: elem.label,
            elementValue: elem.value
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
      // 如果金额是手动设置的，保留手动输入的值
      if (product._amountManuallySet && product.amount !== undefined) {
        // 保持手动输入的金额
        product.amount = parseFloat(product.amount).toFixed(2)
      } else {
        // 否则按单价*数量计算
        if (product.unitPrice !== undefined && product.quantity !== undefined) {
          product.amount = (product.unitPrice * product.quantity).toFixed(2)
        } else {
          product.amount = '0.00'
        }
      }
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
        // 如果金额是手动设置的，保留手动输入的值
        let finalAmount = product.amount;
        if (product._amountManuallySet && product.amount !== undefined) {
          finalAmount = parseFloat(product.amount).toFixed(2);
        } else {
          // 否则按单价*数量计算
          if (product.unitPrice !== undefined && product.quantity !== undefined) {
            finalAmount = (product.unitPrice * product.quantity).toFixed(2);
          } else {
            finalAmount = '0.00';
          }
        }
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
        // 3. 如果是水单提交模式 (isPaymentMode)，不改变 isReadonly
        // 4. 否则根据状态判断：状态 0/2 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value) {
          isReadonly.value = true
          console.log('查看模式或审核模式, 设置为只读')
        } else if (!isPaymentMode.value) {
          const editableStatuses = [0, 2]
          if (!editableStatuses.includes(submittedStatus)) {
            isReadonly.value = true
            console.log('申报单状态=' + submittedStatus + ', 设置为只读模式')
          } else {
            isReadonly.value = false
            console.log('申报单状态=' + submittedStatus + ', 可编辑模式')
          }
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
            // 初始化手动金额设置标记 - 智能判断金额是否为手动设置
            _amountManuallySet: Math.abs(((product.unitPrice || 0) * (product.quantity || 0)) - (product.amount || 0)) > 0.01, // 如果金额与单价*数量计算结果差异大于0.01，则认为是手动设置的
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
          console.log('加载水单列表成功:', remittanceList.value.length + ' 条记录')
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
  color: #1E40AF;
  font-size: 13px;
  text-transform: none;
  letter-spacing: normal;
  border-bottom: 2px solid #E2E8F0 !important;
}

:deep(.ant-table-cell) {
  border-bottom: 1px solid #f1f5f9;
}

/* 主按钮样式已通过全局CSS优化，这里保持基础覆盖以确保一致性 */
:deep(.ant-btn-primary) {
  background: #2563EB !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2) !important;
}

:deep(.ant-btn-primary:hover) {
  background: #1D4ED8 !important;
  box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.3) !important;
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
  overflow-x: hidden;
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
  color: #1E40AF;
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
  color: #2563EB;
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
  border-color: #1890ff;
  background: #e6f7ff;
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
  color: #1890ff;
  font-size: 16px;
  font-weight: bold;
}

.calc-value.final-value {
  color: #722ed1;
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
  background: linear-gradient(135deg, #f9f0ff, #fff);
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
