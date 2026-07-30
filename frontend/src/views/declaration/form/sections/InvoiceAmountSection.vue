<template>
  <div class="section-wrapper">
    <a-card v-if="showInvoiceAmountSection" id="section-invoice-amount" title="申请开票金额" size="small" class="section-card">
      <template #extra>
        <a-space>
          <a-button
            v-if="canSubmitInvoiceAmount"
            type="primary" size="small"
            @click="$emit('submit-invoice-amount')"
            :loading="submitting"
            v-permission="['business:declaration:invoice-amount:submit']"
          >
            <template #icon><UploadOutlined /></template>
            提交开票金额
          </a-button>

          <template v-if="canAuditInvoiceAmount">
            <a-button
              type="primary" size="small"
              @click="$emit('invoice-amount-audit-approve')"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice-amount']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger size="small"
              @click="$emit('invoice-amount-audit-reject')"
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
            <div class="progress-desc">系统根据收汇、商品退税率、发票资料项自动计算</div>
          </div>
          <div class="progress-right">
            <!-- <a-button v-if="showInvoiceAmountSection" type="link" @click="$emit('load-invoice-amount-detail')">
              <template #icon><ReloadOutlined /></template>
              {{ isInvoiceAmountEditable ? '刷新计算' : '加载详情' }}
            </a-button> -->
            <a-button type="link" @click="$emit('download-invoice-package')">
              <template #icon><DownloadOutlined /></template>
              下载开票文件
            </a-button>
          </div>
        </div>

        <a-alert v-if="isInvoiceAmountEditable" type="info" show-icon message="提交开票金额申请前，请确认外汇水单已提交。系统将自动计算开票金额。" style="margin-bottom: 12px" />

        <!-- 关联水单 -->
        <div style="margin-bottom: 16px;">
          <div style="display: flex; align-items: center; margin-bottom: 8px;">
            <LinkOutlined style="margin-right: 6px; color: #1677ff;" />
            <span style="font-weight: 600; font-size: 14px;">关联水单</span>
            <a-tag v-if="invoiceAmountRemittances.length > 0" style="margin-left: 8px;">{{ invoiceAmountRemittances.length }} 笔</a-tag>
          </div>
          <a-table v-if="invoiceAmountRemittances.length > 0" :dataSource="invoiceAmountRemittances" :columns="remittanceColumns" :pagination="false" size="small" rowKey="id" :scroll="{ x: 860 }" bordered />
          <a-empty v-else description="暂无关联水单，请先在水单管理中关联并审核通过" :image-style="{ height: '30px' }" />
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 申请开票金额 Section
 * - 数据/计算属性通过 inject 获取
 * - 操作类事件通过 emit 通知父组件
 */
import { toRefs } from 'vue'
import { useFormState } from '../composables/useDeclarationForm'
import {
  UploadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  CalculatorOutlined, DownloadOutlined,
  LinkOutlined, 
} from '@ant-design/icons-vue'

const emit = defineEmits<{
  'submit-invoice-amount': []
  'invoice-amount-audit-approve': []
  'invoice-amount-audit-reject': []
  'load-invoice-amount-detail': []
  'download-invoice-package': []
}>()

const state = useFormState()
const {
  showInvoiceAmountSection, canSubmitInvoiceAmount, canAuditInvoiceAmount,
  submitting, invoiceAmountLoading, isInvoiceAmountEditable,
  invoiceAmountRemittances, remittanceColumns,
} = toRefs(state) as any
</script>
