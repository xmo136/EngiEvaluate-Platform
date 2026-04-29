<template>
  <div v-if="currentUser?.mustChangePassword" class="password-overlay">
    <div class="password-card">
      <div class="panel-title">
        <h3>首次登录请先修改密码</h3>
        <span>学生账号需要先完成改密，之后才能进入考试页面</span>
      </div>
      <form class="login-form" @submit.prevent="changePassword">
        <label>
          当前密码
          <input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" required />
        </label>
        <label>
          新密码
          <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" required />
        </label>
        <label>
          确认新密码
          <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" required />
        </label>
        <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
        <button class="primary-button full" type="submit">
          <ShieldCheck :size="17" />
          <span>保存新密码</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { ShieldCheck } from 'lucide-vue-next'

const app = inject('assessmentApp')
const { changePassword, currentUser, passwordError, passwordForm } = app
</script>
