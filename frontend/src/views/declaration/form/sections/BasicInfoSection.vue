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
                  <a-auto-complete
                    v-model:value="formData.consigneeCompany"
                    :options="consigneeOptions"
                    :filter-option="false"
                    placeholder="选择或输入收货人公司名"
                    :disabled="isFormReadonly"
                    allow-clear
                    style="width: 100%"
                    @select="handleConsigneeSelect"
                  >
                    <template #option="item">
                      <span v-if="item.addNew" style="color: var(--color-primary); font-weight: 500;">
                        <PlusOutlined /> {{ item.label }}
                      </span>
                      <span v-else>{{ item.label }}</span>
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
                    :disabled="isFormReadonly || transportModeLocked"
                    style="width: 100%"
                  />
                  <div v-if="transportModeLocked" style="font-size: 11px; color: #999; margin-top: 2px;">新建时已选定，不可修改</div>
                </a-form-item>
              </a-col>
              <a-col :span="5">
                <a-form-item label="贸易方式">
                  <a-select 
                    v-model:value="formData.tradeTerm" 
                    :options="filteredTradeTermOptions"
                    placeholder="请选择贸易方式" 
                    :disabled="isFormReadonly"
                    style="width: 100%"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="5" v-if="isArrivalPortRequired">
                <a-form-item label="到达港口" required>
                  <a-input 
                    v-model:value="formData.arrivalPort" 
                    placeholder="请输入到达港口" 
                    :disabled="isFormReadonly"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <!-- EXW 贸易方式：买方承担费用，需录入杂费（与到达港口同槽位，条件互斥） -->
              <a-col :span="5" v-if="isExwTradeTerm">
                <a-form-item label="杂费">
                  <a-input-number
                    v-model:value="formData.miscFee"
                    :min="0"
                    :precision="2"
                    placeholder="请输入杂费金额"
                    :disabled="isFormReadonly"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="hasTermExtraCol ? 5 : 7">
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
              <a-col :span="hasTermExtraCol ? 5 : 8">
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
            <a-row :gutter="16">
              <a-col :span="10">
                <a-form-item label="发票号">
                  <a-input 
                    v-model:value="formData.invoiceNo" 
                    placeholder="请输入发票号，留空则自动生成(ZIYI-yy-mmdd格式)" 
                    :readonly="isFormReadonly"
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
            <!-- 乙方资料紧随发票号：与单证销货方取值直接相关，列宽与上一行发票号对齐 -->
            <a-row :gutter="16">
              <a-col :span="10">
                <a-form-item label="乙方（销货方）">
                  <PartyBSelector
                    v-model="formData.partyBId"
                    :options="partyBOptions"
                    :disabled="isFormReadonly"
                    @saved="handlePartyBSaved"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="14">
                <a-form-item label="销货方信息">
                  <div class="party-b-summary" :title="partyBSummary || undefined">
                    <template v-if="partyBSummaryParts.length">
                      <span v-for="part in partyBSummaryParts" :key="part.label" class="party-b-summary__item">
                        <span class="party-b-summary__label">{{ part.label }}：</span>{{ part.value }}
                      </span>
                    </template>
                    <span v-else>未选择乙方，单证销货方信息保持留空</span>
                  </div>
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
                    <span class="total-value">{{ totals.totalVolume.toFixed(4) }}</span>
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
              :scroll="{ x: 1480 }"
              class="carton-table"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'cartonNo'">
                  <!-- 箱号由系统自动生成（类型英文前缀+连续区间），不可手动修改 -->
                  <span class="value-display">{{ record.cartonNo }}</span>
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
                                    // 类型前缀变了，全表箱号重排
                                    regenerateCartonNos();
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
                    @change="() => handleCartonQuantityChange(record)"
                  />
                  <span v-else class="value-display">{{ record.quantity }}</span>
                </template>
                
                <template v-else-if="column.key === 'lengthCm'">
                  <a-input-number
                    v-if="!isFormReadonly"
                    v-model:value="record.lengthCm"
                    :min="0"
                    size="small"
                    style="width: 100%"
                    @change="() => recalcCartonVolume(record)"
                  />
                  <span v-else class="value-display">{{ record.lengthCm ?? '-' }}</span>
                </template>
                
                <template v-else-if="column.key === 'widthCm'">
                  <a-input-number
                    v-if="!isFormReadonly"
                    v-model:value="record.widthCm"
                    :min="0"
                    size="small"
                    style="width: 100%"
                    @change="() => recalcCartonVolume(record)"
                  />
                  <span v-else class="value-display">{{ record.widthCm ?? '-' }}</span>
                </template>
                
                <template v-else-if="column.key === 'heightCm'">
                  <a-input-number
                    v-if="!isFormReadonly"
                    v-model:value="record.heightCm"
                    :min="0"
                    size="small"
                    style="width: 100%"
                    @change="() => recalcCartonVolume(record)"
                  />
                  <span v-else class="value-display">{{ record.heightCm ?? '-' }}</span>
                </template>
                
                <template v-else-if="column.key === 'unitVolume'">
                  <!-- 单箱体积：长×宽×高(cm) ÷ 1,000,000，三维填全才展示 -->
                  <span class="value-display">{{ cartonUnitVolume(record) }}</span>
                </template>
                
                <template v-else-if="column.key === 'volume'">
                  <!-- 体积统一 4 位小数（与 DB decimal(12,4) 对齐） -->
                  <a-input-number 
                    v-if="!isFormReadonly"
                    v-model:value="record.volume" 
                    :min="0"
                    :step="0.0001"
                    :precision="4"
                    size="small"
                    style="width: 100%"
                  />
                  <span v-else class="value-display">{{ formatVolume(record.volume) }} <span style="font-size: 12px; color: #999;">CBM</span></span>
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

          <!-- 资料（基础资料环节，资料项来自资料模板，草稿阶段可预先上传） -->
          <a-card v-if="showDraftMaterialBox" title="资料" size="small" class="section-card">
            <a-spin :spinning="materialLoading">
              <!-- 进度卡片（与其他资料环节样式统一） -->
              <div class="progress-card">
                <div class="progress-left">
                  <div class="progress-title">
                    <FileDoneOutlined class="progress-icon" />
                    <span>资料上传进度</span>
                  </div>
                  <div class="progress-desc">
                    共 <b>{{ draftMaterialStats.total }}</b> 项，
                    必填 <b class="text-red-500">{{ draftMaterialStats.required }}</b> 项，
                    已上传 <b :class="draftMaterialStats.uploaded === draftMaterialStats.required ? 'text-green-500' : 'text-blue-500'">{{ draftMaterialStats.uploaded }}</b> 项
                  </div>
                </div>
                <div class="progress-right">
                  <a-progress
                    type="circle"
                    :percent="draftMaterialStats.percent"
                    :width="60"
                    :stroke-color="draftMaterialStats.percent === 100 ? '#52c41a' : '#1677ff'"
                  />
                </div>
              </div>
              <a-alert
                v-if="!formId && draftMaterialItems.length > 0"
                type="info"
                show-icon
                message="首次上传资料时将自动保存草稿"
                style="margin-bottom: 12px;"
              />
              <a-table
                v-if="draftMaterialItems.length > 0"
                :dataSource="draftMaterialItems"
                :columns="draftMaterialColumns"
                :pagination="false"
                :rowKey="(r: any) => r.id ?? ('tpl-' + r.templateId)"
                size="middle"
                class="material-table"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    <div class="name-cell">
                      <div class="name-main">
                        <span class="name-text">{{ record.name }}</span>
                        <a-tag v-if="isItemRequiredInStage(record, 'BASIC')" color="red">必填</a-tag>
                        <a-tag v-else>选填</a-tag>
                        <div class="name-upload-actions" v-if="draftMaterialEditable">
                          <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record, 'BASIC')">
                            <a-button type="primary" size="small" class="material-upload-btn">
                              <template #icon><UploadOutlined v-if="record.status !== 1" /><PlusOutlined v-else /></template>
                              {{ record.status === 1 ? '追加' : '上传' }}
                            </a-button>
                          </a-upload>
                        </div>
                      </div>
                      <div v-if="record.remark" class="name-remark">{{ record.remark }}</div>
                      <!-- 附件列表（与资料提交环节同款卡片样式） -->
                      <template v-if="record.attachments && record.attachments.length > 0">
                        <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card" :class="{ 'att-increment': Number(att.supplementId) > 0 }">
                          <div class="att-row-main">
                            <div class="att-file-name">
                              <FileTextOutlined class="file-icon-sm" />
                              <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ att.fileName || '查看附件' }}</a>
                              <a-tag v-if="Number(att.supplementId) > 0" color="orange" style="margin-left:4px">补交待审核</a-tag>
                            </div>
                            <a-popconfirm v-if="draftMaterialEditable && !isAttSupplementLocked(att) && canDeleteAttachment(att, 'BASIC')" title="确定删除？" @confirm="handleDeleteAttachment(record, att, 'BASIC')">
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
              <a-empty v-else-if="!materialLoading" description="暂无资料项（请在资料模板中配置基础资料环节的资料项）" :image-style="{ height: '40px' }" />
            </a-spin>
          </a-card>

    <!-- 快速新增客户弹窗 -->
    <a-modal
      v-model:open="quickAddCustomerVisible"
      title="快速新增客户"
      @ok="handleQuickAddCustomer"
      @cancel="handleQuickAddCustomerCancel"
      :confirm-loading="quickAddCustomerSaving"
      width="500px"
    >
      <a-form layout="vertical">
        <a-form-item label="客户公司名" required>
          <a-input v-model:value="quickAddCustomerName" placeholder="请输入客户公司名" />
        </a-form-item>
        <a-form-item label="收货人地址" required>
          <a-textarea v-model:value="quickAddCustomerAddress" placeholder="请输入收货人地址" :rows="2" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="目的国" required>
              <a-select
                v-model:value="quickAddDestinationCountry"
                show-search
                allow-clear
                placeholder="请选择目的国"
                :options="countryOptions"
                :filter-option="(input: string, option: any) => (option.label || '').toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="贸易国" required>
              <a-select
                v-model:value="quickAddTradeCountry"
                show-search
                allow-clear
                placeholder="请选择贸易国"
                :options="countryOptions"
                :filter-option="(input: string, option: any) => (option.label || '').toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 基本信息 + 产品明细 + 箱子信息
 * - formData 通过 inject 直接读写（v-model 直接修改响应式对象）
 * - 结构性操作（添加/删除产品、箱子）通过 emit 通知父组件
 * - 纯 UI 状态（申报要素弹窗）保留在组件内部
 */
