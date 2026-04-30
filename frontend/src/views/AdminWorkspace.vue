<template>
  <AppShell>
    <section v-if="activeView === 'dashboard'" class="view" aria-label="管理员仪表板">
      <div class="panel">
        <div class="panel-title">
          <h3>统计范围</h3>
          <span>先选择课程与教学班，再查看对应统计结果。</span>
        </div>
        <div class="assignment-summary">
          <label for="admin-dashboard-assignment">
            课程 / 教学班
            <select id="admin-dashboard-assignment" v-model.number="selectedTeachingAssignmentId" aria-label="选择课程和教学班">
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

      <div class="kpi-grid" role="list" aria-label="关键指标">
        <div class="metric" role="listitem">
          <span>学生人数</span>
          <strong>{{ analysis?.studentCount ?? 0 }}</strong>
        </div>
        <div class="metric" role="listitem">
          <span>题目数量</span>
          <strong>{{ analysis?.questionCount ?? 0 }}</strong>
        </div>
        <div class="metric" role="listitem">
          <span>平均分</span>
          <strong>{{ analysis?.averageScore ?? 0 }}</strong>
        </div>
        <div class="metric" role="listitem">
          <span>成绩记录</span>
          <strong>{{ selectedTeachingAssignmentResults.length }}</strong>
        </div>
      </div>

      <div class="chart-grid" role="img" aria-label="统计图表">
        <div class="panel">
          <div class="panel-title">
            <h3>成绩分布</h3>
            <span>按分数段统计</span>
          </div>
          <div ref="scoreChartRef" class="chart" role="img" aria-label="成绩分布图表"></div>
        </div>
        <div class="panel">
          <div class="panel-title">
            <h3>题型分布</h3>
            <span>题库结构占比</span>
          </div>
          <div ref="typeChartRef" class="chart" role="img" aria-label="题型分布图表"></div>
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

    <section v-if="activeView === 'teaching'" class="view" aria-label="教学班管理">
      <div class="split-layout">
        <div class="panel">
          <div class="panel-title">
            <h3>教学班安排</h3>
            <span>管理员配置课程、教学班、任课老师，并勾选进入该教学班的专业班级。</span>
          </div>
          <div v-if="teachingAssignments.length" class="table-wrap">
            <table aria-label="教学班安排列表">
              <thead>
                <tr>
                  <th>课程</th>
                  <th>课程编码</th>
                  <th>教学班</th>
                  <th>学时/学分</th>
                  <th>学期</th>
                  <th>任课老师</th>
                  <th>专业班级</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teachingAssignments" :key="item.id">
                  <td>{{ item.courseName }}</td>
                  <td>{{ item.courseCode || '-' }}</td>
                  <td>{{ item.className }}</td>
                  <td>{{ item.creditHours || '-' }}/{{ item.credits || '-' }}</td>
                  <td>{{ item.semester || '-' }}</td>
                  <td>{{ item.teacherName }}</td>
                  <td>{{ item.professionalClassNames?.length ? item.professionalClassNames.join('、') : '未选择' }}</td>
                  <td class="row-actions">
                    <button class="ghost-button compact-button" type="button" :aria-label="`编辑教学班：${item.courseName} / ${item.className}`" @click="startEditAssignment(item)">
                      <Pencil :size="15" aria-hidden="true" />
                      <span>编辑</span>
                    </button>
                    <button class="ghost-button compact-button danger-button" type="button" :aria-label="`删除教学班：${item.courseName} / ${item.className}`" @click="confirmDeleteAssignment(item)">
                      <Trash2 :size="15" aria-hidden="true" />
                      <span>删除</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <strong>还没有教学班安排</strong>
            <span>先导入专业班级与学生，再创建教学班并选择进入这个教学班的专业班级。</span>
          </div>
        </div>

        <div class="stack-panel">
          <form class="panel form-panel" @submit.prevent="submitAssignmentForm" aria-label="教学班安排表单">
            <div class="panel-title">
              <h3>{{ editingAssignmentId ? '编辑教学班安排' : '新增教学班安排' }}</h3>
              <span>一个教学班可关联多个专业班级，系统会自动把这些班级的学生归入当前教学班。</span>
            </div>
            <label for="assignment-course-name">
              课程名称
              <input id="assignment-course-name" v-model="assignmentForm.courseName" required aria-required="true" />
            </label>
            <label for="assignment-class-name">
              教学班名称
              <input id="assignment-class-name" v-model="assignmentForm.className" required aria-required="true" />
            </label>
            <label for="assignment-course-code">
              课程编码
              <input id="assignment-course-code" v-model="assignmentForm.courseCode" placeholder="如 1302079" />
            </label>
            <div class="form-row">
              <label for="assignment-credit-hours">
                学时
                <input id="assignment-credit-hours" v-model.number="assignmentForm.creditHours" type="number" min="1" placeholder="如 38" />
              </label>
              <label for="assignment-credits">
                学分
                <input id="assignment-credits" v-model.number="assignmentForm.credits" type="number" min="1" placeholder="如 3" />
              </label>
            </div>
            <label for="assignment-semester">
              学期
              <input id="assignment-semester" v-model="assignmentForm.semester" placeholder="如 2024-2025-1" />
            </label>
            <div class="form-row">
              <label for="assignment-college">
                学院
                <input id="assignment-college" v-model="assignmentForm.college" placeholder="如 计算机科学与技术学院" />
              </label>
              <label for="assignment-grade">
                年级
                <input id="assignment-grade" v-model="assignmentForm.grade" placeholder="如 2021" />
              </label>
            </div>
            <label for="assignment-teacher">
              任课老师
              <select id="assignment-teacher" v-model.number="assignmentForm.teacherAccountId" required aria-required="true">
                <option disabled :value="null">请选择教师账号</option>
                <option v-for="teacher in teacherAccounts" :key="teacher.id" :value="teacher.id">
                  {{ teacher.displayName }}（{{ teacher.username }}）
                </option>
              </select>
            </label>
            <label for="assignment-classes">
              进入教学班的专业班级
              <select id="assignment-classes" v-model="assignmentForm.professionalClassIds" multiple size="6" aria-label="选择专业班级">
                <option v-for="item in professionalClasses" :key="item.id" :value="item.id">
                  {{ item.name }}（{{ item.studentCount }}人）
                </option>
              </select>
            </label>
            <p class="helper-text">按住 Ctrl 或 Command 可多选。保存后，这些专业班级下的学生会自动归入当前教学班。</p>
            <div class="form-action-row">
              <button class="primary-button full" type="submit" :disabled="!teacherAccounts.length || !professionalClasses.length" :aria-label="editingAssignmentId ? '保存教学班修改' : '保存教学班安排'">
                <School :size="17" aria-hidden="true" />
                <span>{{ editingAssignmentId ? '保存修改' : '保存教学班安排' }}</span>
              </button>
              <button v-if="editingAssignmentId" class="ghost-button full" type="button" aria-label="取消编辑" @click="resetAssignmentForm">
                <span>取消编辑</span>
              </button>
            </div>
          </form>

          <div class="panel">
            <div class="panel-title">
              <h3>教师账号</h3>
              <span>管理员维护教师账号，教学班从这里选择任课老师。</span>
            </div>
            <div v-if="teacherAccounts.length" class="table-wrap teacher-account-table">
              <table aria-label="教师账号列表">
                <thead>
                  <tr>
                    <th>姓名</th>
                    <th>账号</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="teacher in teacherAccounts" :key="teacher.id">
                    <td>{{ teacher.displayName }}</td>
                    <td>{{ teacher.username }}</td>
                    <td class="row-actions">
                      <button class="ghost-button compact-button" type="button" :aria-label="`编辑教师：${teacher.displayName}`" @click="startEditTeacher(teacher)">
                        <Pencil :size="15" aria-hidden="true" />
                        <span>编辑</span>
                      </button>
                      <button class="ghost-button compact-button danger-button" type="button" :aria-label="`删除教师：${teacher.displayName}`" @click="confirmDeleteTeacher(teacher)">
                        <Trash2 :size="15" aria-hidden="true" />
                        <span>删除</span>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <form class="teacher-form" @submit.prevent="submitTeacherForm" aria-label="教师账号表单">
              <label for="teacher-username">
                教师账号
                <input id="teacher-username" v-model="teacherForm.username" required aria-required="true" />
              </label>
              <label for="teacher-display-name">
                教师姓名
                <input id="teacher-display-name" v-model="teacherForm.displayName" required aria-required="true" />
              </label>
              <label for="teacher-password">
                {{ editingTeacherId ? '重置密码（留空则不修改）' : '初始密码' }}
                <input id="teacher-password" v-model="teacherForm.password" type="password" />
              </label>
              <div class="form-action-row">
                <button class="primary-button full" type="submit" :aria-label="editingTeacherId ? '保存教师信息' : '创建教师账号'">
                  <UserPlus :size="17" aria-hidden="true" />
                  <span>{{ editingTeacherId ? '保存教师信息' : '创建教师账号' }}</span>
                </button>
                <button v-if="editingTeacherId" class="ghost-button full" type="button" aria-label="取消编辑" @click="resetTeacherForm">
                  <span>取消编辑</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'students'" class="view" aria-label="学生管理">
      <div class="split-layout">
        <div class="panel">
          <div class="panel-title">
            <h3>学生基础信息</h3>
            <span>管理员维护学生基础信息，也可以按专业班级查看学生，并通过专业班级名单一键导入学生账号。</span>
          </div>
          <div class="section-toolbar">
            <div class="toolbar-meta">
              <strong>{{ selectedProfessionalClass ? `当前专业班级：${selectedProfessionalClass.name}` : '当前显示：全部学生' }}</strong>
              <span>{{ filteredAdminStudents.length }} 人</span>
            </div>
            <button
              v-if="selectedProfessionalClass"
              class="ghost-button compact-button"
              type="button"
              @click="clearProfessionalClassFilter"
            >
              <span>查看全部学生</span>
            </button>
          </div>
          <div v-if="filteredAdminStudents.length" class="table-wrap">
            <table aria-label="学生列表">
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>专业班级</th>
                  <th>账号</th>
                  <th>教学班归属</th>
                  <th>密码状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="student in filteredAdminStudents" :key="student.id">
                  <td>{{ student.studentNo }}</td>
                  <td>{{ student.name }}</td>
                  <td>{{ student.className }}</td>
                  <td>{{ student.username || student.studentNo }}</td>
                  <td>{{ studentTeachingAssignmentSummary(student) }}</td>
                  <td>{{ student.passwordChangeRequired ? '首次登录待改密' : '已完成改密' }}</td>
                  <td class="row-actions">
                    <button class="ghost-button compact-button" type="button" :aria-label="`编辑学生：${student.name}`" @click="startEditStudent(student)">
                      <Pencil :size="15" aria-hidden="true" />
                      <span>编辑</span>
                    </button>
                    <button class="ghost-button compact-button danger-button" type="button" :aria-label="`删除学生：${student.name}`" @click="confirmDeleteStudent(student)">
                      <Trash2 :size="15" aria-hidden="true" />
                      <span>删除</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <strong>{{ selectedProfessionalClass ? '当前专业班级下还没有学生' : '还没有学生基础信息' }}</strong>
            <span>{{ selectedProfessionalClass ? '可以继续导入该专业班级学生，或手动新增学生信息。' : '可以手动新增学生，也可以导入专业班级名单，系统会自动创建学生账号。' }}</span>
          </div>
        </div>

        <div class="stack-panel">
          <div class="panel report-panel">
            <div class="panel-title">
              <h3>专业班级管理</h3>
              <span>支持 .xls / .xlsx / .csv，默认读取前三列：学号、姓名、专业班级。点击班级即可查看该班学生。</span>
            </div>
            <div class="import-box">
              <label for="class-import-file" class="sr-only">选择专业班级名单文件</label>
              <input id="class-import-file" ref="classImportFileRef" type="file" accept=".xls,.xlsx,.csv,.txt" aria-label="选择专业班级名单文件" @change="handleClassImportFileChange" />
              <p class="helper-text">导入时会自动创建专业班级，学生账号默认使用学号，初始密码为 123456。</p>
              <button class="primary-button" type="button" :disabled="!classImportFile" aria-label="导入专业班级名单" @click="importProfessionalClasses">
                <Upload :size="17" aria-hidden="true" />
                <span>导入专业班级名单</span>
              </button>
            </div>
            <div v-if="professionalClasses.length" class="table-wrap">
              <table aria-label="专业班级列表">
                <thead>
                  <tr>
                    <th>专业班级</th>
                    <th>学生人数</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in professionalClasses" :key="item.id" :class="{ 'active-filter-row': selectedProfessionalClassId === item.id }">
                    <td>
                      <button class="class-link-button" type="button" @click="selectProfessionalClass(item.id)">
                        {{ item.name }}
                      </button>
                    </td>
                    <td>{{ item.studentCount }}</td>
                    <td>
                      <button class="ghost-button compact-button" type="button" @click="selectProfessionalClass(item.id)">
                        <span>{{ selectedProfessionalClassId === item.id ? '取消筛选' : '查看学生' }}</span>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <form class="panel form-panel" @submit.prevent="submitStudentForm" aria-label="学生信息表单">
            <div class="panel-title">
              <h3>{{ editingStudentId ? '编辑学生信息' : '手动新增学生' }}</h3>
              <span>新增后会自动创建学生账号，账号名默认等于学号。</span>
            </div>
            <label for="student-no">
              学号
              <input id="student-no" v-model="studentForm.studentNo" required aria-required="true" />
            </label>
            <label for="student-name">
              姓名
              <input id="student-name" v-model="studentForm.name" required aria-required="true" />
            </label>
            <label for="student-class">
              专业班级
              <input id="student-class" v-model="studentForm.className" required aria-required="true" />
            </label>
            <p class="helper-text">如果输入的是新班级名称，系统会自动创建这个专业班级。</p>
            <div class="form-action-row">
              <button class="primary-button full" type="submit" :aria-label="editingStudentId ? '保存学生信息' : '新增学生信息'">
                <Users :size="17" aria-hidden="true" />
                <span>{{ editingStudentId ? '保存学生信息' : '新增学生信息' }}</span>
              </button>
              <button v-if="editingStudentId" class="ghost-button full" type="button" aria-label="取消编辑" @click="resetStudentForm">
                <span>取消编辑</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'questions'" class="view" aria-label="题库管理">
      <div class="panel">
        <div class="panel-title">
          <h3>题库</h3>
          <span>支持选择、填空、简答、设计、综合分析题。</span>
        </div>
        <div class="section-toolbar question-toolbar">
          <div class="toolbar-meta">
            <strong>{{ hasQuestionFilters ? `筛选结果 ${filteredQuestions.length} 道题` : `共 ${questions.length} 道题` }}</strong>
            <span v-if="canEditQuestion">{{ selectedFilteredQuestionCount }} / {{ filteredQuestions.length }} 已选</span>
            <span v-else>可按题型和课程目标筛选查看。</span>
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
            <label v-if="canEditQuestion && filteredQuestions.length" class="question-select-all">
              <input v-model="allFilteredQuestionsSelected" type="checkbox" />
              <span>全选当前</span>
            </label>
            <button
              v-if="canEditQuestion"
              class="ghost-button compact-button danger-button"
              type="button"
              :disabled="!selectedFilteredQuestionCount"
              aria-label="批量删除选中题目"
              @click="confirmDeleteSelectedQuestions"
            >
              <Trash2 :size="15" aria-hidden="true" />
              <span>批量删除</span>
            </button>
          </div>
        </div>
        <div v-if="filteredQuestions.length" class="question-list" role="list" aria-label="题目列表">
          <article v-for="question in filteredQuestions" :key="question.id" class="question-card" role="listitem" :aria-label="`${typeLabel(question.type)}：${question.title}，${question.score} 分`">
            <div class="question-card-head">
              <div class="question-card-meta">
                <label v-if="canEditQuestion" class="question-checkbox">
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
              <div v-if="canEditQuestion" class="question-card-actions">
                <button class="ghost-button compact-button" type="button" :aria-label="`编辑题目：${question.title}`" @click="startEditQuestion(question)">
                  <Pencil :size="15" aria-hidden="true" />
                  <span>编辑</span>
                </button>
                <button class="ghost-button compact-button danger-button" type="button" :aria-label="`删除题目：${question.title}`" @click="confirmDeleteQuestion(question)">
                  <Trash2 :size="15" aria-hidden="true" />
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
          <span>可以调整题型或课程目标筛选条件后再查看。</span>
        </div>
      </div>
    </section>

    <section v-if="activeView === 'reports'" class="view" aria-label="报告导出">
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

      <div class="report-layout" role="list" aria-label="报告导出选项">
        <div class="panel report-panel" role="listitem">
          <FileSpreadsheet :size="32" aria-hidden="true" />
          <h3>统计小分及分析 Excel</h3>
          <p>导出学生成绩、课程目标达成、题型分布和图表分析结果。</p>
          <button class="primary-button" aria-label="导出成绩分析 Excel 文件" @click="download('/api/reports/score-analysis.xls')">
            <FileDown :size="17" aria-hidden="true" />
            <span>导出 .xls</span>
          </button>
        </div>
        <div class="panel report-panel" role="listitem">
          <FileText :size="32" aria-hidden="true" />
          <h3>课程目标达成评价 Word</h3>
          <p>根据考试数据生成课程目标达成评价报告，并附带持续改进建议。</p>
          <button class="primary-button" aria-label="导出课程目标评价 Word 文件" @click="download('/api/reports/objective-report.docx')">
            <FileDown :size="17" aria-hidden="true" />
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
import { FileDown, FileSpreadsheet, FileText, Pencil, Plus, School, Trash2, Upload, UserPlus, Users } from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'

