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
            <a-button v-if="showInvoiceAmountSection" type="link" @click="$emit('load-invoice-amount-detail')">
              <template #icon><ReloadOutlined /></template>
              {{ isInvoiceAmountEditable ? '刷新计算' : '加载详情' }}
            </a-button>
            <a-button v-if="invoiceAmountCalcDetail" type="link" @click="$emit('download-invoice-package')">
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

        <!-- 计算详情 -->
        <div v-if="invoiceAmountCalcDetail" class="calc-detail-wrap">
          <!-- 收入部分 -->
          <div class="calc-section calc-income">
            <div class="calc-section-title"><RiseOutlined style="margin-right: 6px;" /> 收入部分</div>
            <div v-for="(rd, idx) in (invoiceAmountCalcDetail.remittanceDetails || [])" :key="'rd-' + idx" style="margin-bottom: 12px; padding: 10px; background: #f9f9f9; border-radius: 4px;">
              <div class="calc-row">
                <span class="calc-label" style="font-weight: 600;">{{ rd.remittanceName || '水单' }}</span>
                <span class="calc-value">{{ fmtAmt(rd.amount) }} {{ rd.currency || 'USD' }} × {{ Number(rd.taxRate || 0).toFixed(4) }} = <b>{{ fmtAmt(rd.cnyAmount) }} CNY</b></span>
              </div>
              <!-- <div v-if="rd.proportion && rd.proportion < 100" style="font-size: 12px; color: #666; margin-left: 12px; margin-top: 4px;">
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
              </div> -->
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
                  <div style="margin-left: 12px;">原币: {{ fmtAmt(pd.amount) }} × 汇率: {{ invoiceAmountCalcDetail.weightedExchangeRate }} = {{ fmtAmt(pd.cnyAmount) }} CNY</div>
                  <div style="margin-left: 12px;">{{ fmtAmt(pd.cnyAmount) }} × (1+{{ pd.taxRefundRate }}%) = <b style="color: #16a34a;">{{ fmtAmt(pd.amountWithTaxRefund) }} CNY</b></div>
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
            <div class="calc-section-title"><FallOutlined style="margin-right: 6px;" /> 支出部分（扣减项）</div>
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


        </div>
        <a-empty v-else :description="isInvoiceAmountEditable ? '点击刷新计算加载开票金额详情' : '暂无开票金额计算数据'" />
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
  CalculatorOutlined, ReloadOutlined, DownloadOutlined,
  LinkOutlined, RiseOutlined, FallOutlined,
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
  invoiceAmountCalcDetail, calcExpenseTotal,
  invoiceAmountRemittances, remittanceColumns,
} = toRefs(state) as any

/** 金额格式化 */
const fmtAmt = (v: any): string => {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}
</script>