import { ref, computed, toRefs, watch } from 'vue'
import { AutoComplete as AAutoComplete } from 'ant-design-vue'
import { useFormState } from '../composables/useDeclarationForm'
import {
  PlusOutlined, DeleteOutlined, EnvironmentOutlined,
  CloseOutlined, CheckOutlined, UploadOutlined, FileTextOutlined,
  FileDoneOutlined, UserOutlined, EditOutlined, ClockCircleOutlined, CloudUploadOutlined,
} from '@ant-design/icons-vue'
import { findUnitByCode } from '@/utils/measurement-unit'
import PartyBSelector from '../PartyBSelector.vue'
import { hasStage, isItemRequiredInStage } from '@/api/system/materialTemplate'
import { canDeleteAttachment } from '@/api/business/materialItem'

// emit：结构性操作（父组件处理数据增删 + API）
const props = withDefaults(defineProps<{
  /** 补交进行中：开放基础资料框的增量上传（存量附件锁定） */
  supplementActive?: boolean
}>(), {
  supplementActive: false
})

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
  formId, formStatus,
  materialItems, materialLoading,
  beforeMaterialUpload, handleDeleteAttachment, previewFile,
  entityList, productList, cartonList,
  cityOptions, countryOptions, currencyOptions,
  transportModeOptions, tradeTermOptions, paymentMethodOptions, productOptions,
  productColumns, cartonColumns, productAutoCompleteOptionsWithCustom,
  hsOptions, measurementUnits,
  totals, getProductCartonInfo, getProductDisplayById,
  syncProductDetails, getMaxQuantity, getMaxWeight,
  handleCompanyChange, filterCompanyOption,
  onDepartureCityChange, filterCountrySelectOption,
  customerList, onCustomerSelect,
  partyBOptions, partyBSummary, partyBSummaryParts, handlePartyBSaved,
  quickAddCustomerVisible, quickAddCustomerName, quickAddCustomerAddress,
  quickAddDestinationCountry, quickAddTradeCountry, quickAddCustomerSaving, handleQuickAddCustomer,
  handleQuantityOrPriceChange, handleUnitChange, handleAmountChange,
  updateProductName, onHsCodeChange,
} = toRefs(state) as any

