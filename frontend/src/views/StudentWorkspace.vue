<template>
  <AppShell>
    <section v-if="activeView === 'exam'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>我的考试</h3>
          <span>选择考试后即可在线答题，客观题会自动判分。</span>
        </div>
        <div v-if="exams.length" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>考试名称</th>
                <th>课程 / 班级</th>
                <th>开始时间</th>
                <th>时长</th>
                <th>题量</th>
                <th>总分</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="exam in exams"
                :key="exam.id"
                :class="{ active: exam.id === selectedExamId }"
                @click="selectedExamId = exam.id"
              >
                <td><strong>{{ exam.paperName }}</strong></td>
                <td>{{ exam.courseName }} / {{ exam.className }}</td>
                <td>{{ formatTime(exam.startTime) || '未设置' }}</td>
                <td>{{ exam.durationMinutes }} 分钟</td>
                <td>{{ exam.questionCount }}</td>
                <td>{{ exam.totalScore }}</td>
                <td>{{ exam.submitted ? '已提交' : '待作答' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <strong>当前没有可参加的考试</strong>
          <span>老师发布考试后会显示在这里。</span>
        </div>
      </div>

      <div class="student-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>{{ selectedExam?.paperName || '考试内容' }}</h3>
            <span v-if="selectedExam">
              {{ selectedExam.courseName }} / {{ selectedExam.className }} / {{ selectedExam.durationMinutes }} 分钟
            </span>
            <span v-else>先从上面的考试列表中选择一场考试。</span>
          </div>

          <div v-if="selectedExam" class="exam-meta">
            <label>
              开始时间
              <input :value="formatTime(selectedExam.startTime) || '未设置'" readonly />
            </label>
            <label>
              考试时长
              <input :value="`${selectedExam.durationMinutes} 分钟`" readonly />
            </label>
            <label>
              试卷信息
              <input :value="`${selectedExam.questionCount} 题 / ${selectedExam.totalScore} 分`" readonly />
            </label>
          </div>

          <div v-if="examQuestions.length" class="exam-list">
            <article v-for="question in examQuestions" :key="question.id" class="exam-question">
              <header>
                <span>{{ typeLabel(question.type) }}</span>
                <strong>{{ question.score }} 分</strong>
              </header>
              <h4>{{ question.title }}</h4>
              <div v-if="question.options?.length" class="option-list">
                <label v-for="option in question.options" :key="option">
                  <input v-model="examAnswers[question.id]" type="radio" :value="option.slice(0, 1)" />
                  <span>{{ option }}</span>
                </label>
              </div>
              <textarea v-else v-model="examAnswers[question.id]" rows="4" placeholder="请输入答案"></textarea>
            </article>
          </div>
          <div v-else class="empty-state">
            <strong>暂无试题</strong>
            <span>选择一场考试后，这里会显示题目。</span>
          </div>

          <button
            class="primary-button submit-button"
            type="button"
            :disabled="!selectedExam || selectedExam.submitted || !examQuestions.length"
            @click="submitExam"
          >
            <Send :size="17" />
            <span>{{ selectedExam?.submitted ? '该考试已提交' : '提交试卷' }}</span>
          </button>
        </div>

        <div class="panel report-panel">
          <div class="panel-title">
            <h3>考试说明</h3>
            <span>提交后会进入自动判分流程，老师可继续人工调整主观题得分。</span>
          </div>
          <div v-if="selectedExam" class="objective-list">
            <div class="objective-row">
              <span>考试状态</span>
              <strong>{{ selectedExam.submitted ? '已提交' : '未提交' }}</strong>
            </div>
            <div class="objective-row">
              <span>交卷人数</span>
              <strong>{{ selectedExam.submittedCount }}</strong>
            </div>
            <div class="objective-row">
              <span>考试说明</span>
              <strong>{{ selectedExam.description || '无' }}</strong>
            </div>
          </div>
          <div v-else class="empty-state">
            <strong>未选择考试</strong>
            <span>点击考试列表中的任意一行即可开始作答。</span>
          </div>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { inject } from 'vue'
import { Send } from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'

const app = inject('assessmentApp')

const {
  activeView,
  examAnswers,
  examQuestions,
  exams,
  formatTime,
  selectedExam,
  selectedExamId,
  submitExam,
  typeLabel
} = app
</script>
