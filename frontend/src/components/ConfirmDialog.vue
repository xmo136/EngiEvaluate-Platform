<template>
  <div v-if="confirmDialog.open" class="password-overlay">
    <div class="password-card confirm-card">
      <div class="panel-title">
        <h3>{{ confirmDialog.title }}</h3>
        <span>{{ confirmDialog.subtitle }}</span>
      </div>
      <p class="confirm-copy">{{ confirmDialog.message }}</p>
      <div class="form-action-row">
        <button class="ghost-button full" type="button" @click="closeConfirmDialog">
          <span>取消</span>
        </button>
        <button class="primary-button full danger-solid" type="button" @click="runConfirmDialog">
          <span>{{ confirmDialog.confirmText }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const app = inject('assessmentApp')
const { closeConfirmDialog, confirmDialog, runConfirmDialog } = app
</script>
