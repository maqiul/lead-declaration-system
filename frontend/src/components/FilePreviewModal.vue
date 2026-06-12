<template>
  <a-modal
    v-model:open="visible"
    :title="previewTitle"
    :width="isPdf ? '90%' : '700px'"
    :footer="null"
    :destroy-on-close="true"
    class="file-preview-modal"
  >
    <div v-if="url" class="preview-container">
      <!-- PDF 预览 -->
      <div v-if="isPdf" class="pdf-preview">
        <iframe :src="pdfSrc" frameborder="0" class="pdf-iframe"></iframe>
      </div>

      <!-- 图片预览 -->
      <div v-else-if="isImage" class="image-preview">
        <img :src="authedUrl" class="preview-image" @click="openFullImage" />
        <div class="image-tip">点击图片可在新窗口查看原图</div>
      </div>

      <!-- 其他文件 -->
      <div v-else class="other-file">
        <a-result status="info" title="该文件类型不支持在线预览">
          <template #extra>
            <a-button type="primary" @click="downloadFile">下载文件</a-button>
          </template>
        </a-result>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getToken } from '@/utils/auth'

interface Props {
  visible: boolean
  url: string
  title?: string
}

interface Emit {
  (e: 'update:visible', visible: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  url: '',
  title: ''
})

const emit = defineEmits<Emit>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 文件类型判断
const isPdf = computed(() => /\.pdf(\?.*)?$/i.test(props.url || ''))
const isImage = computed(() => /\.(jpe?g|png|gif|webp|bmp|svg)(\?.*)?$/i.test(props.url || ''))

// 预览标题
const previewTitle = computed(() => {
  if (props.title) return props.title
  if (isPdf.value) return 'PDF 文件预览'
  if (isImage.value) return '图片预览'
  return '文件预览'
})

// 带认证参数的URL（iframe/img都无法发送自定义header）
const authedUrl = computed(() => {
  if (!props.url) return ''
  const token = getToken()
  if (!token) return props.url
  const separator = props.url.includes('?') ? '&' : '?'
  return `${props.url}${separator}satoken=${token}`
})

// PDF iframe src
const pdfSrc = authedUrl

// 在新窗口打开原图
const openFullImage = () => {
  if (authedUrl.value) window.open(authedUrl.value, '_blank')
}

// 下载文件
const downloadFile = () => {
  if (authedUrl.value) {
    const a = document.createElement('a')
    a.href = authedUrl.value
    a.download = ''
    a.target = '_blank'
    a.click()
  }
}
</script>

<style scoped>
.preview-container {
  min-height: 200px;
}

.pdf-preview {
  width: 100%;
}

.pdf-iframe {
  width: 100%;
  height: 75vh;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  display: block;
}

.image-preview {
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 65vh;
  cursor: pointer;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-tip {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.other-file {
  padding: 20px 0;
}
</style>
