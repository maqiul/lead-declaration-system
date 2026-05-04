<template>
  <div class="declaration-manage bg-white p-6 min-h-full">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="申报单号">
          <a-input-search 
            v-model:value="searchForm.formNo" 
            placeholder="搜索申报单号" 
            @search="loadData"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="searchForm.status"
            placeholder="状态筛选"
            style="width: 140px"
            @change="loadData"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">待初审</a-select-option>
            <a-select-option :value="2">待资料提交</a-select-option>
            <a-select-option :value="3">待资料审核</a-select-option>
            <a-select-option :value="4">待发票提交</a-select-option>
            <a-select-option :value="5">待发票审核</a-select-option>
            <a-select-option :value="6">已完成</a-select-option>
            <a-select-option :value="9">退回待审</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期">
          <a-range-picker 
            v-model:value="searchForm.dateRange" 
            style="width: 200px"
            @change="loadData"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData" v-permission="['business:declaration:view']">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetSearch">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleAdd" v-permission="['business:declaration:create']">
          <template #icon><plus-outlined /></template>
          新增申报单
        </a-button>
        <a-button @click="handleExport" v-permission="['business:declaration:export']">
          <template #icon><download-outlined /></template>
          导出
        </a-button>
      </a-space>
    </a-card>

    <a-card class="ui-card">
      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1500 }"
        rowKey="id"
        @change="handleTableChange"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <a @click="handleView(record as any)">{{ record.formNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <!--
                简化后的操作按钮逻辑:
                - 草稿状态(status=0): [编辑] [提交]
                - 待审核(status=1): [审核] (仅审核角色)
                - 所有状态都有: [更多▼]
              -->
              
              <!-- 草稿状态: 编辑、提交 -->
              <template v-if="record.status === 0">
                <a-button type="link" size="small" @click="handleEdit(record as any)" v-permission="['business:declaration:update']">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button type="link" size="small" @click="handleStatusSubmit(record as any)" v-permission="['business:declaration:submit']">
                  <template #icon><SendOutlined /></template>
                  提交
                </a-button>
              </template>

              <!-- 待初审状态: 初审按钮 -->
              <template v-if="record.status === 1">
                <a-button type="link" size="small" style="color: #faad14;" @click="handleAudit(record as any, 'deptAudit')" v-permission="['business:declaration:audit:initial']">
                  <template #icon><CheckCircleOutlined /></template>
                  初审
                </a-button>
              </template>

              <!-- 待资料提交状态: 提交资料按钮 -->
              <template v-if="record.status === 2">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleMaterialSubmit(record as any)" v-permission="['business:declaration:material:submit']">
                  <template #icon><UploadOutlined /></template>
                  提交资料
                </a-button>
              </template>

              <!-- 待资料审核状态: 资料审核按钮 -->
              <template v-if="record.status === 3">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleMaterialAudit(record as any)" v-permission="['business:declaration:audit:material']">
                  <template #icon><CheckCircleOutlined /></template>
                  资料审核
                </a-button>
              </template>

              <!-- 待发票提交状态: 提交发票按钮 -->
              <template v-if="record.status === 4">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleGoSubmitInvoice(record as any)" v-permission="['business:declaration:invoice:submit']">
                  <template #icon><UploadOutlined /></template>
                  提交发票
                </a-button>
              </template>

              <!-- 待发票审核状态: 发票审核按钮 -->
              <template v-if="record.status === 5">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleInvoiceAudit(record as any)" v-permission="['business:declaration:audit:invoice']">
                  <template #icon><CheckCircleOutlined /></template>
                  发票审核
                </a-button>
              </template>

              <!-- 更多操作菜单 -->
              <a-dropdown>
                <a-button type="link" size="small">
                  更多
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <!-- 上传发票 -->
                    <a-menu-item
                      v-if="checkPermission(['business:declaration:update'])"
                      key="invoice"
                      @click="handleUploadInvoice(record as any)"
                    >
                      <FileTextOutlined /> 上传发票
                    </a-menu-item>

                    <!-- 查看资料 (status>=3) -->
                    <a-menu-item
                      v-if="record.status >= 3"
                      key="viewMaterial"
                      @click="handleViewMaterial(record as any)"
                    >
                      <FileTextOutlined /> 查看资料
                    </a-menu-item>

                    <!-- 财务补充 (仅财务角色，需发票审核通过) -->
                    <a-menu-item
                      v-if="record.status >= 2 && checkPermission(['business:declaration:finance:supplement'])"
                      key="finance"
                      @click="handleFinanceUpload(record as any)"
                    >
                      <MoneyCollectOutlined /> 财务补充
                    </a-menu-item>

                    <!-- 关联水单 -->
                    <a-menu-item
                      v-if="checkPermission(['business:declaration:finance:remittance'])"
                      key="relate"
                      @click="handleRelateRemittance(record as any)"
                    >
                      <LinkOutlined /> 关联水单
                    </a-menu-item>

                    <!-- 创建水单 -->
                    <a-menu-item
                      v-if="checkPermission(['business:remittance:create'])"
                      key="createRemittance"
                      @click="handleCreateAndRelateRemittance(record as any)"
                    >
                      <PlusOutlined /> 创建水单
                    </a-menu-item>

                    <!-- 单证下载 -->
                    <a-menu-item key="download" @click="handleDownload(record as any)">
                      <DownloadOutlined /> 单证下载
                    </a-menu-item>

                    <!-- 恢复老流程：仅当 status 在 2~5 且无活跃任务时显示（老数据修复，需专用权限） -->
                    <a-menu-item
                      v-if="record.status >= 2 && record.status <= 5 && (!record.activeTasks || record.activeTasks.length === 0) && checkPermission(['business:declaration:resume:flow'])"
                      key="resumeFlow"
                      @click="handleResumeFlow(record as any)"
                    >
                      <ReloadOutlined /> 恢复流程
                    </a-menu-item>

                    <!-- 生成合同 -->
                    <a-menu-item
                      v-if="!record.hasContract && checkPermission(['business:declaration:contract'])"
                      key="contract"
                      @click="handleOpenGenerate(record)"
                    >
                      <FileTextOutlined /> 生成合同
                    </a-menu-item>

                    <!-- 申请退回草稿 (已提交状态，不包括退回待审) -->
                    <a-menu-item
                      v-if="record.status >= 2 && record.status !== 9 && checkPermission(['business:declaration:return:apply'])"
                      key="returnApply"
                      @click="handleReturnApply(record as any)"
                    >
                      <ReloadOutlined /> 申请退回草稿
                    </a-menu-item>

                    <!-- 退回审核 (仅退回待审状态) -->
                    <a-menu-item
                      v-if="record.status === 9 && checkPermission(['business:declaration:return:audit'])"
                      key="returnAudit"
                      @click="handleReturnAudit(record as any)"
                    >
                      <AuditOutlined /> 退回审核
                    </a-menu-item>

                    <!-- 审核详情 -->
                    <a-menu-item
                      key="returnHistory"
                      @click="viewReturnHistory(record as any)"
                    >
                      <HistoryOutlined /> 审核详情
                    </a-menu-item>

                    <!-- 删除 (仅草稿) -->
                    <a-menu-item v-if="record.status === 0" key="delete" danger>
                      <a-popconfirm
                        title="确定要删除该申报单吗?"
                        @confirm="handleDelete(record as any)"
                        placement="left"
                      >
                        <DeleteOutlined /> 删除
                      </a-popconfirm>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 附件下载弹窗 -->
    <a-modal 
      v-model:open="attachmentModalVisible" 
      title="附件管理" 
      width="700px"
    >
      <template #footer>
        <a-button @click="attachmentModalVisible = false">
          <template #icon><CloseOutlined /></template>
          关闭
        </a-button>
      </template>
      
      <a-list :dataSource="currentAttachments" bordered size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <div class="attachment-title">
                  <FileOutlined v-if="isDocumentFile(item.fileName)" style="color: #FA8C16; margin-right: 8px;" />
                  <PictureOutlined v-else-if="isImageFile(item.fileName)" style="color: #52c41a; margin-right: 8px;" />
                  <FileUnknownOutlined v-else style="color: #faad14; margin-right: 8px;" />
                  {{ item.fileName }}
                </div>
              </template>
              <template #description>
                <div class="attachment-info">
                  <a-tag :color="getFileTypeColor(item.fileType)">
                    {{ getFileTypeText(item.fileType) }}
                  </a-tag>
                  <span class="file-size">{{ formatFileSize(item.fileSize) }}</span>
                  <span class="create-time">{{ formatDate(item.createTime) }}</span>
                </div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-space>
                <a-button type="link" size="small" @click="downloadAttachment(item)" v-permission="['business:declaration:download']">
                  <template #icon><DownloadOutlined /></template>
                  下载
                </a-button>
                <!-- 统一的重新生成按钮 -->
                <a-button 
                  type="link" 
                  size="small" 
                  style="color: #FA8C16;" 
                  @click="handleRegenerateSimple(item)"
                  v-permission="['business:declaration:audit']"
                >
                  <template #icon><ReloadOutlined /></template>
                  重新生成
                </a-button>
                <a-button 
                  type="link" 
                  size="small" 
                  style="color: #faad14;"
                  @click="showReplaceModal(item)"
                  v-permission="['business:declaration:audit']"
                >
                  <template #icon><UploadOutlined /></template>
                  替换
                </a-button>
              </a-space>
            </template>
          </a-list-item>
        </template>
        <template v-if="currentAttachments.length === 0" #header>
          <div style="text-align: center; color: #999;">暂无自动生成的全套单证或水单文件</div>
        </template>
      </a-list>
      
      <!-- 合同列表 -->
      <div style="margin-top: 24px;">
        <h3>相关合同</h3>
        <a-list :dataSource="currentContracts" bordered size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <div class="attachment-title">
                    <FileTextOutlined style="color: #D46B08; margin-right: 8px;" />
                    {{ item.generatedFileName }}
                  </div>
                </template>
                <template #description>
                  <div class="attachment-info">
                    <a-tag color="#D46B08">合同</a-tag>
                    <span class="file-size">{{ formatFileSize(item.fileSize) }}</span>
                    <span class="create-time">{{ formatDate(item.generatedTime) }}</span>
                    <span v-if="item.templateName" style="margin-left: 8px; color: #999;">模板: {{ item.templateName }}</span>
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-space>
                  <a-button type="link" size="small" @click="downloadContract(item.id)" v-permission="['business:contract:download']">
                    <template #icon><DownloadOutlined /></template>
                    下载
                  </a-button>
                  <a-button 
                    type="link" 
                    size="small" 
                    style="color: #faad14;"
                    @click="showReplaceContractModal(item)"
                     v-permission="['business:declaration:audit']"
                  >
                    <template #icon><UploadOutlined /></template>
                    替换
                  </a-button>
                </a-space>
              </template>
            </a-list-item>
          </template>
          <template v-if="currentContracts.length === 0" #header>
            <div style="text-align: center; color: #999;">暂无相关合同</div>
          </template>
        </a-list>
      </div>
    </a-modal>
    
    <!-- 合同替换弹窗 -->
    <a-modal
      v-model:open="replaceContractModalVisible"
      title="替换合同"
      @ok="handleReplaceContract"
      :confirmLoading="replaceContractLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="当前合同">
          <div>{{ currentReplacingContract?.generatedFileName }}</div>
          <div v-if="currentReplacingContract?.templateName" style="color: #999; font-size: 12px; margin-top: 4px;">
            模板: {{ currentReplacingContract.templateName }}
          </div>
        </a-form-item>
        <a-form-item label="选择新合同文件" required>
          <a-upload
            :before-upload="beforeReplaceContractUpload"
            :file-list="replaceContractFileList"
            :max-count="1"
            accept=".docx"
          >
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择文件
            </a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">
            仅支持.docx格式的Word文档，单个文件不超过10MB
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="replaceModalVisible"
      title="替换附件"
      @ok="handleReplaceAttachment"
      :confirmLoading="replaceLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="当前文件">
          <div>{{ currentReplacingAttachment?.fileName }}</div>
        </a-form-item>
        <a-form-item label="选择新文件" required>
          <a-upload
            :before-upload="beforeReplaceUpload"
            :file-list="replaceFileList"
            :max-count="1"
            accept=".xlsx,.xls,.pdf,.jpg,.jpeg,.png"
          >
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择文件
            </a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">
            支持Excel、PDF、图片格式，单个文件不超过10MB
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 生成合同弹窗 -->
    <a-modal
      v-model:open="generateModalVisible"
      title="生成合同"
      @ok="handleConfirmGenerate"
      :confirmLoading="generateLoading"
    >
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="选择合同模板" required>
          <a-select 
            v-model:value="generateForm.templateId"
            placeholder="请选择合同模板"
            :options="templateOptions"
            :fieldNames="{ label: 'templateName', value: 'id' }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    
    <!-- 财务补充弹窗 -->
    <FinanceModal
      v-model:visible="financeModalVisible"
      :form-id="currentRecordForFinance?.id || 0"
      :form-no="currentRecordForFinance?.formNo || ''"
      @save-success="handleFinanceSaveSuccess"
    />

    <!-- 关联水单弹窗 -->
    <RemittanceRelationModal
      v-model:visible="remittanceRelationVisible"
      :form-id="currentFormIdForRemittance"
      :form-no="currentFormNoForRemittance"
      @success="loadData"
    />

    <!-- 资料审核弹窗 -->
    <MaterialAuditModal
      v-model:open="materialAuditVisible"
      :form-id="currentFormIdForMaterial"
      @audited="loadData"
    />

    <!-- 发票审核弹窗 -->
    <InvoiceAuditModal
      v-model:open="invoiceAuditVisible"
      :form-id="currentFormIdForInvoice"
      @audited="loadData"
    />

    <!-- 创建水单弹窗 -->
    <a-modal
      v-model:open="createRemittanceVisible"
      :title="`创建水单并关联 - ${currentFormNoForRemittance}`"
      width="700px"
      :confirm-loading="createRemittanceLoading"
      @ok="handleSubmitCreateRemittance"
      @cancel="createRemittanceVisible = false"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="币种">
              <a-select v-model:value="remittanceFormData.currency">
                <a-select-option value="USD">USD - 美元</a-select-option>
                <a-select-option value="CNY">CNY - 人民币</a-select-option>
                <a-select-option value="EUR">EUR - 欧元</a-select-option>
                <a-select-option value="GBP">GBP - 英镑</a-select-option>
                <a-select-option value="JPY">JPY - 日元</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收汇名称" required>
              <a-input v-model:value="remittanceFormData.remittanceName" placeholder="请输入收汇名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="收汇日期" required>
              <a-date-picker v-model:value="remittanceFormData.remittanceDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收汇金额" required>
              <a-input-number
                v-model:value="remittanceFormData.remittanceAmount"
                :min="0"
                :precision="2"
                placeholder="请输入收汇金额"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注">
          <a-textarea v-model:value="remittanceFormData.remarks" placeholder="请输入备注" :rows="2" />
        </a-form-item>

        <a-form-item label="水单文件" required>
          <a-upload
            :before-upload="handleUploadRemittancePhoto"
            :file-list="remittanceFormData.fileList"
            @remove="handleRemoveRemittancePhoto"
            :max-count="1"
          >
            <a-button v-if="!remittanceFormData.photoUrl">
              <template #icon><UploadOutlined /></template>
              上传文件
            </a-button>
          </a-upload>
          <div v-if="remittanceFormData.photoUrl" style="margin-top: 8px">
            <a-tag color="blue">
              <FileOutlined /> {{ getRemittanceFileExtension() }}
            </a-tag>
            <a-button type="link" size="small" @click="previewRemittanceFile">
              <template #icon><EyeOutlined /></template>
              查看文件
            </a-button>
          </div>
          <div v-if="!remittanceFormData.photoUrl" style="margin-top: 8px; color: #ff4d4f; font-size: 12px;">
            * 水单文件为必填项，支持图片、PDF等格式
          </div>
        </a-form-item>

        <a-alert 
          message="提示" 
          description="创建水单后将自动关联当前申报单并提交审核" 
          type="info" 
          show-icon 
          style="margin-top: 16px" 
        />
      </a-form>
    </a-modal>

    <!-- 退回草稿申请弹窗 -->
    <a-modal
      v-model:open="returnApplyVisible"
      title="申请退回草稿"
      @ok="submitReturnApply"
      :confirmLoading="returnApplyLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="退回原因" required>
          <a-textarea v-model:value="returnApplyForm.reason" placeholder="请输入退回草稿的原因" :rows="4" />
          <div style="margin-top: 8px; color: #999; font-size: 12px;">注：申请后单据将进入“退回待审”状态。</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 退回审核弹窗 -->
    <a-modal
      v-model:open="returnAuditVisible"
      title="审核退回申请"
      @ok="submitReturnAudit"
      :confirmLoading="returnAuditLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="returnAuditForm.approved">
            <a-radio :value="true">通过 (重置为草稿)</a-radio>
            <a-radio :value="false">驳回 (恢复原状态)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核备注" required>
          <a-textarea v-model:value="returnAuditForm.remark" placeholder="请输入审核备注" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审核历史弹窗 -->
    <a-modal
      v-model:open="returnHistoryVisible"
      title="审核历史详情"
      width="1200px"
      :footer="null"
    >
      <a-table
        :dataSource="returnHistoryList"
        :columns="returnHistoryColumns"
        :loading="returnHistoryLoading"
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
            <a-tag color="orange">{{ getBusinessTypeText(record.businessType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'preStatus'">
            <a-tag>{{ getStatusText(record.preStatus) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 发票上传弹窗 -->
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
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择文件
            </a-button>
          </a-upload>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DownloadOutlined, EditOutlined, CheckCircleOutlined, DeleteOutlined, SendOutlined, UploadOutlined, FileTextOutlined, FileOutlined, PictureOutlined, FileUnknownOutlined, ReloadOutlined, MoneyCollectOutlined, DownOutlined, HistoryOutlined, LinkOutlined, SearchOutlined, CloseOutlined, EyeOutlined, AuditOutlined } from '@ant-design/icons-vue'
import { checkPermission } from '@/directives/permission'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  getDeclarationList,
  deleteDeclaration as deleteDeclarationApi,
  getDeclarationDetail,
  submitDeclaration,
  getDeclarationAttachments,
  regenerateDocuments,
  regenerateAllDocuments,
  regenerateRemittanceReport,
  getBatchActiveTasks,
  resumeDeclarationFlow,
  applyReturnToDraft,
  auditReturnToDraft,
  getReturnAuditHistory,
  uploadBusinessInvoice
} from '@/api/business/declaration'
import { getEnabledTemplates, generateContract, downloadContract, getContractsByDeclaration, replaceContractFile } from '@/api/business/contract'
import { createRemittance, relateToForm, submitRemittanceAudit } from '@/api/business/remittance'
import { uploadFile } from '@/api/business/declaration'
import { h } from 'vue'
// import { hasPermission } from '@/utils/permission'

import { useRoute } from 'vue-router'

import FinanceModal from '../finance/components/FinanceModal.vue'
import RemittanceRelationModal from './components/RemittanceRelationModal.vue'
import MaterialAuditModal from '../material/components/MaterialAuditModal.vue'
import InvoiceAuditModal from '../material/components/InvoiceAuditModal.vue'

const router = useRouter()
const route = useRoute()
const searchForm = reactive({
  formNo: '',
  status: '',
  dateRange: undefined as [Dayjs, Dayjs] | undefined
})

// 表格数据
interface DeclarationRecord {
  id: number
  formNo: string
  shipperCompany?: string
  consigneeCompany?: string
  declarationDate?: string
  totalAmount?: number
  totalCartons?: number
  status: number
  createTime?: string
  financeUploadPending?: boolean
  attachments?: any[]
  hasContract?: boolean
  regenerateButtons?: any[]
  activeTasks?: string[]
}

const dataSource = ref<DeclarationRecord[]>([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 160 },
  { title: '申报人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 150 },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 150 },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate', width: 120 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 80 },
  { 
    title: '状态', 
    key: 'status', 
    width: 100,
    customRender: ({ record }: { record: any }) => {
      const statusText = getStatusText(record.status)
      const statusColor = getStatusColor(record.status)
      return h('a-tag', { color: statusColor }, statusText)
    }
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right' as const
  }
]

// 弹窗相关
// 附件下载弹窗
const attachmentModalVisible = ref(false)
const currentAttachments = ref<any[]>([])
const currentContracts = ref<any[]>([]) // 新增：合同列表
const currentContractsLoading = ref(false) // 合同数据加载状态
const currentDeclaration = ref<DeclarationRecord | null>(null)

// 财务补充弹窗
const financeModalVisible = ref(false)
const currentRecordForFinance = ref<DeclarationRecord | null>(null)

// 资料审核弹窗
const materialAuditVisible = ref(false)
const currentFormIdForMaterial = ref<number | string | null>(null)
// 发票审核弹窗
const invoiceAuditVisible = ref(false)
const currentFormIdForInvoice = ref<number | string | null>(null)

// 关联水单弹窗
const remittanceRelationVisible = ref(false)
const currentFormIdForRemittance = ref<number>(0)
const currentFormNoForRemittance = ref<string>('')

// 创建水单弹窗
const createRemittanceVisible = ref(false)
const createRemittanceLoading = ref(false)
const remittanceFormData = reactive({
  remittanceName: '',
  remittanceDate: undefined as Dayjs | string | undefined,
  remittanceAmount: undefined as number | undefined,
  currency: 'USD',
  remarks: '',
  photoUrl: '',
  fileList: [] as any[]
})

// 附件替换弹窗
const replaceModalVisible = ref(false)
const replaceLoading = ref(false)
const currentReplacingAttachment = ref<any>(null)
const replaceFileList = ref<any[]>([])

// 合同替换弹窗
const replaceContractModalVisible = ref(false)
const replaceContractLoading = ref(false)
const currentReplacingContract = ref<any>(null)
const replaceContractFileList = ref<any[]>([])

// 退回申请相关
const returnApplyVisible = ref(false)
const returnApplyLoading = ref(false)
const returnApplyForm = reactive({
  id: 0,
  reason: '申报错误'
})

// 退回审核相关
const returnAuditVisible = ref(false)
const returnAuditLoading = ref(false)
const returnAuditForm = reactive({
  id: 0,
  approved: true,
  remark: '已核对数据，通过'
})

// 监听审核结果变化，自动更新默认备注
watch(() => returnAuditForm.approved, (newVal) => {
  if (newVal) {
    returnAuditForm.remark = '已核对数据，通过'
  } else {
    returnAuditForm.remark = '数据填写错误'
  }
})

// 审核详情相关
const returnHistoryVisible = ref(false)
const returnHistoryLoading = ref(false)
const returnHistoryList = ref<any[]>([])

// 发票上传相关
const invoiceModalVisible = ref(false)
const invoiceFileList = ref<any[]>([])
const tempInvoiceFile = ref<any>(null)
const currentInvoiceFormId = ref<number>(0)
const invoiceForm = reactive({
  invoiceType: 1,
  invoiceName: '',
  invoiceNo: '',
  amount: undefined as number | undefined,
  taxAmount: undefined as number | undefined
})

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      formNo: searchForm.formNo || undefined,
      status: searchForm.status !== '' ? Number(searchForm.status) : undefined
    }
    
    const response = await getDeclarationList(params)
    
    if (response.data.code === 200) {
      // 增加数组安全性检查
      const rawRecords = response.data.data?.records
      const records = Array.isArray(rawRecords) ? rawRecords : []
      const recordsWithAttachments = await Promise.all(
        records.map(async (record) => {
          // 预加载附件信息
          if (record.status >= 1) { // 只为需要显示按钮的记录加载附件（status>=1时已生成预录入单）
            try {
              const attachmentResponse = await getDeclarationAttachments(record.id)
              if (attachmentResponse.data && attachmentResponse.data.code === 200) {
                const attachments = attachmentResponse.data.data || []
                record.attachments = attachments
                            
                // 不再预处理按钮信息，改为运行时判断
              }
            } catch (error) {
              console.warn(`获取申报单${record.id}附件信息失败:`, error)
              record.attachments = []
              record.regenerateButtons = []
            }
          } else {
            record.attachments = []
            record.regenerateButtons = []
          }
          
          // 预加载合同信息（用于生成合同按钮判断）
          if (record.status >= 1) { // 初审通过后的申报单才需要判断合同
            try {
              const contractResponse = await getContractsByDeclaration(record.id)
              if (contractResponse.data && contractResponse.data.code === 200) {
                record.hasContract = (contractResponse.data.data || []).length > 0
              } else {
                record.hasContract = false
              }
            } catch (error) {
              console.warn(`获取申报单${record.id}合同信息失败:`, error)
              record.hasContract = false
            }
          } else {
            record.hasContract = false
          }
          
          return record
        })
      )
      
      dataSource.value = recordsWithAttachments
      pagination.total = response.data.data.total
      
      // 批量获取需要显示审核按钮的记录的活跃任务（状态2,3,4,5,7）
      const processingIds = dataSource.value
        .filter((r: any) => [2, 3, 4, 5, 7].includes(r.status))
        .map((r: any) => r.id)
      
      if (processingIds.length > 0) {
        try {
          console.log('获取批量任务，IDs:', processingIds.join(','))
          const taskRes = await getBatchActiveTasks(processingIds.join(','))
          console.log('批量任务响应:', taskRes)
          if (taskRes.data && taskRes.data.code === 200 && taskRes.data.data) {
            dataSource.value.forEach((r: any) => {
              r.activeTasks = taskRes.data.data[String(r.id)] || []
            })
          } else {
            console.warn('批量任务API返回异常:', taskRes.data)
            message.warning('获取任务信息失败')
          }
        } catch (e: any) {
          console.error('获取批量任务失败:', e)
          message.error('获取任务信息失败: ' + (e.response?.data?.message || e.message || '未知错误'))
        }
      }
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    
  } catch (error: any) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败: ' + (error.message || '未知错误'))
    // 出错时使用空数据
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 定时刷新活跃任务信息（每30秒刷新一次）
let refreshTimer: number | null = null
const startAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = window.setInterval(() => {
    // 只刷新需要显示审核按钮的记录的活跃任务（状态2,3,4,5,7）
    const processingIds = dataSource.value
      .filter((r: any) => [2, 3, 4, 5, 7].includes(r.status))
      .map((r: any) => r.id)
    
    if (processingIds.length > 0) {
      getBatchActiveTasks(processingIds.join(','))
        .then(taskRes => {
          if (taskRes.data && taskRes.data.code === 200 && taskRes.data.data) {
            dataSource.value.forEach((r: any) => {
              const newTasks = taskRes.data.data[String(r.id)] || []
              // 只有当任务列表发生变化时才更新，避免不必要的重新渲染
              if (JSON.stringify(r.activeTasks || []) !== JSON.stringify(newTasks)) {
                r.activeTasks = newTasks
              }
            })
          }
        })
        .catch(e => {
          console.warn('定时刷新任务失败:', e)
        })
    }
  }, 30000) // 30秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.formNo = ''
  searchForm.status = ''
  searchForm.dateRange = undefined
  pagination.current = 1
  loadData()
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新增申报单
const handleAdd = () => {
  router.push('/declaration/form')
}

// 查看详情
const handleView = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&readonly=true&status=${record.status}`)
}

// 提交操作 (从列表直接触发流程启动)
const handleStatusSubmit = async (record: DeclarationRecord) => {
  try {
    loading.value = true
    const res = await submitDeclaration(record.id)
    if (res.data && res.data.code === 200) {
      message.success('流程启动完成，已进入部门初审阶段')
      loadData()
    } else {
      message.error('提交失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('提交操作失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 编辑申报单
const handleEdit = (record: DeclarationRecord) => {
  // 检查状态,如果已提交则不允许编辑
  if (record.status !== 0) {
    message.warning('只有草稿状态的申报单可以编辑')
    return
  }
  router.push(`/declaration/form?id=${record.id}&status=${record.status}`)
}

// 付款操作（定金/尾款）- 跳转到水单提交模式
// const handlePayment = (record: DeclarationRecord, type?: 'deposit' | 'balance') => {
//   console.log('handlePayment called with:', { record, type })
//   const query: Record<string, any> = { 
//     id: record.id, 
//     status: record.status, 
//     mode: 'payment' 
//   }
//   if (type) query.type = type
//   console.log('Router push query:', query)
//   router.push({ path: '/declaration/form', query })
// }

// 审核申报单
const handleAudit = (record: DeclarationRecord, taskKey?: string) => {
  const query: Record<string, any> = { id: record.id, mode: 'audit' }
  if (taskKey) query.taskKey = taskKey
  router.push({ path: '/declaration/form', query })
}

// 财务补充 - 弹窗模式（有数据则编辑，无数据则新增）
const handleFinanceUpload = (record: DeclarationRecord) => {
  currentRecordForFinance.value = record
  financeModalVisible.value = true
}

// 提交申报资料——跳转到主表单 mode=material
const handleMaterialSubmit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=material`)
}

// 查看已提交的资料——跳转到主表单 mode=material且 readonly
const handleViewMaterial = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=material&readonly=true`)
}

// 资料审核（跳转到详情页审核）
const handleMaterialAudit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=materialAudit`)
}

