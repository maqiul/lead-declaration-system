<template>
  <DeclarationListCore
    :status-filter="[8, 9]"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="true"
    :status-options="[
      { value: 8, label: '待发票提交' },
      { value: 9, label: '待发票审核' }
    ]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
