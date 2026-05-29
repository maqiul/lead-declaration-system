<template>
  <DeclarationListCore
    :status-filter="[10]"
    :show-add-button="false"
    :show-export-button="true"
    :show-status-select="false"
    :status-options="[]"
  />
</template>

<script setup lang="ts">
import DeclarationListCore from '../shared/DeclarationListCore.vue'
</script>
