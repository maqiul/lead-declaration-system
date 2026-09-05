<template>
  <a-modal
    v-model:open="visible"
    :title="previewTitle"
    :width="isPdf || isOffice ? '90%' : '700px'"
    :footer="null"
    :destroy-on-close="true"
    class="file-preview-modal"
  >
    <!-- 工具栏 -->
    <div v-if="url" class="preview-toolbar">
      <a-button size="small" @click="downloadFile">
        <template #icon><DownloadOutlined /></template>
        下载文件
      </a-button>
    </div>
    <div v-if="url" class="preview-container">
      <!-- PDF 预览 -->
      <div v-if="isPdf" class="pdf-preview">
        <iframe :src="pdfSrc" frameborder="0" class="pdf-iframe"></iframe>
      </div>

      <!-- Office 文件预览（Word/Excel） -->
      <div v-else-if="isOffice" class="office-preview">
        <a-spin :spinning="officeLoading" tip="文件加载中...">
          <div ref="officeContainer" class="office-container"></div>
          <a-alert v-if="officeError" :message="officeError" type="error" show-icon />
        </a-spin>
      </div>

      <!-- 图片预览 -->
      <div v-else-if="isImage" class="image-preview">
        <img :src="authedUrl" class="preview-image" @click="openFullImage" />
        <div class="image-tip">点击图片可在新窗口查看原图</div>
      </div>

      <!-- 其他文件 -->
      <div v-else class="other-file">
        <a-result status="info" title="该文件类型不支持在线预览，请点击上方下载按钮" />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { withAuthToken } from '@/utils/auth'

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
const isOffice = computed(() => /\.(docx?|xlsx?)(\?.*)?$/i.test(props.url || ''))
const isWord = computed(() => /\.docx(\?.*)?$/i.test(props.url || ''))
const isExcel = computed(() => /\.xlsx?(\?.*)?$/i.test(props.url || ''))

// 预览标题
const previewTitle = computed(() => {
  if (props.title) return props.title
  if (isPdf.value) return 'PDF 文件预览'
  if (isImage.value) return '图片预览'
  if (isWord.value) return 'Word 文件预览'
  if (isExcel.value) return 'Excel 文件预览'
  return '文件预览'
})

// 带认证参数的URL：预览/下载走 iframe 与新窗口，带不了请求头，只能把凭证拼在地址上
const authedUrl = computed(() => {
  if (!props.url) return ''
  return withAuthToken(props.url)
})

// PDF iframe src
const pdfSrc = authedUrl

// Office 预览状态
const officeLoading = ref(false)
const officeError = ref('')
const officeContainer = ref<HTMLElement | null>(null)

// 监听 visible 和 url 变化，自动加载 Office 文件
watch([() => props.visible, () => props.url], async ([isVisible]) => {
  if (isVisible && isOffice.value && props.url) {
    await nextTick()
    loadOfficeFile()
  }
})

// 加载 Office 文件
const loadOfficeFile = async () => {
  officeLoading.value = true
  officeError.value = ''

  try {
    const response = await fetch(authedUrl.value)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const arrayBuffer = await response.arrayBuffer()

    if (isWord.value) {
      await renderWord(arrayBuffer)
    } else if (isExcel.value) {
      await renderExcel(arrayBuffer)
    }
  } catch (e: any) {
    officeError.value = `文件加载失败: ${e.message || '未知错误'}`
  } finally {
    officeLoading.value = false
  }
}

// 渲染 Word (.docx)
const renderWord = async (buffer: ArrayBuffer) => {
  const container = officeContainer.value
  if (!container) return

  try {
    const docx = await import('docx-preview')
    await docx.renderAsync(buffer, container, undefined, {
      className: 'docx-wrapper',
      inWrapper: true,
      ignoreWidth: false,
      ignoreHeight: false,
      ignoreFonts: false,
      breakPages: true,
      ignoreLastRenderedPageBreak: true,
      experimental: false,
    })
  } catch (e: any) {
    officeError.value = `Word 渲染失败: ${e.message || '未知错误'}`
  }
}

// 渲染 Excel (.xlsx/.xls)
const renderExcel = async (buffer: ArrayBuffer) => {
  const container = officeContainer.value
  if (!container) return

  try {
    const XLSX = await import('xlsx')
    const workbook = XLSX.read(buffer, { type: 'array' })

    let html = ''
    for (const sheetName of workbook.SheetNames) {
      const worksheet = workbook.Sheets[sheetName]
      html += `<div class="sheet-title">${sheetName}</div>`
      html += XLSX.utils.sheet_to_html(worksheet, { editable: false })
    }

    container.innerHTML = html
  } catch (e: any) {
    officeError.value = `Excel 渲染失败: ${e.message || '未知错误'}`
  }
}

// 在新窗口打开原图
const openFullImage = () => {
  if (authedUrl.value) window.open(authedUrl.value, '_blank')
}

// 下载文件
const downloadFile = () => {
  if (!authedUrl.value) return
  const a = document.createElement('a')
  a.href = authedUrl.value
  // 从 URL 中提取文件名作为下载文件名
  const urlPath = authedUrl.value.split('?')[0]
  const fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1) || 'download'
  a.download = decodeURIComponent(fileName)
  a.click()
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

.office-preview {
  width: 100%;
}

.office-container {
  min-height: 200px;
  max-height: 75vh;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 16px;
}

/* docx-preview 渲染样式覆盖 */
.office-container :deep(.docx-wrapper) {
  background: white;
  padding: 0;
}

.office-container :deep(section.docx) {
  box-shadow: none;
  padding: 20px;
  margin: 0;
}

/* Excel 渲染样式 */
.office-container :deep(.sheet-title) {
  font-size: 16px;
  font-weight: bold;
  margin: 16px 0 8px;
  color: #333;
}

.office-container :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}

.office-container :deep(td),
.office-container :deep(th) {
  border: 1px solid #ccc;
  padding: 6px 10px;
  text-align: left;
  font-size: 13px;
}

.office-container :deep(th) {
  background: #f5f5f5;
  font-weight: bold;
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

.preview-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.other-file {
  padding: 20px 0;
}
</style>
