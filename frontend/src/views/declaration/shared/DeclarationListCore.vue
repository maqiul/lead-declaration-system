<template>
  <div class="declaration-manage bg-white p-6 min-h-full">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="发票号">
          <a-input v-model:value="searchForm.invoiceNo" placeholder="发票号" style="width: 140px" />
        </a-form-item>
        <a-form-item label="申报单号">
          <a-input 
            v-model:value="searchForm.formNo" 
            placeholder="搜索申报单号" 
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="状态" v-if="showStatusSelect">
          <a-select
            v-model:value="searchForm.status"
            placeholder="状态筛选"
            style="width: 140px"
            @change="loadData"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发货人">
          <a-input v-model:value="searchForm.shipper" placeholder="发货人" style="width: 140px" />
        </a-form-item>
        <a-form-item label="收货人">
          <a-input v-model:value="searchForm.consignee" placeholder="收货人" style="width: 140px" />
        </a-form-item>
        <a-form-item label="日期">
          <a-range-picker 
            v-model:value="searchForm.dateRange" 
            style="width: 200px"
            @change="loadData"
          />
        </a-form-item>
      </a-form>
      <div class="search-btn-row">
        <a-button type="primary" @click="loadData" v-permission="['business:declaration:view']">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
        <a-button @click="resetSearch">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </div>
    </a-card>

    <!-- 操作按钮 -->
    <a-card class="operation-card">
      <a-space>
        <a-button v-if="showAddButton" type="primary" @click="handleAdd" v-permission="['business:declaration:create']">
          <template #icon><plus-outlined /></template>
          新增申报单
        </a-button>
        <a-button v-if="showExportButton" @click="handleExport" v-permission="['business:declaration:export']">
          <template #icon><download-outlined /></template>
          导出
        </a-button>
      </a-space>
      <a-popover title="自定义列" trigger="click" placement="bottomRight" :overlay-style="{ minWidth: '220px' }">
        <template #content>
          <div style="display: flex; flex-direction: column; gap: 6px;">
            <a-checkbox
              v-for="col in allColumns.filter(c => c.key !== 'action')"
              :key="col.key"
              :checked="visibleColumnKeys.includes(col.key)"
              @change="(e: any) => toggleColumn(col.key, e.target.checked)"
              :disabled="['formNo', 'status'].includes(col.key)"
            >{{ col.title }}</a-checkbox>
          </div>
          <div style="margin-top: 10px; text-align: right;">
            <a-button size="small" @click="resetColumns">重置</a-button>
          </div>
        </template>
        <a-button style="float: right;" size="small">
          <template #icon><setting-outlined /></template>
          列设置
        </a-button>
      </a-popover>
    </a-card>

    <a-card class="ui-card">
      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX }"
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
            <a-tag v-if="record.pendingRollback" color="orange" style="margin-top: 2px;">
              退回待审
            </a-tag>
            <a-tag v-if="record.pendingSupplementId && record.pendingSupplementStatus === -1" color="blue" style="margin-top: 2px;">
              补交草稿
            </a-tag>
            <!-- 草稿已提交审核（status=0）：明确展示待审档，避免误读为仍在补交 -->
            <a-tag v-else-if="record.pendingSupplementId" color="orange" style="margin-top: 2px;">
              补交待审核
            </a-tag>
            <a-tag v-if="record.exemptionStatus === 0" color="blue" style="margin-top: 2px;">
              豁免审核中
            </a-tag>
            <a-tag v-else-if="record.exemptionStatus === 2" color="red" style="margin-top: 2px;">
              豁免已驳回
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <!-- 草稿状态: 编辑、提交 -->
              <template v-if="!onlyPendingSupplement && record.status === 0">
                <a-button type="link" size="small" @click="handleEdit(record as any)" v-permission="['business:declaration:update']">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button type="link" size="small" @click="handleStatusSubmit(record as any)" v-permission="['business:declaration:submit', 'business:declaration:submit:others']">
                  <template #icon><SendOutlined /></template>
                  提交
                </a-button>
              </template>

              <!-- 待初审状态: 初审按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 1, 'deptAudit')">
                <a-button type="link" size="small" style="color: #faad14;" @click="handleAudit(record as any)">
                  <template #icon><CheckCircleOutlined /></template>
                  初审
                </a-button>
              </template>

              <!-- 待资料提交状态: 提交资料按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 2, 'materialSubmit')">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleMaterialSubmit(record as any)">
                  <template #icon><UploadOutlined /></template>
                  提交资料
                </a-button>
              </template>

              <!-- 待资料提交状态 + 当前用户有权审核豁免: 豁免审核按钮 -->
              <template v-if="!onlyPendingSupplement && record.status === 2 && record.canAuditExemption">
                <a-button type="link" size="small" style="color: #fa541c;" @click="handleExemptionAudit(record as any)">
                  <template #icon><AuditOutlined /></template>
                  豁免审核
                </a-button>
              </template>

              <!-- 待资料审核状态: 资料审核按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 3, 'materialAudit')">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleMaterialAudit(record as any)">
                  <template #icon><CheckCircleOutlined /></template>
                  资料审核
                </a-button>
              </template>

              <!-- 待补充资料提交: 提交补充资料按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 4, 'supplementSubmit')">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleGoMode(record as any, 'supplement')">
                  <template #icon><UploadOutlined /></template>
                  补充资料
                </a-button>
              </template>

              <!-- 待补充资料审核: 补充审核按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 5, 'supplementAudit')">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleGoMode(record as any, 'supplementAudit')">
                  <template #icon><CheckCircleOutlined /></template>
                  补充审核
                </a-button>
              </template>

              <!-- 待开票金额提交: 申请开票金额按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 6, 'invoiceAmountSubmit')">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleGoMode(record as any, 'invoiceAmount')">
                  <template #icon><MoneyCollectOutlined /></template>
                  开票金额
                </a-button>
              </template>

              <!-- 待开票金额审核: 金额审核按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 7, 'invoiceAmountAudit')">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleGoMode(record as any, 'invoiceAmountAudit')">
                  <template #icon><CheckCircleOutlined /></template>
                  金额审核
                </a-button>
              </template>

              <!-- 待发票提交状态: 提交发票按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 8, 'invoiceSubmit')">
                <a-button type="link" size="small" style="color: #1677ff;" @click="handleGoSubmitInvoice(record as any)">
                  <template #icon><UploadOutlined /></template>
                  提交发票
                </a-button>
              </template>

              <!-- 待发票审核状态: 发票审核按钮 -->
              <template v-if="!onlyPendingSupplement && hasMyTaskForStatus(record, 9, 'invoiceAudit')">
                <a-button type="link" size="small" style="color: #52c41a;" @click="handleInvoiceAudit(record as any)">
                  <template #icon><CheckCircleOutlined /></template>
                  发票审核
                </a-button>
              </template>

              <!-- 资料补交：已进入资料环节（status>=2，退回待审除外）且无在途补交时，申报人可发起 -->
              <template v-if="!onlyPendingSupplement && record.status >= 2 && record.status !== 11 && !record.pendingSupplementId">
                <a-button type="link" size="small" style="color: #722ed1;" v-permission="['business:declaration:supplement:initiate']" @click="openSupplementStart(record as any)">
                  <template #icon><PlusOutlined /></template>
                  发起补交
                </a-button>
              </template>

              <!-- 资料补交草稿：申报人可继续上传补交资料 -->
              <template v-if="!onlyPendingSupplement && record.pendingSupplementStatus === -1">
                <a-button type="link" size="small" style="color: #1677ff;" v-permission="['business:declaration:supplement:initiate']" @click="handleContinueSupplement(record as any)">
                  <template #icon><EditOutlined /></template>
                  继续补交
                </a-button>
                <a-button type="link" size="small" style="color: #ff4d4f;" v-permission="['business:declaration:supplement:initiate']" @click="handleCancelSupplement(record as any)">
                  <template #icon><CloseOutlined /></template>
                  取消补交
                </a-button>
              </template>

              <!-- 资料补交审核：存在在途补交单时，有审核权限的用户可见 -->
              <template v-if="record.pendingSupplementId && record.pendingSupplementStatus !== -1">
                <a-button type="link" size="small" style="color: #52c41a;" v-permission="['business:declaration:audit:material']" @click="handleSupplementAudit(record as any)">
                  <template #icon><CheckCircleOutlined /></template>
                  补交审核
                </a-button>
              </template>

              <!-- 退回待审: 退回审核（与「更多」内入口一致，便于发现） -->
              <template v-if="!onlyPendingSupplement && record.status === 11">
                <a-button type="link" size="small" style="color: #fa8c16;" @click="handleReturnAudit(record as any)" v-permission="['business:declaration:return:audit']">
                  <template #icon><AuditOutlined /></template>
                  退回审核
                </a-button>
              </template>

              <!-- 更多操作菜单（补交审核列表不展示） -->
              <a-dropdown v-if="!onlyPendingSupplement">
                <a-button type="link" size="small">
                  更多
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="download" @click="handleDownload(record as any)">
                      <DownloadOutlined /> 单证下载
                    </a-menu-item>
                    <a-menu-item
                      v-if="canShowResumeFlow(record as any)"
                      key="resumeFlow"
                      @click="handleResumeFlow(record as any)"
                    >
                      <ReloadOutlined /> 恢复流程
                    </a-menu-item>
                    <a-menu-item
                      v-if="[4,6,8].includes(record.status) && !record.pendingRollback && checkPermission(['business:declaration:rollback'])"
                      key="rollback"
                      @click="handleRollback(record as any)"
                    >
                      <UndoOutlined /> 退回上一步
                    </a-menu-item>
                    <a-menu-item
                      v-if="[4,6,8].includes(record.status) && record.pendingRollback && checkPermission(['business:declaration:rollback:audit'])"
                      key="rollbackAudit"
                      @click="handleRollbackAudit(record as any)"
                    >
                      <AuditOutlined /> 退回审核
                    </a-menu-item>
                    <a-menu-item
                      v-if="!record.hasContract && checkPermission(['business:declaration:contract'])"
                      key="contract"
                      @click="handleOpenGenerate(record)"
                    >
                      <FileTextOutlined /> 生成合同
                    </a-menu-item>
                    <a-menu-item
                      v-if="record.status >= 2 && record.status !== 11 && checkPermission(['business:declaration:return:apply'])"
                      key="returnApply"
                      @click="handleReturnApply(record as any)"
                    >
                      <ReloadOutlined /> 申请退回草稿
                    </a-menu-item>
                    <a-menu-item
                      v-if="record.status === 11 && checkPermission(['business:declaration:return:audit'])"
                      key="returnAudit"
                      @click="handleReturnAudit(record as any)"
                    >
                      <AuditOutlined /> 退回审核
                    </a-menu-item>
                    <a-menu-item key="returnHistory" @click="viewReturnHistory(record as any)">
                      <HistoryOutlined /> 审核详情
                    </a-menu-item>
                    <a-menu-item v-if="record.status === 0" key="delete" danger>
                      <a-popconfirm title="确定要删除该申报单吗?" @confirm="handleDelete(record as any)" placement="left">
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
    <a-modal v-model:open="attachmentModalVisible" title="附件管理" width="700px">
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
                  <a-tag :color="getFileTypeColor(item.fileType)">{{ getFileTypeText(item.fileType) }}</a-tag>
                  <span class="file-size">{{ formatFileSize(item.fileSize) }}</span>
                  <span class="create-time">{{ fmtDateTime(item.createTime, 'yyyy-MM-dd') }}</span>
                </div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-space>
                <a-button type="link" size="small" @click="previewAttachment(item)">
                  <template #icon><EyeOutlined /></template>预览
                </a-button>
                <a-button type="link" size="small" @click="downloadAttachment(item)" v-permission="['business:declaration:download']">
                  <template #icon><DownloadOutlined /></template>下载
                </a-button>
                <a-button type="link" size="small" style="color: #FA8C16;" @click="handleRegenerateSimple(item)" v-permission="['business:declaration:audit']">
                  <template #icon><ReloadOutlined /></template>重新生成
                </a-button>
                <a-button type="link" size="small" style="color: #faad14;" @click="showReplaceModal(item)" v-permission="['business:declaration:audit']">
                  <template #icon><UploadOutlined /></template>替换
                </a-button>
              </a-space>
            </template>
          </a-list-item>
        </template>
        <template v-if="currentAttachments.length === 0" #header>
          <div style="text-align: center; color: #999;">暂无自动生成的全套单证或水单文件</div>
        </template>
      </a-list>
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
                    <span class="create-time">{{ fmtDateTime(item.generatedTime, 'yyyy-MM-dd') }}</span>
                    <span v-if="item.templateName" style="margin-left: 8px; color: #999;">模板: {{ item.templateName }}</span>
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-space>
                  <a-button type="link" size="small" @click="handlePreviewContract(item.id)" v-permission="['business:contract:download']">
                    <template #icon><EyeOutlined /></template>预览
                  </a-button>
                  <a-button type="link" size="small" @click="downloadContract(item.id)" v-permission="['business:contract:download']">
                    <template #icon><DownloadOutlined /></template>下载
                  </a-button>
                  <a-button type="link" size="small" style="color: #faad14;" @click="showReplaceContractModal(item)" v-permission="['business:declaration:audit']">
                    <template #icon><UploadOutlined /></template>替换
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
    <a-modal v-model:open="replaceContractModalVisible" title="替换合同" @ok="handleReplaceContract" :confirmLoading="replaceContractLoading">
      <a-form layout="vertical">
        <a-form-item label="当前合同">
          <div>{{ currentReplacingContract?.generatedFileName }}</div>
          <div v-if="currentReplacingContract?.templateName" style="color: #999; font-size: 12px; margin-top: 4px;">模板: {{ currentReplacingContract.templateName }}</div>
        </a-form-item>
        <a-form-item label="选择新合同文件" required>
          <a-upload :before-upload="beforeReplaceContractUpload" :file-list="replaceContractFileList" :max-count="1" accept=".docx">
            <a-button><template #icon><UploadOutlined /></template>选择文件</a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">仅支持.docx格式的Word文档，单个文件不超过10MB</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 附件替换弹窗 -->
    <a-modal v-model:open="replaceModalVisible" title="替换附件" @ok="handleReplaceAttachment" :confirmLoading="replaceLoading">
      <a-form layout="vertical">
        <a-form-item label="当前文件"><div>{{ currentReplacingAttachment?.fileName }}</div></a-form-item>
        <a-form-item label="选择新文件" required>
          <a-upload :before-upload="beforeReplaceUpload" :file-list="replaceFileList" :max-count="1" accept=".xlsx,.xls,.pdf,.jpg,.jpeg,.png">
            <a-button><template #icon><UploadOutlined /></template>选择文件</a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">支持Excel、PDF、图片格式，单个文件不超过10MB</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 生成合同弹窗 -->
    <a-modal v-model:open="generateModalVisible" title="生成合同" @ok="handleConfirmGenerate" :confirmLoading="generateLoading">
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="选择合同模板" required>
          <a-select v-model:value="generateForm.templateId" placeholder="请选择合同模板" :options="templateOptions" :fieldNames="{ label: 'templateName', value: 'id' }" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 资料审核弹窗 -->
    <MaterialAuditModal v-model:open="materialAuditVisible" :form-id="currentFormIdForMaterial" @audited="loadData" />

    <!-- 发票审核弹窗 -->
    <InvoiceAuditModal v-model:open="invoiceAuditVisible" :form-id="currentFormIdForInvoice" @audited="loadData" />

    <!-- 退回草稿申请弹窗 -->
    <a-modal v-model:open="returnApplyVisible" title="申请退回草稿" @ok="submitReturnApply" :confirmLoading="returnApplyLoading">
      <a-form layout="vertical">
        <a-form-item label="退回原因" required>
          <a-textarea v-model:value="returnApplyForm.reason" placeholder="请输入退回草稿的原因" :rows="4" />
          <div style="margin-top: 8px; color: #999; font-size: 12px;">注：申请后单据将进入"退回待审"状态。</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 退回审核弹窗 -->
    <a-modal v-model:open="returnAuditVisible" title="审核退回申请" @ok="submitReturnAudit" :confirmLoading="returnAuditLoading">
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

    <!-- 退回上一步审核弹窗 -->
    <a-modal v-model:open="rollbackAuditVisible" title="审核退回上一步申请" @ok="submitRollbackAudit" :confirmLoading="rollbackAuditLoading">
      <a-form layout="vertical">
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="rollbackAuditForm.approved">
            <a-radio :value="true">通过 (退回到上一审核节点)</a-radio>
            <a-radio :value="false">驳回 (保持当前状态)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核备注" required>
          <a-textarea v-model:value="rollbackAuditForm.remark" placeholder="请输入审核备注" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审核历史弹窗 -->
    <a-modal v-model:open="returnHistoryVisible" title="审核历史详情" width="1200px" :footer="null">
      <a-table :dataSource="returnHistoryList" :columns="returnHistoryColumns" :loading="returnHistoryLoading" rowKey="id" size="small" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'auditStatus'">
            <a-tag :color="record.auditStatus === 1 ? 'success' : record.auditStatus === 2 ? 'error' : 'processing'">
              {{ record.auditStatus === 1 ? '通过' : record.auditStatus === 2 ? '驳回' : '待审核' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'businessType'">
            <a-tag :color="getBusinessTypeColor(record.businessType)">{{ getBusinessTypeText(record.businessType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'preStatus'">
            <a-tag>{{ getStatusText(record.preStatus) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 文件预览弹窗 -->
    <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />

    <!-- 流程预览弹窗 -->
    <BpmnPreviewModal v-model:visible="bpmnPreviewVisible" :templateId="previewTemplateId" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, onActivated, watch, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { DownloadOutlined, EditOutlined, CheckCircleOutlined, DeleteOutlined, SendOutlined, UploadOutlined, FileTextOutlined, FileOutlined, PictureOutlined, FileUnknownOutlined, ReloadOutlined, MoneyCollectOutlined, DownOutlined, HistoryOutlined, SearchOutlined, CloseOutlined, AuditOutlined, UndoOutlined, EyeOutlined, SettingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { checkPermission } from '@/directives/permission'
import { getEnabledTransportModes } from '@/api/system/transportMode'
import type { Dayjs } from 'dayjs'
import {
  getDeclarationList, deleteDeclaration as deleteDeclarationApi, getDeclarationDetail,
  getDeclarationAttachments, regenerateDocuments, regenerateAllDocuments,
  regenerateRemittanceReport, getBatchActiveTasks, getActiveTasks, resumeDeclarationFlow, rollbackDeclaration, rollbackAuditDeclaration, getBatchRollbackPending,
  applyReturnToDraft, auditReturnToDraft, getReturnAuditHistory
} from '@/api/business/declaration'
import { getEnabledTemplates, generateContract, downloadContract, getContractsByDeclaration, replaceContractFile, getContractDownloadUrl } from '@/api/business/contract'
import { getBatchPendingExemptions, getBatchActiveSupplements, cancelMaterialSupplement, startMaterialSupplement, getBatchMaterialProgress } from '@/api/business/materialItem'
import MaterialAuditModal from '../material/components/MaterialAuditModal.vue'
import InvoiceAuditModal from '../material/components/InvoiceAuditModal.vue'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import BpmnPreviewModal from '@/components/BpmnPreviewModal.vue'
import { formatDate as fmtDateTime } from '@/utils/common'

// Props
const props = withDefaults(defineProps<{
  statusFilter?: number[]
  showAddButton?: boolean
  showExportButton?: boolean
  showStatusSelect?: boolean
  statusOptions?: { value: number; label: string }[]
  declarationType?: string  // SELF/EXTERNAL，不传则从 route query 读取
  /** 仅展示有待审补交单（status=0）的申报单：补交审核列表页专用 */
  onlyPendingSupplement?: boolean
}>(), {
  showAddButton: false,
  showExportButton: true,
  showStatusSelect: true,
  statusOptions: () => [
    { value: 0, label: '草稿' }, { value: 1, label: '待初审' }, { value: 2, label: '待资料提交' },
    { value: 3, label: '待资料审核' }, { value: 4, label: '待补充资料提交' }, { value: 5, label: '待补充资料审核' },
    { value: 6, label: '待开票金额提交' }, { value: 7, label: '待开票金额审核' },
    { value: 8, label: '待发票提交' }, { value: 9, label: '待发票审核' },
    { value: 10, label: '已完成' }, { value: 11, label: '退回待审' }
  ],
  declarationType: undefined,
  onlyPendingSupplement: false
})

const router = useRouter()
const route = useRoute()
const searchForm = reactive({ formNo: '', status: '', consignee: '', shipper: '', invoiceNo: '', dateRange: undefined as [Dayjs, Dayjs] | undefined })

/** 根据 props / query / 路径前缀 确定 declarationType */
const currentDeclarationType = computed(() => {
  if (props.declarationType) return props.declarationType
  if (route.query.declarationType) return route.query.declarationType as string
  if (route.path.startsWith('/declaration-self')) return 'SELF'
  if (route.path.startsWith('/declaration-external')) return 'EXTERNAL'
  return 'EXTERNAL'
})

/** 根据 declarationType 确定路由前缀 */
const declarationPrefix = computed(() => {
  return currentDeclarationType.value === 'SELF' ? '/declaration-self' : '/declaration-external'
})

interface DeclarationRecord {
  id: number; formNo: string; shipperCompany?: string; consigneeCompany?: string
  declarationDate?: string; totalAmount?: number; totalCartons?: number; status: number
  createTime?: string; financeUploadPending?: boolean; attachments?: any[]
  hasContract?: boolean; regenerateButtons?: any[]; activeTasks?: string[]; myTasks?: string[]
  needsFlowMigration?: boolean; pendingRollback?: boolean; pendingExemptionId?: number; exemptionStatus?: number; canAuditExemption?: boolean
  /** 在途/草稿补交单ID（非空=补交中或补交草稿，列表页展示补交入口） */
  pendingSupplementId?: number | null
  /** 补交单状态：-1=草稿（申报人继续补交）0=在途（待审核） */
  pendingSupplementStatus?: number | null
}

const dataSource = ref<DeclarationRecord[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条记录` })

// 新增申报单 - 直接跳转表单页

const handleAdd = async () => {
  try {
    const transportRes = await getEnabledTransportModes()

    // 运输方式：自动选择第一个（如果只有一个）
    let transport = ''
    if (transportRes.data?.code === 200) {
      const modes = (transportRes.data.data || []).map((t: any) => t.name)
      if (modes.length === 1) transport = modes[0]
    }

    // 申报类型由路由前缀推导、模板由表单页按类型自动匹配默认值（规则与原列表页选择一致），
    // URL 只需携带运输方式预选
    const params = new URLSearchParams()
    if (transport) params.set('transport', transport)
    const query = params.toString()
    router.push(`${declarationPrefix.value}/form-v2${query ? '?' + query : ''}`)
  } catch {
    message.warning('加载运输方式失败')
  }
}

// 流程预览
const bpmnPreviewVisible = ref(false)
const previewTemplateId = ref<number | null>(null)

// --- 列配置 ---
/** 资料进度单元格：已上传/必填 + 百分比进度条（数据由 refreshMaterialProgress 回填） */
const renderMaterialProgress = (record: any) => {
  const p = record.materialProgress
  if (!p || p.required <= 0) return h('span', { style: 'color:#bfbfbf' }, '-')
  const pct = Math.min(p.percent ?? 0, 100)
  const color = pct >= 100 ? '#52c41a' : '#1677ff'
  return h('div', { style: 'min-width:100px' }, [
    h('div', { style: 'display:flex;justify-content:space-between;font-size:12px;line-height:18px' }, [
      h('span', `${p.uploaded}/${p.required}`),
      h('span', { style: `color:${color}` }, `${pct}%`)
    ]),
    h('div', { style: 'height:6px;border-radius:3px;background:#f0f0f0;overflow:hidden' }, [
      h('div', { style: `width:${pct}%;height:100%;background:${color}` })
    ])
  ])
}
const allColumns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 160 },
  { title: '申报人', dataIndex: 'applicantName', key: 'applicantName', width: 100, ellipsis: true },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 150, ellipsis: true },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 150, ellipsis: true },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 120, ellipsis: true },
  { title: '贸易国', dataIndex: 'tradeCountry', key: 'tradeCountry', width: 100, ellipsis: true },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate', width: 120, customRender: ({ text }: any) => text ? fmtDateTime(text, 'yyyy-MM-dd') : '-' },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 80 },
  { title: '状态', key: 'status', width: 100, customRender: ({ record }: { record: any }) => h('a-tag', { color: getStatusColor(record.status) }, getStatusText(record.status)) },
  { title: '资料进度', key: 'materialProgress', width: 130, customRender: ({ record }: { record: any }) => renderMaterialProgress(record) },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 , customRender: ({ text }: any) => text ? fmtDateTime(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
]
const DEFAULT_VISIBLE_KEYS = allColumns.map(c => c.key)
// 列配置按页面分桶存储：补交审核列表独立一份，避免与其他环节页互相影响，默认全列展示（与申报页一致）
const STORAGE_KEY = props.onlyPendingSupplement ? 'supplement-audit-column-keys' : 'declaration-column-keys'

const visibleColumnKeys = ref<string[]>(
  JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') || DEFAULT_VISIBLE_KEYS
)
// 新增列兼容：老用户已存的列配置里没有资料进度列时默认展示
if (!visibleColumnKeys.value.includes('materialProgress')) {
  visibleColumnKeys.value.push('materialProgress')
}

const toggleColumn = (key: string, checked: boolean) => {
  if (checked) {
    if (!visibleColumnKeys.value.includes(key)) visibleColumnKeys.value.push(key)
  } else {
    visibleColumnKeys.value = visibleColumnKeys.value.filter(k => k !== key)
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(visibleColumnKeys.value))
}

const resetColumns = () => {
  visibleColumnKeys.value = [...DEFAULT_VISIBLE_KEYS]
  localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_VISIBLE_KEYS))
}

const columns = computed(() => [
  ...allColumns.filter(c => visibleColumnKeys.value.includes(c.key)),
  { title: '操作', key: 'action', width: 400, fixed: 'right' as const }
])

const scrollX = computed(() => columns.value.reduce((sum, c: any) => sum + (c.width || 100), 0))

// 弹窗状态
const attachmentModalVisible = ref(false)
const currentAttachments = ref<any[]>([])
const currentContracts = ref<any[]>([])
const currentContractsLoading = ref(false)
const currentDeclaration = ref<DeclarationRecord | null>(null)
const materialAuditVisible = ref(false)
const currentFormIdForMaterial = ref<number | string | null>(null)
const invoiceAuditVisible = ref(false)
const currentFormIdForInvoice = ref<number | string | null>(null)
const replaceModalVisible = ref(false)
const replaceLoading = ref(false)
const currentReplacingAttachment = ref<any>(null)
const replaceFileList = ref<any[]>([])
const replaceContractModalVisible = ref(false)
const replaceContractLoading = ref(false)
const currentReplacingContract = ref<any>(null)
const replaceContractFileList = ref<any[]>([])
const returnApplyVisible = ref(false)
const returnApplyLoading = ref(false)
const returnApplyForm = reactive({ id: 0, reason: '申报错误' })
const returnAuditVisible = ref(false)
const returnAuditLoading = ref(false)
const returnAuditForm = reactive({ id: 0, approved: true, remark: '已核对数据，通过' })
const returnHistoryVisible = ref(false)
const returnHistoryLoading = ref(false)
const returnHistoryList = ref<any[]>([])
const rollbackAuditVisible = ref(false)
const rollbackAuditLoading = ref(false)
const rollbackAuditForm = reactive({ id: 0, approved: true, remark: '已核对数据，通过' })
const generateModalVisible = ref(false)
const generateLoading = ref(false)
const templateOptions = ref<any[]>([])
const generateForm = reactive({ declarationFormId: undefined as number | undefined, templateId: undefined as number | undefined })

watch(() => rollbackAuditForm.approved, (newVal) => {
  rollbackAuditForm.remark = newVal ? '已核对数据，通过' : '不同意退回'
})

watch(() => returnAuditForm.approved, (newVal) => {
  returnAuditForm.remark = newVal ? '已核对数据，通过' : '数据填写错误'
})

// 数据加载
const loadData = async () => {
  try {
    loading.value = true
    const params: any = {
      current: pagination.current, size: pagination.pageSize,
      formNo: searchForm.formNo || undefined,
      consignee: searchForm.consignee || undefined,
      shipper: searchForm.shipper || undefined,
      invoiceNo: searchForm.invoiceNo || undefined
    }
    // declarationType 过滤：优先用 prop，其次读 route query，最后从路径推断
    const dt = currentDeclarationType.value
    if (dt) params.declarationType = dt
    // 补交审核列表：仅查有待审补交单的申报单
    if (props.onlyPendingSupplement) params.pendingSupplementAudit = true
    // 多状态过滤优先
    if (props.statusFilter && props.statusFilter.length > 0) {
      if (searchForm.status !== '') {
        params.status = Number(searchForm.status)
      } else {
        params.statusList = props.statusFilter.join(',')
      }
    } else {
      if (searchForm.status !== '') params.status = Number(searchForm.status)
    }

    const response = await getDeclarationList(params)
    if (response.data.code === 200) {
      const rawRecords = response.data.data?.records
      const records = Array.isArray(rawRecords) ? rawRecords : []
      const recordsWithAttachments = await Promise.all(
        records.map(async (record: any) => {
          if (record.status >= 1) {
            try {
              const attRes = await getDeclarationAttachments(record.id)
              if (attRes.data?.code === 200) record.attachments = attRes.data.data || []
            } catch { record.attachments = []; record.regenerateButtons = [] }
          } else { record.attachments = []; record.regenerateButtons = [] }
          if (record.status >= 1) {
            try {
              const cRes = await getContractsByDeclaration(record.id)
              record.hasContract = cRes.data?.code === 200 ? (cRes.data.data || []).length > 0 : false
            } catch { record.hasContract = false }
          } else { record.hasContract = false }
          return record
        })
      )
      dataSource.value = recordsWithAttachments
      pagination.total = response.data.data.total
      // 查询状态 1-9 的申报单的活跃任务（恢复流程按钮需要）
      const processingIds = dataSource.value.filter((r: any) => r.status >= 1 && r.status <= 9 && r.status !== 11).map((r: any) => r.id)
      if (processingIds.length > 0) {
        try {
          const taskRes = await getBatchActiveTasks(processingIds.join(','))
          if (taskRes.data?.code === 200 && taskRes.data.data) {
            const payload = taskRes.data.data
            const taskMap = payload.tasks ?? payload
            const myTaskMap = payload.myTasks ?? {}
            const migrationMap = payload.migration ?? {}
            dataSource.value.forEach((r: any) => {
              r.activeTasks = taskMap[String(r.id)] || []
              r.myTasks = myTaskMap[String(r.id)] || []
              r.needsFlowMigration = migrationMap[String(r.id)] === true
            })
          }
        } catch (e: any) { console.error('获取批量任务失败:', e) }
      }
      // 查询状态 4/6/8 的申报单是否有待审核的退回上一步申请
      const rollbackCheckIds = dataSource.value.filter((r: any) => [4, 6, 8].includes(r.status)).map((r: any) => r.id)
      if (rollbackCheckIds.length > 0) {
        try {
          const rbRes = await getBatchRollbackPending(rollbackCheckIds.join(','))
          if (rbRes.data?.code === 200 && rbRes.data.data) {
            const pendingMap = rbRes.data.data
            dataSource.value.forEach((r: any) => {
              r.pendingRollback = pendingMap[String(r.id)] === true
            })
          }
        } catch (e: any) { console.error('查询待审核退回申请失败:', e) }
      }
      // 查询所有申报单的豁免状态
      const exemptionCheckIds = dataSource.value.map((r: any) => r.id)
      if (exemptionCheckIds.length > 0) {
        try {
          const exRes = await getBatchPendingExemptions(exemptionCheckIds.join(','))
          if (exRes.data?.code === 200 && exRes.data.data) {
            const exMap = exRes.data.data as Record<string, { id: number; status: number; canAudit?: boolean }>
            dataSource.value.forEach((r: any) => {
              const ex = exMap[String(r.id)]
              if (ex) {
                r.pendingExemptionId = ex.status === 0 ? ex.id : undefined
                r.exemptionStatus = ex.status
                r.canAuditExemption = ex.canAudit === true
              }
            })
          }
        } catch (e: any) { console.error('查询豁免状态失败:', e) }
      }
      // 查询已过资料提交环节的申报单的在途补交单（补交中标签 + 发起/审核入口）
      await refreshSupplementFlags()
      // 回填资料上传进度（不阻塞列表渲染，失败仅该列显示 -）
      refreshMaterialProgress()
    } else { dataSource.value = []; pagination.total = 0 }
  } catch (error: any) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败: ' + (error.message || '未知错误'))
    dataSource.value = []; pagination.total = 0
  } finally { loading.value = false }
}

let refreshTimer: number | null = null
/** 页面从后台回到前台：补一次列表刷新（后台期间定时器被跳过） */
const handleListVisibilityChange = () => {
  if (!document.hidden) loadData()
}
const startAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = window.setInterval(() => {
    // 页面在后台（切走标签页/锁屏）时跳过刷新，避免回前台后积压补跳连续发请求堆提示
    if (document.hidden) return
    // 查询状态 1-9 的申报单的活跃任务
    const processingIds = dataSource.value.filter((r: any) => r.status >= 1 && r.status <= 9 && r.status !== 11).map((r: any) => r.id)
    if (processingIds.length > 0) {
      getBatchActiveTasks(processingIds.join(',')).then(taskRes => {
        if (taskRes.data?.code === 200 && taskRes.data.data) {
          const payload = taskRes.data.data
          const taskMap = payload.tasks ?? payload
          const myTaskMap = payload.myTasks ?? {}
          dataSource.value.forEach((r: any) => {
            const newTasks = taskMap[String(r.id)] || []
            const newMyTasks = myTaskMap[String(r.id)] || []
            if (JSON.stringify(r.activeTasks || []) !== JSON.stringify(newTasks)) r.activeTasks = newTasks
            if (JSON.stringify(r.myTasks || []) !== JSON.stringify(newMyTasks)) r.myTasks = newMyTasks
          })
        }
      }).catch(() => {})
    }
    // 同步刷新在途补交单标记
    refreshSupplementFlags()
    // 同步刷新资料上传进度
    refreshMaterialProgress()
  }, 30000)
  document.addEventListener('visibilitychange', handleListVisibilityChange)
}

/** 批量刷新在途补交单标记（pendingSupplementId），供首次加载与定时刷新复用 */
const refreshSupplementFlags = async () => {
  // 与后端补交发起门槛一致：status>=2（进入资料环节）即可存在在途补交单，需回填补交标记
  const supplementCheckIds = dataSource.value.filter((r: any) => r.status >= 2 && r.status !== 11).map((r: any) => r.id)
  if (supplementCheckIds.length === 0) return
  try {
    const supRes = await getBatchActiveSupplements(supplementCheckIds.join(','))
    if (supRes.data?.code === 200 && supRes.data.data) {
      const supMap = supRes.data.data as Record<string, number>
      dataSource.value.forEach((r: any) => {
        // 后端约定：正值=在途补交单ID，负值=-id 表示草稿补交单
        const v = supMap[String(r.id)] ?? null
        r.pendingSupplementId = v != null ? Math.abs(v) : null
        r.pendingSupplementStatus = v == null ? null : (v < 0 ? -1 : 0)
      })
    }
  } catch (e: any) { console.error('查询在途补交单失败:', e) }
}
/** 批量回填资料上传进度（materialProgress），口径与表单页进度一致：全环节必填项 vs 已上传项 */
const refreshMaterialProgress = async () => {
  const ids = dataSource.value.map((r: any) => r.id)
  if (ids.length === 0) return
  try {
    const res = await getBatchMaterialProgress(ids.join(','))
    if (res.data?.code === 200 && res.data.data) {
      const map = res.data.data as Record<string, { total: number; required: number; uploaded: number; percent: number }>
      dataSource.value.forEach((r: any) => { r.materialProgress = map[String(r.id)] ?? null })
    }
  } catch (e: any) { console.error('查询资料进度失败:', e) }
}
const stopAutoRefresh = () => { if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }; document.removeEventListener('visibilitychange', handleListVisibilityChange) }

const resetSearch = () => { searchForm.formNo = ''; searchForm.status = ''; searchForm.consignee = ''; searchForm.shipper = ''; searchForm.invoiceNo = ''; searchForm.dateRange = undefined; pagination.current = 1; loadData() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadData() }
const handleView = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&readonly=true`) }
const handleStatusSubmit = (record: DeclarationRecord) => {
  // 跳转到编辑页并自动触发提交：完整复用编辑页 handleSubmit 的校验与提交逻辑，避免两处维护
  router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&autoSubmit=1`)
}
const handleEdit = (record: DeclarationRecord) => { if (record.status !== 0) { message.warning('只有草稿状态可编辑'); return }; router.push(`${declarationPrefix.value}/form-v2?id=${record.id}`) }
// 跳转只带 id：入口模式由表单页按后端实时任务/状态推断（兼容带 mode 的老链接）
const handleAudit = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}`) }
const handleMaterialSubmit = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&scrollTo=material`) }
const handleMaterialAudit = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&scrollTo=material`) }
const handleExemptionAudit = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?exemptionId=${record.pendingExemptionId}`) }
const handleGoMode = (record: DeclarationRecord, mode: string) => {
  // 根据 mode 确定滚动位置（mode 不再随 URL 传递，仅供滚动映射使用）
  const scrollMap: Record<string, string> = {
    'supplement': 'supplement',
    'supplementAudit': 'supplement',
    'invoiceAmount': 'invoice-amount',
    'invoiceAmountAudit': 'invoice-amount',
    'invoiceUpload': 'invoice',
    'invoiceAudit': 'invoice'
  }
  const scrollTo = scrollMap[mode] || ''
  router.push(`${declarationPrefix.value}/form-v2?id=${record.id}${scrollTo ? '&scrollTo=' + scrollTo : ''}`)
}
const handleGoSubmitInvoice = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&scrollTo=invoice`) }
const handleInvoiceAudit = (record: DeclarationRecord) => { router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&scrollTo=invoice`) }

// ==================== 资料补交（列表页发起/审核） ====================

/** 发起补交：先在服务端创建草稿补交单（补交意图落库），再进入补交上传模式。
 *  supplementDraft=1 仅在确有在途补交单时有效，避免补交通过/取消后残留 URL 误入补交模式 */
const openSupplementStart = async (record: DeclarationRecord) => {
  try {
    const res = await startMaterialSupplement({ formId: record.id, reason: '' })
    if (res.data?.code === 200) {
      router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&supplementDraft=1`)
    } else {
      message.error(res.data?.message || '发起补交失败')
    }
  } catch (e: any) {
    message.error(e?.message || '发起补交失败')
  }
}
/** 继续补交：草稿补交单重新进入补交上传模式（同样不滚动，保持页面顶部） */
const handleContinueSupplement = (record: DeclarationRecord) => {
  router.push(`${declarationPrefix.value}/form-v2?id=${record.id}&supplementDraft=1`)
}
/** 取消补交：作废草稿补交单（同时清除草稿期上传的增量资料） */
const handleCancelSupplement = (record: DeclarationRecord) => {
  if (!record.pendingSupplementId) return
  Modal.confirm({
    title: '确认取消本次资料补交？',
    content: `申报单 ${record.formNo || record.id} 的补交草稿将被作废，草稿期间已上传的补交资料会一并清除，存量资料不受影响。`,
    okText: '取消补交',
    okType: 'danger',
    cancelText: '再想想',
    onOk: async () => {
      try {
        const res = await cancelMaterialSupplement(record.pendingSupplementId!)
        if (res.data?.code === 200) {
          message.success('补交草稿已取消')
          await loadData()
        } else {
          message.error(res.data?.message || '取消补交失败')
        }
      } catch (e: any) {
        message.error('取消补交失败: ' + (e?.message || '未知错误'))
      }
    }
  })
}
/** 补交审核：进入详情页补交审核模式，审核卡片置顶展示，不再滚动到资料区 */
const handleSupplementAudit = (record: DeclarationRecord) => {
  // 只带 supplementId：表单页反查补交单后自动进入补交审核模式（带 id 会跳过反查）
  router.push(`${declarationPrefix.value}/form-v2?supplementId=${record.pendingSupplementId}`)
}
/** 更多菜单：无活跃任务时恢复流程（非迁移场景） */
const canShowResumeFlow = (record: DeclarationRecord) => {
  if (record.needsFlowMigration) return false
  if (record.status == null || record.status < 1 || record.status > 9) return false
  if (!checkPermission(['business:declaration:resume:flow'])) return false
  const tasks = record.activeTasks || []
  return tasks.length === 0
}

