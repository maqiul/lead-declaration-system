<template>
  <div class="not-found">
    <div class="content">
      <a-result
        status="404"
        title="404"
        sub-title="抱歉，您访问的页面不存在。"
      >
        <template #extra>
          <a-button type="primary" @click="goHome">返回首页</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}

.content {
  max-width: 600px;
  width: 100%;
}
</style>