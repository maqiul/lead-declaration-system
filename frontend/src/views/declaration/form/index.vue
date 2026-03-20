<template>
  <div class="declaration-form-page">
    <a-card :title="isPaymentMode ? (formStatus === 2 ? '定金水单提交' : '尾款水单提交') : '出口申报表单'">
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          
          <!-- 审核模式下的按钮 -->
          <template v-if="isAudit">
            <a-button 
              type="primary" 
              @click="handleApprove" 
              :loading="submitting"
              v-permission="['business:declaration:audit']"
            >{{ getAuditActionText() }}通过</a-button>
            <a-button 
              danger 
              @click="handleReject" 
              :loading="submitting"
              v-permission="['business:declaration:audit']"
            >{{ getAuditActionText() }}驳回</a-button>
          </template>
          
          <!-- 水单提交模式下的按钮 -->
          <template v-else-if="isPaymentMode">
            <a-button v-if="formStatus === 2" type="primary" @click="handleSubmitAudit('deposit')" :loading="submitting" v-permission="['business:declaration:payment']">提交定金审核</a-button>
            <a-button v-if="formStatus === 4" type="primary" @click="handleSubmitAudit('balance')" :loading="submitting" v-permission="['business:declaration:payment']">提交尾款审核</a-button>
            <a-button v-if="formStatus === 6" type="primary" @click="handleSubmitAudit('pickup')" :loading="submitting" :disabled="pickupAttachments.length === 0" v-permission="['business:declaration:pickup-submit']">提交提货单审核</a-button>
          </template>
          
          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 保存草稿按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="handleSaveDraft" :loading="submitting" v-permission="['business:declaration:add']">保存草稿</a-button>
            
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting" v-permission="['business:declaration:submit']">提交申报</a-button>
            
            <!-- 提货单待传状态下显示提货单提交按钮 -->
            <a-button 
              v-if="formStatus === 6 && !isReadonly" 
              type="primary" 
              @click="handleSubmitAudit('pickup')" 
              :loading="submitting" 
              :disabled="pickupAttachments.length === 0"
              v-permission="['business:declaration:pickup-submit']"
            >
              提交提货单审核 (附件: {{ pickupAttachments.length }}个)
            </a-button>
            
            <!-- 提货单待审状态下显示审核按钮 -->
            <template v-if="formStatus === 7 && !isReadonly">
              <a-button 
                type="primary" 
                @click="handleApprove" 
                :loading="submitting"
                v-permission="['business:declaration:pickup-audit']"
              >审核通过</a-button>
              <a-button 
                danger 
                @click="handleReject" 
                :loading="submitting"
                v-permission="['business:declaration:pickup-audit']"
              >审核驳回</a-button>
            </template>
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
              <a-input v-model:value="formData.departureCity" placeholder="例如：SHANGHAI, CHINA" :readonly="isFormReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="目的国">
              <a-select 
                v-model:value="formData.destinationCountry" 
                :options="countryOptions"
                placeholder="请选择目的国家" 
                :disabled="isFormReadonly"
                show-search
                option-filter-prop="label"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
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
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发票号">
              <a-input 
                v-model:value="formData.invoiceNo" 
                placeholder="请输入发票号，留空则自动生成(ZIYI-yy-mmdd格式)" 
                :readonly="isFormReadonly"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="币种">
                            <a-select v-model:value="formData.currency" style="width: 100%" :disabled="isFormReadonly">
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
              <a-input 
                v-if="!isFormReadonly" 
                v-model:value="record.productName" 
                placeholder="产品名称"
              />
              <span v-else>{{ record.productName }}</span>
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
                @change="() => calculateAmount(record)"
                style="width: 100%"
              />
              <span v-else>{{ record.quantity }}</span>
            </template>
            
            <template v-else-if="column.key === 'unit'">
              <a-select 
                v-if="!isFormReadonly"
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
                v-if="!isFormReadonly"
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
              <span>{{ calculateAmount(record) }}</span>
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

      <!-- 定金水单 (初审通过后显示) -->
      <a-card v-if="formStatus && formStatus >= 2" title="定金水单" size="small" class="section-card">
        <template #extra>
          <a-button v-if="isDepositEditable" type="primary" size="small" @click="handleAddRemittance(1)">
            <template #icon><PlusOutlined /></template>
            添加定金水单
          </a-button>
        </template>

        <a-table 
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
                <a-image v-if="record.photoUrl" :src="record.photoUrl" :width="48" :height="48" :preview="true" class="remittance-photo" />
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
                <a-button type="link" size="small" @click="handleSaveRemittance(record)">{{ record.id ? '更新' : '保存' }}</a-button>
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

      <!-- 尾款水单 (定金审核通过后显示) -->
      <a-card v-if="formStatus && formStatus >= 4" title="尾款水单" size="small" class="section-card">
        <template #extra>
          <a-button v-if="isBalanceEditable" type="primary" size="small" @click="handleAddRemittance(2)">
            <template #icon><PlusOutlined /></template>
            添加尾款水单
          </a-button>
        </template>

        <a-table 
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
                <a-image v-if="record.photoUrl" :src="record.photoUrl" :width="48" :height="48" :preview="true" class="remittance-photo" />
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
                <a-button type="link" size="small" @click="handleSaveRemittance(record)">{{ record.id ? '更新' : '保存' }}</a-button>
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

      <!-- 提货单附件 (尾款审核通过后显示) -->
      <a-card v-if="formStatus && formStatus >= 6" title="提货单附件" size="small" class="section-card">
        <template #extra>
          <a-button v-if="!isReadonly && formStatus === 6" type="primary" size="small" @click="handleUploadPickup" v-permission="['business:declaration:pickup-submit']">
            <template #icon><UploadOutlined /></template>
            上传提货单
          </a-button>
        </template>
        
        <a-table 
          :dataSource="pickupAttachments" 
          :columns="pickupColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'fileName'">
              <a :href="getFilePreviewUrl(record.id)" target="_blank">{{ record.fileName }}</a>
            </template>
            <template v-else-if="column.key === 'fileType'">
              <a-tag :color="getFileTagColor(record.fileType)">
                {{ getFileTypeDisplay(record.fileType) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ record.createTime ? dayjs(record.createTime).format('YYYY-MM-DD HH:mm') : '' }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="handleDownloadFile(record)" v-permission="['business:declaration:download']">下载</a-button>
                <a-button v-if="!isReadonly && formStatus === 6" type="link" size="small" danger @click="handleDeletePickup(record)" v-permission="['business:declaration:pickup-delete']">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
        <a-empty v-if="pickupAttachments.length === 0" description="暂无提货单附件" />
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined, CameraOutlined } from '@ant-design/icons-vue'
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
  saveRemittance,
  updateRemittance,
  deleteRemittance,
  getPickupAttachments,
  savePickupAttachment,
  deleteAttachment
} from '@/api/business/declaration'
import { getProductTypes } from '@/api/system/product'
import { getTransportModes } from '@/api/system/config'

