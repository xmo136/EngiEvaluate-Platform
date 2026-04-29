<template>
  <div v-if="questionManagerOpen && activeView === 'questions' && canEditQuestion" class="password-overlay">
    <div class="password-card question-manager-card">
      <div class="question-manager-header">
        <div class="panel-title">
          <div>
            <h3>题库维护</h3>
            <span>在这里新增单题或批量导入题库。</span>
          </div>
        </div>
        <button class="ghost-button compact-button" type="button" @click="closeQuestionManager">
          <X :size="16" />
          <span>关闭</span>
        </button>
      </div>

      <div class="question-manager-tabs">
        <button
          :class="['tab-button', { active: questionManagerMode === 'create' }]"
          type="button"
          @click="openQuestionManager('create')"
        >
          <Plus :size="16" />
          <span>{{ editingQuestionId ? '编辑题目' : '新增题目' }}</span>
        </button>
        <button
          :class="['tab-button', { active: questionManagerMode === 'import' }]"
          type="button"
          @click="openQuestionManager('import')"
        >
          <Upload :size="16" />
          <span>批量导入</span>
        </button>
      </div>

      <div v-if="questionManagerMode === 'import'" class="question-manager-body">
        <div class="panel-title">
          <h3>批量导入题目</h3>
          <span>支持 `.xls / .xlsx / .csv / .txt`。</span>
        </div>
        <div class="form-action-row">
          <button class="ghost-button" type="button" @click="toggleQuestionImportGuide">
            <span>{{ questionImportGuideVisible ? '收起教程' : '导入教程' }}</span>
          </button>
        </div>
        <div v-if="questionImportGuideVisible" class="import-guide">
          <p>固定列顺序：课程名称、题干、题型、课程目标、分值、选项、标准答案、解析。</p>
          <p>其中选项一格内用 <code>|</code> 分隔，例如：<code>A. 需求分析|B. 概要设计|C. 编码实现|D. 测试验收</code></p>
          <p>题型支持：选择题、填空题、简答题、设计题、综合分析题。</p>
          <p>课程目标支持：课程目标1/2/3/4，或 OBJECTIVE_1/2/3/4。</p>
        </div>
        <div class="import-box">
          <input ref="questionImportFileRef" type="file" accept=".xls,.xlsx,.csv,.txt" @change="handleQuestionImportFileChange" />
          <button class="primary-button" type="button" :disabled="!questionImportFile" @click="importQuestions">
            <Upload :size="17" />
            <span>导入题目</span>
          </button>
        </div>
      </div>

      <form v-else class="question-manager-body question-manager-form" @submit.prevent="addQuestion">
        <div class="panel-title">
          <h3>{{ editingQuestionId ? '编辑试题' : '新增试题' }}</h3>
          <span>保存后会立即同步到题库列表。</span>
        </div>
        <label>
          题干
          <textarea v-model="newQuestion.title" rows="4" required></textarea>
        </label>
        <div class="form-row">
          <label>
            题型
            <select v-model="newQuestion.type">
              <option v-for="item in questionTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            课程目标
            <select v-model="newQuestion.objective">
              <option v-for="item in objectives" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
        </div>
        <div class="form-row">
          <label>
            分值
            <input v-model.number="newQuestion.score" min="1" type="number" />
          </label>
          <label>
            标准答案 / 关键词
            <input v-model="newQuestion.answer" required />
          </label>
        </div>
        <label>
          选项
          <input v-model="optionText" placeholder="选择题可填写：A. ...；B. ..." />
        </label>
        <label>
          解析
          <input v-model="newQuestion.analysis" />
        </label>
        <div class="form-action-row">
          <button class="primary-button full" type="submit">
            <Plus :size="17" />
            <span>{{ editingQuestionId ? '保存题目' : '加入题库' }}</span>
          </button>
          <button v-if="editingQuestionId" class="ghost-button full" type="button" @click="resetQuestionForm">
            <span>取消编辑</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { Plus, Upload, X } from 'lucide-vue-next'

const app = inject('assessmentApp')

const {
  activeView,
  addQuestion,
  canEditQuestion,
  closeQuestionManager,
  editingQuestionId,
  handleQuestionImportFileChange,
  importQuestions,
  newQuestion,
  objectives,
  openQuestionManager,
  optionText,
  questionImportFile,
  questionImportFileRef,
  questionImportGuideVisible,
  questionManagerMode,
  questionManagerOpen,
  questionTypes,
  resetQuestionForm,
  toggleQuestionImportGuide
} = app
</script>
