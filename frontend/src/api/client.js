const API_BASE = ''

function withQuery(path, params = {}) {
  const entries = Object.entries(params).filter(([, value]) => value !== null && value !== undefined && value !== '')
  if (!entries.length) {
    return path
  }
  const query = new URLSearchParams(entries).toString()
  return `${path}${path.includes('?') ? '&' : '?'}${query}`
}

function authHeaders() {
  const raw = localStorage.getItem('assessment_user')
  if (!raw) {
    return {}
  }
  try {
    const user = JSON.parse(raw)
    return {
      'X-Username': encodeURIComponent(user.username ?? ''),
      'X-User-Role': user.role
    }
  } catch {
    return {}
  }
}

async function readError(response) {
  const text = await response.text()
  if (!text) {
    return `Request failed: ${response.status}`
  }
  try {
    const payload = JSON.parse(text)
    return payload.message || text
  } catch {
    return text
  }
}

async function request(path, options = {}) {
  const headers = {
    ...authHeaders(),
    ...options.headers
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })

  if (!response.ok) {
    const error = new Error(await readError(response))
    error.status = response.status
    throw error
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export const api = {
  login: (payload) => request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  changePassword: (payload) => request('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  demoAccounts: () => request('/api/auth/demo-accounts'),
  questions: () => request('/api/questions'),
  addQuestion: (payload) => request('/api/questions', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateQuestion: (questionId, payload) => request(`/api/questions/${questionId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  }),
  deleteQuestion: (questionId) => request(`/api/questions/${questionId}`, {
    method: 'DELETE'
  }),
  importQuestions: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request('/api/questions/import', {
      method: 'POST',
      body: formData
    })
  },
  teacherAccounts: () => request('/api/teacher-accounts'),
  addTeacherAccount: (payload) => request('/api/teacher-accounts', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateTeacherAccount: (teacherAccountId, payload) => request(`/api/teacher-accounts/${teacherAccountId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  }),
  deleteTeacherAccount: (teacherAccountId) => request(`/api/teacher-accounts/${teacherAccountId}`, {
    method: 'DELETE'
  }),
  teachingAssignments: () => request('/api/teaching-assignments'),
  addTeachingAssignment: (payload) => request('/api/teaching-assignments', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateTeachingAssignment: (teachingAssignmentId, payload) => request(`/api/teaching-assignments/${teachingAssignmentId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  }),
  deleteTeachingAssignment: (teachingAssignmentId) => request(`/api/teaching-assignments/${teachingAssignmentId}`, {
    method: 'DELETE'
  }),
  candidateStudents: (teachingAssignmentId) => request(`/api/teaching-assignments/${teachingAssignmentId}/candidate-students`),
  addStudentToTeachingAssignment: (teachingAssignmentId, studentId) => request(`/api/teaching-assignments/${teachingAssignmentId}/students/${studentId}`, {
    method: 'POST'
  }),
  removeStudentFromTeachingAssignment: (teachingAssignmentId, studentId) => request(`/api/teaching-assignments/${teachingAssignmentId}/students/${studentId}`, {
    method: 'DELETE'
  }),
  professionalClasses: () => request('/api/professional-classes'),
  importProfessionalClasses: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request('/api/professional-classes/import', {
      method: 'POST',
      body: formData
    })
  },
  students: () => request('/api/students'),
  addStudent: (payload) => request('/api/students', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateStudent: (studentId, payload) => request(`/api/students/${studentId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  }),
  deleteStudent: (studentId) => request(`/api/students/${studentId}`, {
    method: 'DELETE'
  }),
  importStudents: (teachingAssignmentId, file) => {
    const formData = new FormData()
    formData.append('teachingAssignmentId', teachingAssignmentId)
    formData.append('file', file)
    return request('/api/students/import', {
      method: 'POST',
      body: formData
    })
  },
  exams: () => request('/api/exams'),
  createExam: (payload) => request('/api/exams', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  generateMockResults: (examId) => request(`/api/exams/${examId}/mock-results`, {
    method: 'POST'
  }),
  examQuestions: (examId) => request(`/api/exams/${examId}/questions`),
  results: () => request('/api/results'),
  submitExam: (payload) => request('/api/exams/submit', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  confirmScore: (payload) => request('/api/results/score', {
    method: 'PATCH',
    body: JSON.stringify(payload)
  }),
  analysis: (teachingAssignmentId) => request(withQuery('/api/analysis', { teachingAssignmentId }))
}

export async function downloadReport(path) {
  const response = await fetch(path, {
    headers: authHeaders()
  })
  if (!response.ok) {
    throw new Error(await readError(response))
  }
  const blob = await response.blob()
  const disposition = response.headers.get('content-disposition') || ''
  const match = disposition.match(/filename\*=UTF-8''(.+)$/)
  const fileName = match ? decodeURIComponent(match[1]) : path.split('/').pop()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