// 提交业务发票——跳转到主表单 mode=invoiceUpload
const handleGoSubmitInvoice = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=invoiceUpload`)
}

// 发票审核（跳转到详情页审核）
const handleInvoiceAudit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=invoiceAudit`)
}

// 恢复老流程：老流程在 status=2 就结束，使用本功能一键启动新版流程并跳至对应节点
const handleResumeFlow = (record: DeclarationRecord) => {
  Modal.confirm({
    title: '确认恢复流程？',
    content: `申报单 ${record.formNo || record.id} 当前状态=${record.status}，将迁移到新版流程对应节点。`,
    okText: '确认恢复',
    onOk: async () => {
      try {
        const res: any = await resumeDeclarationFlow(record.id)
        if (res.data?.code === 200) {
          message.success(`流程已恢复，当前节点：${res.data.data?.targetActivityId || '-'}`)
          loadData()
        } else {
          message.error(res.data?.message || '恢复失败')
        }
      } catch (e: any) {
        message.error('恢复失败: ' + (e.response?.data?.message || e.message || '未知错误'))
      }
    }
  })
}

// 财务补充保存成功
const handleFinanceSaveSuccess = () => {
  message.success('财务补充保存成功')
  loadData()
}

// 关联水单
const handleRelateRemittance = (record: DeclarationRecord) => {
  currentFormIdForRemittance.value = record.id
  currentFormNoForRemittance.value = record.formNo
  remittanceRelationVisible.value = true
}

