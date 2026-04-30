<template>
  <AppShell>
    <section v-if="activeView === 'exam'" class="view" aria-label="学生考试">
      <!-- Exam List Panel -->
      <div class="panel">
        <div class="panel-title">
          <h3>我的考试</h3>
          <span>支持按考试进入作答，实时查看状态、剩余时间和作答进度。</span>
        </div>

        <div v-if="exams.length" class="exam-board" role="list" aria-label="考试列表">
          <button
            v-for="exam in exams"
            :key="exam.id"
            type="button"
            role="listitem"
            :disabled="!canOpenExam(exam)"
            :aria-label="`${exam.paperName} - ${resolveExamStatusLabel(exam)}`"
            :aria-current="exam.id === selectedExamId ? 'true' : undefined"
            :class="[
              'exam-summary-card',
              'student-exam-card',
              { active: exam.id === selectedExamId, ended: !canOpenExam(exam) }
            ]"
            @click="openExam(exam)"
          >
            <div class="exam-summary-head">
              <strong>{{ exam.paperName }}</strong>
              <span
                class="status-chip"
                :class="resolveExamStatusTone(exam)"
                role="status"
              >
                {{ resolveExamStatusLabel(exam) }}
              </span>
            </div>
            <span>{{ exam.courseName }} / {{ exam.className }}</span>
            <div class="exam-summary-meta">
              <span>{{ formatTime(exam.startTime) || '未设置开始时间' }}</span>
              <span>{{ exam.durationMinutes }} 分钟</span>
              <span>{{ exam.questionCount }} 题 / {{ exam.totalScore }} 分</span>
            </div>
            <div class="exam-summary-footer">
              <strong>{{ examScoreLabel(exam) }}</strong>
              <span>{{ canOpenExam(exam) ? '点击查看' : '已截止，不能查看' }}</span>
            </div>
          </button>
        </div>
        <div v-else class="empty-state" role="status">
          <strong>当前没有可参加的考试</strong>
          <span>老师发布考试后会显示在这里。</span>
        </div>
      </div>

      <!-- Exam Detail Grid -->
      <div class="student-grid">
        <!-- Main Exam Panel -->
        <div class="panel">
          <div class="panel-title">
            <h3>{{ selectedExam?.paperName || '考试内容' }}</h3>
            <span v-if="selectedExam">
              {{ selectedExam.courseName }} / {{ selectedExam.className }}
            </span>
            <span v-else>先在上方查看所有考试和分数，再选择一场进入。</span>
          </div>

          <!-- Exam Overview Metrics -->
          <div v-if="selectedExam" class="exam-overview" role="list" aria-label="考试概览">
            <div class="metric" role="listitem">
              <span>考试状态</span>
              <strong>{{ selectedExamStatusLabel }}</strong>
            </div>
            <div class="metric" role="listitem">
              <span>时间信息</span>
              <strong>{{ selectedExamCountdownLabel }}</strong>
            </div>
            <div class="metric" role="listitem">
              <span>作答进度</span>
              <strong>{{ examAnsweredCount }} / {{ examQuestions.length || 0 }}</strong>
            </div>
            <div class="metric" role="listitem">
              <span>试卷结构</span>
              <strong>{{ selectedExam.questionCount }} 题 / {{ selectedExam.totalScore }} 分</strong>
            </div>
            <div class="metric" role="listitem">
              <span>当前得分</span>
              <strong>{{ selectedExamResult ? `${selectedExamResult.totalScore} 分` : '暂无成绩' }}</strong>
            </div>
          </div>

          <!-- Progress Bar -->
          <div
            v-if="canAnswerSelectedExam"
            class="exam-progress"
            role="progressbar"
            :aria-valuenow="examProgressPercent"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-label="`作答进度 ${examProgressPercent}%`"
          >
            <div class="exam-progress-copy">
              <strong>已完成 {{ examProgressPercent }}%</strong>
              <span v-if="examUnansweredQuestions.length">
                还有 {{ examUnansweredQuestions.length }} 题未作答
              </span>
              <span v-else>所有题目都已填写，可以直接交卷</span>
            </div>
            <div class="exam-progress-track">
              <div
                class="exam-progress-fill"
                :style="{ width: `${examProgressPercent}%` }"
              ></div>
            </div>
          </div>

          <!-- Question Navigator -->
          <div
            v-if="canAnswerSelectedExam && examQuestions.length"
            class="exam-navigator"
            role="navigation"
            aria-label="题目导航"
          >
            <button
              v-for="(question, index) in examQuestions"
              :key="`nav-${question.id}`"
              type="button"
              :class="['exam-nav-button', { answered: answeredExamQuestionIds.includes(question.id) }]"
              :aria-label="`第 ${index + 1} 题${answeredExamQuestionIds.includes(question.id) ? '（已作答）' : ''}`"
              :aria-current="false"
              @click="jumpToExamQuestion(question.id)"
            >
              {{ index + 1 }}
            </button>
          </div>

          <!-- Question List -->
          <div
            v-if="canAnswerSelectedExam && examQuestions.length"
            class="exam-list"
            role="list"
            aria-label="试题列表"
          >
            <article
              v-for="(question, index) in examQuestions"
              :id="`exam-question-${question.id}`"
              :key="question.id"
              class="exam-question"
              role="listitem"
              :aria-label="`第 ${index + 1} 题，${typeLabel(question.type)}，${question.score} 分`"
            >
              <header>
                <span>第 {{ index + 1 }} 题 · {{ typeLabel(question.type) }}</span>
                <strong>{{ question.score }} 分</strong>
              </header>
              <h4>{{ question.title }}</h4>
              <div v-if="question.options?.length" class="option-list" role="radiogroup" :aria-label="`第 ${index + 1} 题选项`">
                <label v-for="option in question.options" :key="option">
                  <input
                    v-model="examAnswers[question.id]"
                    type="radio"
                    :name="`question-${question.id}`"
                    :value="option.slice(0, 1)"
                  />
                  <span>{{ option }}</span>
                </label>
              </div>
              <label v-else :for="`answer-${question.id}`" class="sr-only">
                第 {{ index + 1 }} 题答案
              </label>
              <textarea
                v-if="!question.options?.length"
                :id="`answer-${question.id}`"
                v-model="examAnswers[question.id]"
                rows="4"
                placeholder="请输入答案"
                :aria-label="`第 ${index + 1} 题答案输入`"
              ></textarea>
            </article>
          </div>

          <!-- Empty States -->
          <div v-else-if="selectedExamStatus === 'submitted'" class="empty-state" role="status">
            <strong>该考试已提交</strong>
            <span>{{ selectedExamResult ? `当前成绩：${selectedExamResult.totalScore} 分。` : '系统已收卷，成绩生成后会显示在考试列表里。' }}</span>
          </div>
          <div v-else-if="selectedExamStatus === 'upcoming'" class="empty-state" role="status">
            <strong>考试尚未开始</strong>
            <span>{{ selectedExamCountdownLabel }}</span>
          </div>
          <div v-else-if="selectedExamStatus === 'ended'" class="empty-state" role="status">
            <strong>考试已截止</strong>
            <span>截止后的考试不能再查看或作答，请直接在上方查看其他考试成绩。</span>
          </div>
          <div v-else-if="selectedExam" class="empty-state" role="status">
            <strong>暂无试题</strong>
            <span>当前考试还没有加载到试题内容。</span>
          </div>
          <div v-else class="empty-state" role="status">
            <strong>未选择考试</strong>
            <span>上方会先列出所有考试、状态和分数，点击可查看的考试后才会显示答题界面。</span>
          </div>

          <!-- Submit Button -->
          <Transition name="fade">
            <button
              v-if="canAnswerSelectedExam"
              class="primary-button submit-button"
              type="button"
              :disabled="!canSubmitSelectedExam"
              :title="submitDisabledReason"
              :aria-label="submitDisabledReason || '提交试卷'"
              @click="submitExam"
            >
              <Send :size="17" aria-hidden="true" />
              <span>{{ submitDisabledReason || '提交试卷' }}</span>
            </button>
          </Transition>
        </div>

        <!-- Exam Info Sidebar -->
        <div class="panel report-panel">
          <div class="panel-title">
            <h3>考试信息</h3>
            <span>交卷前可以在这里确认开始时间、时长和未作答题目。</span>
          </div>

          <div v-if="selectedExam" class="objective-list" role="list" aria-label="考试详情">
            <div class="objective-row" role="listitem">
              <span>开始时间</span>
              <strong>{{ formatTime(selectedExamStartAt) || '未设置' }}</strong>
            </div>
            <div class="objective-row" role="listitem">
              <span>结束时间</span>
              <strong>{{ formatTime(selectedExamEndAt) || '未设置' }}</strong>
            </div>
            <div class="objective-row" role="listitem">
              <span>考试时长</span>
              <strong>{{ selectedExam.durationMinutes }} 分钟</strong>
            </div>
            <div class="objective-row" role="listitem">
              <span>考试说明</span>
              <strong>{{ selectedExam.description || '无' }}</strong>
            </div>
            <div class="objective-row" role="listitem">
              <span>考试成绩</span>
              <strong>{{ selectedExamResult ? `${selectedExamResult.totalScore} 分` : '暂无成绩' }}</strong>
            </div>
          </div>

          <!-- Unanswered Questions -->
          <div
            v-if="canAnswerSelectedExam && examUnansweredQuestions.length"
            class="exam-pending-list"
            role="alert"
            aria-label="未作答题目提醒"
          >
            <strong>未作答题目</strong>
            <div class="exam-pending-actions">
              <button
                v-for="(question, index) in examUnansweredQuestions"
                :key="`pending-${question.id}`"
                type="button"
                class="ghost-button compact-button"
                :aria-label="`跳转到第 ${examQuestions.findIndex(item => item.id === question.id) + 1 || index + 1} 题`"
                @click="jumpToExamQuestion(question.id)"
              >
                第 {{ examQuestions.findIndex(item => item.id === question.id) + 1 || index + 1 }} 题
              </button>
            </div>
          </div>

          <div v-else-if="canAnswerSelectedExam && selectedExam" class="empty-state" role="status">
            <strong>当前没有遗漏题目</strong>
            <span>所有题目都已经填写完成。</span>
          </div>
          <div v-else-if="selectedExamStatus === 'ended'" class="empty-state" role="status">
            <strong>该考试已截止</strong>
            <span>已截止的考试只保留状态和成绩展示，不再开放查看和作答。</span>
          </div>
          <div v-else-if="selectedExamStatus === 'submitted'" class="empty-state" role="status">
            <strong>该考试已完成提交</strong>
            <span>{{ selectedExamResult ? `你的成绩是 ${selectedExamResult.totalScore} 分。` : '系统正在整理成绩，请稍后刷新。' }}</span>
          </div>
          <div v-else class="empty-state" role="status">
            <strong>未选择考试</strong>
            <span>点击上方可查看的考试卡片后，这里会显示对应的考试信息。</span>
          </div>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { computed, inject } from 'vue'
