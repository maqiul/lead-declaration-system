<template>
  <DeclarationListCore
    :status-filter="[4, 5]"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="true"
    :status-options="[
      { value: 4, label: '待补充资料提交' },
      { value: 5, label: '待补充资料审核' }
    ]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
