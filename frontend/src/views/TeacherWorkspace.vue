<template>
  <AppShell>
    <section v-if="activeView === 'dashboard'" class="view">
      <div class="kpi-grid">
        <div class="metric">
          <span>学生人数</span>
          <strong>{{ analysis?.studentCount ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>题库题量</span>
          <strong>{{ analysis?.questionCount ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>平均分</span>
          <strong>{{ analysis?.averageScore ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>提交记录</span>
          <strong>{{ results.length }}</strong>
        </div>
      </div>

      <div class="chart-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>成绩分布</h3>
            <span>按分数段统计</span>
          </div>
          <div ref="scoreChartRef" class="chart"></div>
        </div>
        <div class="panel">
          <div class="panel-title">
            <h3>题型分布</h3>
            <span>题库结构概览</span>
          </div>
          <div ref="typeChartRef" class="chart"></div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <h3>课程目标达成</h3>
          <span>按课程目标统计</span>
        </div>
        <div class="objective-list">
          <div v-for="(value, name) in analysis?.objectiveAverage" :key="name" class="objective-row">
            <span>{{ name }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: `${Math.min(value, 100)}%` }"></div>
            </div>
            <strong>{{ value }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'students'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>课程学生管理</h3>
          <span>按教学安排查看、补充和移出当前课程学生。</span>
        </div>
        <div class="assignment-summary">
          <label>
            教学安排
            <select v-model.number="selectedTeachingAssignmentId">
              <option v-for="item in teachingAssignments" :key="item.id" :value="item.id">
                {{ item.courseName }} / {{ item.className }} / {{ item.teacherName }}
              </option>
            </select>
          </label>
          <div v-if="selectedTeachingAssignment" class="status-pill">
            <strong>{{ selectedTeachingAssignment.courseName }}</strong>
            <span>{{ selectedTeachingAssignment.className }} / {{ selectedTeachingAssignment.teacherName }}</span>
          </div>
        </div>
      </div>

      <div class="student-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>当前课程学生</h3>
            <span>移出课程不会删除学生基础信息和历史成绩。</span>
          </div>
          <div v-if="filteredStudents.length" class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>班级</th>
                  <th>账号</th>
                  <th>密码状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="student in filteredStudents" :key="student.id">
                  <td>{{ student.studentNo }}</td>
                  <td>{{ student.name }}</td>
                  <td>{{ student.className }}</td>
                  <td>{{ student.username || student.studentNo }}</td>
                  <td>{{ student.passwordChangeRequired ? '待修改初始密码' : '已完成修改' }}</td>
                  <td class="row-actions">
                    <button class="ghost-button compact-button danger-button" type="button" @click="confirmRemoveStudentFromSelectedTeachingAssignment(student)">
                      <Trash2 :size="15" />
                      <span>移出课程</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <strong>当前课程还没有学生</strong>
            <span>可以从右侧候选列表里补充学生。</span>
          </div>
        </div>

        <div class="panel report-panel">
          <div class="panel-title">
            <h3>添加学生到当前课程</h3>
            <span>从全校学生基础库中选择学生加入当前教学安排。</span>
          </div>
          <div class="import-box">
            <input v-model="candidateKeyword" type="text" placeholder="搜索学号、姓名或班级" />
          </div>
          <div v-if="filteredCandidateStudents.length" class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>专业班级</th>
                  <th>当前课程</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="student in filteredCandidateStudents" :key="`candidate-${student.id}`">
                  <td>{{ student.studentNo }}</td>
                  <td>{{ student.name }}</td>
                  <td>{{ student.className }}</td>
                  <td>{{ studentTeachingAssignmentSummary(student) }}</td>
                  <td class="row-actions">
                    <button class="primary-button compact-button" type="button" @click="addStudentToSelectedTeachingAssignment(student)">
                      <Plus :size="15" />
                      <span>加入课程</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <strong>没有可添加的学生</strong>
            <span>当前搜索条件下没有候选学生。</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'questions'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>题库</h3>
          <span>支持题型筛选、课程目标筛选、编辑和批量删除。</span>
        </div>
        <div class="section-toolbar question-toolbar">
          <div class="toolbar-meta">
            <strong>{{ hasQuestionFilters ? `筛选结果 ${filteredQuestions.length} 题` : `共 ${questions.length} 题` }}</strong>
            <span>{{ selectedFilteredQuestionCount }} / {{ filteredQuestions.length }} 已选</span>
          </div>
          <div class="question-toolbar-actions">
            <label class="question-filter-field">
              <span>题型</span>
              <select v-model="questionTypeFilter">
                <option value="ALL">全部题型</option>
                <option v-for="item in questionTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="question-filter-field">
              <span>课程目标</span>
              <select v-model="questionObjectiveFilter">
                <option value="ALL">全部目标</option>
                <option v-for="item in objectives" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <button v-if="hasQuestionFilters" class="ghost-button compact-button" type="button" @click="clearQuestionFilters">
              <span>清空筛选</span>
            </button>
            <label v-if="filteredQuestions.length" class="question-select-all">
              <input v-model="allFilteredQuestionsSelected" type="checkbox" />
              <span>全选当前</span>
            </label>
            <button
              class="ghost-button compact-button danger-button"
              type="button"
              :disabled="!selectedFilteredQuestionCount"
              @click="confirmDeleteSelectedQuestions"
            >
              <Trash2 :size="15" />
              <span>批量删除</span>
            </button>
          </div>
        </div>

        <div v-if="filteredQuestions.length" class="question-list">
          <article v-for="question in filteredQuestions" :key="question.id" class="question-card">
            <div class="question-card-head">
              <div class="question-card-meta">
                <label class="question-checkbox">
                  <input
                    :checked="selectedQuestionIds.includes(question.id)"
                    type="checkbox"
                    @change="toggleQuestionSelection(question.id)"
                  />
                </label>
                <div>
                  <span class="tag">{{ typeLabel(question.type) }}</span>
                  <span class="tag muted">{{ objectiveLabel(question.objective) }}</span>
                </div>
              </div>
              <div class="question-card-actions">
                <button class="ghost-button compact-button" type="button" @click="startEditQuestion(question)">
                  <Pencil :size="15" />
                  <span>编辑</span>
                </button>
                <button class="ghost-button compact-button danger-button" type="button" @click="confirmDeleteQuestion(question)">
                  <Trash2 :size="15" />
                  <span>删除</span>
                </button>
              </div>
            </div>
            <h4>{{ question.title }}</h4>
            <p v-if="question.options?.length">{{ question.options.join(' / ') }}</p>
            <footer>
              <span>{{ question.score }} 分</span>
              <span>{{ question.analysis }}</span>
            </footer>
          </article>
        </div>
        <div v-else class="empty-state">
          <strong>当前筛选条件下没有题目</strong>
          <span>调整筛选条件后再看看。</span>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'exam'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>考试管理</h3>
          <span>为课程创建考试，设置开考时间与时长，并从题库选题组卷。</span>
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
                <th>已交卷</th>
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
                <td>{{ exam.submittedCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <strong>还没有已创建的考试</strong>
          <span>先在下面填写考试信息并选择题目组卷。</span>
        </div>
      </div>

      <div class="student-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>创建考试</h3>
            <span>考试创建后会复制题库题目，后续修改题库不会影响已发布考试。</span>
          </div>
          <div class="form-grid">
            <label>
              课程班级
              <select v-model.number="examForm.teachingAssignmentId">
                <option v-for="item in teachingAssignments" :key="item.id" :value="item.id">
                  {{ item.courseName }} / {{ item.className }}
                </option>
              </select>
            </label>
            <label>
              考试名称
              <input v-model="examForm.paperName" type="text" placeholder="例如：期中考试 A 卷" />
            </label>
            <label>
              开始时间
              <input v-model="examForm.startTime" type="datetime-local" />
            </label>
            <label>
              考试时长（分钟）
              <input v-model.number="examForm.durationMinutes" type="number" min="1" />
            </label>
          </div>
          <label>
            说明
            <textarea v-model="examForm.description" rows="3" placeholder="可选：填写考试说明"></textarea>
          </label>

          <div class="section-toolbar question-toolbar">
            <div class="toolbar-meta">
              <strong>已选 {{ selectedExamQuestionCount }} 题 / {{ selectedExamTotalScore }} 分</strong>
              <span>从当前题库中勾选试题进行组卷</span>
            </div>
            <div class="question-toolbar-actions">
              <label class="question-filter-field">
                <span>题型</span>
                <select v-model="questionTypeFilter">
                  <option value="ALL">全部题型</option>
                  <option v-for="item in questionTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label class="question-filter-field">
                <span>课程目标</span>
                <select v-model="questionObjectiveFilter">
                  <option value="ALL">全部目标</option>
                  <option v-for="item in objectives" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label v-if="filteredQuestions.length" class="question-select-all">
                <input v-model="allFilteredExamQuestionsSelected" type="checkbox" />
                <span>全选当前</span>
              </label>
            </div>
          </div>

          <div v-if="filteredQuestions.length" class="question-list">
            <article v-for="question in filteredQuestions" :key="`exam-bank-${question.id}`" class="question-card">
              <div class="question-card-head">
                <div class="question-card-meta">
                  <label class="question-checkbox">
                    <input
                      :checked="selectedExamQuestionIds.includes(question.id)"
                      type="checkbox"
                      @change="toggleExamQuestionSelection(question.id)"
                    />
                  </label>
                  <div>
                    <span class="tag">{{ typeLabel(question.type) }}</span>
                    <span class="tag muted">{{ objectiveLabel(question.objective) }}</span>
                  </div>
                </div>
                <strong>{{ question.score }} 分</strong>
              </div>
              <h4>{{ question.title }}</h4>
              <p v-if="question.options?.length">{{ question.options.join(' / ') }}</p>
            </article>
          </div>
          <div v-else class="empty-state">
            <strong>没有可选题目</strong>
            <span>请调整筛选条件或先去题库新增题目。</span>
          </div>

          <button class="primary-button submit-button" type="button" :disabled="!selectedExamQuestionCount" @click="createExam">
            <Plus :size="17" />
            <span>创建考试</span>
          </button>
        </div>

        <div class="panel report-panel">
          <div class="panel-title">
            <h3>当前考试预览</h3>
            <span>查看已选考试的题目结构和开考信息。</span>
          </div>
          <div v-if="selectedExam" class="objective-list">
            <div class="objective-row">
              <span>考试名称</span>
              <strong>{{ selectedExam.paperName }}</strong>
            </div>
            <div class="objective-row">
              <span>课程班级</span>
              <strong>{{ selectedExam.courseName }} / {{ selectedExam.className }}</strong>
            </div>
            <div class="objective-row">
              <span>开始时间</span>
              <strong>{{ formatTime(selectedExam.startTime) || '未设置' }}</strong>
            </div>
            <div class="objective-row">
              <span>时长 / 总分</span>
              <strong>{{ selectedExam.durationMinutes }} 分钟 / {{ selectedExam.totalScore }} 分</strong>
            </div>
          </div>
          <div v-if="examQuestions.length" class="answer-list">
            <div v-for="question in examQuestions" :key="`preview-${question.id}`" class="answer-row">
              <div>
                <strong>{{ typeLabel(question.type) }} · {{ question.title }}</strong>
                <p>{{ objectiveLabel(question.objective) }}</p>
              </div>
              <strong>{{ question.score }} 分</strong>
            </div>
          </div>
          <div v-else class="empty-state">
            <strong>还没有选中考试</strong>
            <span>点击上方考试列表，可以在这里预览试卷。</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'results'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>学生答题与评分</h3>
          <span>按考试查看学生提交，支持教师修改每题得分。</span>
        </div>
        <div class="assignment-summary">
          <label>
            选择考试
            <select v-model.number="selectedResultExamId">
              <option v-for="exam in exams" :key="`result-exam-${exam.id}`" :value="exam.id">
                {{ exam.paperName }} / {{ exam.className }}
              </option>
            </select>
          </label>
          <div class="status-pill">
            <strong>{{ filteredResults.length }}</strong>
            <span>份答卷</span>
          </div>
        </div>
        <div v-if="filteredResults.length" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>考试</th>
                <th>学生</th>
                <th>班级</th>
                <th>总分</th>
                <th>提交时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="result in filteredResults" :key="result.id">
                <td>{{ result.paperName }}</td>
                <td>{{ result.student.studentNo }} / {{ result.student.name }}</td>
                <td>{{ result.className }}</td>
                <td><strong>{{ result.totalScore }}</strong></td>
                <td>{{ formatTime(result.submittedAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <strong>当前没有答卷</strong>
          <span>学生提交后会在这里出现。</span>
        </div>
      </div>

      <div class="result-grid">
        <article v-for="result in filteredResults" :key="`detail-${result.id}`" class="panel result-card">
          <div class="panel-title">
            <h3>{{ result.student.name }} / {{ result.totalScore }} 分</h3>
            <span>{{ result.paperName }} · {{ result.className }}</span>
          </div>
          <div class="answer-list">
            <div v-for="answer in result.answers" :key="`${result.id}-${answer.questionId}`" class="answer-row">
              <div>
                <strong>{{ answer.questionTitle }}</strong>
                <p>学生答案：{{ answer.studentAnswer || '未作答' }}</p>
                <p>{{ answer.suggestion }}</p>
              </div>
              <label>
                最终分
                <input v-model.number="answer.score" min="0" :max="answer.maxScore" type="number" />
              </label>
              <button class="ghost-button" type="button" @click="confirmScore(result.id, answer.questionId, answer.score)">
                <Check :size="17" />
                <span>确认</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="activeView === 'reports'" class="view">
      <div class="report-layout">
        <div class="panel report-panel">
          <FileSpreadsheet :size="32" />
          <h3>成绩分析 Excel</h3>
          <p>导出学生成绩、课程目标达成和题型分布统计。</p>
          <button class="primary-button" @click="download('/api/reports/score-analysis.xls')">
            <FileDown :size="17" />
            <span>导出 .xls</span>
          </button>
        </div>
        <div class="panel report-panel">
          <FileText :size="32" />
          <h3>课程目标评价 Word</h3>
          <p>根据考试数据生成课程目标达成评价报告。</p>
          <button class="primary-button" @click="download('/api/reports/objective-report.docx')">
            <FileDown :size="17" />
            <span>导出 .docx</span>
          </button>
        </div>
      </div>
      <div class="panel">
        <div class="panel-title">
          <h3>持续改进建议</h3>
          <span>报告会同步写入以下内容。</span>
        </div>
        <ul class="suggestions">
          <li v-for="item in analysis?.improvementSuggestions" :key="item">{{ item }}</li>
        </ul>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { inject } from 'vue'
import { Check, FileDown, FileSpreadsheet, FileText, Pencil, Plus, Trash2 } from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'

const app = inject('assessmentApp')

const {
  activeView,
  addStudentToSelectedTeachingAssignment,
  allFilteredExamQuestionsSelected,
  allFilteredQuestionsSelected,
  analysis,
  candidateKeyword,
  clearQuestionFilters,
  confirmDeleteQuestion,
  confirmDeleteSelectedQuestions,
  confirmRemoveStudentFromSelectedTeachingAssignment,
  confirmScore,
  createExam,
  download,
  examForm,
  examQuestions,
  exams,
  filteredCandidateStudents,
  filteredQuestions,
  filteredResults,
  filteredStudents,
  formatTime,
  hasQuestionFilters,
  objectiveLabel,
  objectives,
  questionObjectiveFilter,
  questionTypeFilter,
  questionTypes,
  questions,
  results,
  scoreChartRef,
  selectedExam,
  selectedExamId,
  selectedExamQuestionCount,
  selectedExamQuestionIds,
  selectedExamTotalScore,
  selectedFilteredQuestionCount,
  selectedQuestionIds,
  selectedResultExamId,
  selectedTeachingAssignment,
  selectedTeachingAssignmentId,
  startEditQuestion,
  studentTeachingAssignmentSummary,
  teachingAssignments,
  toggleExamQuestionSelection,
  toggleQuestionSelection,
  typeChartRef,
  typeLabel
} = app
</script>
