<template>
  <div class="app-shell">
    <!-- Skip link for keyboard accessibility -->
    <a href="#main-content" class="skip-link">跳转到主要内容</a>

    <!-- Sidebar Navigation -->
    <aside class="sidebar" role="navigation" aria-label="主导航">
      <div class="brand">
        <img
          v-if="logoVisible"
          class="brand-mark brand-logo"
          :src="logoSrc"
          alt="工程教育评估平台 Logo"
          @error="logoVisible = false"
        />
        <div v-else class="brand-mark" aria-hidden="true">评</div>
        <div>
          <h1>工程教育评估平台</h1>
          <p>基础台账、课程导入、考试与分析一体化</p>
        </div>
      </div>

      <div class="user-card" role="status" aria-label="当前用户信息">
        <ShieldCheck :size="18" aria-hidden="true" />
        <div>
          <strong>{{ currentUser.displayName }}</strong>
          <span>{{ currentUser.roleLabel }}</span>
        </div>
      </div>

      <nav class="nav" aria-label="功能菜单">
        <button
          v-for="item in visibleNavItems"
          :key="item.key"
          :class="['nav-item', { active: activeView === item.key }]"
          :title="item.label"
          :aria-current="activeView === item.key ? 'page' : undefined"
          @click="activeView = item.key"
        >
          <component :is="item.icon" :size="18" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <!-- Main Content Area -->
    <main id="main-content" class="workspace" role="main" tabindex="-1">
      <header class="topbar">
        <div>
          <p class="eyebrow">{{ topbarEyebrow }}</p>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="actions" role="toolbar" aria-label="页面操作">
          <button
            class="ghost-button"
            title="刷新数据"
            aria-label="刷新数据"
            @click="loadAll"
          >
            <RefreshCw :size="17" aria-hidden="true" />
            <span>刷新</span>
          </button>
          <button
            v-if="activeView === 'questions' && canEditQuestion"
            class="ghost-button"
            title="打开题库维护"
            aria-label="打开题库维护"
            @click="openQuestionManager()"
          >
            <Plus :size="17" aria-hidden="true" />
            <span>题库维护</span>
          </button>
          <button
            v-if="canView('reports')"
            class="primary-button"
            title="生成报告"
            aria-label="生成报告"
            @click="activeView = 'reports'"
          >
            <FileDown :size="17" aria-hidden="true" />
            <span>报告</span>
          </button>
          <button
            class="ghost-button"
            title="退出登录"
            aria-label="退出登录"
            @click="logout"
          >
            <LogOut :size="17" aria-hidden="true" />
            <span>退出</span>
          </button>
        </div>
      </header>

      <!-- Toast Notifications -->
      <Transition name="toast">
        <div
          v-if="pageMessage"
          class="toast success"
          role="status"
          aria-live="polite"
        >
          <CheckCircle :size="18" aria-hidden="true" />
          <span>{{ pageMessage }}</span>
        </div>
      </Transition>
      <Transition name="toast">
        <div
          v-if="pageError"
          class="toast error"
          role="alert"
          aria-live="assertive"
        >
          <AlertCircle :size="18" aria-hidden="true" />
          <span>{{ pageError }}</span>
        </div>
      </Transition>

      <slot />
    </main>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { AlertCircle, CheckCircle, FileDown, LogOut, Plus, RefreshCw, ShieldCheck } from 'lucide-vue-next'

const app = inject('assessmentApp')

const {
  activeView,
  canEditQuestion,
  canView,
  currentTitle,
  currentUser,
  loadAll,
  logoSrc,
  logoVisible,
  pageError,
  pageMessage,
  openQuestionManager,
  topbarEyebrow,
  visibleNavItems,
  logout
} = app
</script>

<style scoped>
/* Toast notification styles */
.toast {
  position: fixed;
  top: var(--space-4);
  right: var(--space-4);
  z-index: var(--z-toast);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  box-shadow: var(--shadow-lg);
  max-width: 400px;
  pointer-events: auto;
}

.toast.success {
  background: var(--color-success);
  color: #fff;
}

.toast.error {
  background: var(--color-danger);
  color: #fff;
}

/* Toast transition animation */
.toast-enter-active {
  animation: toastIn 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  animation: toastOut 200ms ease-in forwards;
}

@keyframes toastIn {
  from {
    opacity: 0;
    transform: translateX(100%) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes toastOut {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(100%) scale(0.95);
  }
}

/* Sidebar navigation keyboard focus styles */
.nav-item:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: -2px;
  border-radius: var(--radius-md);
}

/* Main content focus reset */
.workspace:focus {
  outline: none;
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active {
    animation: none;
  }
}
</style>
