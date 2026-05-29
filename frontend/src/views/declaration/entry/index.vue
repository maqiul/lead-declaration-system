<template>
  <DeclarationListCore
    :status-filter="[0, 1, 11]"
    :show-add-button="true"
    :show-export-button="true"
    :show-status-select="true"
    :status-options="[
      { value: 0, label: '草稿' },
      { value: 1, label: '待初审' },
      { value: 11, label: '退回待审' }
    ]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