// 文件预览 URL 生成函数
const FILE_DOWNLOAD_URL = '/api/v1/files/download'
const getFilePreviewUrl = (id: number | string) => `${FILE_DOWNLOAD_URL}?id=${id}`

const route = useRoute()
const router = useRouter()

// 页面状态
const isAudit = ref(route.query.mode === 'audit')
const isPaymentMode = ref(route.query.mode === 'payment') // 水单提交模式
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(route.query.status ? Number(route.query.status) : null)
const submitting = ref(false)

// 基本信息是否只读（审核模式、查看模式、水单提交模式都只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value)

// 定金水单是否可编辑（付定金时可编辑，付尾款时只读）
const isDepositEditable = computed(() => !isReadonly.value && !isAudit.value && formStatus.value === 2)

// 尾款水单是否可编辑（付尾款时可编辑）
const isBalanceEditable = computed(() => !isReadonly.value && !isAudit.value && formStatus.value === 4)

// 获取当前审核阶段文本
const getAuditActionText = () => {
  if (formStatus.value === 1) return '初审'
  if (formStatus.value === 3) return '定金审核'
  if (formStatus.value === 5) return '尾款审核'
  if (formStatus.value === 7) return '提货单审核'
  return '审批'
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
  submitting.value = true
  try {
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

// 尾款水单列表
const balanceRemittanceList = computed(() => 
  remittanceList.value.filter(r => r.remittanceType === 2)
)

// 提货单附件列表
const pickupAttachments = ref<any[]>([])

// 提货单表格列配置
const pickupColumns = [
  { title: '文件名', key: 'fileName', dataIndex: 'fileName' },
  { title: '类型', key: 'fileType', width: 100 },
  { title: '上传时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 120 }
]

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
  destinationCountry: undefined as string | undefined,
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

// 国家选项
const countryOptions = ref<any[]>([])

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

// 加载国家选项
const loadCountries = async () => {
  try {
    // 这里应该调用实际的国家API
    // const response = await getEnabledCountries()
    // 模拟数据
    countryOptions.value = [
      { label: '中国(CHN)', value: 'CHN' },
      { label: '美国(USA)', value: 'USA' },
      { label: '英国(GBR)', value: 'GBR' },
      { label: '德国(DEU)', value: 'DEU' },
      { label: '法国(FRA)', value: 'FRA' },
      { label: '日本(JPN)', value: 'JPN' },
      { label: '韩国(KOR)', value: 'KOR' }
    ]
  } catch (error) {
    console.error('加载国家数据失败:', error)
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
  { title: '关联箱号', key: 'cartonInfo', width: 150 },
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
          productList.value[index].chineseName = productType.chineseName
        } else {
          // 从选项标签中提取中文名（如果有）
          const chineseMatch = option.label.match(/^([^\s-]+.*?)\s*-\s*(.+)$/)
          if (chineseMatch && chineseMatch[1]) {
            productList.value[index].chineseName = chineseMatch[1].trim()
          }
        }
        
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
    hsCode: '',
    quantity: 1,
    unit: 'PCS',
    unitPrice: 0,
    amount: '0.00', // 初始化金额字段
    grossWeight: 0,
    netWeight: 0,
    cartons: 1,    // 默认1箱
    volume: 0,     // 默认0
    imageId: null, // 产品图片ID
    productPhoto: '', // 产品照片URL
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

// 保存草稿
const handleSaveDraft = async () => {
  submitting.value = true
  try {
    // 将关联箱子的 cartons 和 volume 赋值到产品中
    productList.value.forEach(product => {
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
        id: product.id ? Number(product.id) : undefined,  // 确保ID是数字类型
        imageId: product.imageId ? Number(product.imageId) : null,  // 确保imageId是数字类型
        productPhoto: product.productPhoto, // 显式包含图片URL
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
      
      // 如果是新草稿，更新ID和URL，避免重复创建
      if (!formId.value) {
        formId.value = newDraftId
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
    
    if (unassignedProducts.length > 0) {
      const productNames = unassignedProducts.map(p => p.productName || '未命名产品').join('、')
      message.error(`以下产品未分配箱子: ${productNames}，请在箱子信息中选择关联产品`)
      return
    }
    
    submitting.value = true
    
    // 确保所有产品的金额都已计算
    productList.value.forEach(product => {
      if (product.unitPrice !== undefined && product.quantity !== undefined) {
        product.amount = (product.unitPrice * product.quantity).toFixed(2)
      } else {
        product.amount = '0.00'
      }
    })
    
    // 将关联箱子的 cartons 和 volume 赋值到产品中
    productList.value.forEach(product => {
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
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      status: 0, // 初始保存为草稿状态，由后续 /submit 启动流程并改为1
      products: productList.value.map((product: any) => ({
        ...product,
        elementValues: (product.declarationElements || []).map((elem: any) => ({
          elementName: elem.label,
          elementValue: elem.value && elem.value.trim() ? elem.value : '无'
        }))
      })),
      cartons: cartonList.value,
      cartonProducts: cartonProducts 
    }
    
    console.log('提交的数据:', submitData)
    
    // 如果是从草稿提交，我们需要告诉后端这个表单原本在草稿表
    // 或者后端可以在 submit 逻辑中自动处理
    
    let finalId = formId.value
    if (formId.value && formStatus.value !== 0) {
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
    loadTransportModes()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value, formStatus.value ?? undefined)
      console.log('=== 申报单详情API响应 ===', response)
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
        console.log('🔄 更新formStatus为:', submittedStatus)
        
        // 只读状态判断：
        // 1. 如果 URL 参数 readonly=true，保持只读
        // 2. 如果是审核模式 (isAudit)，保持只读
        // 3. 如果是水单提交模式 (isPaymentMode)，不改变 isReadonly
        // 4. 否则根据状态判断：状态 0/2/4 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value) {
          isReadonly.value = true
          console.log('查看模式或审核模式, 设置为只读')
        } else if (!isPaymentMode.value) {
          const editableStatuses = [0, 2, 4]
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
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
        formData.destinationCountry = detailData.destinationCountry || ''
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
          console.log('加载水单列表成功:', remittanceList.value.length + ' 条记录')
        }
        // 填充提货单附件(若需要)，如果是状态 >=6 开始加载
        const fStatus = formStatus.value || 0
        if (fStatus >= 6 && formId.value) {
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
        
        message.success('数据加载成功')
      } else {
        console.error('API返回异常:', response.data)
        message.error('获取申报单详情失败')
      }
    } catch (error: any) {
      console.error('加载申报单详情失败:', error)
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

// 上传提货单
const handleUploadPickup = async () => {
  if (!formId.value) return message.error('申报单ID不存在')
  
  try {
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.accept = '.pdf,.doc,.docx,.jpg,.jpeg,.png'
    fileInput.onchange = async (e: any) => {
      const file = e.target.files[0]
      if (file) {
        // 第一步：上传物理文件
        const response = await uploadFile(file, 'PickupList')  // 直接指定文件类型为PickupList
        if (response.data && response.data.code === 200) {
          const fileInfo = response.data.data
          
          // fileInfo已经是完整的DeclarationAttachment对象，包含了正确的ID和fileUrl
          // 直接保存到数据库，避免重复上传
          const attachRes = await savePickupAttachment(formId.value!, {
            fileName: fileInfo.fileName,
            fileUrl: fileInfo.fileUrl,
            fileType: 'PickupList'  // 确保文件类型正确
          })
          
          if (attachRes.data && attachRes.data.code === 200) {
            message.success('提货单上传成功')
            // 重新加载提货单列表
            const pickupRes = await getPickupAttachments(formId.value!)
            if (pickupRes.data && pickupRes.data.code === 200) {
              pickupAttachments.value = pickupRes.data.data.map((att: any) => ({
                id: att.id,
                fileName: att.fileName,
                fileUrl: att.fileUrl || `/api/v1/files/download?id=${att.id}`,
                fileType: 'PickupList',
                createTime: att.createTime
              }))
            }
          } else {
            message.error(attachRes.data.message || '保存提货单记录失败')
          }
        }
      }
    }
    fileInput.click()
  } catch (error) {
    message.error('上传失败')
  }
}

// 获取文件类型标签颜色
const getFileTagColor = (fileType: string) => {
  const colorMap: Record<string, string> = {
    'PickupList': 'orange',
    'Invoice': 'blue',
    'PackingList': 'green',
    'Remittance': 'purple',
    'Contract': 'magenta',
    'FullDocuments': 'cyan'
  }
  return colorMap[fileType] || 'default'
}

// 获取文件类型显示文本
const getFileTypeDisplay = (fileType: string) => {
  const textMap: Record<string, string> = {
    'PickupList': '提货单',
    'Invoice': '商业发票',
    'PackingList': '装箱单',
    'Remittance': '水单',
    'Contract': '合同',
    'FullDocuments': '全套单证'
  }
  return textMap[fileType] || fileType
}

// 下载文件
const handleDownloadFile = (record: any) => {
  const link = document.createElement('a')
  link.href = getFilePreviewUrl(record.id)
  link.download = record.fileName
  link.click()
}

// 删除提货单
const handleDeletePickup = async (record: any) => {
  try {
    const res = await deleteAttachment(record.id)
    if (res.data && res.data.code === 200) {
      pickupAttachments.value = pickupAttachments.value.filter(item => item.id !== record.id)
      message.success('删除成功')
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadData()
  loadCountries()
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
</style>