// 本地 UI 状态：申报要素弹窗
const elementsModalVisible = ref(false)

// ========== 基础资料“资料”框（BASIC 环节，独立于资料提交环节） ==========
// 草稿期可上传/删除；提交后只读展示已传文件；补交进行中开放增量上传
// 注意：不受 isFormReadonly 拦截——补交草稿入口下 isFormReadonly 恒为 true（其它模块只读），
// 但资料框需开放补交上传；查看/审核模式由 supplementActive 为 false 天然拦住
const draftMaterialEditable = computed(() => {
  const s = formStatus.value
  return ((s == null || s === 0) && !isFormReadonly.value) || props.supplementActive
})
// 补交锁定：补交中存量附件（非增量）不可删除，与 MaterialManager 锁规则一致
const isAttSupplementLocked = (att: any): boolean =>
  props.supplementActive && !(Number(att?.supplementId) > 0)
// 仅展示“基础资料”环节的资料项（来自资料模板 + 已上传实例合并视图，stage 支持多环节包含匹配）
const draftMaterialItems = computed(() =>
  ((materialItems.value || []) as any[]).filter(i => hasStage(i.stage, 'BASIC')))
/** 进度统计（口径与 MaterialManager.getSectionStats 一致，必填按基础资料环节判定） */
const draftMaterialStats = computed(() => {
  const items = draftMaterialItems.value
  const total = items.length
  const required = items.filter(i => isItemRequiredInStage(i, 'BASIC')).length
  const uploaded = items.filter(i => isItemRequiredInStage(i, 'BASIC') && i.status === 1).length
  const percent = required > 0 ? Math.round((uploaded / required) * 100) : (total > 0 ? 100 : 0)
  return { total, required, uploaded, percent }
})
const showDraftMaterialBox = computed(() => {
  // 草稿期：始终显示（含未保存时的提示态）
  if (draftMaterialEditable.value) return true
  // 提交后：只要配置了基础资料项就展示（未上传的项走“尚未上传”空态），不再仅限已上传
  return draftMaterialItems.value.length > 0
})
const draftMaterialColumns = [{ title: '资料项', key: 'name' }]

