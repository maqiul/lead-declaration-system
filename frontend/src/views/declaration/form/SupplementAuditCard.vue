<template>
  <!-- 资料补交审核卡片：头部操作按钮 + 申请信息 + 增量明细（由父组件置顶渲染） -->
  <div class="supp-audit-card">
    <div class="supp-audit-head">
      <div class="supp-audit-title">
        <span class="supp-audit-icon"><AuditOutlined /></span>
        <span class="supp-audit-name">资料补交审核</span>
        <a-tag color="orange">待审核</a-tag>
      </div>
      <a-space v-if="showActions">
        <a-button
          v-permission="['business:declaration:audit:material']"
          danger
          size="small"
          @click="emit('audit', false)"
        >
          <template #icon><CloseCircleOutlined /></template>
          驳回
        </a-button>
        <a-button
          v-permission="['business:declaration:audit:material']"
          type="primary"
          size="small"
          class="supp-audit-btn-pass"
          @click="emit('audit', true)"
        >
          <template #icon><CheckCircleOutlined /></template>
          审核通过
        </a-button>
      </a-space>
    </div>
    <div class="supp-audit-info">
      <div class="supp-audit-reason">
        <span class="supp-audit-label">补交原因</span>
        <span class="supp-audit-value">{{ supplement.reason || '未填写原因' }}</span>
      </div>
      <div class="supp-audit-info-grid">
        <span class="supp-audit-info-item"><UserOutlined class="supp-audit-info-icon" />发起人：{{ supplement.initiatorName || '-' }}</span>
        <span class="supp-audit-info-item"><ClockCircleOutlined class="supp-audit-info-icon" />发起时间：{{ supplement.createTime ? supplement.createTime.substring(0, 16) : '-' }}</span>
      </div>
    </div>
    <a-spin :spinning="loading">
      <template v-if="increments">
        <div v-if="increments.items?.length" class="supp-incr-body">
          <div class="supp-incr-section-title">补交新增资料项（{{ increments.items.length }}）<span class="supp-incr-section-hint">下方列表中对应橙色「补交待审核」标签</span></div>
          <a-tag v-for="it in increments.items" :key="it.id" color="orange" style="margin-bottom:4px">{{ it.name }}<span v-if="it.stage">（{{ stageLabel(it.stage) }}）</span></a-tag>
        </div>
        <div v-if="increments.attachments?.length" class="supp-incr-body">
          <div class="supp-incr-section-title">补交上传文件（{{ increments.attachments.length }}）</div>
          <div v-for="att in increments.attachments" :key="att.id" class="supp-incr-file">
            <span class="supp-incr-file-icon"><FileTextOutlined /></span>
            <div class="supp-incr-file-main">
              <a @click.prevent="emit('preview', att.fileUrl)" class="supp-incr-file-name" :title="att.fileName">{{ att.fileName }}</a>
              <div class="supp-incr-file-meta">
                <span class="supp-incr-stage-label">提交环节：</span>
                <a-tag color="purple" class="supp-incr-stage-tag">{{ stageLabel(att.stage) }}</a-tag>
                <span class="supp-incr-item-name">{{ att.itemName || '未关联资料项' }}</span>
              </div>
            </div>
            <span class="supp-incr-file-time">{{ att.uploadTime ? att.uploadTime.substring(0, 16) : '' }}</span>
          </div>
        </div>
        <a-empty v-if="!increments.items?.length && !increments.attachments?.length" description="暂无增量数据" :image-style="{ height: '40px' }" />
      </template>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import {
  AuditOutlined, CloseCircleOutlined, CheckCircleOutlined,
  UserOutlined, ClockCircleOutlined, FileTextOutlined
} from '@ant-design/icons-vue'

defineProps<{
  /** 待审补交单 */
  supplement: any
  /** 补交增量明细（资料项 + 附件） */
  increments?: { items?: any[]; attachments?: any[] } | null
  /** 增量明细加载中 */
  loading?: boolean
  /** 是否展示驳回/审核通过按钮（审核模式或任务中心进入） */
  showActions?: boolean
}>()

const emit = defineEmits<{
  /** 审核操作：approved=true 通过 / false 驳回 */
  audit: [approved: boolean]
  /** 预览补交文件 */
  preview: [url: string]
}>()

/** 环节编码 → 中文名称（支持逗号分隔多环节） */
const STAGE_LABELS: Record<string, string> = {
  BASIC: '基础资料',
  MATERIAL_SUBMIT: '资料提交',
  SUPPLEMENT: '补充资料',
  INVOICE: '发票资料'
}
const stageLabel = (stage?: string | null): string =>
  (stage || '').split(',').map(s => STAGE_LABELS[s.trim()] || s.trim()).filter(Boolean).join(' / ') || '未分类'
</script>

<style scoped>
/* 补交审核卡片：头部标题+操作按钮，申请信息，增量明细 */
.supp-audit-card {
  margin: 4px 0 14px;
  background: #fffbf2;
  border: 1px solid #ffe7ba;
  border-radius: 10px;
  padding: 0 16px 12px;
}
.supp-audit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-bottom: 1px dashed #ffe7ba;
}
.supp-audit-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.supp-audit-icon {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #fff;
  background: linear-gradient(135deg, #ffa940, #fa8c16);
  border-radius: 7px;
}
.supp-audit-name {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
}
/* 审核通过按钮：绿色主按钮 */
.supp-audit-btn-pass {
  background: #52c41a;
  border-color: #52c41a;
}
.supp-audit-btn-pass:hover,
.supp-audit-btn-pass:focus {
  background: #73d13d !important;
  border-color: #73d13d !important;
}
/* 申请信息：原因突出展示 + 发起人/时间一行 */
.supp-audit-info {
  padding: 10px 0 2px;
}
.supp-audit-reason {
  background: #fff;
  border-left: 3px solid #fa8c16;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
}
.supp-audit-label {
  color: #999;
  margin-right: 8px;
}
.supp-audit-info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 28px;
  margin-top: 8px;
  font-size: 13px;
  color: #595959;
}
.supp-audit-info-icon {
  color: #fa8c16;
  margin-right: 4px;
}
/* 增量明细分区 */
.supp-incr-body {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ffe7ba;
}
.supp-incr-section-title {
  color: #666;
  font-size: 13px;
  margin-bottom: 6px;
}
.supp-incr-section-hint {
  color: #bbb;
  font-size: 12px;
  margin-left: 6px;
}
.supp-incr-file {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
  transition: border-color 0.2s;
}
.supp-incr-file:hover {
  border-color: #ffd591;
}
.supp-incr-file-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fa8c16;
  background: #fff7e6;
  border-radius: 5px;
}
.supp-incr-file-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.supp-incr-file-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
}
.supp-incr-stage-tag {
  margin: 0;
}
.supp-incr-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.supp-incr-file-name {
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.supp-incr-file-time {
  color: #999;
  font-size: 12px;
  margin-left: auto;
  flex-shrink: 0;
}
</style>
