<template>
  <GuestView v-if="!currentUser" />
  <AdminWorkspace v-else-if="currentUser.role === 'ADMIN'" />
  <TeacherWorkspace v-else-if="currentUser.role === 'TEACHER'" />
  <StudentWorkspace v-else />
  <PasswordChangeDialog />
  <ConfirmDialog />
  <QuestionManagerDialog />
</template>

<script setup>
import { provide } from 'vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import PasswordChangeDialog from './components/PasswordChangeDialog.vue'
import QuestionManagerDialog from './components/QuestionManagerDialog.vue'
import { useAssessmentApp } from './composables/useAssessmentApp'
import AdminWorkspace from './views/AdminWorkspace.vue'
import GuestView from './views/GuestView.vue'
import StudentWorkspace from './views/StudentWorkspace.vue'
import TeacherWorkspace from './views/TeacherWorkspace.vue'

const app = useAssessmentApp()

provide('assessmentApp', app)

const { currentUser } = app
</script>