// 贸易方式联动逻辑：根据运输方式过滤可选贸易方式
const filteredTradeTermOptions = computed(() => {
  const mode = formData.value?.transportMode
  if (!mode) return tradeTermOptions.value || []
  // 将运输方式的 name(value) 映射为 code，与关联表中的 transport_mode_code 匹配
  const modeOption = (transportModeOptions.value || []).find((opt: any) => opt.value === mode)
  const modeCode = modeOption?.code || mode
  return (tradeTermOptions.value || []).filter((opt: any) => {
    const modes = opt.transportModes || []
    return modes.length === 0 || modes.includes(modeCode)
  })
})

// 运输方式变更时，若已选贸易方式不在过滤列表中则清空
watch(() => formData.value?.transportMode, () => {
  const current = formData.value?.tradeTerm
  if (current && !filteredTradeTermOptions.value.some((opt: any) => opt.value === current)) {
    formData.value.tradeTerm = undefined
  }
})

// 贸易方式联动逻辑：C组和D组需要到达港口
const isTradeTermRequired = computed(() => {
  const tradeTerm = formData.value?.tradeTerm
  if (!tradeTerm) return false
  const option = filteredTradeTermOptions.value?.find((opt: any) => opt.value === tradeTerm)
  return option?.groupName === 'C组' || option?.groupName === 'D组'
})

const isArrivalPortRequired = computed(() => {
  return isTradeTermRequired.value
})

// 下拉选项：基于当前输入的公司名实时计算（a-auto-complete 的 v-model 就是输入文本）
// 区分已有客户与“快速新增”；快速新增项 value 即输入文本，选中后输入框仍显示文本
const consigneeOptions = computed(() => {
  const q = (formData.value?.consigneeCompany || '') as string
  const list = customerList.value || []
  const opts = list
    .filter((c: any) => !q || c.customerName.toLowerCase().includes(q.toLowerCase()))
    .map((c: any) => ({ value: c.customerName, label: c.customerName, addNew: false }))
  if (q && !list.some((c: any) => c.customerName.toLowerCase() === q.toLowerCase())) {
    opts.push({ value: q, label: `快速新增：${q}`, addNew: true })
  }
  return opts
})