// 创建并关联水单
const handleCreateAndRelateRemittance = (record: DeclarationRecord) => {
  currentFormIdForRemittance.value = record.id
  currentFormNoForRemittance.value = record.formNo
  createRemittanceVisible.value = true
  // 重置表单数据
  remittanceFormData.remittanceName = ''
  remittanceFormData.remittanceDate = undefined
  remittanceFormData.remittanceAmount = undefined
  remittanceFormData.currency = 'USD'
  remittanceFormData.remarks = ''
  remittanceFormData.photoUrl = ''
  remittanceFormData.fileList = []
}

// 上传发票 - 跳转到申报详情页
const handleUploadInvoice = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&mode=invoiceUpload`)
}

// 发票文件上传前校验
const beforeInvoiceUpload = (file: File) => {
  const isAllowed = file.type === 'application/pdf' || 
                    file.type === 'image/jpeg' || 
                    file.type === 'image/png'
  if (!isAllowed) {
    message.error('只支持PDF、JPG、PNG格式')
    return false
  }
  tempInvoiceFile.value = file
  invoiceFileList.value = [{ uid: '-1', name: file.name, status: 'done' }]
  return false // 阻止自动上传
}

// 提交发票上传
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
    await uploadBusinessInvoice(currentInvoiceFormId.value, formData)
    message.success('发票上传成功')
    invoiceModalVisible.value = false
  } catch (error) {
    message.error('发票上传失败')
  }
}

// 提交创建水单
const handleSubmitCreateRemittance = async () => {
  if (!remittanceFormData.remittanceName) {
    message.warning('请输入收汇名称')
    return
  }
  if (!remittanceFormData.remittanceDate) {
    message.warning('请选择收汇日期')
    return
  }
  if (!remittanceFormData.remittanceAmount || remittanceFormData.remittanceAmount <= 0) {
    message.warning('请输入收汇金额')
    return
  }
  if (!remittanceFormData.photoUrl) {
    message.warning('请上传水单照片')
    return
  }

  createRemittanceLoading.value = true
  try {
    // 1. 创建水单
    const remittanceData = {
      remittanceName: remittanceFormData.remittanceName,
      remittanceDate: remittanceFormData.remittanceDate ? dayjs(remittanceFormData.remittanceDate).format('YYYY-MM-DD') : undefined,
      remittanceAmount: remittanceFormData.remittanceAmount,
      currency: remittanceFormData.currency,
      remarks: remittanceFormData.remarks,
      photoUrl: remittanceFormData.photoUrl
    }

    const result: any = await createRemittance(remittanceData)
    const remittanceId = result?.data?.data?.id || result?.data?.id
    
    if (!remittanceId) {
      throw new Error('创建水单失败，未返回水单ID')
    }

    // 2. 关联申报单（传递水单金额作为关联金额）
    await relateToForm(remittanceId, currentFormIdForRemittance.value, remittanceFormData.remittanceAmount)

    // 3. 提交审核
    await submitRemittanceAudit(remittanceId)

    message.success('水单创建、关联并提交审核成功！')
    createRemittanceVisible.value = false
    loadData()
  } catch (error: any) {
    message.error(error?.message || '操作失败')
  } finally {
    createRemittanceLoading.value = false
  }
}

// 上传水单照片
const handleUploadRemittancePhoto = async (file: File) => {
  try {
    const response: any = await uploadFile(file, 'remittance')
    const data = response.data
    if (data?.code === 200) {
      remittanceFormData.photoUrl = data.data?.url || data.data
      message.success('上传成功')
    } else {
      message.error('上传失败')
    }
  } catch (error) {
    message.error('上传失败')
  }
  return false // 阻止默认上传
}

// 移除水单照片
const handleRemoveRemittancePhoto = () => {
  remittanceFormData.photoUrl = ''
  remittanceFormData.fileList = []
}

// 获取水单文件扩展名
const getRemittanceFileExtension = () => {
  if (!remittanceFormData.photoUrl) return 'FILE'
  const parts = remittanceFormData.photoUrl.split('.')
  const ext = parts[parts.length - 1]?.split('?')[0]?.toUpperCase()
  return ext || 'FILE'
}

// 预览水单文件
const previewRemittanceFile = () => {
  if (remittanceFormData.photoUrl) {
    window.open(remittanceFormData.photoUrl, '_blank')
  }
}

// 下载单证
const handleDownload = async (record: DeclarationRecord) => {
  try {
    loading.value = true
    currentDeclaration.value = record
    const response = await getDeclarationDetail(record.id, record.status)
    if (response.data && response.data.code === 200) {
      currentAttachments.value = response.data.data.attachments || []
      
      // 加载相关合同
      try {
        currentContractsLoading.value = true
        const contractResponse = await getContractsByDeclaration(record.id)
        if (contractResponse.data && contractResponse.data.code === 200) {
          currentContracts.value = contractResponse.data.data || []
        }
      } catch (contractError) {
        console.warn('加载合同失败:', contractError)
        currentContracts.value = []
      } finally {
        currentContractsLoading.value = false
      }
      
      attachmentModalVisible.value = true
    } else {
      message.error('获取附件列表失败')
    }
  } catch (error) {
    message.error('获取附件列表失败')
  } finally {
    loading.value = false
  }
}

// 导出申报单
const handleExport = () => {
  message.info('批量导出功能开发中...')
}

// 删除申报单
const handleDelete = async (record: DeclarationRecord) => {
  try {
    await deleteDeclarationApi(record.id, record.status)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}


// // 重新生成标准单证
// const handleRegenerateStandard = (record: DeclarationRecord) => {
//   Modal.confirm({
//     title: '确认重新生成',
//     content: `确定要重新生成申报单 ${record.formNo} 的标准单证吗？`,
//     onOk: async () => {
//       try {
//         const res = await regenerateDocuments(record.id)
//         if (res.data && res.data.code === 200) {
//           message.success('标准单证重新生成成功')
//         } else {
//           message.error('生成失败: ' + (res.data?.message || '未知错误'))
//         }
//       } catch (e: any) {
//         message.error('生成失败: ' + (e.message || '网络错误'))
//       }
//     }
//   })
// }

// // 重新生成全套单证
// const handleRegenerateAll = (record: DeclarationRecord) => {
//   Modal.confirm({
//     title: '确认重新生成',
//     content: `确定要重新生成申报单 ${record.formNo} 的全套单证吗？`,
//     onOk: async () => {
//       try {
//         const res = await regenerateAllDocuments(record.id)
//         if (res.data && res.data.code === 200) {
//           message.success('全套单证重新生成成功')
//         } else {
//           message.error('生成失败: ' + (res.data?.message || '未知错误'))
//         }
//       } catch (e: any) {
//         message.error('生成失败: ' + (e.message || '网络错误'))
//       }
//     }
//   })
// }

// // 重新生成水单报告
// const handleRegenerateRemittance = (record: DeclarationRecord, type: number) => {
//   const typeName = type === 1 ? '定金' : '尾款'
//   Modal.confirm({
//     title: '确认重新生成',
//     content: `确定要重新生成申报单 ${record.formNo} 的${typeName}水单吗？`,
//     onOk: async () => {
//       try {
//         const res = await regenerateRemittanceReport(record.id, type)
//         if (res.data && res.data.code === 200) {
//           message.success(`${typeName}水单重新生成成功`)
//         } else {
//           message.error('生成失败: ' + (res.data?.message || '未知错误'))
//         }
//       } catch (e: any) {
//         message.error('生成失败: ' + (e.message || '网络错误'))
//       }
//     }
//   })
// }

// 生成合同相关
const generateModalVisible = ref(false)
const generateLoading = ref(false)
const templateOptions = ref<any[]>([])
const generateForm = reactive({
  declarationFormId: undefined as number | undefined,
  templateId: undefined as number | undefined
})

const handleOpenGenerate = async (record: any) => {
  generateForm.declarationFormId = record.id
  generateForm.templateId = undefined
  
  try {
    const res = await getEnabledTemplates()
    if (res.data && res.data.code === 200) {
      templateOptions.value = res.data.data || []
      generateModalVisible.value = true
    } else {
      message.error('获取合同模板失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error) {
    message.error('获取合同模板失败')
  }
}

const handleConfirmGenerate = async () => {
  if (!generateForm.templateId) {
    message.warning('请选择合同模板')
    return
  }
  if (!generateForm.declarationFormId) return
  
  generateLoading.value = true
  try {
    const res = await generateContract(generateForm.templateId, generateForm.declarationFormId, {})
    if (res.data && res.data.code === 200) {
      const generation = res.data.data
      message.success('合同生成成功')
      generateModalVisible.value = false
      
      // 成功后提示并提供下载
      if (generation && generation.id) {
        Modal.confirm({
          title: '合同已生成',
          content: `合同编号：${generation.contractNo}，是否立即下载？`,
          okText: '下载',
          cancelText: '关闭',
          onOk: () => {
            // 统一使用API封装的方式下载
            downloadContract(generation.id)
          }
        })
      }
    } else {
      message.error('合同生成失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error) {
    message.error('合同生成请求失败')
  } finally {
    generateLoading.value = false
  }
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待初审',
    2: '待资料提交',
    3: '待资料审核',
    4: '待发票提交',
    5: '待发票审核',
    6: '已完成',
    9: '退回待审'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',      // 草稿
    1: 'processing',   // 待初审
    2: 'blue',         // 待资料提交
    3: 'purple',       // 待资料审核
    4: 'geekblue',     // 待发票提交
    5: 'magenta',      // 待发票审核
    6: 'success',      // 已完成
    9: 'warning'       // 退回待审
  }
  return colorMap[status] || 'default'
}

// 附件相关函数
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN')
}

// 判断是否为文档文件
const isDocumentFile = (fileName: string) => {
  const docExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return docExtensions.includes(ext)
}

// 判断是否为图片文件
const isImageFile = (fileName: string) => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(ext)
}

// 获取文件类型颜色
const getFileTypeColor = (fileType: string) => {
  const colorMap: Record<string, string> = {
    'Invoice': 'blue',
    'PackingList': 'green',
    'FullDocuments': 'purple',
    'PickupList': 'orange',
    'Remittance': 'cyan',
    'Contract': 'magenta',
    'AllDocuments': 'cyan'
  }
  return colorMap[fileType] || 'default'
}

// 获取文件类型文本
const getFileTypeText = (fileType: string) => {
  const textMap: Record<string, string> = {
    'Invoice': '商业发票',
    'PackingList': '装箱单',
    'FullDocuments': '海关附件',
    'PickupList': '提货单',
    'Remittance': '水单',
    'Contract': '合同',
    'AllDocuments': '海关资料'
  }
  return textMap[fileType] || fileType
}

// 获取文件扩展名
const getFileExtension = (fileName: string) => {
  return fileName.substring(fileName.lastIndexOf('.'))
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (!size) return '0 KB'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}



// // 根据附件类型生成按钮配置
// const getButtonConfigFromAttachments = (attachments: any[]) => {
//   const fileTypes = []
//   const existingTypes = new Set(attachments.map((att: any) => att.fileType))
  
//   // 根据实际存在的附件类型显示对应按钮
//   if (existingTypes.has('FullDocuments')) {
//     fileTypes.push({ type: 'standard', label: '海关附件', color: '#1890ff' })
//   }
  
//   if (existingTypes.has('AllDocuments')) {
//     fileTypes.push({ type: 'all', label: '海关文件', color: '#722ed1' })
//   }
  
//   if (existingTypes.has('Remittance_Deposit')) {
//     fileTypes.push({ type: 'deposit', label: '定金水单', color: '#52c41a' })
//   }
  
//   if (existingTypes.has('Remittance_Balance')) {
//     fileTypes.push({ type: 'balance', label: '尾款水单', color: '#fa8c16' })
//   }
  
//   return fileTypes
// }







// 获取附件对应的申报单信息
const getDeclarationForAttachment = (attachment: any) => {
  // 从附件信息中获取申报单ID，然后在当前数据源中查找
  const formId = attachment.formId
  if (!formId || !dataSource.value) return null
  
  return dataSource.value.find((record: any) => record.id === formId) || null
}


// 简化版重新生成处理
const handleRegenerateSimple = async (attachment: any) => {
  const declaration = getDeclarationForAttachment(attachment)
  if (!declaration) return
  
  const fileType = attachment.fileType || attachment.type
  if (!fileType) {
    message.warning('无法识别文件类型')
    return
  }
  
  // 根据文件类型调用不同接口
  try {
    let response
    switch (fileType) {
      case 'FullDocuments':
        response = await regenerateDocuments(declaration.id)
        break
      case 'AllDocuments':
        response = await regenerateAllDocuments(declaration.id)
        break
      case 'Remittance_Deposit':
        // 定金水单 - type=1
        response = await regenerateRemittanceReport(declaration.id, 1)
        break
      case 'Remittance_Balance':
        // 尾款水单 - type=2
        response = await regenerateRemittanceReport(declaration.id, 2)
        break
      default:
        message.warning('不支持的文件类型')
        return
    }
    
    if (response.data && response.data.code === 200) {
      message.success(`重新生成成功`)
      // 重新加载附件列表
      await loadAttachmentsForDeclaration(declaration)
    } else {
      message.error(`重新生成失败: ${response.data?.message || '未知错误'}`)
    }
  } catch (error: any) {
    message.error(`重新生成失败: ${error.message || '网络错误'}`)
  }
}

// // 获取文件类型名称
// const getFileTypeName = (fileType: string) => {
//   const typeMap: Record<string, string> = {
//     'standard': '标准单证',
//     'all': '全套单证',
//     'deposit': '定金水单',
//     'balance': '尾款水单'
//   }
//   return typeMap[fileType] || fileType
// }

// 为申报单重新加载附件
const loadAttachmentsForDeclaration = async (declaration: any) => {
  try {
    const response = await getDeclarationAttachments(declaration.id)
    if (response.data && response.data.code === 200) {
      const attachments = response.data.data || []
      declaration.attachments = attachments
      
      // 更新当前显示的附件（如果是当前申报单）
      if (currentDeclaration.value && currentDeclaration.value.id === declaration.id) {
        currentAttachments.value = attachments
      }
    }
  } catch (error) {
    console.warn(`重新加载申报单${declaration.id}附件失败:`, error)
  }
}


const downloadAttachment = (attachment: any) => {
  if (attachment.fileUrl) {
    window.open(attachment.fileUrl, '_blank')
  }
}




// const canReplaceAttachment = (attachment: any) => {
//   console.log('Checking attachment replacement:', attachment)
//   // 只有已完成状态的申报单才能替换附件
//   return true // 暂时允许所有状态替换
// }






// 显示合同替换弹窗
const showReplaceContractModal = (contract: any) => {
  currentReplacingContract.value = contract
  replaceContractFileList.value = []
  replaceContractModalVisible.value = true
}

// 合同文件上传前校验
const beforeReplaceContractUpload = (file: any) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过10MB!')
    return false
  }
  
  const isDocx = file.name.toLowerCase().endsWith('.docx')
  if (!isDocx) {
    message.error('只支持.docx格式的Word文档!')
    return false
  }
  
  replaceContractFileList.value = [file]
  return false
}

// 处理合同替换
const handleReplaceContract = async () => {
  if (!currentReplacingContract.value || replaceContractFileList.value.length === 0) {
    message.warning('请选择要上传的合同文件')
    return
  }
  
  replaceContractLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', replaceContractFileList.value[0])
    
    const response = await replaceContractFile(currentReplacingContract.value.id, formData)
    
    if (response.data && response.data.code === 200) {
      message.success('合同替换成功')
      replaceContractModalVisible.value = false
      
      // 重新加载合同列表
      if (currentDeclaration.value) {
        const contractResponse = await getContractsByDeclaration(currentDeclaration.value.id)
        if (contractResponse.data && contractResponse.data.code === 200) {
          currentContracts.value = contractResponse.data.data || []
        }
      }
    } else {
      message.error('合同替换失败: ' + (response.data?.message || '未知错误'))
    }
  } catch (error: any) {
    console.error('合同替换失败:', error)
    message.error('合同替换失败: ' + (error.message || '网络错误'))
  } finally {
    replaceContractLoading.value = false
  }
}

const showReplaceModal = (attachment: any) => {
  currentReplacingAttachment.value = attachment
  replaceFileList.value = []
  replaceModalVisible.value = true
}

const beforeReplaceUpload = (file: any) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过10MB!')
    return false
  }
  
  // 校验文件格式必须与原文件一致
  if (currentReplacingAttachment.value) {
    const originalExt = getFileExtension(currentReplacingAttachment.value.fileName)
    const newExt = getFileExtension(file.name)
    
    if (originalExt.toLowerCase() !== newExt.toLowerCase()) {
      message.error(`文件格式必须与原文件一致 (${originalExt})`)
      return false
    }
  }
  
  replaceFileList.value = [file]
  return false
}

const handleReplaceAttachment = async () => {
  if (!currentReplacingAttachment.value || replaceFileList.value.length === 0) {
    message.warning('请选择要上传的文件')
    return
  }
  
  replaceLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', replaceFileList.value[0])
    
    const response = await fetch(`/api/v1/declarations/${currentReplacingAttachment.value.formId}/attachments/${currentReplacingAttachment.value.id}/replace`, {
      method: 'POST',
      body: formData
    })
    
    const result = await response.json()
    if (result.code === 200) {
      message.success('附件替换成功')
      replaceModalVisible.value = false
      // 重新加载附件列表
      if (currentAttachments.value.length > 0) {
        const formId = currentAttachments.value[0].formId
        const response = await getDeclarationDetail(formId, 8) // 假设已完成状态
        if (response.data && response.data.code === 200) {
          currentAttachments.value = response.data.data.attachments || []
        }
      }
    } else {
      message.error('替换失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    message.error('替换失败: 网络错误')
  } finally {
    replaceLoading.value = false
  }
}

// 退回草稿相关
const returnHistoryColumns = [
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

// 获取业务类型文本
const getBusinessTypeText = (type: string) => {
  const map: Record<string, string> = {
    'DECLARATION_RETURN': '退回草稿',
    'DECLARATION_AUDIT': '申报审核',
    'REMittance_AUDIT': '水单审核',
    'DELIVERY_ORDER_AUDIT': '提货单审核',
    'DECLARATION_SUBMIT': '申报提交',
    'REMITTANCE_AUDIT': '水单审核',
  }
  return map[type] || type
}

const handleReturnApply = (record: DeclarationRecord) => {
  returnApplyForm.id = record.id
  returnApplyForm.reason = '申报错误'
  returnApplyVisible.value = true
}

const handleReturnAudit = (record: DeclarationRecord) => {
  returnAuditForm.id = record.id
  returnAuditForm.approved = true
  returnAuditForm.remark = '已核对数据，通过'
  returnAuditVisible.value = true
}

const submitReturnApply = async () => {
  if (!returnApplyForm.reason) {
    message.warning('请输入退回原因')
    return
  }
  returnApplyLoading.value = true
  try {
    const res = await applyReturnToDraft(returnApplyForm.id, returnApplyForm.reason)
    if (res.data && res.data.code === 200) {
      message.success('退回申请已提交，请等待审核')
      returnApplyVisible.value = false
      loadData()
    } else {
      message.error('申请失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('申请过程发生错误')
  } finally {
    returnApplyLoading.value = false
  }
}


const submitReturnAudit = async () => {
  // 验证必填项
  if (!returnAuditForm.remark || !returnAuditForm.remark.trim()) {
    message.warning('请输入审核意见')
    return
  }
  
  returnAuditLoading.value = true
  try {
    const res = await auditReturnToDraft(returnAuditForm.id, {
      approved: returnAuditForm.approved,
      remark: returnAuditForm.remark.trim()
    })
    if (res.data && res.data.code === 200) {
      message.success(returnAuditForm.approved ? '审核通过，已退回草稿' : '已驳回申请')
      returnAuditVisible.value = false
      loadData()
    } else {
      message.error('审核失败：' + (res.data?.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('审核过程发生错误')
  } finally {
    returnAuditLoading.value = false
  }
}

const viewReturnHistory = async (record: DeclarationRecord) => {
  returnHistoryVisible.value = true
  returnHistoryLoading.value = true
  try {
    const res = await getReturnAuditHistory(record.id)
    if (res.data && res.data.code === 200) {
      returnHistoryList.value = res.data.data || []
    }
  } catch (error) {
    message.error('加载审核历史失败')
  } finally {
    returnHistoryLoading.value = false
  }
}

// 页面加载
onMounted(() => {
  loadData()
  startAutoRefresh() // 启动自动刷新
  
  // 拦截来自工作台待办任务的特定参数，并自动开启审核弹窗/进入审核流程
  if (route.query.action === 'audit' && route.query.id) {
    const id = Number(route.query.id)
    if (!isNaN(id)) {
      // 稍微延迟确保数据渲染，不过我们这是路由跳转，此时直接用 params 跳表单详情即可
      setTimeout(() => {
        router.push(`/declaration/form?id=${id}&mode=audit`)
      }, 300)
    }
  }
})

onUnmounted(() => {
  stopAutoRefresh() // 停止自动刷新
})
</script>

<style scoped>
.declaration-manage {
  height: 100%;
  overflow-x: hidden;
}

/* 附件列表样式 */
.attachment-title {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.file-size {
  font-size: 12px;
  color: #888;
}

.create-time {
  font-size: 12px;
  color: #888;
}
</style>