const handleResumeFlow = async (record: DeclarationRecord) => {
  if (!record.needsFlowMigration) {
    try {
      const taskRes = await getActiveTasks(record.id)
      const raw = taskRes.data?.data
      const tasks = Array.isArray(raw) ? raw : (raw?.tasks ?? [])
      if (tasks && tasks.length > 0) {
        message.warning('该申报单已有活跃流程，无需恢复')
        return
      }
    } catch (e) {
      console.warn('查询活跃任务失败，继续恢复流程', e)
    }
  }

  Modal.confirm({
    title: record.needsFlowMigration ? '确认迁移到新版流程？' : '确认恢复流程？',
    content: record.needsFlowMigration
      ? (record.status === 2 || record.status === 3
          ? `申报单 ${record.formNo || record.id} 当前为旧版流程（资料提交/审核阶段），迁移后挂到新版流程对应节点，可继续补充资料等环节。`
          : `申报单 ${record.formNo || record.id} 当前为旧版流程，将在列表直接迁移到新版对应节点。`)
      : `申报单 ${record.formNo || record.id} 将迁移到新版流程对应节点。`,
    okText: '确认',
    onOk: async () => { 
      try { 
        const res: any = await resumeDeclarationFlow(record.id)
        if (res.data?.code === 200) { 
          message.success('流程已恢复'); 
          loadData() 
        } else {
          message.error(res.data?.message || '恢复失败')
        }
      } catch (e: any) { 
        message.error('恢复失败: ' + (e.response?.data?.message || e.message)) 
      } 
    }
  })
}