const app = inject('assessmentApp')

const {
  activeView,
  addQuestion,
  allFilteredQuestionsSelected,
  analysis,
  assignmentForm,
  canEditQuestion,
  classImportFile,
  classImportFileRef,
  clearQuestionFilters,
  clearProfessionalClassFilter,
  confirmDeleteAssignment,
  confirmDeleteQuestion,
  confirmDeleteSelectedQuestions,
  confirmDeleteStudent,
  confirmDeleteTeacher,
  download,
  editingAssignmentId,
  editingQuestionId,
  editingStudentId,
  editingTeacherId,
  filteredAdminStudents,
  filteredQuestions,
  handleClassImportFileChange,
  handleQuestionImportFileChange,
  hasQuestionFilters,
  importQuestions,
  importProfessionalClasses,
  newQuestion,
  objectiveLabel,
  objectives,
  optionText,
  professionalClasses,
  questionImportFile,
  questionImportFileRef,
  questionImportGuideVisible,
  questionObjectiveFilter,
  questionTypeFilter,
  questionTypes,
  questions,
  resetAssignmentForm,
  resetQuestionForm,
  resetStudentForm,
  resetTeacherForm,
  results,
  scoreChartRef,
  selectProfessionalClass,
  selectedFilteredQuestionCount,
  selectedProfessionalClass,
  selectedProfessionalClassId,
  selectedTeachingAssignment,
  selectedTeachingAssignmentId,
  selectedTeachingAssignmentResults,
  selectedQuestionIds,
  startEditAssignment,
  startEditQuestion,
  startEditStudent,
  startEditTeacher,
  studentForm,
  studentTeachingAssignmentSummary,
  submitAssignmentForm,
  submitStudentForm,
  submitTeacherForm,
  teacherAccounts,
  teacherForm,
  teachingAssignments,
  toggleQuestionSelection,
  toggleQuestionImportGuide,
  typeChartRef,
  typeLabel
} = app
</script>

<style scoped>
/* Panel stagger animation */
.panel {
  animation: fadeInUp 400ms ease-out both;
}

.panel:nth-child(1) { animation-delay: 50ms; }
.panel:nth-child(2) { animation-delay: 100ms; }
.panel:nth-child(3) { animation-delay: 150ms; }
.panel:nth-child(4) { animation-delay: 200ms; }

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

/* Table row hover feedback */
.table-wrap tr {
  transition: background-color var(--transition-fast);
}

.table-wrap tr:hover {
  background: var(--color-primary-light);
}

/* Question card focus styles */
.question-card:focus-within {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Form focus styles */
.form-panel input:focus-visible,
.form-panel select:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .panel {
    animation: none;
  }

  .table-wrap tr {
    transition: none;
  }
}
</style>
