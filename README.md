# 工程教育评估平台

工程教育评估平台是一个面向课程目标达成度评价的前后端分离 Web 系统。项目以工程教育认证中的课程考核、成绩统计、目标达成分析和评价报告归档为核心场景，提供管理员、教师、学生三类角色的工作台。

![项目封面](frontend/public/landing-hero-campus.jpg)

## 项目特点

- 角色分工：支持管理员、教师、学生三类账号，不同角色拥有独立菜单和操作权限。
- 教学管理：管理员可维护教师账号、专业班级、学生基础信息和教学安排。
- 题库管理：支持选择题、填空题、简答题、设计题、综合分析题等题型。
- 在线考试：教师可基于题库组卷，学生进入对应课程考试并提交答案。
- 自动评分：客观题自动判分，主观题根据关键词和规则生成建议分，教师可复核调整。
- 成绩确认：教师可查看学生答题明细并确认每道题最终得分。
- 统计分析：按分数段、题型、课程目标等维度生成达成情况分析。
- 报告导出：基于模板导出 Excel 成绩分析表和 Word 课程目标达成评价报告。
- 数据持久化：后端使用 MySQL 和 Spring Data JPA 保存用户、学生、题库、试卷、答题和成绩记录。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3, Vite, ECharts, lucide-vue-next |
| 后端 | Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA |
| 数据库 | MySQL 8 |
| 文档导出 | Apache POI |
| 构建工具 | npm, Maven |

## 目录结构

```text
.
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/example/assessment/
│   │   ├── controller/              # REST API 控制器
│   │   ├── dto/                     # 请求和响应 DTO
│   │   ├── model/                   # 业务模型和枚举
│   │   ├── persistence/             # JPA 实体和 Repository
│   │   └── service/                 # 业务服务和报告生成
│   └── src/main/resources/
│       ├── application.yml          # 后端配置
│       └── templates/               # Excel / Word 导出模板
├── frontend/                        # Vue 前端
│   ├── public/                      # 静态图片资源
│   └── src/
│       ├── api/                     # API 请求封装
│       ├── components/              # 通用组件
│       ├── composables/             # 页面状态和业务逻辑
│       ├── styles/                  # 全局样式
│       └── views/                   # 管理员、教师、学生视图
├── scripts/                         # 辅助脚本
└── README.md
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
cd 工程教育评估平台
```

### 2. 准备数据库

后端默认连接 `engineering_assessment` 数据库，并启用 `createDatabaseIfNotExist=true`。建议通过环境变量配置数据库账号密码。

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

后端服务地址：

```text
http://localhost:8080
```

首次启动且数据库为空时，系统会自动初始化演示账号、学生名单、默认题库、试卷和部分示例成绩数据。

### 4. 启动前端

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

前端开发地址：

```text
http://localhost:5173
```

Vite 已配置开发代理，前端请求 `/api` 会转发到 `http://localhost:8080`。

## 演示账号

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | 管理教师账号、教学安排、学生信息、题库和报告 |
| 教师 | `teacher` | `123456` | 管理课程学生、题库、考试、成绩确认和报告 |
| 学生 | `student` | `123456` | 进入在线考试并提交答案 |
| 学生 | `student2` | `123456` | 进入在线考试并提交答案 |

> 当前版本采用轻量演示鉴权方案：登录后前端保存用户信息，接口通过 `X-Username` 和 `X-User-Role` 请求头进行角色校验。生产环境建议升级为密码加密存储和 JWT / Session 鉴权。

## 常用命令

前端：

```bash
cd frontend
npm install
npm run dev
npm run build
npm run preview
```

后端：

```bash
cd backend
mvn spring-boot:run
mvn test
mvn package
```

## 主要 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 用户登录 |
| `POST` | `/api/auth/change-password` | 修改密码 |
| `GET` | `/api/auth/demo-accounts` | 获取演示账号 |
| `GET` | `/api/questions` | 获取题库 |
| `POST` | `/api/questions` | 新增题目 |
| `PATCH` | `/api/questions/{questionId}` | 修改题目 |
| `DELETE` | `/api/questions/{questionId}` | 删除题目 |
| `POST` | `/api/questions/import` | 导入题目 |
| `GET` | `/api/students` | 获取学生列表 |
| `POST` | `/api/students` | 新增学生 |
| `POST` | `/api/students/import` | 导入课程学生名单 |
| `GET` | `/api/teacher-accounts` | 获取教师账号 |
| `POST` | `/api/teacher-accounts` | 新增教师账号 |
| `GET` | `/api/teaching-assignments` | 获取教学安排 |
| `POST` | `/api/teaching-assignments` | 新增教学安排 |
| `GET` | `/api/exams` | 获取试卷列表 |
| `POST` | `/api/exams` | 创建试卷 |
| `GET` | `/api/exams/{examId}/questions` | 获取试卷题目 |
| `POST` | `/api/exams/submit` | 提交试卷 |
| `GET` | `/api/results` | 获取成绩列表 |
| `PATCH` | `/api/results/score` | 确认或修改成绩 |
| `GET` | `/api/analysis` | 获取统计分析 |
| `GET` | `/api/reports/score-analysis.xls` | 导出成绩分析 Excel |
| `GET` | `/api/reports/objective-report.docx` | 导出课程目标评价 Word |

## 数据和模板说明

- `backend/src/main/resources/templates/score-analysis-template.xls`：成绩统计与分析 Excel 模板。
- `backend/src/main/resources/templates/objective-report-template.docx`：课程目标达成情况评价 Word 模板。
- 根目录中的 `.xls`、`.docx`、`.pptx` 文件是课程设计展示和样例材料，可根据需要保留或迁移到独立文档目录。
- `.gitignore` 已排除 `backend/target/`、`frontend/node_modules/`、`frontend/dist/` 和日志文件，上传 GitHub 时无需提交构建产物和本地依赖目录。

## 构建部署

前端生产构建：

```bash
cd frontend
npm run build
```

后端打包：

```bash
cd backend
mvn package
java -jar target/engineering-assessment-0.0.1-SNAPSHOT.jar
```

部署时请通过环境变量配置数据库连接，并根据实际域名、反向代理或静态资源服务方式调整前端 API 访问路径。

## 后续优化方向

- 引入密码加密、JWT / Session 和更完整的权限模型。
- 补充单元测试、接口测试和端到端测试。
- 增加导入模板下载、导入错误明细和批量数据校验。
- 支持更多课程、专业和评价指标的配置化管理。
- 增加 Docker Compose，简化 MySQL、后端和前端的一键启动。