const statusTextMap: Record<number, string> = {
  4: '资料审核', 6: '补充资料审核', 8: '开票金额审核'
}
const handleRollback = (record: DeclarationRecord) => {
  const targetText = statusTextMap[record.status] || '上一审核节点'
  Modal.confirm({
    title: '确认申请退回上一步？',
    content: `申报单 ${record.formNo || record.id} 将申请退回到「${targetText}」节点，需审核通过后才会退回。`,
    okText: '提交申请',
    onOk: async () => {
      try {
        const res: any = await rollbackDeclaration(record.id)
        if (res.data?.code === 200) {
          message.success('退回申请已提交，等待审核')
          loadData()
        } else {
          message.error(res.data?.message || '申请失败')
        }
      } catch (e: any) {
        message.error('申请失败: ' + (e.response?.data?.message || e.message))
      }
    }
  })
}

const handleRollbackAudit = (record: DeclarationRecord) => {
  rollbackAuditForm.id = record.id
  rollbackAuditForm.approved = true
  rollbackAuditForm.remark = '已核对数据，通过'
  rollbackAuditVisible.value = true
}
const submitRollbackAudit = async () => {
  if (!rollbackAuditForm.remark?.trim()) { message.warning('请输入审核意见'); return }
  rollbackAuditLoading.value = true
  try {
    const res: any = await rollbackAuditDeclaration(rollbackAuditForm.id, rollbackAuditForm.approved, rollbackAuditForm.remark.trim())
    if (res.data?.code === 200) {
      message.success(rollbackAuditForm.approved ? '已审核通过，流程已退回' : '已驳回退回申请')
      rollbackAuditVisible.value = false
      loadData()
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch (e: any) {
    message.error('操作失败: ' + (e.response?.data?.message || e.message))
  } finally {
    rollbackAuditLoading.value = false
  }
}

const handleDownload = async (record: DeclarationRecord) => {
  try { loading.value = true; currentDeclaration.value = record; const r = await getDeclarationDetail(record.id)
    if (r.data?.code === 200) { currentAttachments.value = r.data.data.attachments || []
      try { currentContractsLoading.value = true; const cr = await getContractsByDeclaration(record.id); currentContracts.value = cr.data?.code === 200 ? cr.data.data || [] : [] } catch { currentContracts.value = [] } finally { currentContractsLoading.value = false }
      attachmentModalVisible.value = true } else message.error('获取附件列表失败')
  } catch { message.error('获取附件列表失败') } finally { loading.value = false }
}
const handleExport = () => { message.info('批量导出功能开发中...') }
const handleDelete = async (record: DeclarationRecord) => { try { await deleteDeclarationApi(record.id, record.status); message.success('删除成功'); loadData() } catch { message.error('删除失败') } }
const handleOpenGenerate = async (record: any) => {
  generateForm.declarationFormId = record.id; generateForm.templateId = undefined
  try { const res = await getEnabledTemplates(); if (res.data?.code === 200) { templateOptions.value = res.data.data || []; generateModalVisible.value = true } else message.error('获取合同模板失败') } catch { message.error('获取合同模板失败') }
}
const handleConfirmGenerate = async () => {
  if (!generateForm.templateId) { message.warning('请选择合同模板'); return }; if (!generateForm.declarationFormId) return
  generateLoading.value = true
  try { const res = await generateContract(generateForm.templateId, generateForm.declarationFormId!, {})
    if (res.data?.code === 200) { const g = res.data.data; message.success('合同生成成功'); generateModalVisible.value = false
      if (g?.id) Modal.confirm({ title: '合同已生成', content: `合同编号：${g.contractNo}，是否立即下载？`, okText: '下载', cancelText: '关闭', onOk: () => downloadContract(g.id) })
    } else message.error('合同生成失败')
  } catch { message.error('合同生成请求失败') } finally { generateLoading.value = false }
}

const getStatusText = (s: number) => ({ 0: '草稿', 1: '待初审', 2: '待资料提交', 3: '待资料审核', 4: '待补充资料提交', 5: '待补充资料审核', 6: '待开票金额提交', 7: '待开票金额审核', 8: '待发票提交', 9: '待发票审核', 10: '已完成', 11: '退回待审' }[s] || '未知')
const getStatusColor = (s: number) => ({ 0: 'default', 1: 'processing', 2: 'blue', 3: 'purple', 4: 'cyan', 5: 'lime', 6: 'gold', 7: 'geekblue', 8: 'geekblue', 9: 'magenta', 10: 'success', 11: 'warning' }[s] || 'default')

/** 判断当前用户是否有该状态对应的 Flowable 任务 */
const hasMyTaskForStatus = (record: any, status: number, taskKey: string): boolean => {
  if (record.status !== status) return false
  // myTasks 已加载时，精确匹配 Flowable 任务
  if (Array.isArray(record.myTasks)) return record.myTasks.includes(taskKey)
  // myTasks 未加载时不显示按钮，避免普通用户误看到审批入口
  return false
}
const isDocumentFile = (f: string) => ['.pdf','.doc','.docx','.xls','.xlsx','.ppt','.pptx','.txt'].some(e => f.toLowerCase().endsWith(e))
const isImageFile = (f: string) => ['.jpg','.jpeg','.png','.gif','.bmp','.webp','.svg'].some(e => f.toLowerCase().endsWith(e))
const getFileTypeColor = (t: string) => ({ Invoice: 'blue', PackingList: 'green', FullDocuments: 'purple', PickupList: 'orange', Remittance: 'cyan', Contract: 'magenta', AllDocuments: 'cyan', AllDocumentsPdf: 'red', FullDocumentsPdf: 'red' }[t] || 'default')
const getFileTypeText = (t: string) => ({ Invoice: '商业发票', PackingList: '装箱单', FullDocuments: '海关附件', PickupList: '提货单', Remittance: '水单', Contract: '合同', AllDocuments: '海关资料', AllDocumentsPdf: '报关单PDF', FullDocumentsPdf: '预录入PDF' }[t] || t)
const getFileExtension = (f: string) => f.substring(f.lastIndexOf('.'))
const formatFileSize = (s: number) => !s ? '0 KB' : s < 1024 ? s + ' B' : s < 1048576 ? (s / 1024).toFixed(1) + ' KB' : (s / 1048576).toFixed(1) + ' MB'

const getDeclarationForAttachment = (att: any) => dataSource.value.find((r: any) => r.id === att.formId) || null
const handleRegenerateSimple = async (att: any) => {
  const decl = getDeclarationForAttachment(att); if (!decl) return; const ft = att.fileType || att.type; if (!ft) { message.warning('无法识别文件类型'); return }
  if (ft === 'AllDocuments') { askMergeProductsChoice(decl); return }; await doRegenerateByFileType(decl, ft)
}
const askMergeProductsChoice = (decl: any) => {
  Modal.confirm({ title: '重新生成全套单据', content: '是否合并同款商品？', okText: '合并', cancelText: '不合并', onOk: () => doRegenerate(decl, true), onCancel: () => doRegenerate(decl, false) })
}
const doRegenerateByFileType = async (decl: any, ft: string) => {
  try { let r; switch (ft) { case 'FullDocuments': r = await regenerateDocuments(decl.id); break; case 'Remittance': r = await regenerateRemittanceReport(decl.id); break; default: message.warning('不支持的文件类型'); return }
    if (r.data?.code === 200) { message.success('重新生成成功'); await loadAttachmentsForDeclaration(decl) } else message.error('重新生成失败')
  } catch (e: any) { message.error('重新生成失败: ' + e.message) }
}
const doRegenerate = async (decl: any, merge: boolean) => {
  try { const r = await regenerateAllDocuments(decl.id, merge)
    if (r.data?.code === 200) { message.success(`重新生成成功${merge ? '（已合并）' : ''}`); await loadAttachmentsForDeclaration(decl) } else message.error('重新生成失败')
  } catch (e: any) { message.error('重新生成失败: ' + e.message) }
}
const loadAttachmentsForDeclaration = async (decl: any) => {
  try { const r = await getDeclarationAttachments(decl.id); if (r.data?.code === 200) { decl.attachments = r.data.data || []; if (currentDeclaration.value?.id === decl.id) currentAttachments.value = decl.attachments } } catch {}
}
const previewVisible = ref(false)
const previewUrl = ref('')
const handlePreviewContract = (id: number) => { previewUrl.value = getContractDownloadUrl(id); previewVisible.value = true }
const previewAttachment = (att: any) => { if (att.fileUrl) { previewUrl.value = att.fileUrl; previewVisible.value = true } }
const downloadAttachment = (att: any) => {
  if (!att.fileUrl) return
  const a = document.createElement('a')
  a.href = att.fileUrl
  a.download = att.fileName || 'download'
  a.click()
}
const showReplaceContractModal = (c: any) => { currentReplacingContract.value = c; replaceContractFileList.value = []; replaceContractModalVisible.value = true }
const beforeReplaceContractUpload = (file: any) => { if (file.size / 1048576 > 10) { message.error('不超过10MB!'); return false }; if (!file.name.toLowerCase().endsWith('.docx')) { message.error('只支持.docx!'); return false }; replaceContractFileList.value = [file]; return false }
const handleReplaceContract = async () => {
  if (!currentReplacingContract.value || !replaceContractFileList.value.length) { message.warning('请选择文件'); return }
  replaceContractLoading.value = true
  try { const fd = new FormData(); fd.append('file', replaceContractFileList.value[0]); const r = await replaceContractFile(currentReplacingContract.value.id, fd)
    if (r.data?.code === 200) { message.success('替换成功'); replaceContractModalVisible.value = false; if (currentDeclaration.value) { const cr = await getContractsByDeclaration(currentDeclaration.value.id); if (cr.data?.code === 200) currentContracts.value = cr.data.data || [] } } else message.error('替换失败')
  } catch (e: any) { message.error('替换失败: ' + e.message) } finally { replaceContractLoading.value = false }
}
const showReplaceModal = (att: any) => { currentReplacingAttachment.value = att; replaceFileList.value = []; replaceModalVisible.value = true }
const beforeReplaceUpload = (file: any) => { if (file.size / 1048576 > 10) { message.error('不超过10MB!'); return false }; if (currentReplacingAttachment.value) { const oe = getFileExtension(currentReplacingAttachment.value.fileName); if (oe.toLowerCase() !== getFileExtension(file.name).toLowerCase()) { message.error(`格式须与原文件一致 (${oe})`); return false } }; replaceFileList.value = [file]; return false }
const handleReplaceAttachment = async () => {
  if (!currentReplacingAttachment.value || !replaceFileList.value.length) { message.warning('请选择文件'); return }
  replaceLoading.value = true
  try { const fd = new FormData(); fd.append('file', replaceFileList.value[0]); const r = await fetch(`/api/v1/declarations/${currentReplacingAttachment.value.formId}/attachments/${currentReplacingAttachment.value.id}/replace`, { method: 'POST', body: fd }); const res = await r.json()
    if (res.code === 200) { message.success('替换成功'); replaceModalVisible.value = false; if (currentAttachments.value.length) { const fId = currentAttachments.value[0].formId; const dr = await getDeclarationDetail(fId); if (dr.data?.code === 200) currentAttachments.value = dr.data.data.attachments || [] } } else message.error('替换失败')
  } catch { message.error('替换失败: 网络错误') } finally { replaceLoading.value = false }
}

const returnHistoryColumns = [
  { title: '状态', key: 'auditStatus', width: 70 }, { title: '业务类型', key: 'businessType', width: 120 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 90 },
  { title: '原因', dataIndex: 'applyReason', key: 'applyReason', ellipsis: true, minWidth: 150 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 , customRender: ({ text }: any) => text ? fmtDateTime(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
  { title: '审核人', dataIndex: 'auditorName', key: 'auditorName', width: 90 },
  { title: '备注', dataIndex: 'auditRemark', key: 'auditRemark', ellipsis: true, minWidth: 150 },
  { title: '审核时间', dataIndex: 'auditTime', key: 'auditTime', width: 160 , customRender: ({ text }: any) => text ? fmtDateTime(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
  { title: '原状态', key: 'preStatus', width: 70 }
]
const getBusinessTypeText = (t: string) => ({ DECLARATION_RETURN: '退回草稿', DECLARATION_ROLLBACK: '退回上一步', DECLARATION_AUDIT: '申报审核', DECLARATION_SUBMIT: '申报提交', DECLARATION_MATERIAL_AUDIT: '资料审核', DECLARATION_SUPPLEMENT_AUDIT: '补充资料审核', DECLARATION_INVOICE_AMOUNT_AUDIT: '开票金额审核', DECLARATION_INVOICE_AUDIT: '业务发票审核', REMITTANCE_AUDIT: '水单审核', DELIVERY_ORDER_AUDIT: '提货单审核' }[t] || t)
const getBusinessTypeColor = (t: string) => ({ DECLARATION_RETURN: 'orange', DECLARATION_ROLLBACK: 'volcano', DECLARATION_AUDIT: 'blue', DECLARATION_SUBMIT: 'cyan', DECLARATION_MATERIAL_AUDIT: 'purple', DECLARATION_INVOICE_AUDIT: 'magenta' }[t] || 'default')
const handleReturnApply = (r: DeclarationRecord) => { returnApplyForm.id = r.id; returnApplyForm.reason = '申报错误'; returnApplyVisible.value = true }
const handleReturnAudit = (r: DeclarationRecord) => { returnAuditForm.id = r.id; returnAuditForm.approved = true; returnAuditForm.remark = '已核对数据，通过'; returnAuditVisible.value = true }
const submitReturnApply = async () => {
  if (!returnApplyForm.reason) { message.warning('请输入退回原因'); return }; returnApplyLoading.value = true
  try { const r = await applyReturnToDraft(returnApplyForm.id, returnApplyForm.reason); if (r.data?.code === 200) { message.success('退回申请已提交'); returnApplyVisible.value = false; loadData() } else message.error('申请失败') } catch { message.error('申请过程发生错误') } finally { returnApplyLoading.value = false }
}
const submitReturnAudit = async () => {
  if (!returnAuditForm.remark?.trim()) { message.warning('请输入审核意见'); return }; returnAuditLoading.value = true
  try { const r = await auditReturnToDraft(returnAuditForm.id, { approved: returnAuditForm.approved, remark: returnAuditForm.remark.trim() }); if (r.data?.code === 200) { message.success(returnAuditForm.approved ? '审核通过，已退回草稿' : '已驳回申请'); returnAuditVisible.value = false; loadData() } else message.error('审核失败') } catch { message.error('审核过程发生错误') } finally { returnAuditLoading.value = false }
}
const viewReturnHistory = async (r: DeclarationRecord) => {
  returnHistoryVisible.value = true; returnHistoryLoading.value = true
  try { const res = await getReturnAuditHistory(r.id); if (res.data?.code === 200) returnHistoryList.value = res.data.data || [] } catch { message.error('加载审核历史失败') } finally { returnHistoryLoading.value = false }
}

onMounted(() => {
  loadData(); startAutoRefresh()
  if (route.query.action === 'audit' && route.query.id) { const id = Number(route.query.id); if (!isNaN(id)) setTimeout(() => router.push(`${declarationPrefix.value}/form-v2?id=${id}`), 300) }
})

// keep-alive 缓存页：从表单提交/审核返回时自动刷新列表（跳过首次挂载，数据已由 onMounted 加载）
const isFirstActivation = ref(true)
onActivated(() => {
  if (isFirstActivation.value) { isFirstActivation.value = false; return }
  loadData()
})

// 监听申报类型变化（从 SELF 切到 EXTERNAL 或反之），重新加载数据
watch(currentDeclarationType, () => {
  pagination.current = 1
  loadData()
})

onUnmounted(() => { stopAutoRefresh() })
</script>

<style scoped>
.declaration-manage { height: 100%; overflow-x: hidden; }
.search-btn-row { display: flex; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.attachment-title { display: flex; align-items: center; font-weight: 500; }
.attachment-info { display: flex; align-items: center; gap: 12px; margin-top: 4px; }
.file-size { font-size: 12px; color: #888; }
.create-time { font-size: 12px; color: #888; }
</style>