import { Send } from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'

const app = inject('assessmentApp')

const {
  activeView,
  answeredExamQuestionIds,
  canSubmitSelectedExam,
  canOpenExam,
  examAnsweredCount,
  examAnswers,
  examProgressPercent,
  examQuestions,
  exams,
  examUnansweredQuestions,
  formatTime,
  jumpToExamQuestion,
  results,
  resolveExamStatusLabel,
  resolveExamStatusTone,
  selectedExam,
  selectedExamCountdownLabel,
  selectedExamEndAt,
  selectedExamId,
  selectedExamResult,
  selectedExamStartAt,
  selectedExamStatus,
  selectedExamStatusLabel,
  submitDisabledReason,
  submitExam,
  typeLabel
} = app

const examResultMap = computed(() => new Map(results.value.map(result => [result.examId, result])))

const canAnswerSelectedExam = computed(() =>
  Boolean(selectedExam.value && ['ongoing', 'available'].includes(selectedExamStatus.value))
)

function openExam(exam) {
  if (!canOpenExam(exam)) {
    return
  }
  selectedExamId.value = exam.id
}

function examScoreLabel(exam) {
  const result = examResultMap.value.get(exam.id)
  if (result) {
    return `成绩 ${result.totalScore} 分`
  }
  if (exam.submitted) {
    return '已交卷，待查看成绩'
  }
  return '暂无成绩'
}
</script>

<style scoped>
/* Fade transition for submit button */
.fade-enter-active {
  transition: opacity 300ms ease-out;
}

.fade-leave-active {
  transition: opacity 200ms ease-in;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Exam card stagger animation */
.exam-summary-card {
  animation: fadeInUp 400ms ease-out both;
}

.exam-summary-card:nth-child(1) { animation-delay: 50ms; }
.exam-summary-card:nth-child(2) { animation-delay: 100ms; }
.exam-summary-card:nth-child(3) { animation-delay: 150ms; }
.exam-summary-card:nth-child(4) { animation-delay: 200ms; }
.exam-summary-card:nth-child(5) { animation-delay: 250ms; }
.exam-summary-card:nth-child(6) { animation-delay: 300ms; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Question navigator focus styles */
.exam-nav-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Option list hover feedback */
.option-list label {
  transition: all var(--transition-fast);
}

.option-list label:has(input:checked) {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .exam-summary-card {
    animation: none;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
