<template>
  <div class="login-screen">
    <section class="landing-shell" aria-label="欢迎页面">
      <div class="landing-hero">
        <img
          v-if="heroImageVisible"
          class="landing-hero-image"
          :src="heroImageSrc"
          alt=""
          role="presentation"
          @error="heroImageVisible = false"
        />
        <div class="landing-hero-scrim" aria-hidden="true"></div>
        <div class="landing-hero-grid">
          <div class="landing-copy">
            <div class="landing-badge" aria-label="平台定位">
              <Sparkles :size="14" aria-hidden="true" />
              <span>课程目标达成与教学质量协同平台</span>
            </div>
            <div class="landing-copy-main">
              <h1>工程教育<span>评估平台</span></h1>
              <p>
                管理员维护基础信息与教学安排，老师完成课程导入、在线考试和成绩确认，
                学生首次改密后进入考试流程，整套数据链路都沉淀在同一平台中。
              </p>
            </div>

            <div class="landing-action-row">
              <button
                class="primary-button landing-cta"
                type="button"
                @click="focusLogin"
              >
                <LogIn :size="17" aria-hidden="true" />
                <span>进入平台</span>
              </button>
              <button
                class="ghost-button landing-ghost"
                type="button"
                @click="scrollToHighlights"
              >
                <ArrowRight :size="17" aria-hidden="true" />
                <span>查看能力</span>
              </button>
            </div>

            <div class="landing-metric-grid" role="list" aria-label="平台数据">
              <div
                v-for="item in landingMetrics"
                :key="item.label"
                class="landing-metric"
                role="listitem"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>

            <div class="landing-story">
              <div class="landing-story-header">
                <div>
                  <span class="landing-story-kicker">核心闭环</span>
                  <h2>从台账维护，到课程导入，再到考试与分析归档</h2>
                </div>
                <div class="landing-story-seal">
                  <BadgeCheck :size="18" aria-hidden="true" />
                  <span>可交付流程</span>
                </div>
              </div>
              <div class="landing-flow" role="list" aria-label="核心流程">
                <div
                  v-for="item in landingFlow"
                  :key="item.title"
                  class="landing-flow-item"
                  role="listitem"
                >
                  <component :is="item.icon" :size="18" aria-hidden="true" />
                  <div>
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.text }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Login Panel -->
          <aside class="login-panel" aria-label="登录">
            <div class="login-brand">
              <img
                v-if="logoVisible"
                class="brand-mark brand-logo"
                :src="logoSrc"
                alt="工程教育评估平台 Logo"
                @error="logoVisible = false"
              />
              <div v-else class="brand-mark" aria-hidden="true">评</div>
              <div>
                <h2>登录工作台</h2>
                <p>管理员、老师、学生进入各自工作界面</p>
              </div>
            </div>

            <form class="login-form" @submit.prevent="login" novalidate>
              <label for="login-username">
                用户名
                <input
                  id="login-username"
                  ref="loginUsernameRef"
                  v-model="loginForm.username"
                  autocomplete="username"
                  required
                  aria-required="true"
                  :aria-invalid="!!loginError"
                  aria-describedby="login-error"
                />
              </label>
              <label for="login-password">
                密码
                <input
                  id="login-password"
                  v-model="loginForm.password"
                  autocomplete="current-password"
                  type="password"
                  required
                  aria-required="true"
                  :aria-invalid="!!loginError"
                  aria-describedby="login-error"
                />
              </label>
              <p
                v-if="loginError"
                id="login-error"
                class="error-text"
                role="alert"
              >
                {{ loginError }}
              </p>
              <button class="primary-button full" type="submit">
                <LogIn :size="17" aria-hidden="true" />
                <span>登录系统</span>
              </button>
            </form>

            <div class="login-divider" role="separator"></div>

            <div class="login-preview">
              <div class="login-preview-copy">
                <span>当前流程重点</span>
                <strong>管理员维护学生基础信息，老师按课程名单导入这些学生并开展考试</strong>
              </div>
            </div>

            <div class="access-role-list" role="list" aria-label="角色说明">
              <article
                v-for="role in accessRoles"
                :key="role.title"
                class="access-role"
                role="listitem"
              >
                <component :is="role.icon" :size="16" aria-hidden="true" />
                <div>
                  <strong>{{ role.title }}</strong>
                  <p>{{ role.text }}</p>
                </div>
              </article>
            </div>

            <p class="login-note">
              学生账号默认使用学号，初始密码为 123456，首次登录必须修改密码。
            </p>
          </aside>
        </div>
      </div>

      <div
        v-if="landingHighlights.length"
        ref="landingHighlightsRef"
        class="landing-band"
        role="list"
        aria-label="平台亮点"
      >
        <article
          v-for="item in landingHighlights"
          :key="item.title"
          class="landing-feature"
          role="listitem"
        >
          <component :is="item.icon" :size="18" aria-hidden="true" />
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.text }}</p>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { ArrowRight, BadgeCheck, LogIn, Sparkles } from 'lucide-vue-next'

const app = inject('assessmentApp')

const {
  accessRoles,
  focusLogin,
  heroImageSrc,
  heroImageVisible,
  landingFlow,
  landingHighlights,
  landingHighlightsRef,
  landingMetrics,
  login,
  loginError,
  loginForm,
  loginUsernameRef,
  logoSrc,
  logoVisible,
  scrollToHighlights
} = app
</script>

<style scoped>
/* Staggered entrance animation for landing elements */
.landing-badge {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 100ms;
}

.landing-copy-main {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 200ms;
}

.landing-action-row {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 300ms;
}

.landing-metric-grid {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 400ms;
}

.landing-story {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 500ms;
}

.login-panel {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 300ms;
}

.landing-band {
  animation: fadeInUp 600ms ease-out both;
  animation-delay: 600ms;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Login form focus styles */
.login-form input:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .landing-badge,
  .landing-copy-main,
  .landing-action-row,
  .landing-metric-grid,
  .landing-story,
  .login-panel,
  .landing-band {
    animation: none;
  }
}
</style>
