<template>
  <div class="supp-audit-page">
    <div class="supp-audit-header" :class="isSelf ? 'theme-self' : 'theme-ext'">
      <div class="supp-audit-header-left">
        <span class="supp-audit-header-icon"><AuditOutlined /></span>
        <div>
          <div class="supp-audit-header-title">
            {{ isSelf ? '内部申报' : '外部申报' }} · 待审资料补交
          </div>
          <div class="supp-audit-header-desc">
            共 <b>{{ dataSource.length }}</b> 条待审补交单，点击「去审核」查看增量资料并通过/驳回
          </div>
        </div>
      </div>
      <a-button size="small" class="supp-audit-refresh-btn" :loading="loading" @click="loadData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <div class="supp-audit-body">
      <a-alert
        type="info"
        show-icon
        message="申报人发起补交并上传资料后提交审核，此处展示当前分类下所有待审补交单。审核通过后增量资料转正生效，驳回则清除补交的增量资料（驳回原因必填）。"
        style="margin-bottom: 12px"
      />
      <a-table
        :dataSource="dataSource"
        :columns="columns"
        :loading="loading"
        :pagination="false"
        rowKey="id"
        size="small"
        class="supp-audit-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <span class="supp-audit-form-no">{{ record.formNo || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'reason'">
            <span v-if="record.reason" class="supp-audit-reason" :title="record.reason">{{ record.reason }}</span>
            <span v-else class="supp-audit-muted">未填写原因</span>
          </template>
          <template v-else-if="column.key === 'initiatorName'">
            <UserOutlined class="supp-audit-user-icon" />
            {{ record.initiatorName || '-' }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ record.createTime ? formatDate(record.createTime, 'yyyy-MM-dd HH:mm') : '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="primary" size="small" class="supp-audit-go-btn" @click="goToAudit(record as MaterialSupplement)">
              <template #icon><CheckCircleOutlined /></template>
              去审核
            </a-button>
          </template>
        </template>
        <template #emptyText>
          <a-empty description="暂无待审补交单" :image-style="{ height: '60px' }" />
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined, CheckCircleOutlined, AuditOutlined, UserOutlined } from '@ant-design/icons-vue'
import { getPendingSupplements, type MaterialSupplement } from '@/api/business/materialItem'
import { formatDate } from '@/utils/common'

const router = useRouter()
const route = useRoute()

/** 根据路径前缀区分内部/外部申报（与 DeclarationListCore 保持一致） */
const isSelf = computed(() => route.path.startsWith('/declaration-self'))
const declarationPrefix = computed(() => isSelf.value ? '/declaration-self' : '/declaration-external')
/** 申报类型过滤参数：内部 SELF / 外部 EXTERNAL */
const declarationType = computed(() => isSelf.value ? 'SELF' : 'EXTERNAL')

const loading = ref(false)
const dataSource = ref<MaterialSupplement[]>([])

const columns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 200 },
  { title: '补交原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '发起人', dataIndex: 'initiatorName', key: 'initiatorName', width: 130 },
  { title: '发起时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' as const }
]

const loadData = async () => {
  try {
    loading.value = true
    const res = await getPendingSupplements({ declarationType: declarationType.value })
    if (res.data?.code === 200) {
      dataSource.value = res.data.data || []
    } else {
      message.error(res.data?.message || '加载待审补交列表失败')
    }
  } catch (e: any) {
    message.error('加载待审补交列表失败: ' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/** 去审核：进入申报详情页补交审核模式，自动定位该补交单（查看增量 + 通过/驳回） */
const goToAudit = (record: MaterialSupplement) => {
  router.push(`${declarationPrefix.value}/form-v2?id=${record.formId}&supplementId=${record.id}&mode=materialSupplementAudit&scrollTo=material`)
}

onMounted(loadData)
</script>

<style scoped>
.supp-audit-page {
  padding: 16px;
}
/* 头部横幅：内部蓝 / 外部绿 */
.supp-audit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 10px;
  margin-bottom: 12px;
}
.supp-audit-header.theme-self {
  background: linear-gradient(135deg, #e8f1ff, #f0f7ff);
  border: 1px solid #bdd7ff;
}
.supp-audit-header.theme-ext {
  background: linear-gradient(135deg, #e9f9ef, #f2fcf5);
  border: 1px solid #b3e8c5;
}
.supp-audit-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.supp-audit-header-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  color: #fff;
  border-radius: 9px;
  flex-shrink: 0;
}
.theme-self .supp-audit-header-icon {
  background: linear-gradient(135deg, #599bff, #1677ff);
}
.theme-ext .supp-audit-header-icon {
  background: linear-gradient(135deg, #5cd685, #2ca54f);
}
.supp-audit-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}
/* 刷新按钮：白底常规样式，避免 ghost 在浅色背景上发灰；悬停色跟随主题 */
.supp-audit-refresh-btn {
  background: #fff;
  border-color: #d9d9d9;
  color: #333;
}
.theme-self .supp-audit-refresh-btn:hover,
.theme-self .supp-audit-refresh-btn:focus {
  color: #1677ff !important;
  border-color: #1677ff !important;
  background: #fff !important;
}
.theme-ext .supp-audit-refresh-btn:hover,
.theme-ext .supp-audit-refresh-btn:focus {
  color: #2ca54f !important;
  border-color: #2ca54f !important;
  background: #fff !important;
}
.supp-audit-header-desc {
  margin-top: 3px;
  font-size: 12.5px;
  color: #666;
}
.supp-audit-header-desc b {
  color: #fa8c16;
  font-size: 14px;
  margin: 0 2px;
}
.supp-audit-body {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
}
.supp-audit-form-no {
  font-weight: 500;
  color: #1f1f1f;
}
.supp-audit-reason {
  color: #333;
}
.supp-audit-muted {
  color: #bbb;
}
.supp-audit-user-icon {
  color: #999;
  margin-right: 4px;
}
/* 去审核按钮：绿色主按钮 */
.supp-audit-go-btn {
  background: #52c41a;
  border-color: #52c41a;
}
.supp-audit-go-btn:hover,
.supp-audit-go-btn:focus {
  background: #73d13d !important;
  border-color: #73d13d !important;
}
</style>
