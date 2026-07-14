<template>
  <a-modal
    :open="visible"
    title="流程预览"
    :footer="null"
    :width="480"
    @cancel="$emit('update:visible', false)"
    destroyOnClose
  >
    <a-spin :spinning="loading">
      <div v-if="!loading && steps.length === 0" style="text-align: center; padding: 40px; color: #999;">
        暂无流程数据
      </div>
      <div v-else class="flow-steps">
        <!-- 开始 -->
        <div class="flow-node flow-start">
          <div class="node-circle start-circle">开始</div>
        </div>
        <div v-for="(step, index) in steps" :key="index">
          <div class="flow-arrow"><DownOutlined /></div>
          <div class="flow-node">
            <div class="node-box" :class="{ disabled: !step.enabled }">
              <span class="node-index">{{ index + 1 }}</span>
              <span class="node-name">{{ step.nodeName || step.nodeKey }}</span>
              <a-tag v-if="!step.enabled" color="default" size="small" style="margin-left: 6px;">已跳过</a-tag>
            </div>
          </div>
        </div>
        <!-- 结束 -->
        <div class="flow-arrow"><DownOutlined /></div>
        <div class="flow-node flow-end">
          <div class="node-circle end-circle">结束</div>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { DownOutlined } from '@ant-design/icons-vue'
import { getFlowTemplateNodes } from '@/api/system/flowTemplate'

interface StepItem {
  nodeKey: string
  nodeName: string
  enabled: number
}

const props = defineProps<{
  visible: boolean
  templateId: number | null
}>()

defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const loading = ref(false)
const steps = ref<StepItem[]>([])

watch(
  () => [props.visible, props.templateId] as const,
  async ([visible, templateId]) => {
    if (visible && templateId) {
      loading.value = true
      steps.value = []
      try {
        const res = await getFlowTemplateNodes(templateId)
        if (res.data?.code === 200) {
          const nodes = (res.data.data || []) as any[]
          steps.value = nodes.map((n: any) => ({
            nodeKey: n.node?.nodeKey || n.nodeKey || '',
            nodeName: n.node?.nodeName || n.stepName || n.nodeKey || '',
            enabled: n.enabled ?? 1,
          }))
        }
      } catch (e) {
        console.error('加载流程节点失败:', e)
      } finally {
        loading.value = false
      }
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.flow-steps {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.flow-node {
  display: flex;
  justify-content: center;
}

.node-box {
  display: flex;
  align-items: center;
  padding: 8px 20px;
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 8px;
  font-size: 14px;
  min-width: 200px;
  justify-content: center;
}

.node-box.disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #999;
}

.node-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  margin-right: 10px;
  flex-shrink: 0;
}

.node-box.disabled .node-index {
  background: #bfbfbf;
}

.node-name {
  font-weight: 500;
}

.node-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
}

.start-circle {
  background: #f6ffed;
  border: 2px solid #52c41a;
  color: #389e0d;
}

.end-circle {
  background: #fff2f0;
  border: 2px solid #ff4d4f;
  color: #cf1322;
}

.flow-arrow {
  display: flex;
  justify-content: center;
  padding: 4px 0;
  color: #bfbfbf;
  font-size: 16px;
}
</style>
