# 工程教育评估平台

工程教育评估平台是一个面向课程考核、题库管理、在线考试、成绩分析与课程目标达成评价的前后端分离 Web 系统。项目围绕工程教育认证场景设计，支持管理员、教师、学生三类角色，覆盖从基础数据维护、考试组织、阅卷复核到报告导出的完整流程。

![项目封面](frontend/public/landing-hero-campus.jpg)

## 项目亮点

- 多角色工作台：管理员、教师、学生拥有独立菜单和权限边界
- 教学安排管理：维护课程、班级、教师、学生归属关系
- 题库管理：支持题型筛选、课程目标筛选、编辑、批量删除、批量导入
- 考试管理：教师可按课程创建考试，设置开始时间和时长，并从题库选题组卷
- 学生考试页：先展示全部考试、状态和成绩，再点击进入答题；已截止考试不可作答
- 自动判分：选择题自动评分，填空题和主观题支持 AI 优先评分，失败时回退到规则评分
- 模拟答卷：教师可一键生成模拟学生答题数据，便于联调和演示
- 阅卷复核：教师可查看学生答案、AI 评分理由，并手动调整最终分数
- 统计分析：按成绩分布、题型、课程目标等维度生成分析结果
- 报告导出：支持导出 Excel 成绩分析表和 Word 课程目标评价报告

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、ECharts、lucide-vue-next |
| 后端 | Java 17、Spring Boot 3.2、Spring Web、Spring Data JPA |
| 数据库 | MySQL 8 |
| 文档导出 | Apache POI |
| AI 阅卷 | OpenAI 兼容接口，当前可接入 `https://ai.gitee.com/v1` |

## 目录结构

```text
.
|-- backend/                         # Spring Boot 后端
|   |-- src/main/java/com/example/assessment/
|   |   |-- controller/             # REST API
|   |   |-- dto/                    # 请求 DTO
|   |   |-- model/                  # 业务模型
|   |   |-- persistence/            # JPA 实体与 Repository
|   |   `-- service/                # 业务逻辑、AI 阅卷、报告导出
|   `-- src/main/resources/
|       |-- application.yml         # 后端配置
|       `-- templates/              # Excel / Word 模板
|-- frontend/                       # Vue 前端
|   |-- public/
|   `-- src/
|       |-- api/                    # API 请求封装
|       |-- components/             # 通用组件
|       |-- composables/            # 状态与业务逻辑
|       |-- styles/                 # 全局样式
|       `-- views/                  # 管理员 / 教师 / 学生页面
`-- README.md
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+

## 快速开始

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd "EngiEvaluate Platform"
```

### 2. 配置数据库

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/engineering_assessment?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
```

macOS / Linux:

```bash
export DB_URL="jdbc:mysql://localhost:3306/engineering_assessment?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 演示账号

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | 管理教师账号、教学安排、学生信息、题库、报告 |
| 教师 | `teacher` | `123456` | 管理课程学生、题库、考试、阅卷与报告 |
| 学生 | `student` | `123456` | 参加考试并查看个人考试状态与成绩 |
| 学生 | `student2` | `123456` | 参加考试并查看个人考试状态与成绩 |

## AI 阅卷配置

项目已预留 AI 阅卷能力，默认关闭。启用后：

- 选择题：按标准答案自动判分
- 填空题：优先做精确匹配，不匹配时可交给 AI 做语义判断
- 简答题 / 设计题 / 综合分析题：基于参考答案和评分要点调用模型评分
- 若 AI 接口调用失败：自动回退到规则评分，保证学生可正常交卷

PowerShell:

```powershell
$env:AI_GRADING_ENABLED="true"
$env:AI_GRADING_API_KEY="your_api_key"
$env:AI_GRADING_BASE_URL="https://ai.gitee.com/v1"
$env:AI_GRADING_MODEL="DeepSeek-V4-Flash"
```

macOS / Linux:

```bash
export AI_GRADING_ENABLED="true"
export AI_GRADING_API_KEY="your_api_key"
export AI_GRADING_BASE_URL="https://ai.gitee.com/v1"
export AI_GRADING_MODEL="DeepSeek-V4-Flash"
```

说明：

- 不要把真实 API Key 提交到仓库
- 教师端阅卷页会单独高亮显示 AI 评分理由，便于老师复核和改分

## 当前核心功能

### 管理员端

- 教师账号管理
- 专业班级与学生基础信息维护
- 教学安排配置
- 题库管理
- 报告导出

### 教师端

- 课程学生补充与移除
- 题库筛选、编辑、批量删除
- 考试管理面板
- 按课程创建考试并组卷
- 已发布考试详情查看
- 一键生成模拟答题数据
- 学生答卷查看与评分确认
- AI 评分理由高亮展示

### 学生端

- 查看全部考试列表、状态和成绩
- 点击进入可作答考试
- 已截止考试禁用查看
- 作答进度、未答题提醒、题号导航
- 提交试卷后自动判分

## 常用接口

- `GET /api/questions`：题库列表
- `POST /api/questions`：新增试题
- `DELETE /api/questions/batch`：批量删除试题
- `GET /api/exams`：考试列表
- `POST /api/exams`：创建考试
- `POST /api/exams/submit`：提交试卷并判分
- `GET /api/results`：成绩列表
- `PATCH /api/results/score`：教师确认或修改最终分数
- `POST /api/results/mock-generation`：生成模拟答题数据
- `GET /api/analysis`：统计分析

## 后续可继续扩展

- 更完整的权限体系，如 JWT / Session / RBAC
- 更细粒度的课程目标达成度分析
- 试卷模板、随机组卷和防作弊能力
- 更强的 AI 阅卷策略，如评分 rubric、近义表达识别、教师复核建议

## License

This project is for course design, demo, and learning purposes.
