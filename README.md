<p align="center">
  <img src="frontend/public/site-logo.png" alt="EngiEvaluate Logo" width="120">
</p>

<h1 align="center">EngiEvaluate Platform</h1>

<p align="center">
  面向工程教育认证的课程考核与达成度评价平台
</p>

<p align="center">
  <a href="#快速开始">快速开始</a> ·
  <a href="#功能模块">功能模块</a> ·
  <a href="#技术栈">技术栈</a> ·
  <a href="#api-接口">API</a>
</p>

---

## 简介

EngiEvaluate 是一个前后端分离的 Web 系统，围绕工程教育认证场景设计，覆盖从题库管理、在线考试、AI 阅卷到课程目标达成度评价的完整流程。支持管理员、教师、学生三类角色。

## 功能模块

### 管理员

- 教师账号管理
- 专业班级与学生信息维护
- 教学安排配置（课程、班级、教师、学生归属）
- 题库管理
- 报告导出

### 教师

- 课程学生管理（补充 / 移除）
- 题库筛选、编辑、批量删除、批量导入
- 考试管理（创建、组卷、设置时间）
- 自动判分（选择题自动评分，主观题支持 AI 评分）
- 阅卷复核（查看 AI 评分理由，手动调整分数）
- 平时成绩录入（上机、作业、课堂表现）
- 统计分析与报告导出（Excel 成绩分析、Word 达成度报告）

### 学生

- 查看考试列表、状态和成绩
- 在线答题（进度保存、题号导航）
- 查看个人成绩

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 · Vite · ECharts · Lucide Icons |
| 后端 | Java 17 · Spring Boot 3.2 · Spring Data JPA |
| 数据库 | MySQL 8 |
| 文档导出 | Apache POI (.xls / .docx) |
| AI 阅卷 | OpenAI 兼容接口（支持 DeepSeek 等模型） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+

### 1. 配置数据库

```bash
# PowerShell
$env:DB_URL="jdbc:mysql://localhost:3306/engineering_assessment?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"

# macOS / Linux
export DB_URL="jdbc:mysql://localhost:3306/engineering_assessment?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`。

## 演示账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |
| 教师 | `teacher` | `123456` |
| 学生 | `student` | `123456` |

## AI 阅卷配置

AI 阅卷默认关闭，通过环境变量启用：

```bash
export AI_GRADING_ENABLED="true"
export AI_GRADING_API_KEY="your_api_key"
export AI_GRADING_BASE_URL="https://ai.gitee.com/v1"
export AI_GRADING_MODEL="DeepSeek-V4-Flash"
```

> [!NOTE]
> 不要将真实 API Key 提交到仓库。

- 选择题：按标准答案自动判分
- 填空题：精确匹配优先，不匹配时交 AI 语义判断
- 主观题：基于参考答案和评分要点调用模型评分
- AI 调用失败时自动回退到规则评分

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/questions` | 题库列表 |
| POST | `/api/questions` | 新增试题 |
| DELETE | `/api/questions/batch` | 批量删除 |
| GET | `/api/exams` | 考试列表 |
| POST | `/api/exams` | 创建考试 |
| POST | `/api/exams/submit` | 提交试卷 |
| GET | `/api/results` | 成绩列表 |
| PATCH | `/api/results/score` | 修改分数 |
| GET | `/api/analysis` | 统计分析 |
| GET | `/api/reports/score-analysis` | 导出 Excel |
| GET | `/api/reports/objective-report` | 导出 Word |

## 项目结构

```
.
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/example/assessment/
│       ├── controller/               # REST API
│       ├── dto/                      # 请求 / 响应 DTO
│       ├── model/                    # 业务模型
│       ├── persistence/              # JPA 实体与 Repository
│       └── service/                  # 业务逻辑、AI 阅卷、报告导出
├── frontend/                         # Vue 前端
│   └── src/
│       ├── api/                      # API 请求封装
│       ├── components/               # 通用组件
│       ├── composables/              # 状态与业务逻辑
│       └── views/                    # 页面视图
└── scripts/                          # 辅助脚本
```
