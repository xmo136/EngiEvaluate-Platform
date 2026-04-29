import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  BarChart3,
  BookOpen,
  ClipboardCheck,
  FileText,
  GraduationCap,
  ListChecks,
  School,
  ShieldCheck,
  Trash2,
  Upload,
  Users
} from 'lucide-vue-next'
import { api, downloadReport } from '../api/client'

const defaultCourseName = '软件项目策划与管理'

export function useAssessmentApp() {
  const allNavItems = [
    { key: 'dashboard', label: '统计分析', icon: BarChart3 },
    { key: 'teaching', label: '教学安排', icon: School },
    { key: 'students', label: '学生管理', icon: Users },
    { key: 'questions', label: '题库管理', icon: BookOpen },
    { key: 'exam', label: '在线考试', icon: ClipboardCheck },
    { key: 'results', label: '成绩确认', icon: ListChecks },
    { key: 'reports', label: '报告导出', icon: FileText }
  ]

  const questionTypes = [
    { value: 'SINGLE_CHOICE', label: '选择题' },
    { value: 'FILL_BLANK', label: '填空题' },
    { value: 'SHORT_ANSWER', label: '简答题' },
    { value: 'DESIGN', label: '设计题' },
    { value: 'COMPREHENSIVE', label: '综合分析题' }
  ]

  const objectives = [
    { value: 'OBJECTIVE_1', label: '课程目标 1' },
    { value: 'OBJECTIVE_2', label: '课程目标 2' },
    { value: 'OBJECTIVE_3', label: '课程目标 3' },
    { value: 'OBJECTIVE_4', label: '课程目标 4' }
  ]

  const landingMetrics = [
    { label: '角色分工', value: '管理员 / 老师 / 学生' },
    { label: '基础账号', value: '学号 / 姓名 / 班级' },
    { label: '课程导入', value: '名单自动匹配' },
    { label: '成果导出', value: 'Excel + Word' }
  ]

  const landingFlow = [
    { title: '基础信息维护', text: '管理员统一维护学生基础信息、教师账号和教学安排。', icon: School },
    { title: '课程名单导入', text: '老师上传课程名单，系统从学生基础库中匹配并导入当前课程。', icon: Users },
    { title: '考试与分析归档', text: '老师完成考试与成绩确认，再导出统计结果和评价报告。', icon: BarChart3 }
  ]

  const landingHighlights = [
    { title: '管理员职责更清晰', text: '不再进入在线考试和成绩确认页面，专注基础数据和教学安排。', icon: ShieldCheck },
    { title: '老师导入更贴近课程', text: '按课程名单导入已有学生信息，避免基础台账和课程名单混用。', icon: Upload },
    { title: '删除操作有二次确认', text: '所有删除动作都会明确说明影响范围，再由用户确认执行。', icon: Trash2 }
  ]

  const accessRoles = [
    { title: '管理员', text: '维护教师账号、教学安排和学生基础信息，不参与考试和成绩确认。', icon: ShieldCheck },
    { title: '老师', text: '按课程导入学生名单、维护题库、组织考试并确认成绩。', icon: BookOpen },
    { title: '学生', text: '首次登录先修改密码，再进入考试任务并形成可追溯成绩记录。', icon: GraduationCap }
  ]

  const savedUser = localStorage.getItem('assessment_user')
  const currentUser = ref(savedUser ? JSON.parse(savedUser) : null)
  const heroImageVisible = ref(true)
  const logoVisible = ref(true)
  const heroImageSrc = '/landing-hero-campus.jpg'
  const logoSrc = '/site-logo.png'
  const loginUsernameRef = ref(null)
  const landingHighlightsRef = ref(null)
  const loginForm = ref({ username: '', password: '' })
  const loginError = ref('')
  const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const passwordError = ref('')
  const pageMessage = ref('')
  const pageError = ref('')
  const activeView = ref(currentUser.value?.allowedViews?.[0] ?? 'dashboard')
  const questions = ref([])
  const students = ref([])
  const results = ref([])
  const exams = ref([])
  const examQuestions = ref([])
  const analysis = ref(null)
  const teachingAssignments = ref([])
  const teacherAccounts = ref([])
  const professionalClasses = ref([])
  const selectedTeachingAssignmentId = ref(null)
  const selectedProfessionalClassId = ref(null)
  const selectedStudentId = ref(null)
  const selectedExamId = ref(null)
  const selectedResultExamId = ref(null)
  const selectedExamQuestionIds = ref([])
  const candidateStudents = ref([])
  const candidateKeyword = ref('')
  const examAnswers = ref({})
  const examAnswerSheets = ref({})
  const nowTick = ref(Date.now())
  const optionText = ref('')
  const importFile = ref(null)
  const importFileRef = ref(null)
  const classImportFile = ref(null)
  const classImportFileRef = ref(null)
  const questionImportFile = ref(null)
  const questionImportFileRef = ref(null)
  const questionImportGuideVisible = ref(false)
  const questionManagerOpen = ref(false)
  const questionManagerMode = ref('create')
  const questionTypeFilter = ref('ALL')
  const questionObjectiveFilter = ref('ALL')
  const selectedQuestionIds = ref([])
  const mockResultGenerating = ref(false)
  const mockResultFeedback = ref('')
  const mockResultFeedbackTone = ref('muted')
  const scoreChartRef = ref(null)
  const typeChartRef = ref(null)
  const examStartedAt = ref(new Date().toLocaleString('zh-CN'))
  const examForm = ref({
    teachingAssignmentId: null,
    paperName: '',
    description: '',
    startTime: '',
    durationMinutes: 120
  })
  let examClockTimer = 0

  const assignmentForm = ref({
    courseName: defaultCourseName,
    className: '',
    teacherAccountId: null,
    professionalClassIds: []
  })
  const editingAssignmentId = ref(null)
  const teacherForm = ref({ username: '', displayName: '', password: '123456' })
  const editingTeacherId = ref(null)
  const studentForm = ref({ studentNo: '', name: '', className: '' })
  const editingStudentId = ref(null)
  const editingQuestionId = ref(null)
  const newQuestion = ref({
    courseName: defaultCourseName,
    title: '',
    type: 'SINGLE_CHOICE',
    objective: 'OBJECTIVE_1',
    score: 10,
    options: [],
    answer: '',
    analysis: ''
  })

  const confirmDialog = ref({
    open: false,
    title: '',
    subtitle: '',
    message: '',
    confirmText: '确认',
    action: null
  })

  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
  const isTeacher = computed(() => currentUser.value?.role === 'TEACHER')
  const visibleNavItems = computed(() => allNavItems.filter(item => canView(item.key)))
  const currentTitle = computed(() => allNavItems.find(item => item.key === activeView.value)?.label ?? '统计分析')
  const canEditQuestion = computed(() => ['ADMIN', 'TEACHER'].includes(currentUser.value?.role))
  const selectedTeachingAssignment = computed(() =>
    teachingAssignments.value.find(item => item.id === selectedTeachingAssignmentId.value) ?? null
  )
  const selectedExam = computed(() =>
    exams.value.find(item => item.id === selectedExamId.value) ?? null
  )
  const selectedProfessionalClass = computed(() =>
    professionalClasses.value.find(item => item.id === selectedProfessionalClassId.value) ?? null
  )
  const filteredStudents = computed(() => {
    if (!selectedTeachingAssignmentId.value) {
      return isAdmin.value ? students.value : []
    }
    return students.value.filter(item => item.teachingAssignmentIds?.includes(selectedTeachingAssignmentId.value))
  })
  const filteredAdminStudents = computed(() => {
    if (!isAdmin.value) {
      return []
    }
    if (!selectedProfessionalClassId.value) {
      return students.value
    }
    return students.value.filter(item => item.professionalClassId === selectedProfessionalClassId.value)
  })
  const filteredCandidateStudents = computed(() => {
    const keyword = candidateKeyword.value.trim().toLowerCase()
    if (!keyword) {
      return candidateStudents.value
    }
    return candidateStudents.value.filter(item =>
      [item.studentNo, item.name, item.className]
        .filter(Boolean)
        .some(value => value.toLowerCase().includes(keyword))
    )
  })
  const filteredQuestions = computed(() =>
    questions.value.filter(question => {
      const typeMatched = questionTypeFilter.value === 'ALL' || question.type === questionTypeFilter.value
      const objectiveMatched = questionObjectiveFilter.value === 'ALL' || question.objective === questionObjectiveFilter.value
      return typeMatched && objectiveMatched
    })
  )
  const allFilteredQuestionIds = computed(() => filteredQuestions.value.map(question => question.id))
  const selectedFilteredQuestionCount = computed(() =>
    allFilteredQuestionIds.value.filter(id => selectedQuestionIds.value.includes(id)).length
  )
  const allFilteredQuestionsSelected = computed({
    get() {
      return allFilteredQuestionIds.value.length > 0
        && allFilteredQuestionIds.value.every(id => selectedQuestionIds.value.includes(id))
    },
    set(checked) {
      if (checked) {
        selectedQuestionIds.value = Array.from(new Set([
          ...selectedQuestionIds.value,
          ...allFilteredQuestionIds.value
        ]))
        return
      }
      const filteredIdSet = new Set(allFilteredQuestionIds.value)
      selectedQuestionIds.value = selectedQuestionIds.value.filter(id => !filteredIdSet.has(id))
    }
  })
  const hasQuestionFilters = computed(() => questionTypeFilter.value !== 'ALL' || questionObjectiveFilter.value !== 'ALL')
  const selectedExamBankQuestions = computed(() =>
    questions.value.filter(question => selectedExamQuestionIds.value.includes(question.id))
  )
  const selectedExamQuestionCount = computed(() => selectedExamQuestionIds.value.length)
  const selectedExamTotalScore = computed(() =>
    selectedExamBankQuestions.value.reduce((sum, question) => sum + Number(question.score || 0), 0)
  )
  const allFilteredExamQuestionsSelected = computed({
    get() {
      return allFilteredQuestionIds.value.length > 0
        && allFilteredQuestionIds.value.every(id => selectedExamQuestionIds.value.includes(id))
    },
    set(checked) {
      if (checked) {
        selectedExamQuestionIds.value = Array.from(new Set([
          ...selectedExamQuestionIds.value,
          ...allFilteredQuestionIds.value
        ]))
        return
      }
      const filteredIdSet = new Set(allFilteredQuestionIds.value)
      selectedExamQuestionIds.value = selectedExamQuestionIds.value.filter(id => !filteredIdSet.has(id))
    }
  })
  const filteredResults = computed(() => {
    if (!selectedResultExamId.value) {
      return results.value
    }
    return results.value.filter(result => result.examId === selectedResultExamId.value)
  })
  const selectedTeachingAssignmentResults = computed(() => {
    if (!selectedTeachingAssignmentId.value) {
      return results.value
    }
    return results.value.filter(result => result.teachingAssignmentId === selectedTeachingAssignmentId.value)
  })
  const selectedExamResult = computed(() =>
    results.value.find(result => result.examId === selectedExamId.value) ?? null
  )
  const selectedExamStartAt = computed(() => {
    if (!selectedExam.value?.startTime) {
      return null
    }
    const parsed = new Date(selectedExam.value.startTime)
    return Number.isNaN(parsed.getTime()) ? null : parsed
  })
  const selectedExamEndAt = computed(() => {
    if (!selectedExamStartAt.value || !selectedExam.value?.durationMinutes) {
      return null
    }
    return new Date(selectedExamStartAt.value.getTime() + Number(selectedExam.value.durationMinutes) * 60 * 1000)
  })
  const selectedExamStatus = computed(() => resolveExamStatus(selectedExam.value))
  const selectedExamStatusLabel = computed(() => resolveExamStatusLabel(selectedExam.value))
  const selectedExamCountdownLabel = computed(() => {
    if (selectedExamStatus.value === 'upcoming' && selectedExamStartAt.value) {
      return `距离开始 ${formatDuration(Math.max(0, Math.floor((selectedExamStartAt.value.getTime() - nowTick.value) / 1000)))}`
    }
    if (selectedExamStatus.value === 'ongoing' && selectedExamEndAt.value) {
      return `剩余 ${formatDuration(Math.max(0, Math.floor((selectedExamEndAt.value.getTime() - nowTick.value) / 1000)))}`
    }
    if (selectedExamStatus.value === 'ended') {
      return '考试时间已截止'
    }
    if (selectedExamStatus.value === 'submitted') {
      return '该考试已完成提交'
    }
    return '可随时进入答题'
  })
  const answeredExamQuestionIds = computed(() =>
    examQuestions.value
      .filter(question => hasAnswer(examAnswers.value[question.id]))
      .map(question => question.id)
  )
  const examAnsweredCount = computed(() => answeredExamQuestionIds.value.length)
  const examUnansweredQuestions = computed(() =>
    examQuestions.value.filter(question => !hasAnswer(examAnswers.value[question.id]))
  )
  const examProgressPercent = computed(() => {
    if (!examQuestions.value.length) {
      return 0
    }
    return Math.round((examAnsweredCount.value / examQuestions.value.length) * 100)
  })
  const canSubmitSelectedExam = computed(() =>
    Boolean(
      selectedExam.value
      && examQuestions.value.length
      && !['submitted', 'upcoming', 'ended', 'unselected'].includes(selectedExamStatus.value)
    )
  )
  const submitDisabledReason = computed(() => {
    if (!selectedExam.value) {
      return '请先选择考试'
    }
    if (selectedExamStatus.value === 'submitted') {
      return '该考试已提交'
    }
    if (selectedExamStatus.value === 'upcoming') {
      return '考试尚未开始'
    }
    if (selectedExamStatus.value === 'ended') {
      return '考试已结束，无法提交'
    }
    if (!examQuestions.value.length) {
      return '当前考试暂无试题'
    }
    return ''
  })
  const topbarEyebrow = computed(() => {
    if (selectedTeachingAssignment.value && ['dashboard', 'students', 'exam', 'results', 'reports'].includes(activeView.value)) {
      return `${selectedTeachingAssignment.value.courseName} / ${selectedTeachingAssignment.value.className}`
    }
    return '高校课程评估与考试管理'
  })

  function resolveExamStatus(exam) {
    if (!exam) {
      return 'unselected'
    }
    if (exam.submitted) {
      return 'submitted'
    }
    if (!exam.startTime) {
      return 'available'
    }
    const startAt = new Date(exam.startTime)
    if (Number.isNaN(startAt.getTime())) {
      return 'available'
    }
    const endAt = new Date(startAt.getTime() + Number(exam.durationMinutes || 0) * 60 * 1000)
    const now = nowTick.value
    if (now < startAt.getTime()) {
      return 'upcoming'
    }
    if (now > endAt.getTime()) {
      return 'ended'
    }
    return 'ongoing'
  }

  function resolveExamStatusLabel(exam) {
    switch (resolveExamStatus(exam)) {
      case 'submitted':
        return '已提交'
      case 'upcoming':
        return '未开始'
      case 'ended':
        return '已截止'
      case 'ongoing':
        return '进行中'
      case 'available':
        return '可作答'
      default:
        return '未选择'
    }
  }

  function resolveExamStatusTone(exam) {
    switch (resolveExamStatus(exam)) {
      case 'submitted':
        return 'success'
      case 'upcoming':
        return 'warning'
      case 'ended':
        return 'danger'
      case 'ongoing':
      case 'available':
        return 'info'
      default:
        return 'muted'
    }
  }

  function canOpenExam(exam) {
    return resolveExamStatus(exam) !== 'ended'
  }

  function canView(view) {
    return currentUser.value?.allowedViews?.includes(view)
  }

  function focusLogin() {
    loginUsernameRef.value?.focus()
  }

  function scrollToHighlights() {
    landingHighlightsRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  function clearFeedback() {
    pageMessage.value = ''
    pageError.value = ''
  }

  function setSuccess(message) {
    pageMessage.value = message
    pageError.value = ''
  }

  function setError(error, fallback = '操作失败，请稍后重试') {
    pageMessage.value = ''
    pageError.value = error?.message || fallback
  }

  function openConfirmDialog(config) {
    confirmDialog.value = { open: true, ...config }
  }

  function closeConfirmDialog() {
    confirmDialog.value = {
      open: false,
      title: '',
      subtitle: '',
      message: '',
      confirmText: '确认',
      action: null
    }
  }

  function openQuestionManager(mode = 'create') {
    questionManagerMode.value = mode
    questionManagerOpen.value = true
  }

  function closeQuestionManager() {
    questionManagerOpen.value = false
    questionImportGuideVisible.value = false
  }

  function toggleQuestionSelection(questionId) {
    if (selectedQuestionIds.value.includes(questionId)) {
      selectedQuestionIds.value = selectedQuestionIds.value.filter(id => id !== questionId)
      return
    }
    selectedQuestionIds.value = [...selectedQuestionIds.value, questionId]
  }

  function clearQuestionSelection() {
    selectedQuestionIds.value = []
  }

  function clearQuestionFilters() {
    questionTypeFilter.value = 'ALL'
    questionObjectiveFilter.value = 'ALL'
  }

  async function runConfirmDialog() {
    const action = confirmDialog.value.action
    closeConfirmDialog()
    if (typeof action === 'function') {
      await action()
    }
  }

  function syncSelection() {
    if ((isTeacher.value || canView('teaching')) && (!selectedTeachingAssignmentId.value || !teachingAssignments.value.some(item => item.id === selectedTeachingAssignmentId.value))) {
      selectedTeachingAssignmentId.value = teachingAssignments.value[0]?.id ?? null
    }
    if (canView('exam')) {
      if (currentUser.value?.role === 'STUDENT') {
        if (selectedExamId.value && !exams.value.some(item => item.id === selectedExamId.value)) {
          selectedExamId.value = null
        }
      } else if (!selectedExamId.value || !exams.value.some(item => item.id === selectedExamId.value)) {
        selectedExamId.value = exams.value[0]?.id ?? null
      }
      if (selectedResultExamId.value && !exams.value.some(item => item.id === selectedResultExamId.value)) {
        selectedResultExamId.value = null
      }
      if (!selectedResultExamId.value) {
        selectedResultExamId.value = results.value[0]?.examId ?? exams.value[0]?.id ?? null
      }
      if (!examForm.value.teachingAssignmentId || !teachingAssignments.value.some(item => item.id === examForm.value.teachingAssignmentId)) {
        examForm.value.teachingAssignmentId = selectedTeachingAssignmentId.value ?? teachingAssignments.value[0]?.id ?? null
      }
    }
    if (currentUser.value?.role === 'STUDENT') {
      selectedStudentId.value = currentUser.value.studentId
      return
    }
    if (!selectedStudentId.value || !students.value.some(item => item.id === selectedStudentId.value)) {
      selectedStudentId.value = students.value[0]?.id ?? null
    }
    if (selectedProfessionalClassId.value && !professionalClasses.value.some(item => item.id === selectedProfessionalClassId.value)) {
      selectedProfessionalClassId.value = null
    }
  }

  function resetAssignmentForm() {
    editingAssignmentId.value = null
    assignmentForm.value = {
      courseName: defaultCourseName,
      className: '',
      teacherAccountId: teacherAccounts.value[0]?.id ?? null,
      professionalClassIds: []
    }
  }

  function resetExamForm() {
    examForm.value = {
      teachingAssignmentId: selectedTeachingAssignmentId.value ?? teachingAssignments.value[0]?.id ?? null,
      paperName: '',
      description: '',
      startTime: '',
      durationMinutes: 120
    }
    selectedExamQuestionIds.value = []
  }

  function startEditAssignment(item) {
    editingAssignmentId.value = item.id
    assignmentForm.value = {
      courseName: item.courseName,
      className: item.className,
      teacherAccountId: item.teacherAccountId,
      professionalClassIds: [...(item.professionalClassIds ?? [])]
    }
  }

  function resetTeacherForm() {
    editingTeacherId.value = null
    teacherForm.value = { username: '', displayName: '', password: '123456' }
  }

  function startEditTeacher(teacher) {
    editingTeacherId.value = teacher.id
    teacherForm.value = {
      username: teacher.username,
      displayName: teacher.displayName,
      password: ''
    }
  }

  function resetStudentForm() {
    editingStudentId.value = null
    studentForm.value = {
      studentNo: '',
      name: '',
      className: selectedProfessionalClass.value?.name ?? ''
    }
  }

  function resetQuestionForm() {
    editingQuestionId.value = null
    questionManagerMode.value = 'create'
    newQuestion.value = {
      courseName: defaultCourseName,
      title: '',
      type: 'SINGLE_CHOICE',
      objective: 'OBJECTIVE_1',
      score: 10,
      options: [],
      answer: '',
      analysis: ''
    }
    optionText.value = ''
  }

  function startEditStudent(student) {
    editingStudentId.value = student.id
    studentForm.value = {
      studentNo: student.studentNo,
      name: student.name,
      className: student.className
    }
  }

  function startEditQuestion(question) {
    editingQuestionId.value = question.id
    questionManagerMode.value = 'create'
    questionManagerOpen.value = true
    newQuestion.value = {
      courseName: question.courseName || defaultCourseName,
      title: question.title,
      type: question.type,
      objective: question.objective,
      score: question.score,
      options: [...(question.options ?? [])],
      answer: question.answer,
      analysis: question.analysis ?? ''
    }
    optionText.value = (question.options ?? []).join(' ; ')
  }

  function selectProfessionalClass(professionalClassId) {
    selectedProfessionalClassId.value = selectedProfessionalClassId.value === professionalClassId ? null : professionalClassId
    if (!editingStudentId.value) {
      studentForm.value.className = selectedProfessionalClass.value?.name ?? ''
    }
  }

  function clearProfessionalClassFilter() {
    selectedProfessionalClassId.value = null
    if (!editingStudentId.value) {
      studentForm.value.className = ''
    }
  }

  function confirmDeleteAssignment(item) {
    openConfirmDialog({
      title: '删除教学安排',
      subtitle: `${item.courseName} / ${item.className}`,
      message: '删除后，这条教学安排会从系统中移除。如果该安排下仍有学生，系统会阻止删除并提示你先处理学生归属。',
      confirmText: '确认删除',
      action: () => removeTeachingAssignment(item.id)
    })
  }

  function confirmDeleteTeacher(teacher) {
    openConfirmDialog({
      title: '删除教师账号',
      subtitle: `${teacher.displayName}（${teacher.username}）`,
      message: '删除后，该教师将无法登录平台。如果该教师仍被教学安排引用，系统会阻止删除。',
      confirmText: '确认删除',
      action: () => removeTeacherAccount(teacher.id)
    })
  }

  function confirmDeleteStudent(student) {
    openConfirmDialog({
      title: '删除学生基础信息',
      subtitle: `${student.studentNo} / ${student.name}`,
      message: '删除后，将一并删除该学生的平台账号以及已有考试成绩记录。这个操作不可撤销。',
      confirmText: '确认删除',
      action: () => removeStudent(student.id)
    })
  }

  function confirmDeleteQuestion(question) {
    openConfirmDialog({
      title: '删除题目',
      subtitle: question.title,
      message: '删除后，这道题会从题库中移除。如果已经有学生提交过这道题的答案，系统会阻止删除。',
      confirmText: '确认删除',
      action: () => removeQuestion(question.id)
    })
  }

  function confirmDeleteSelectedQuestions() {
    if (!selectedFilteredQuestionCount.value) {
      setError(new Error('请先选择要删除的题目'))
      return
    }
    openConfirmDialog({
      title: '批量删除题目',
      subtitle: `当前已选 ${selectedFilteredQuestionCount.value} 道题`,
      message: '系统会依次删除当前选中的题目。如果其中有题目已经被学生作答，对应题目会保留并提示删除失败。',
      confirmText: '确认批量删除',
      action: () => removeSelectedQuestions()
    })
  }

  async function login() {
    loginError.value = ''
    clearFeedback()
    try {
      const user = await api.login(loginForm.value)
      currentUser.value = user
      localStorage.setItem('assessment_user', JSON.stringify(user))
      activeView.value = user.allowedViews[0]
      if (user.mustChangePassword) {
        setSuccess('请先修改初始密码，修改完成后再进入系统。')
        return
      }
      await loadAll()
    } catch (error) {
      loginError.value = error?.message || '登录失败，请检查用户名和密码'
    }
  }

  async function changePassword() {
    passwordError.value = ''
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      passwordError.value = '两次输入的新密码不一致'
      return
    }
    try {
      const user = await api.changePassword({
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
      currentUser.value = user
      localStorage.setItem('assessment_user', JSON.stringify(user))
      passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
      setSuccess('密码修改成功，已进入系统。')
      await loadAll()
    } catch (error) {
      passwordError.value = error?.message || '密码修改失败'
    }
  }

  function logout() {
    localStorage.removeItem('assessment_user')
    currentUser.value = null
    loginForm.value = { username: '', password: '' }
    loginError.value = ''
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    passwordError.value = ''
    results.value = []
    exams.value = []
    examQuestions.value = []
    analysis.value = null
    questions.value = []
    students.value = []
    teachingAssignments.value = []
    teacherAccounts.value = []
    professionalClasses.value = []
    candidateStudents.value = []
    candidateKeyword.value = ''
    selectedTeachingAssignmentId.value = null
    selectedStudentId.value = null
    selectedExamId.value = null
    selectedResultExamId.value = null
    selectedExamQuestionIds.value = []
    examAnswers.value = {}
    examAnswerSheets.value = {}
    importFile.value = null
    classImportFile.value = null
    questionImportFile.value = null
    questionImportGuideVisible.value = false
    questionManagerOpen.value = false
    questionManagerMode.value = 'create'
    questionTypeFilter.value = 'ALL'
    questionObjectiveFilter.value = 'ALL'
    selectedQuestionIds.value = []
    activeView.value = 'dashboard'
    resetAssignmentForm()
    resetExamForm()
    resetTeacherForm()
    resetStudentForm()
    resetQuestionForm()
    clearFeedback()
    closeConfirmDialog()
  }

  async function loadAll() {
    if (!currentUser.value || currentUser.value.mustChangePassword) {
      return
    }
    clearFeedback()
    try {
      const [questionData, studentData, assignmentData, examData, resultData, teacherData, classData] = await Promise.all([
        api.questions(),
        api.students(),
        canView('teaching') || canView('students') ? api.teachingAssignments() : Promise.resolve([]),
        canView('exam') ? api.exams() : Promise.resolve([]),
        (canView('results') || currentUser.value?.role === 'STUDENT') ? api.results() : Promise.resolve([]),
        isAdmin.value ? api.teacherAccounts() : Promise.resolve([]),
        isAdmin.value ? api.professionalClasses() : Promise.resolve([])
      ])

      questions.value = questionData
      selectedQuestionIds.value = selectedQuestionIds.value.filter(id => questions.value.some(item => item.id === id))
      selectedExamQuestionIds.value = selectedExamQuestionIds.value.filter(id => questions.value.some(item => item.id === id))
      if (editingQuestionId.value && !questions.value.some(item => item.id === editingQuestionId.value)) {
        resetQuestionForm()
      }
      students.value = studentData
      teachingAssignments.value = assignmentData
      exams.value = examData
      results.value = resultData
      teacherAccounts.value = teacherData
      professionalClasses.value = classData
      syncSelection()
      if (canView('dashboard') || canView('reports')) {
        analysis.value = await api.analysis(selectedTeachingAssignmentId.value)
      } else {
        analysis.value = null
      }
      if (isTeacher.value) {
        await loadCandidateStudents()
      }
      await loadExamQuestions()
      if (isAdmin.value && !editingAssignmentId.value && !assignmentForm.value.teacherAccountId && teacherAccounts.value.length) {
        assignmentForm.value.teacherAccountId = teacherAccounts.value[0].id
      }
      await nextTick()
      renderCharts()
    } catch (error) {
      setError(error, '数据加载失败')
    }
  }

  async function submitAssignmentForm() {
    if (!assignmentForm.value.courseName?.trim() || !assignmentForm.value.className?.trim()) {
      setError(new Error('请先填写课程名称和教学班名称'))
      return
    }
    if (!assignmentForm.value.teacherAccountId) {
      setError(new Error('请先选择任课老师'))
      return
    }
    if (!assignmentForm.value.professionalClassIds?.length) {
      setError(new Error('请至少选择一个专业班级'))
      return
    }
    try {
      if (editingAssignmentId.value) {
        await api.updateTeachingAssignment(editingAssignmentId.value, assignmentForm.value)
        setSuccess('教学安排已更新。')
      } else {
        await api.addTeachingAssignment(assignmentForm.value)
        setSuccess('教学安排已保存。')
      }
      resetAssignmentForm()
      await loadAll()
    } catch (error) {
      setError(error, editingAssignmentId.value ? '更新教学安排失败' : '保存教学安排失败')
    }
  }

  async function submitTeacherForm() {
    try {
      if (editingTeacherId.value) {
        await api.updateTeacherAccount(editingTeacherId.value, teacherForm.value)
        setSuccess('教师信息已更新。')
      } else {
        await api.addTeacherAccount(teacherForm.value)
        setSuccess('教师账号已创建。')
      }
      resetTeacherForm()
      await loadAll()
    } catch (error) {
      setError(error, editingTeacherId.value ? '更新教师信息失败' : '创建教师账号失败')
    }
  }

  async function submitStudentForm() {
    try {
      if (editingStudentId.value) {
        await api.updateStudent(editingStudentId.value, studentForm.value)
        setSuccess('学生信息已更新。')
      } else {
        await api.addStudent(studentForm.value)
        setSuccess('学生信息已新增，并已自动创建平台账号。')
      }
      resetStudentForm()
      await loadAll()
    } catch (error) {
      if (editingStudentId.value && error?.status === 404) {
        resetStudentForm()
        await loadAll()
        setError(new Error('要编辑的学生记录已不存在，列表已刷新。'))
        return
      }
      setError(error, editingStudentId.value ? '更新学生信息失败' : '新增学生信息失败')
    }
  }

  async function removeTeacherAccount(teacherAccountId) {
    try {
      await api.deleteTeacherAccount(teacherAccountId)
      if (editingTeacherId.value === teacherAccountId) {
        resetTeacherForm()
      }
      await loadAll()
      setSuccess('教师账号已删除。')
    } catch (error) {
      setError(error, '删除教师账号失败')
    }
  }

  async function removeTeachingAssignment(teachingAssignmentId) {
    try {
      await api.deleteTeachingAssignment(teachingAssignmentId)
      if (editingAssignmentId.value === teachingAssignmentId) {
        resetAssignmentForm()
      }
      await loadAll()
      setSuccess('教学安排已删除。')
    } catch (error) {
      setError(error, '删除教学安排失败')
    }
  }

  async function removeStudent(studentId) {
    try {
      await api.deleteStudent(studentId)
      if (editingStudentId.value === studentId) {
        resetStudentForm()
      }
      await loadAll()
      setSuccess('学生基础信息、账号和相关成绩记录已删除。')
    } catch (error) {
      if (error?.status === 404) {
        if (editingStudentId.value === studentId) {
          resetStudentForm()
        }
        await loadAll()
        setError(new Error('该学生记录已不存在，列表已刷新。'))
        return
      }
      setError(error, '删除学生信息失败')
    }
  }

  function handleImportFileChange(event) {
    const [file] = event.target.files || []
    importFile.value = file || null
  }

  function handleClassImportFileChange(event) {
    const [file] = event.target.files || []
    classImportFile.value = file || null
  }

  function handleQuestionImportFileChange(event) {
    const [file] = event.target.files || []
    questionImportFile.value = file || null
  }

  async function importStudents() {
    if (!selectedTeachingAssignmentId.value || !importFile.value) {
      setError(new Error('请先选择教学安排并上传文件'))
      return
    }
    try {
      const result = await api.importStudents(selectedTeachingAssignmentId.value, importFile.value)
      importFile.value = null
      if (importFileRef.value) {
        importFileRef.value.value = ''
      }
      await loadAll()
      setSuccess(result.messages?.join('；') || `已导入 ${result.createdCount} 名学生`)
    } catch (error) {
      setError(error, '导入课程学生失败')
    }
  }

  async function importProfessionalClasses() {
    if (!classImportFile.value) {
      setError(new Error('请先上传专业班级导入文件'))
      return
    }
    try {
      const result = await api.importProfessionalClasses(classImportFile.value)
      classImportFile.value = null
      if (classImportFileRef.value) {
        classImportFileRef.value.value = ''
      }
      await loadAll()
      setSuccess(result.messages?.join('；') || '专业班级导入完成')
    } catch (error) {
      setError(error, '导入专业班级失败')
    }
  }

  async function importQuestions() {
    if (!questionImportFile.value) {
      setError(new Error('请先上传题目导入文件'))
      return
    }
    try {
      const result = await api.importQuestions(questionImportFile.value)
      questionImportFile.value = null
      if (questionImportFileRef.value) {
        questionImportFileRef.value.value = ''
      }
      closeQuestionManager()
      await loadAll()
      setSuccess(result.messages?.join('；') || '题目导入完成')
    } catch (error) {
      setError(error, '批量导入题目失败')
    }
  }

  function toggleQuestionImportGuide() {
    questionImportGuideVisible.value = !questionImportGuideVisible.value
  }

  function toggleExamQuestionSelection(questionId) {
    if (selectedExamQuestionIds.value.includes(questionId)) {
      selectedExamQuestionIds.value = selectedExamQuestionIds.value.filter(id => id !== questionId)
      return
    }
    selectedExamQuestionIds.value = [...selectedExamQuestionIds.value, questionId]
  }

  async function loadExamQuestions() {
    if (!canView('exam') || !selectedExamId.value) {
      examQuestions.value = []
      examAnswers.value = {}
      return
    }
    try {
      examQuestions.value = await api.examQuestions(selectedExamId.value)
      const cachedAnswers = examAnswerSheets.value[selectedExamId.value] ?? {}
      const nextAnswers = {}
      for (const question of examQuestions.value) {
        nextAnswers[question.id] = cachedAnswers[question.id] ?? ''
      }
      examAnswers.value = nextAnswers
    } catch (error) {
      examQuestions.value = []
      examAnswers.value = {}
      setError(error, '加载考试题目失败')
    }
  }

  async function createExam() {
    if (!examForm.value.teachingAssignmentId) {
      setError(new Error('请先选择课程班级'))
      return
    }
    if (!examForm.value.paperName?.trim()) {
      setError(new Error('请填写考试名称'))
      return
    }
    if (!examForm.value.durationMinutes || Number(examForm.value.durationMinutes) <= 0) {
      setError(new Error('考试时长必须大于 0'))
      return
    }
    if (!selectedExamQuestionIds.value.length) {
      setError(new Error('请先从题库中选择试题'))
      return
    }
    try {
      const created = await api.createExam({
        teachingAssignmentId: examForm.value.teachingAssignmentId,
        paperName: examForm.value.paperName.trim(),
        description: examForm.value.description?.trim() || '',
        startTime: examForm.value.startTime || null,
        durationMinutes: Number(examForm.value.durationMinutes),
        questionIds: selectedExamQuestionIds.value
      })
      selectedExamId.value = created.id
      selectedResultExamId.value = created.id
      resetExamForm()
      await loadAll()
      setSuccess('考试已创建并完成组卷')
    } catch (error) {
      setError(error, '创建考试失败')
    }
  }

  async function generateMockResults(examId = selectedExamId.value) {
    if (!examId) {
      mockResultFeedback.value = '请先选择一场考试。'
      mockResultFeedbackTone.value = 'danger'
      setError(new Error('请先选择一场考试'))
      return
    }
    mockResultGenerating.value = true
    mockResultFeedback.value = ''
    mockResultFeedbackTone.value = 'muted'
    try {
      const result = await api.generateMockResults(examId)
      await loadAll()
      selectedResultExamId.value = examId
      const createdCount = Number(result?.createdCount || 0)
      const skippedCount = Number(result?.skippedCount || 0)
      const parts = []

      if (createdCount > 0) {
        parts.push(`已生成 ${createdCount} 份模拟答卷`)
      }
      if (skippedCount > 0) {
        parts.push(`跳过 ${skippedCount} 名已提交学生`)
      }
      if (!parts.length) {
        parts.push('当前没有可生成的模拟答卷')
      }

      const feedback = parts.join('，')
      mockResultFeedback.value = feedback
      mockResultFeedbackTone.value = createdCount > 0 ? 'success' : 'warning'
      setSuccess(feedback)
    } catch (error) {
      mockResultFeedback.value = error?.message || '生成模拟答卷失败，请稍后重试。'
      mockResultFeedbackTone.value = 'danger'
      setError(error, '生成模拟答卷失败')
    } finally {
      mockResultGenerating.value = false
    }
  }

  async function addQuestion() {
    const isEditingQuestion = Boolean(editingQuestionId.value)
    try {
      const payload = {
        ...newQuestion.value,
        options: optionText.value
          ? optionText.value.split(/[;,，；]/).map(item => item.trim()).filter(Boolean)
          : []
      }
      if (isEditingQuestion) {
        await api.updateQuestion(editingQuestionId.value, payload)
      } else {
        await api.addQuestion(payload)
      }
      resetQuestionForm()
      closeQuestionManager()
      await loadAll()
      setSuccess(isEditingQuestion ? '题目已更新。' : '试题已加入题库。')
    } catch (error) {
      setError(error, isEditingQuestion ? '更新题目失败' : '新增试题失败')
    }
  }

  async function removeQuestion(questionId) {
    try {
      await api.deleteQuestion(questionId)
      selectedQuestionIds.value = selectedQuestionIds.value.filter(id => id !== questionId)
      if (editingQuestionId.value === questionId) {
        resetQuestionForm()
      }
      await loadAll()
      setSuccess('题目已删除。')
    } catch (error) {
      if (editingQuestionId.value === questionId && error?.status === 404) {
        resetQuestionForm()
      }
      setError(error, '删除题目失败')
    }
  }

  async function removeSelectedQuestions() {
    const ids = Array.from(new Set(selectedQuestionIds.value.filter(id => allFilteredQuestionIds.value.includes(id))))
    if (!ids.length) {
      setError(new Error('请先选择要删除的题目'))
      return
    }

    const questionTitleMap = new Map(questions.value.map(question => [question.id, question.title]))
    const deletedIds = []
    const failedMessages = []

    for (const id of ids) {
      try {
        await api.deleteQuestion(id)
        deletedIds.push(id)
        if (editingQuestionId.value === id) {
          resetQuestionForm()
        }
      } catch (error) {
        const title = questionTitleMap.get(id) || `题目 ${id}`
        failedMessages.push(`${title}：${error?.message || '删除失败'}`)
      }
    }

    selectedQuestionIds.value = selectedQuestionIds.value.filter(id => !deletedIds.includes(id))
    await loadAll()

    if (!failedMessages.length) {
      setSuccess(`已删除 ${deletedIds.length} 道题目。`)
      return
    }

    if (deletedIds.length) {
      setError(new Error(`已删除 ${deletedIds.length} 道题，${failedMessages.length} 道删除失败。${failedMessages[0]}`))
      return
    }

    setError(new Error(failedMessages[0]), '批量删除题目失败')
  }

  async function submitExam() {
    if (!selectedExamId.value) {
      setError(new Error('请先选择考试'))
      return
    }
    if (!selectedStudentId.value) {
      setError(new Error('缺少考生信息'))
      return
    }
    try {
      await api.submitExam({
        examId: selectedExamId.value,
        studentId: selectedStudentId.value,
        answers: examAnswers.value
      })
      examAnswers.value = {}
      if (canView('results')) {
        activeView.value = 'results'
        selectedResultExamId.value = selectedExamId.value
      }
      await loadAll()
      setSuccess('考试已提交并完成自动判分。')
    } catch (error) {
      setError(error, '提交考试失败')
    }
  }

  async function submitExamWithValidation() {
    if (!selectedExamId.value) {
      setError(new Error('请先选择考试'))
      return
    }
    if (!selectedStudentId.value) {
      setError(new Error('缺少考生信息'))
      return
    }
    if (!canSubmitSelectedExam.value) {
      setError(new Error(submitDisabledReason.value || '当前无法提交试卷'))
      return
    }
    if (examUnansweredQuestions.value.length > 0) {
      const shouldContinue = window.confirm(`还有 ${examUnansweredQuestions.value.length} 道题未作答，仍要提交吗？`)
      if (!shouldContinue) {
        return
      }
    }
    try {
      await api.submitExam({
        examId: selectedExamId.value,
        studentId: selectedStudentId.value,
        answers: examAnswers.value
      })
      delete examAnswerSheets.value[selectedExamId.value]
      examAnswers.value = {}
      if (canView('results')) {
        activeView.value = 'results'
        selectedResultExamId.value = selectedExamId.value
      }
      await loadAll()
      setSuccess('考试已提交并完成自动判分。')
    } catch (error) {
      setError(error, '提交考试失败')
    }
  }

  async function confirmScore(resultId, questionId, score) {
    try {
      await api.confirmScore({ resultId, questionId, score })
      await loadAll()
      setSuccess('成绩已确认。')
    } catch (error) {
      setError(error, '确认成绩失败')
    }
  }

  function renderCharts() {
    if (!analysis.value || activeView.value !== 'dashboard') {
      return
    }
    if (scoreChartRef.value) {
      const scoreChart = echarts.init(scoreChartRef.value)
      scoreChart.setOption({
        tooltip: {},
        grid: { left: 42, right: 16, top: 24, bottom: 28 },
        xAxis: { type: 'category', data: Object.keys(analysis.value.scoreBands) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'bar',
          data: Object.values(analysis.value.scoreBands),
          itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] }
        }]
      })
    }
    if (typeChartRef.value) {
      const typeChart = echarts.init(typeChartRef.value)
      typeChart.setOption({
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['42%', '70%'],
          data: Object.entries(analysis.value.questionTypeCount).map(([name, value]) => ({ name, value })),
          color: ['#2563eb', '#16a34a', '#f97316', '#7c3aed', '#0891b2']
        }]
      })
    }
  }

  function typeLabel(type) {
    return questionTypes.find(item => item.value === type)?.label ?? type
  }

  function objectiveLabel(objective) {
    return objectives.find(item => item.value === objective)?.label ?? objective
  }

  function questionTitle(questionId) {
    const question = questions.value.find(item => item.id === questionId)
    return question ? `${question.id}. ${question.title}` : `题目 ${questionId}`
  }

  function studentTeachingAssignmentSummary(student) {
    if (!student?.teachingAssignmentNames?.length) {
      return '未进入教学班'
    }
    return student.teachingAssignmentNames.join('；')
  }

  async function loadCandidateStudents() {
    if (!isTeacher.value || !selectedTeachingAssignmentId.value) {
      candidateStudents.value = []
      return
    }
    try {
      candidateStudents.value = await api.candidateStudents(selectedTeachingAssignmentId.value)
    } catch (error) {
      candidateStudents.value = []
      setError(error, '加载可添加学生失败')
    }
  }

  async function addStudentToSelectedTeachingAssignment(student) {
    if (!selectedTeachingAssignmentId.value || !student?.id) {
      setError(new Error('请先选择教学安排和学生'))
      return
    }
    try {
      await api.addStudentToTeachingAssignment(selectedTeachingAssignmentId.value, student.id)
      await loadAll()
      setSuccess(`已将 ${student.name} 添加到当前课程。`)
    } catch (error) {
      setError(error, '添加课程学生失败')
    }
  }

  function confirmRemoveStudentFromSelectedTeachingAssignment(student) {
    if (!selectedTeachingAssignment.value || !student) {
      return
    }
    openConfirmDialog({
      title: '移出课程学生',
      subtitle: `${student.studentNo} / ${student.name}`,
      message: `确认将该学生移出“${selectedTeachingAssignment.value.courseName} / ${selectedTeachingAssignment.value.className}”吗？移出后不会删除学生基础信息和平台账号。`,
      confirmText: '确认移出',
      action: () => removeStudentFromSelectedTeachingAssignment(student)
    })
  }

  async function removeStudentFromSelectedTeachingAssignment(student) {
    if (!selectedTeachingAssignmentId.value || !student?.id) {
      setError(new Error('请先选择教学安排和学生'))
      return
    }
    try {
      await api.removeStudentFromTeachingAssignment(selectedTeachingAssignmentId.value, student.id)
      await loadAll()
      setSuccess(`已将 ${student.name} 移出当前课程。`)
    } catch (error) {
      setError(error, '移出课程学生失败')
    }
  }

  function questionScore(questionId) {
    return questions.value.find(item => item.id === questionId)?.score ?? 100
  }

  function hasAnswer(value) {
    if (Array.isArray(value)) {
      return value.length > 0
    }
    return String(value ?? '').trim().length > 0
  }

  function formatDuration(totalSeconds) {
    const seconds = Math.max(0, Number(totalSeconds) || 0)
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const remainSeconds = seconds % 60
    if (hours > 0) {
      return `${hours}小时 ${minutes}分钟`
    }
    if (minutes > 0) {
      return `${minutes}分钟 ${remainSeconds}秒`
    }
    return `${remainSeconds}秒`
  }

  function jumpToExamQuestion(questionId) {
    if (!questionId) {
      return
    }
    document.getElementById(`exam-question-${questionId}`)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }

  function formatTime(value) {
    return value ? new Date(value).toLocaleString('zh-CN') : ''
  }

  async function download(path) {
    try {
      await downloadReport(appendTeachingAssignmentQuery(path))
    } catch (error) {
      setError(error, '下载报告失败')
    }
  }

  async function refreshAnalysis() {
    if (!currentUser.value || currentUser.value.mustChangePassword) {
      return
    }
    if (!(canView('dashboard') || canView('reports'))) {
      return
    }
    analysis.value = await api.analysis(selectedTeachingAssignmentId.value)
    await nextTick()
    renderCharts()
  }

  function appendTeachingAssignmentQuery(path) {
    if (!path.startsWith('/api/reports/') || !selectedTeachingAssignmentId.value) {
      return path
    }
    const separator = path.includes('?') ? '&' : '?'
    return `${path}${separator}teachingAssignmentId=${encodeURIComponent(selectedTeachingAssignmentId.value)}`
  }

  watch(activeView, async () => {
    if (!canView(activeView.value)) {
      activeView.value = visibleNavItems.value[0]?.key ?? 'dashboard'
    }
    if (activeView.value !== 'questions') {
      clearQuestionSelection()
      closeQuestionManager()
      resetQuestionForm()
    }
    if (!['questions', 'exam'].includes(activeView.value)) {
      clearQuestionFilters()
    }
    await nextTick()
    renderCharts()
  })

  watch(selectedTeachingAssignmentId, async () => {
    if (isTeacher.value) {
      if (!examForm.value.teachingAssignmentId) {
        examForm.value.teachingAssignmentId = selectedTeachingAssignmentId.value
      }
      await loadCandidateStudents()
    }
    try {
      await refreshAnalysis()
    } catch (error) {
      setError(error, '加载统计分析失败')
    }
  })

  watch(selectedExamId, async () => {
    await loadExamQuestions()
  })

  watch(examAnswers, value => {
    if (!selectedExamId.value) {
      return
    }
    examAnswerSheets.value[selectedExamId.value] = { ...value }
  }, { deep: true })

  onMounted(async () => {
    examClockTimer = window.setInterval(() => {
      nowTick.value = Date.now()
    }, 1000)
    if (currentUser.value && !currentUser.value.mustChangePassword) {
      await loadAll()
    }
  })

  onUnmounted(() => {
    if (examClockTimer) {
      window.clearInterval(examClockTimer)
    }
  })

  return {
    accessRoles,
    activeView,
    addQuestion,
    allNavItems,
    analysis,
    assignmentForm,
    canEditQuestion,
    candidateKeyword,
    candidateStudents,
    canView,
    changePassword,
    clearQuestionFilters,
    clearQuestionSelection,
    closeQuestionManager,
    closeConfirmDialog,
    createExam,
    generateMockResults,
    confirmDeleteAssignment,
    confirmDeleteQuestion,
    confirmDeleteSelectedQuestions,
    confirmDeleteStudent,
    confirmDeleteTeacher,
    confirmRemoveStudentFromSelectedTeachingAssignment,
    confirmDialog,
    confirmScore,
    currentTitle,
    currentUser,
    download,
    editingAssignmentId,
    editingQuestionId,
    editingStudentId,
    editingTeacherId,
    examForm,
    classImportFile,
    classImportFileRef,
    examAnswers,
    examQuestions,
    examStartedAt,
    exams,
    filteredResults,
    filteredAdminStudents,
    filteredCandidateStudents,
    filteredQuestions,
    filteredStudents,
    focusLogin,
    formatDuration,
    formatTime,
    handleClassImportFileChange,
    handleQuestionImportFileChange,
    handleImportFileChange,
    heroImageSrc,
    heroImageVisible,
    importFile,
    importFileRef,
    importProfessionalClasses,
    importQuestions,
    importStudents,
    isAdmin,
    isTeacher,
    landingFlow,
    landingHighlights,
    landingHighlightsRef,
    landingMetrics,
    loadAll,
    loadCandidateStudents,
    loadExamQuestions,
    openQuestionManager,
    logout,
    login,
    loginError,
    loginForm,
    loginUsernameRef,
    mockResultFeedback,
    mockResultFeedbackTone,
    mockResultGenerating,
    logoSrc,
    logoVisible,
    newQuestion,
    objectiveLabel,
    objectives,
    optionText,
    pageError,
    pageMessage,
    passwordError,
    passwordForm,
    professionalClasses,
    questionImportFile,
    questionImportFileRef,
    questionImportGuideVisible,
    questionManagerMode,
    questionManagerOpen,
    questionObjectiveFilter,
    questionTypeFilter,
    resetExamForm,
    selectedProfessionalClass,
    selectedProfessionalClassId,
    selectedExam,
    selectedExamCountdownLabel,
    selectedExamEndAt,
    selectedExamId,
    selectedExamResult,
    selectedExamStartAt,
    selectedExamStatus,
    selectedExamStatusLabel,
    selectedExamQuestionCount,
    selectedExamQuestionIds,
    selectedExamTotalScore,
    selectedExamBankQuestions,
    examAnsweredCount,
    examProgressPercent,
    examUnansweredQuestions,
    answeredExamQuestionIds,
    canSubmitSelectedExam,
    selectedFilteredQuestionCount,
    selectedQuestionIds,
    selectedResultExamId,
    submitDisabledReason,
    questionScore,
    questionTitle,
    questionTypes,
    questions,
    renderCharts,
    resetAssignmentForm,
    resetQuestionForm,
    resetStudentForm,
    resetTeacherForm,
    results,
    runConfirmDialog,
    scoreChartRef,
    scrollToHighlights,
    selectProfessionalClass,
    selectedStudentId,
    selectedTeachingAssignment,
    selectedTeachingAssignmentId,
    selectedTeachingAssignmentResults,
    setError,
    setSuccess,
    startEditAssignment,
    startEditQuestion,
    startEditStudent,
    startEditTeacher,
    studentForm,
    studentTeachingAssignmentSummary,
    students,
    addStudentToSelectedTeachingAssignment,
    submitAssignmentForm,
    submitExam: submitExamWithValidation,
    submitStudentForm,
    submitTeacherForm,
    syncSelection,
    teacherAccounts,
    teacherForm,
    teachingAssignments,
    topbarEyebrow,
    jumpToExamQuestion,
    canOpenExam,
    resolveExamStatus,
    resolveExamStatusLabel,
    resolveExamStatusTone,
    toggleExamQuestionSelection,
    toggleQuestionSelection,
    toggleQuestionImportGuide,
    typeChartRef,
    typeLabel,
    allFilteredExamQuestionsSelected,
    allFilteredQuestionsSelected,
    hasQuestionFilters,
    clearProfessionalClassFilter,
    visibleNavItems
  }
}
