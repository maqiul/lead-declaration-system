<template>
  <DeclarationListCore
    :status-filter="[2, 3]"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="true"
    :status-options="[
      { value: 2, label: '待资料提交' },
      { value: 3, label: '待资料审核' }
    ]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
