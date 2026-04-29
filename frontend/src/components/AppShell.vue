<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img
          v-if="logoVisible"
          class="brand-mark brand-logo"
          :src="logoSrc"
          alt="工程教育评估平台 Logo"
          @error="logoVisible = false"
        />
        <div v-else class="brand-mark">评</div>
        <div>
          <h1>工程教育评估平台</h1>
          <p>基础台账、课程导入、考试与分析一体化</p>
        </div>
      </div>

      <div class="user-card">
        <ShieldCheck :size="18" />
        <div>
          <strong>{{ currentUser.displayName }}</strong>
          <span>{{ currentUser.roleLabel }}</span>
        </div>
      </div>

      <nav class="nav">
        <button
          v-for="item in visibleNavItems"
          :key="item.key"
          :class="['nav-item', { active: activeView === item.key }]"
          :title="item.label"
          @click="activeView = item.key"
        >
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">{{ topbarEyebrow }}</p>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="actions">
          <button class="ghost-button" title="刷新数据" @click="loadAll">
            <RefreshCw :size="17" />
            <span>刷新</span>
          </button>
          <button
            v-if="activeView === 'questions' && canEditQuestion"
            class="ghost-button"
            title="打开题库维护"
            @click="openQuestionManager()"
          >
            <Plus :size="17" />
            <span>题库维护</span>
          </button>
          <button v-if="canView('reports')" class="primary-button" title="生成报告" @click="activeView = 'reports'">
            <FileDown :size="17" />
            <span>报告</span>
          </button>
          <button class="ghost-button" title="退出登录" @click="logout">
            <LogOut :size="17" />
            <span>退出</span>
          </button>
        </div>
      </header>

      <div v-if="pageMessage" class="notice-bar success">{{ pageMessage }}</div>
      <div v-if="pageError" class="notice-bar error">{{ pageError }}</div>

      <slot />
    </main>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { FileDown, LogOut, Plus, RefreshCw, ShieldCheck } from 'lucide-vue-next'

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
