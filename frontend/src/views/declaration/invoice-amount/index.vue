<template>
  <DeclarationListCore
    :status-filter="[6, 7]"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="true"
    :status-options="[
      { value: 6, label: '待开票金额提交' },
      { value: 7, label: '待开票金额审核' }
    ]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