// 取消快速新增客户弹窗：保留当前公司名文本
const handleQuickAddCustomerCancel = () => {
  // 不需额外处理，formData.consigneeCompany 已为输入文本
}

// 收货人公司名选择处理（a-auto-complete @select）
// option.addNew 为 true 表示“快速新增”，否则为已有客户
const handleConsigneeSelect = (value: any, option: any) => {
  if (option && option.addNew) {
    // 新客户：打开快速新增弹窗，公司名 = 输入文本
    onCustomerSelect.value('__add_new__', value)
  } else {
    // 选择已有客户，自动填充地址/目的国/贸易国
    onCustomerSelect.value(value)
  }
}

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

// 箱号全自动生成：类型英文前缀 + 全表按数量连续编号（如 CARTONS1-90 / PALLETS91-140），
// 单箱不带区间直接“CARTONS1”，箱号列不可手动修改
function regenerateCartonNos() {
  let start = 1
  ;(cartonList.value || []).forEach((c: any) => {
    const qty = Number(c.quantity) || 1
    const prefix = String(c.typeEnglish || (c.typeChinese === '托盘' ? 'PALLETS' : 'CARTONS')).toUpperCase()
    c.cartonNo = qty > 1 ? `${prefix}${start}-${start + qty - 1}` : `${prefix}${start}`
    start += qty
  })
}
// 增删箱子时重排箱号；首次加载（prev=0）不覆盖历史单证的既有箱号，
// 用户改动数量/类型后才按新规则接管
let lastCartonCount = 0
watch(() => cartonList.value?.length ?? 0, (len) => {
  if (lastCartonCount > 0 && len !== lastCartonCount) regenerateCartonNos()
  lastCartonCount = len
})
function handleCartonQuantityChange(record: any) {
  regenerateCartonNos()
  recalcCartonVolume(record)
}
// 体积展示：统一保留 4 位小数（DB 列为 decimal(12,4)）
function formatVolume(value: any): string {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(4) : '-'
}
// 单箱体积(CBM)：长×宽×高(cm) ÷ 1,000,000 保留4位小数；三维未填全返回 '-'
function cartonUnitVolume(record: any): string {
  const l = Number(record.lengthCm), w = Number(record.widthCm), h = Number(record.heightCm)
  if (l > 0 && w > 0 && h > 0) {
    return (Math.round((l * w * h) / 1e6 * 10000) / 10000).toFixed(4)
  }
  return '-'
}
// 体积自动计算：单箱 长×宽×高(cm) ÷ 1,000,000 → CBM × 数量，保留4位小数；
// 三维填全才重算，否则保留手动体积（兼容老数据）
function recalcCartonVolume(record: any) {
  const l = Number(record.lengthCm), w = Number(record.widthCm), h = Number(record.heightCm), q = Number(record.quantity)
  if (l > 0 && w > 0 && h > 0 && q > 0) {
    record.volume = Math.round((l * w * h * q) / 1e6 * 10000) / 10000
  }
}

// EXW 贸易方式：显示杂费录入
const isExwTradeTerm = computed(() => String(formData.value?.tradeTerm || '').toUpperCase() === 'EXW')
// 运输方式行是否占用了条件槽位（到达港口/杂费二选一，互斥），决定支付方式与币种宽度
const hasTermExtraCol = computed(() => isArrivalPortRequired.value || isExwTradeTerm.value)

// 过滤函数（纯 UI，本地定义）
const filterProductOption = (input: string, option: any) => {
  const label = option.label || ''
  const lower = input.toLowerCase()
  return String(label).toLowerCase().includes(lower)
}
</script>

<style scoped>
/* 销货方信息摘要：字段名与值成对展示，最多两行，折不下时靠 title 悬浮看全文 */
.party-b-summary {
  min-height: 32px;
  max-height: 38px;
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  gap: 2px 16px;
  overflow: hidden;
  font-size: 12px;
  line-height: 18px;
  color: #64748B;
}

.party-b-summary__item {
  white-space: nowrap;
}

.party-b-summary__label {
  color: #94A3B8;
}

/* 补交增量附件高亮：浅橙底色 + 橙色边框，与存量文件区分（与 MaterialManager 同款） */
.att-invoice-card.att-increment {
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
}
</style>
