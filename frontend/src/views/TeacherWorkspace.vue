<template>
  <AppShell>
    <section v-if="activeView === 'dashboard'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>统计范围</h3>
          <span>先选择课程与教学班，再查看对应统计结果。</span>
        </div>
        <div class="assignment-summary">
          <label>
            课程 / 教学班
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
          <strong>{{ selectedTeachingAssignmentResults.length }}</strong>
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
          <span>老师可以在这里统一创建考试、跟踪开考状态，并快速查看组卷情况。</span>
        </div>

        <div class="teacher-summary-grid">
          <article v-for="item in teacherExamStats" :key="item.label" class="teacher-stat-card">
            <component :is="item.icon" :size="18" class="teacher-stat-icon" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.helper }}</small>
            </div>
          </article>
        </div>

        <div v-if="exams.length" class="teacher-exam-list">
          <button
            v-for="exam in exams"
            :key="exam.id"
            class="exam-summary-card teacher-exam-card"
            :class="{ active: exam.id === selectedExamId }"
            type="button"
            @click="selectedExamId = exam.id"
          >
            <div class="exam-summary-head">
              <strong>{{ exam.paperName }}</strong>
              <span class="status-chip" :class="resolveExamState(exam).tone">{{ resolveExamState(exam).label }}</span>
            </div>
            <span>{{ exam.courseName }} / {{ exam.className }}</span>
            <div class="exam-summary-meta">
              <span>{{ formatTime(exam.startTime) || '未设置开考时间' }}</span>
              <span>{{ exam.durationMinutes }} 分钟</span>
            </div>
            <div class="teacher-exam-card-footer">
              <span>{{ exam.questionCount }} 题</span>
              <span>{{ exam.totalScore }} 分</span>
              <span>{{ examSubmissionCount(exam) }} 份答卷</span>
            </div>
          </button>
        </div>
        <div v-else class="empty-state">
          <strong>还没有已创建的考试</strong>
          <span>先在下面填写考试信息并从题库中勾选题目，系统会自动生成试卷。</span>
        </div>
      </div>

      <div class="student-grid teacher-exam-layout">
        <div class="panel">
          <div class="panel-title">
            <h3>创建考试</h3>
            <span>考试发布后会复制题库题目，后续再修改题库不会影响已创建的历史试卷。</span>
          </div>

          <div class="teacher-summary-inline">
            <div class="teacher-inline-stat">
              <span>目标课程</span>
              <strong>{{ draftAssignment ? `${draftAssignment.courseName} / ${draftAssignment.className}` : '未选择' }}</strong>
            </div>
            <div class="teacher-inline-stat">
              <span>当前草稿</span>
              <strong>{{ selectedExamQuestionCount }} 题 / {{ selectedExamTotalScore }} 分</strong>
            </div>
            <div class="teacher-inline-stat">
              <span>建议时长</span>
              <strong>{{ examForm.durationMinutes || 0 }} 分钟</strong>
            </div>
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
              <input v-model="examForm.paperName" type="text" placeholder="例如：软件工程期中考试 A 卷" />
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
            <textarea v-model="examForm.description" rows="3" placeholder="可选：填写考试说明、答题要求或补充提醒"></textarea>
          </label>

          <div class="section-toolbar question-toolbar">
            <div class="toolbar-meta">
              <strong>已选 {{ selectedExamQuestionCount }} 题 / {{ selectedExamTotalScore }} 分</strong>
              <span>按题型和课程目标筛选后勾选，右侧会同步预览组卷结果。</span>
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
            <span>请调整筛选条件，或先到题库中补充题目后再回来组卷。</span>
          </div>

          <div class="teacher-action-row">
            <button class="ghost-button compact-button" type="button" @click="resetExamForm">
              <RotateCcw :size="16" />
              <span>重置草稿</span>
            </button>
            <button class="primary-button compact-button" type="button" :disabled="!selectedExamQuestionCount" @click="createExam">
              <Plus :size="17" />
              <span>创建考试</span>
            </button>
          </div>
        </div>

        <div class="panel report-panel teacher-preview-stack">
          <div class="panel-title">
            <h3>组卷与试卷预览</h3>
            <span>左侧组卷，右侧同步查看草稿摘要和已发布考试的详细内容。</span>
          </div>

          <article class="teacher-preview-card">
            <div class="teacher-preview-head">
              <strong>当前组卷草稿</strong>
              <span>{{ selectedExamQuestionCount }} 题 / {{ selectedExamTotalScore }} 分</span>
            </div>
            <div class="teacher-meta-grid">
              <div>
                <span>课程班级</span>
                <strong>{{ draftAssignment ? `${draftAssignment.courseName} / ${draftAssignment.className}` : '未选择' }}</strong>
              </div>
              <div>
                <span>开始时间</span>
                <strong>{{ examForm.startTime ? formatTime(examForm.startTime) : '未设置' }}</strong>
              </div>
              <div>
                <span>考试时长</span>
                <strong>{{ examForm.durationMinutes || 0 }} 分钟</strong>
              </div>
            </div>
            <div v-if="draftQuestionTypeSummary.length" class="teacher-pill-list">
              <span v-for="item in draftQuestionTypeSummary" :key="`draft-${item.type}`" class="teacher-pill">
                {{ item.label }} {{ item.count }} 题
              </span>
            </div>
            <div v-if="selectedExamBankQuestions.length" class="answer-list">
              <div v-for="question in selectedExamBankQuestions.slice(0, 6)" :key="`draft-preview-${question.id}`" class="answer-row teacher-preview-row">
                <div>
                  <strong>{{ typeLabel(question.type) }} · {{ question.title }}</strong>
                  <p>{{ objectiveLabel(question.objective) }}</p>
                </div>
                <strong>{{ question.score }} 分</strong>
              </div>
            </div>
            <p v-if="selectedExamBankQuestions.length > 6" class="helper-text">
              其余 {{ selectedExamBankQuestions.length - 6 }} 题会在创建考试后一起写入试卷。
            </p>
            <div v-else class="empty-state">
              <strong>草稿里还没有题目</strong>
              <span>从左侧题库勾选试题后，这里会立刻显示组卷结构。</span>
            </div>
          </article>

          <article class="teacher-preview-card">
            <div class="teacher-preview-head">
              <strong>已发布考试详情</strong>
              <span v-if="selectedExam" class="status-chip" :class="resolveExamState(selectedExam).tone">{{ resolveExamState(selectedExam).label }}</span>
            </div>
            <template v-if="selectedExam">
              <div class="teacher-meta-grid">
                <div>
                  <span>考试名称</span>
                  <strong>{{ selectedExam.paperName }}</strong>
                </div>
                <div>
                  <span>课程班级</span>
                  <strong>{{ selectedExam.courseName }} / {{ selectedExam.className }}</strong>
                </div>
                <div>
                  <span>开始时间</span>
                  <strong>{{ formatTime(selectedExam.startTime) || '未设置' }}</strong>
                </div>
              </div>
              <div class="teacher-summary-inline">
                <div class="teacher-inline-stat">
                  <span>试卷规模</span>
                  <strong>{{ selectedExam.questionCount }} 题 / {{ selectedExam.totalScore }} 分</strong>
                </div>
                <div class="teacher-inline-stat">
                  <span>已交答卷</span>
                  <strong>{{ selectedExamResults.length }} 份</strong>
                </div>
                <div class="teacher-inline-stat">
                  <span>平均分</span>
                  <strong>{{ selectedExamAverageScore }} 分</strong>
                </div>
              </div>
              <div class="teacher-action-row teacher-action-row--start">
                <button
                  class="primary-button compact-button"
                  type="button"
                  :disabled="mockResultGenerating"
                  @click="generateMockResults(selectedExam.id)"
                >
                  <ClipboardList :size="16" />
                  <span>{{ mockResultGenerating ? '生成中...' : '生成模拟答卷' }}</span>
                </button>
              </div>
              <p v-if="mockResultFeedback" class="teacher-inline-feedback" :class="mockResultFeedbackTone">
                {{ mockResultFeedback }}
              </p>
              <div v-if="publishedQuestionTypeSummary.length" class="teacher-pill-list">
                <span v-for="item in publishedQuestionTypeSummary" :key="`published-${item.type}`" class="teacher-pill">
                  {{ item.label }} {{ item.count }} 题
                </span>
              </div>
              <div v-if="examQuestions.length" class="answer-list">
                <div v-for="question in examQuestions.slice(0, 6)" :key="`published-preview-${question.id}`" class="answer-row teacher-preview-row">
                  <div>
                    <strong>{{ typeLabel(question.type) }} · {{ question.title }}</strong>
                    <p>{{ objectiveLabel(question.objective) }}</p>
                  </div>
                  <strong>{{ question.score }} 分</strong>
                </div>
              </div>
              <p v-if="examQuestions.length > 6" class="helper-text">
                其余 {{ examQuestions.length - 6 }} 题可在下方阅卷页继续查看学生作答情况。
              </p>
              <p v-if="selectedExam.description" class="helper-text">{{ selectedExam.description }}</p>
            </template>
            <div v-else class="empty-state">
              <strong>还没有选中已发布考试</strong>
              <span>点击上方考试卡片后，这里会显示试卷结构、开考状态和交卷情况。</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'results'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>学生答题与评分</h3>
          <span>按考试筛选答卷，查看提交概览，并逐题修正最终得分。</span>
        </div>

        <div class="assignment-summary teacher-results-filter">
          <label>
            选择考试
            <select v-model.number="selectedResultExamId">
              <option v-for="exam in exams" :key="`result-exam-${exam.id}`" :value="exam.id">
                {{ exam.paperName }} / {{ exam.className }}
              </option>
            </select>
          </label>
          <div class="status-pill">
            <strong>{{ filteredResults.length }} 份</strong>
            <span>当前筛选答卷</span>
          </div>
        </div>

        <div class="teacher-summary-grid">
          <article v-for="item in teacherResultStats" :key="item.label" class="teacher-stat-card">
            <component :is="item.icon" :size="18" class="teacher-stat-icon" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.helper }}</small>
            </div>
          </article>
        </div>

      </div>

      <div class="teacher-review-layout">
        <section class="panel teacher-review-list-panel">
          <div class="panel-title">
            <h3>学生列表</h3>
            <span>点击左侧学生后，右侧展示该学生整份试卷与评分记录。</span>
          </div>

          <div v-if="filteredResults.length" class="teacher-review-list">
            <article
              v-for="result in filteredResults"
              :key="`summary-${result.id}`"
              class="teacher-result-card teacher-result-card--selectable teacher-result-card--row"
              :class="{ active: result.id === selectedReviewResultId }"
              @click="selectedReviewResultId = result.id"
            >
              <div class="teacher-result-head">
                <div>
                  <strong>{{ result.student.name }}</strong>
                  <span>{{ result.student.studentNo }} / {{ result.className }}</span>
                </div>
                <div class="teacher-result-score">
                  <strong>{{ result.totalScore }}</strong>
                  <span>总分</span>
                </div>
              </div>
              <div class="teacher-exam-card-footer">
                <span>{{ result.paperName }}</span>
                <span>{{ formatTime(result.submittedAt) }}</span>
                <span>{{ resultReviewSummary(result) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">
            <strong>当前没有答卷</strong>
            <span>学生提交后会出现在这里，老师点击学生后即可在右侧阅卷。</span>
          </div>
        </section>

        <article v-if="selectedReviewResult" :key="`detail-${selectedReviewResult.id}`" class="panel result-card teacher-score-card teacher-paper-panel">
          <div class="panel-title">
            <h3>{{ selectedReviewResult.student.name }} / {{ selectedReviewResult.totalScore }} 分</h3>
            <span>{{ selectedReviewResult.paperName }} · {{ selectedReviewResult.className }}</span>
          </div>
          <div class="teacher-meta-grid teacher-meta-grid--compact">
            <div>
              <span>学号</span>
              <strong>{{ selectedReviewResult.student.studentNo }}</strong>
            </div>
            <div>
              <span>提交时间</span>
              <strong>{{ formatTime(selectedReviewResult.submittedAt) }}</strong>
            </div>
            <div>
              <span>阅卷进度</span>
              <strong>{{ resultReviewSummary(selectedReviewResult) }}</strong>
            </div>
          </div>
          <div class="answer-list">
            <div v-for="answer in selectedReviewResult.answers" :key="`${selectedReviewResult.id}-${answer.questionId}`" class="answer-row teacher-answer-row">
              <div>
                <strong>{{ answer.questionTitle }}</strong>
                <p>学生答案：{{ answer.studentAnswer || '未作答' }}</p>
                <div v-if="answer.suggestion" class="teacher-ai-feedback">
                  <span>AI 评分理由</span>
                  <p>{{ answer.suggestion }}</p>
                </div>
              </div>
              <label class="teacher-score-input">
                最终分
                <input v-model.number="answer.score" min="0" :max="answer.maxScore" type="number" />
              </label>
              <button class="ghost-button compact-button" type="button" @click="confirmScore(selectedReviewResult.id, answer.questionId, answer.score)">
                <Check :size="17" />
                <span>保存评分</span>
              </button>
            </div>
          </div>
        </article>
        <div v-else class="panel empty-state teacher-paper-panel">
          <strong>请选择一位学生</strong>
          <span>先在左侧学生列表中点选一位学生，再在右侧查看该学生试卷和逐题评分详情。</span>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'reports'" class="view">
      <div class="panel">
        <div class="panel-title">
          <h3>导出范围</h3>
          <span>报告和导出文件会按当前选择的课程与教学班生成。</span>
        </div>
        <div class="assignment-summary">
          <label>
            课程 / 教学班
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
import { computed, inject, ref, watch } from 'vue'
import { CalendarRange, Check, ClipboardList, Clock3, FileDown, FileSpreadsheet, FileText, ListChecks, Pencil, Plus, RotateCcw, Target, Trash2 } from 'lucide-vue-next'
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
  generateMockResults,
  hasQuestionFilters,
  mockResultFeedback,
  mockResultFeedbackTone,
  mockResultGenerating,
  objectiveLabel,
  objectives,
  questionObjectiveFilter,
  questionTypeFilter,
  questionTypes,
  questions,
  resetExamForm,
  results,
  scoreChartRef,
  selectedExam,
  selectedExamBankQuestions,
  selectedExamId,
  selectedExamQuestionCount,
  selectedExamQuestionIds,
  selectedExamTotalScore,
  selectedFilteredQuestionCount,
  selectedQuestionIds,
  selectedResultExamId,
  selectedTeachingAssignment,
  selectedTeachingAssignmentId,
  selectedTeachingAssignmentResults,
  startEditQuestion,
  studentTeachingAssignmentSummary,
  teachingAssignments,
  toggleExamQuestionSelection,
  toggleQuestionSelection,
  typeChartRef,
  typeLabel
} = app

const draftAssignment = computed(() =>
  teachingAssignments.value.find(item => item.id === examForm.value.teachingAssignmentId) ?? null
)

const selectedExamResults = computed(() => {
  if (!selectedExamId.value) {
    return []
  }
  return results.value.filter(item => item.examId === selectedExamId.value)
})

const selectedExamAverageScore = computed(() => {
  if (!selectedExamResults.value.length) {
    return '0'
  }
  const total = selectedExamResults.value.reduce((sum, item) => sum + Number(item.totalScore || 0), 0)
  return formatAverageScore(total / selectedExamResults.value.length)
})

const selectedReviewResultId = ref(null)
const selectedReviewResult = computed(() =>
  filteredResults.value.find(item => item.id === selectedReviewResultId.value) ?? filteredResults.value[0] ?? null
)

watch(filteredResults, value => {
  if (!value.length) {
    selectedReviewResultId.value = null
    return
  }
  if (!value.some(item => item.id === selectedReviewResultId.value)) {
    selectedReviewResultId.value = value[0].id
  }
}, { immediate: true })

const draftQuestionTypeSummary = computed(() => summarizeQuestionTypes(selectedExamBankQuestions.value))
const publishedQuestionTypeSummary = computed(() => summarizeQuestionTypes(examQuestions.value))

const teacherExamStats = computed(() => {
  const examList = exams.value ?? []
  const runningCount = examList.filter(exam => resolveExamState(exam).key === 'ongoing').length
  const upcomingCount = examList.filter(exam => resolveExamState(exam).key === 'upcoming').length

  return [
    {
      label: '已建考试',
      value: `${examList.length} 场`,
      helper: '当前课程下已经发布的试卷数量',
      icon: ClipboardList
    },
    {
      label: '进行中 / 待开始',
      value: `${runningCount} / ${upcomingCount}`,
      helper: '帮助老师快速判断考试节奏',
      icon: Clock3
    },
    {
      label: '累计答卷',
      value: `${results.value.length} 份`,
      helper: '系统已收到的全部学生提交',
      icon: ListChecks
    },
    {
      label: '当前草稿',
      value: `${selectedExamQuestionCount.value} 题 / ${selectedExamTotalScore.value} 分`,
      helper: '左侧勾选题目后这里会同步更新',
      icon: Target
    }
  ]
})

const teacherResultStats = computed(() => {
  const sheetList = filteredResults.value ?? []
  const totalAnswers = sheetList.reduce((sum, item) => sum + (item.answers?.length ?? 0), 0)
  const reviewedAnswers = sheetList.reduce(
    (sum, item) => sum + ((item.answers ?? []).filter(answer => answer.score !== null && answer.score !== undefined && answer.score !== '').length),
    0
  )
  const averageScore = sheetList.length
    ? formatAverageScore(sheetList.reduce((sum, item) => sum + Number(item.totalScore || 0), 0) / sheetList.length)
    : '0'
  const latestSubmission = sheetList.length
    ? formatTime([...sheetList].sort((left, right) => new Date(right.submittedAt || 0) - new Date(left.submittedAt || 0))[0]?.submittedAt)
    : '暂无提交'

  return [
    {
      label: '当前答卷',
      value: `${sheetList.length} 份`,
      helper: '已按考试筛选后的学生提交记录',
      icon: ListChecks
    },
    {
      label: '平均分',
      value: `${averageScore} 分`,
      helper: '基于当前筛选答卷实时计算',
      icon: Target
    },
    {
      label: '阅卷进度',
      value: `${reviewedAnswers} / ${totalAnswers}`,
      helper: '已录入最终分的题目数量',
      icon: ClipboardList
    },
    {
      label: '最近提交',
      value: latestSubmission,
      helper: '方便老师跟进补交与迟交情况',
      icon: CalendarRange
    }
  ]
})

function resolveExamState(exam) {
  if (!exam?.startTime) {
    return { key: 'unset', label: '待排期', tone: 'muted' }
  }

  const startAt = new Date(exam.startTime).getTime()
  if (Number.isNaN(startAt)) {
    return { key: 'invalid', label: '时间异常', tone: 'danger' }
  }

  const endAt = startAt + Math.max(1, Number(exam.durationMinutes) || 0) * 60 * 1000
  const now = Date.now()

  if (now < startAt) {
    return { key: 'upcoming', label: '未开始', tone: 'warning' }
  }
  if (now > endAt) {
    return { key: 'ended', label: '已结束', tone: 'muted' }
  }
  return { key: 'ongoing', label: '进行中', tone: 'success' }
}

function examSubmissionCount(exam) {
  const sheetCount = results.value.filter(item => item.examId === exam?.id).length
  return Math.max(Number(exam?.submittedCount ?? 0), sheetCount)
}

function summarizeQuestionTypes(questionList = []) {
  const counts = new Map()

  questionList.forEach(question => {
    const type = question.type || 'UNKNOWN'
    counts.set(type, (counts.get(type) || 0) + 1)
  })

  return [...counts.entries()]
    .map(([type, count]) => ({
      type,
      label: typeLabel(type),
      count
    }))
    .sort((left, right) => right.count - left.count)
}

function formatAverageScore(value) {
  const numericValue = Number(value || 0)
  if (!Number.isFinite(numericValue)) {
    return '0'
  }
  return Number.isInteger(numericValue) ? String(numericValue) : numericValue.toFixed(1)
}

function resultReviewSummary(result) {
  const answers = result?.answers ?? []
  const reviewedCount = answers.filter(answer => answer.score !== null && answer.score !== undefined && answer.score !== '').length
  return `已评分 ${reviewedCount}/${answers.length}`
}
</script>
