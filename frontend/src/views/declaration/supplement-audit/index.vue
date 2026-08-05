<template>
  <!-- 补交审核列表：与申报列表页同款布局，仅展示存在待审补交单（status=0）的申报单；
       declarationType 由路由前缀（/declaration-self 或 /declaration-external）自动推断 -->
  <DeclarationListCore
    :only-pending-supplement="true"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="true"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
