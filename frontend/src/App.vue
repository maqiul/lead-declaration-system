<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const themeConfig = {
  token: {
    colorPrimary: '#FA8C16',
    borderRadius: 6,
    fontFamily: "'Source Sans 3', sans-serif"
  }
}
</script>

<style scoped>
#app {
  height: 100%;
}
</style>