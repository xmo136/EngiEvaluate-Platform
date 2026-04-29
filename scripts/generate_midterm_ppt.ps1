$ErrorActionPreference = "Stop"

$scriptDir = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) { Join-Path (Get-Location).Path "scripts" } else { $PSScriptRoot }
$root = Split-Path -Parent $scriptDir
$output = Join-Path $root "工程教育评估平台_中期检查汇报.pptx"

function RgbColor([int]$r, [int]$g, [int]$b) {
    return $r + ($g * 256) + ($b * 65536)
}

$blue = RgbColor 37 99 235
$navy = RgbColor 15 23 42
$slate = RgbColor 71 85 105
$muted = RgbColor 100 116 139
$light = RgbColor 248 250 252
$line = RgbColor 226 232 240
$green = RgbColor 22 163 74
$orange = RgbColor 249 115 22
$purple = RgbColor 124 58 237
$cyan = RgbColor 8 145 178
$white = RgbColor 255 255 255

function Add-Text($slide, [string]$text, [double]$x, [double]$y, [double]$w, [double]$h, [int]$size = 20, [int]$color = $navy, [bool]$bold = $false) {
    $shape = $slide.Shapes.AddTextbox(1, $x, $y, $w, $h)
    $shape.TextFrame.TextRange.Text = $text
    $shape.TextFrame.TextRange.Font.NameFarEast = "微软雅黑"
    $shape.TextFrame.TextRange.Font.Name = "Microsoft YaHei"
    $shape.TextFrame.TextRange.Font.Size = $size
    $shape.TextFrame.TextRange.Font.Color.RGB = $color
    $shape.TextFrame.TextRange.Font.Bold = if ($bold) { -1 } else { 0 }
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    return $shape
}

function Add-Box($slide, [double]$x, [double]$y, [double]$w, [double]$h, [int]$fill, [int]$stroke = $line, [double]$radius = 0) {
    $shapeType = if ($radius -gt 0) { 5 } else { 1 }
    $shape = $slide.Shapes.AddShape($shapeType, $x, $y, $w, $h)
    $shape.Fill.ForeColor.RGB = $fill
    $shape.Line.ForeColor.RGB = $stroke
    $shape.Line.Weight = 1
    return $shape
}

function Add-Pill($slide, [string]$text, [double]$x, [double]$y, [double]$w, [int]$fill, [int]$color = $white) {
    $box = Add-Box $slide $x $y $w 28 $fill $fill 1
    $label = Add-Text $slide $text ($x + 10) ($y + 6) ($w - 20) 18 11 $color $true
    $label.TextFrame.TextRange.ParagraphFormat.Alignment = 2
    return $box
}

function Add-Title($slide, [string]$title, [string]$sub = "") {
    Add-Box $slide 0 0 960 540 $light $light | Out-Null
    Add-Text $slide $title 54 34 720 40 26 $navy $true | Out-Null
    if ($sub -ne "") {
        Add-Text $slide $sub 55 74 760 24 12 $muted $false | Out-Null
    }
    $bar = Add-Box $slide 54 105 852 2 $blue $blue
    return $bar
}

function Add-Bullets($slide, [string[]]$items, [double]$x, [double]$y, [double]$w, [double]$h, [int]$size = 18) {
    $text = ($items | ForEach-Object { "• $_" }) -join "`r"
    $shape = Add-Text $slide $text $x $y $w $h $size $slate $false
    $shape.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 8
    return $shape
}

function Add-TableLike($slide, [array]$rows, [double]$x, [double]$y, [double]$w, [double]$rowH, [array]$colWidths) {
    for ($i = 0; $i -lt $rows.Count; $i++) {
        $fill = if ($i -eq 0) { $navy } elseif ($i % 2 -eq 0) { RgbColor 241 245 249 } else { $white }
        $textColor = if ($i -eq 0) { $white } else { $navy }
        $cx = $x
        for ($j = 0; $j -lt $rows[$i].Count; $j++) {
            $cw = $colWidths[$j] * $w
            Add-Box $slide $cx ($y + $i * $rowH) $cw $rowH $fill $line | Out-Null
            Add-Text $slide $rows[$i][$j] ($cx + 8) ($y + $i * $rowH + 9) ($cw - 16) ($rowH - 14) 12 $textColor ($i -eq 0) | Out-Null
            $cx += $cw
        }
    }
}

$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = [Microsoft.Office.Core.MsoTriState]::msoTrue
$presentation = $ppt.Presentations.Add()
$presentation.PageSetup.SlideWidth = 960
$presentation.PageSetup.SlideHeight = 540

# 1
$slide = $presentation.Slides.Add(1, 12)
Add-Box $slide 0 0 960 540 (RgbColor 241 245 249) (RgbColor 241 245 249) | Out-Null
Add-Box $slide 0 0 960 540 (RgbColor 241 245 249) (RgbColor 241 245 249) | Out-Null
Add-Box $slide 54 62 104 104 $blue $blue 1 | Out-Null
Add-Text $slide "评" 83 82 48 58 42 $white $true | Out-Null
Add-Text $slide "工程教育评估平台" 190 68 650 56 38 $navy $true | Out-Null
Add-Text $slide "大项目作业中期检查汇报 | Vue + Spring Boot" 193 130 600 28 17 $slate $false | Out-Null
Add-Pill $slide "当前状态：可运行 Demo" 193 178 154 $green | Out-Null
Add-Pill $slide "核心完成度：约 85%" 360 178 150 $blue | Out-Null
Add-Pill $slide "小组三人协作" 523 178 122 $orange | Out-Null
Add-Box $slide 54 282 852 126 $white $line 1 | Out-Null
Add-Bullets $slide @(
    "围绕在线考试、自动判分、成绩统计、课程目标达成评价和报告导出搭建原型",
    "已补齐登录与权限模块，管理员、教师、学生进入系统后看到不同菜单和操作",
    "中期阶段优先打通核心业务闭环，使用内置演示数据保证 Demo 稳定展示"
) 84 306 800 82 18 | Out-Null
Add-Text $slide "汇报人：第 X 小组    成员：成员A / 成员B / 成员C" 55 475 800 22 13 $muted $false | Out-Null

# 2
$slide = $presentation.Slides.Add(2, 12)
Add-Title $slide "本次中期检查关注点" "围绕老师要求，重点说明需求、技术方案、完成情况和 Demo 完成度" | Out-Null
$cards = @(
    @("01", "需求分析", "已拆分为题库、考试、判分、统计、确认、报告六类核心需求"),
    @("02", "技术方案", "采用 Vue 3 + Spring Boot 前后端分离，REST API 连接业务流程"),
    @("03", "完成情况", "核心业务闭环已打通，当前完成度约 85%，具备可演示 Demo"),
    @("04", "后续安排", "数据库持久化、主观题评分优化、测试、部署和报告模板完善")
)
for ($i = 0; $i -lt $cards.Count; $i++) {
    $x = 72 + ($i % 2) * 420
    $y = 146 + [math]::Floor($i / 2) * 144
    Add-Box $slide $x $y 360 106 $white $line 1 | Out-Null
    Add-Text $slide $cards[$i][0] ($x + 22) ($y + 21) 52 34 24 $blue $true | Out-Null
    Add-Text $slide $cards[$i][1] ($x + 88) ($y + 18) 230 24 18 $navy $true | Out-Null
    Add-Text $slide $cards[$i][2] ($x + 88) ($y + 48) 245 42 13 $slate $false | Out-Null
}

# 3
$slide = $presentation.Slides.Add(3, 12)
Add-Title $slide "项目目标与应用场景" "面向工程教育认证中的课程目标达成评价和持续改进材料生成" | Out-Null
Add-Box $slide 70 138 370 286 $white $line 1 | Out-Null
Add-Text $slide "项目目标" 95 160 180 28 21 $navy $true | Out-Null
Add-Bullets $slide @(
    "帮助教师组织在线考试和题库管理",
    "将每道题绑定课程目标，支撑达成度统计",
    "自动生成成绩分析与评价报告，减少人工整理工作",
    "形成《考试-分析-报告-改进》的闭环"
) 98 205 300 160 17 | Out-Null
Add-Box $slide 520 138 370 286 $white $line 1 | Out-Null
Add-Text $slide "典型用户" 545 160 180 28 21 $navy $true | Out-Null
Add-Bullets $slide @(
    "教师：维护题库、发布考试、确认主观题分数",
    "学生：在线答题并提交试卷",
    "教学管理者：查看成绩统计和课程目标达成情况",
    "课程组：依据报告提出持续改进意见"
) 548 205 310 160 17 | Out-Null

# 4
$slide = $presentation.Slides.Add(4, 12)
Add-Title $slide "需求拆分与当前完成度" "当前核心业务需求约完成 85%，已具备中期演示条件" | Out-Null
$rows = @(
    @("模块", "需求内容", "当前状态", "完成度"),
    @("登录权限", "管理员、教师、学生角色区分", "已完成登录与菜单权限", "85%"),
    @("题库管理", "多题型维护，题目绑定课程目标", "已完成新增与展示", "80%"),
    @("在线考试", "学生答题、提交、考试时间展示", "核心流程可演示", "75%"),
    @("自动判分", "客观题自动判分，主观题建议分", "规则版 AI 建议分", "70%"),
    @("成绩统计", "成绩分段、题型分布、课程目标统计", "图表已展示", "80%"),
    @("教师确认", "教师修改并确认最终分数", "逐题确认已实现", "70%"),
    @("报告生成", "导出 Excel 和 Word 文件", "已能生成文件", "75%")
)
Add-TableLike $slide $rows 56 126 848 40 @(0.17, 0.38, 0.28, 0.17)
Add-Text $slide "说明：当前采用内置演示数据完成业务闭环，后续接入 MySQL 实现真实数据持久化。" 58 474 820 22 13 $muted $false | Out-Null

# 5
$slide = $presentation.Slides.Add(5, 12)
Add-Title $slide "技术方案与系统架构" "前后端分离，后端提供 REST API，前端负责交互与可视化" | Out-Null
$layers = @(
    @("前端展示层", "Vue 3 / Vite / ECharts / 角色菜单", $blue),
    @("接口服务层", "Spring Boot REST Controller / 请求头角色校验", $green),
    @("业务逻辑层", "登录、题库、考试、评分、统计、报告 Service", $orange),
    @("数据与文件层", "演示账号与数据 / 后续 MySQL / Apache POI 导出", $purple)
)
for ($i = 0; $i -lt $layers.Count; $i++) {
    $y = 138 + $i * 78
    Add-Box $slide 120 $y 720 54 $white $line 1 | Out-Null
    Add-Box $slide 120 $y 150 54 $layers[$i][2] $layers[$i][2] | Out-Null
    Add-Text $slide $layers[$i][0] 140 ($y + 16) 115 22 15 $white $true | Out-Null
    Add-Text $slide $layers[$i][1] 300 ($y + 15) 500 24 17 $navy $true | Out-Null
    if ($i -lt 3) {
        Add-Text $slide "↓" 466 ($y + 55) 28 20 18 $muted $true | Out-Null
    }
}

# 6
$slide = $presentation.Slides.Add(6, 12)
Add-Title $slide "核心业务流程" "从题库维护到报告生成，形成工程教育评价闭环" | Out-Null
$steps = @("题库维护", "在线考试", "自动判分", "教师确认", "统计分析", "报告导出")
$colors = @($blue, $green, $orange, $purple, $cyan, $navy)
for ($i = 0; $i -lt $steps.Count; $i++) {
    $x = 62 + $i * 145
    Add-Box $slide $x 212 112 70 $colors[$i] $colors[$i] 1 | Out-Null
    Add-Text $slide $steps[$i] ($x + 15) 237 82 22 16 $white $true | Out-Null
    if ($i -lt $steps.Count - 1) {
        Add-Text $slide "→" ($x + 118) 235 24 24 21 $muted $true | Out-Null
    }
}
Add-Box $slide 102 340 756 82 $white $line 1 | Out-Null
Add-Text $slide "业务闭环价值" 126 360 160 26 18 $navy $true | Out-Null
Add-Text $slide "每道题对应课程目标，考试结果可以直接反推课程目标达成情况，并自动生成持续改进材料。" 126 392 685 24 15 $slate $false | Out-Null

# 7
$slide = $presentation.Slides.Add(7, 12)
Add-Title $slide "当前 Demo 完成情况" "Demo 已覆盖主要页面，能现场跑通核心流程" | Out-Null
$demo = @(
    @("统计分析首页", "学生人数、题目数量、平均分、成绩分布、题型分布、课程目标达成"),
    @("题库管理", "展示多题型题目，支持新增题目并选择课程目标"),
    @("在线考试", "选择学生身份，完成答题并提交系统判分"),
    @("成绩确认", "查看答题记录，教师可逐题修改并确认最终分数"),
    @("报告导出", "导出统计小分及分析 Excel、课程目标达成评价 Word")
)
for ($i = 0; $i -lt $demo.Count; $i++) {
    $y = 135 + $i * 66
    Add-Box $slide 72 $y 816 48 $white $line 1 | Out-Null
    Add-Text $slide ($i + 1).ToString("00") 96 ($y + 13) 36 20 15 $blue $true | Out-Null
    Add-Text $slide $demo[$i][0] 150 ($y + 12) 150 22 15 $navy $true | Out-Null
    Add-Text $slide $demo[$i][1] 315 ($y + 13) 520 20 13 $slate $false | Out-Null
}

# 8
$slide = $presentation.Slides.Add(8, 12)
Add-Title $slide "现场演示路线" "建议按一个完整业务闭环演示，时间控制在 3-5 分钟" | Out-Null
Add-Box $slide 74 142 812 300 $white $line 1 | Out-Null
Add-Bullets $slide @(
    "进入统计分析页：说明系统当前已自动汇总考试情况",
    "进入题库管理页：展示不同题型和课程目标绑定关系",
    "进入在线考试页：选择学生并提交一份试卷",
    "进入成绩确认页：展示自动判分结果，修改主观题分数并确认",
    "进入报告导出页：导出 Excel 与 Word，说明可服务工程教育评价材料整理"
) 112 174 730 190 18 | Out-Null
Add-Text $slide "讲法要点：当前为中期原型，核心链路已跑通；数据采用演示数据，后续接入数据库。" 112 390 720 24 14 $orange $true | Out-Null

# 9
$slide = $presentation.Slides.Add(9, 12)
Add-Title $slide "小组三人分工" "三名成员围绕需求、后端、前端与测试协同推进" | Out-Null
$members = @(
    @("成员 A", "需求分析 / 课程目标评价逻辑", "整理工程教育评估需求，拆分模块，设计课程目标与题目关联规则"),
    @("成员 B", "后端接口 / 自动判分 / 报告导出", "实现 Spring Boot 接口、评分逻辑、统计服务、Excel/Word 文件生成"),
    @("成员 C", "前端页面 / 图表展示 / Demo 集成", "实现 Vue 页面、ECharts 图表、考试交互、成绩确认和报告导出入口")
)
for ($i = 0; $i -lt $members.Count; $i++) {
    $x = 66 + $i * 298
    Add-Box $slide $x 150 256 250 $white $line 1 | Out-Null
    Add-Pill $slide $members[$i][0] ($x + 24) 176 86 $blue | Out-Null
    Add-Text $slide $members[$i][1] ($x + 24) 222 205 46 18 $navy $true | Out-Null
    Add-Text $slide $members[$i][2] ($x + 24) 292 205 74 14 $slate $false | Out-Null
}

# 10
$slide = $presentation.Slides.Add(10, 12)
Add-Title $slide "完成度评估" "当前处于《核心功能可演示，工程化能力待完善》的阶段" | Out-Null
Add-Box $slide 86 158 330 220 $white $line 1 | Out-Null
Add-Text $slide "总体完成度" 118 188 160 28 20 $navy $true | Out-Null
Add-Text $slide "85%" 118 230 150 64 48 $blue $true | Out-Null
Add-Text $slide "核心业务闭环已完成，能够支撑中期检查 Demo。" 120 314 240 42 15 $slate $false | Out-Null
Add-Box $slide 500 150 360 242 $white $line 1 | Out-Null
Add-Text $slide "已完成" 530 174 120 24 18 $green $true | Out-Null
Add-Bullets $slide @("前后端项目结构", "登录与角色权限", "题库与考试流程", "自动判分与教师确认", "统计图表", "Excel/Word 导出") 532 212 260 120 15 | Out-Null
Add-Text $slide "待完善" 530 344 120 24 18 $orange $true | Out-Null
Add-Text $slide "数据库持久化、正式认证、异常处理、测试部署、报告模板美化" 532 374 300 22 14 $slate $false | Out-Null

# 11
$slide = $presentation.Slides.Add(11, 12)
Add-Title $slide "后续开发安排" "把当前原型推进到可交付版本" | Out-Null
$plan = @(
    @("第 1 阶段", "接入 MySQL", "设计学生、题库、试卷、答题记录、成绩表，替换内置演示数据"),
    @("第 2 阶段", "登录与权限", "已完成演示版角色权限，后续升级为数据库权限表和会话认证"),
    @("第 3 阶段", "评分与报告优化", "完善主观题评分规则，优化 Excel/Word 模板和持续改进意见生成"),
    @("第 4 阶段", "测试与答辩", "完成接口测试、前端联调、部署说明、答辩演示材料")
)
for ($i = 0; $i -lt $plan.Count; $i++) {
    $y = 136 + $i * 74
    Add-Box $slide 86 $y 788 54 $white $line 1 | Out-Null
    Add-Text $slide $plan[$i][0] 110 ($y + 16) 90 20 14 $blue $true | Out-Null
    Add-Text $slide $plan[$i][1] 230 ($y + 14) 130 22 16 $navy $true | Out-Null
    Add-Text $slide $plan[$i][2] 386 ($y + 15) 440 20 13 $slate $false | Out-Null
}

# 12
$slide = $presentation.Slides.Add(12, 12)
Add-Title $slide "登录与权限模块" "已完成第 2 阶段要求：区分教师、学生、管理员角色，限制不同页面和操作权限" | Out-Null
$rows = @(
    @("角色", "可访问页面", "主要操作权限"),
    @("管理员", "统计、题库、考试、成绩、报告", "查看全量数据，维护题库，代学生提交演示试卷，确认分数，导出报告"),
    @("教师", "统计、题库、成绩、报告", "维护题库，查看统计，修改并确认最终得分，导出评价材料"),
    @("学生", "在线考试", "只进入考试页面，按绑定学生身份答题并提交")
)
Add-TableLike $slide $rows 68 145 824 62 @(0.16, 0.34, 0.50)
Add-Text $slide "实现方式：前端根据登录返回的 allowedViews 过滤菜单；后端通过 X-User-Role 请求头对接口做角色校验。" 70 435 820 22 13 $muted $false | Out-Null

# 13
$slide = $presentation.Slides.Add(13, 12)
Add-Title $slide "演示账号与登录流程" "现场演示时可快速切换三类角色，体现权限差异" | Out-Null
$rows = @(
    @("角色", "用户名", "密码", "演示重点"),
    @("管理员", "admin", "123456", "全功能菜单，验证系统管理视角"),
    @("教师", "teacher", "123456", "题库维护、成绩确认、报告导出"),
    @("学生", "student", "123456", "只显示在线考试，绑定张三"),
    @("学生", "student2", "123456", "只显示在线考试，绑定李四")
)
Add-TableLike $slide $rows 72 138 816 54 @(0.18, 0.18, 0.18, 0.46)
Add-Bullets $slide @(
    "登录页展示演示账号，点击账号即可填入用户名和密码",
    "登录成功后左侧显示当前用户姓名与角色",
    "退出登录后回到登录页，便于现场切换角色演示"
) 94 420 760 70 14 | Out-Null

# 14
$slide = $presentation.Slides.Add(14, 12)
Add-Title $slide "演示数据规模" "当前使用内置演示数据支撑中期展示，保证老师检查时能稳定跑通" | Out-Null
$metrics = @(
    @("10", "学生样本", "覆盖 2018级软件工程 4 个班级"),
    @("5", "试题样本", "覆盖 5 种题型，总分 100 分"),
    @("9", "成绩记录", "形成较完整的成绩分段分布"),
    @("4", "课程目标", "每道题绑定课程目标 1-4")
)
for ($i = 0; $i -lt $metrics.Count; $i++) {
    $x = 72 + ($i % 2) * 420
    $y = 145 + [math]::Floor($i / 2) * 130
    Add-Box $slide $x $y 360 92 $white $line 1 | Out-Null
    Add-Text $slide $metrics[$i][0] ($x + 24) ($y + 15) 70 44 34 $blue $true | Out-Null
    Add-Text $slide $metrics[$i][1] ($x + 108) ($y + 18) 170 24 18 $navy $true | Out-Null
    Add-Text $slide $metrics[$i][2] ($x + 108) ($y + 50) 220 20 13 $slate $false | Out-Null
}
Add-Text $slide "说明：演示数据不是最终业务数据，后续接入 MySQL 后会改为真实学生、题库和考试记录。" 74 434 812 22 13 $orange $true | Out-Null

# 15
$slide = $presentation.Slides.Add(15, 12)
Add-Title $slide "接口与权限控制" "后端已对关键接口做角色校验，避免只停留在前端菜单限制" | Out-Null
$rows = @(
    @("接口", "用途", "允许角色"),
    @("POST /api/auth/login", "登录并返回角色、姓名、可访问菜单", "公开"),
    @("GET /api/questions", "获取题库", "管理员 / 教师 / 学生"),
    @("POST /api/questions", "新增试题", "管理员 / 教师"),
    @("POST /api/exams/submit", "提交试卷并自动判分", "管理员 / 学生"),
    @("PATCH /api/results/score", "教师确认或修改分数", "管理员 / 教师"),
    @("GET /api/reports/*.docx/xlsx", "导出报告文件", "管理员 / 教师")
)
Add-TableLike $slide $rows 54 128 852 42 @(0.34, 0.38, 0.28)
Add-Text $slide "中期版本采用轻量权限方案，后续可升级为 Spring Security + JWT 或 Session。" 58 452 820 22 13 $muted $false | Out-Null

# 16
$slide = $presentation.Slides.Add(16, 12)
Add-Title $slide "自动判分与教师确认逻辑" "客观题自动判分，主观题给出建议分，最终成绩由教师确认" | Out-Null
Add-Box $slide 70 148 370 250 $white $line 1 | Out-Null
Add-Text $slide "自动判分规则" 98 172 180 26 20 $navy $true | Out-Null
Add-Bullets $slide @(
    "选择题、填空题：与标准答案一致得满分，否则为 0 分",
    "简答、设计、综合分析题：按关键词覆盖情况计算建议分",
    "每道题按课程目标归集得分，参与目标达成统计"
) 100 215 300 105 16 | Out-Null
Add-Box $slide 520 148 370 250 $white $line 1 | Out-Null
Add-Text $slide "教师确认机制" 548 172 180 26 20 $navy $true | Out-Null
Add-Bullets $slide @(
    "成绩确认页展示每名学生逐题答题记录",
    "教师可修改主观题最终得分",
    "修改后自动重算总分和课程目标得分"
) 550 215 300 105 16 | Out-Null
Add-Text $slide "这样既保留自动化效率，也避免主观题完全由系统决定，符合教学评价场景。" 76 440 800 24 14 $orange $true | Out-Null

# 17
$slide = $presentation.Slides.Add(17, 12)
Add-Title $slide "报告生成与工程教育材料" "平台自动生成 Excel 与 Word，减少课程目标评价材料整理工作" | Out-Null
$reportCards = @(
    @("统计小分及分析 Excel", "包含学生成绩、成绩等级、课程目标平均得分、题型分布等工作表", $green),
    @("课程目标达成 Word", "包含学生人数、试题数量、平均分、课程目标达成情况和持续改进建议", $blue),
    @("持续改进建议", "根据最低课程目标与平均分生成改进建议，教师可再修改完善", $orange)
)
for ($i = 0; $i -lt $reportCards.Count; $i++) {
    $y = 145 + $i * 92
    Add-Box $slide 88 $y 784 62 $white $line 1 | Out-Null
    Add-Box $slide 88 $y 12 62 $reportCards[$i][2] $reportCards[$i][2] | Out-Null
    Add-Text $slide $reportCards[$i][0] 124 ($y + 13) 210 24 16 $navy $true | Out-Null
    Add-Text $slide $reportCards[$i][1] 350 ($y + 15) 470 22 13 $slate $false | Out-Null
}
Add-Text $slide "对应老师要求中的自动生成工程教育评估所需表和 Word 文件。" 90 440 760 22 14 $muted $false | Out-Null

# 18
$slide = $presentation.Slides.Add(18, 12)
Add-Title $slide "问题风险与应对" "中期版本已能演示，后续重点处理工程化和真实性问题" | Out-Null
$rows = @(
    @("风险/不足", "当前状态", "后续应对"),
    @("数据未持久化", "使用内置演示数据", "接入 MySQL，设计学生、题库、成绩、试卷表"),
    @("权限仍是轻量方案", "请求头角色校验", "升级 Spring Security + JWT/Session"),
    @("主观题评分较简单", "关键词匹配建议分", "加入评分细则、教师复核记录和 AI 辅助解释"),
    @("测试覆盖不足", "以手动演示为主", "补接口测试、导出测试和前端流程测试")
)
Add-TableLike $slide $rows 64 138 832 58 @(0.24, 0.30, 0.46)

# 19
$slide = $presentation.Slides.Add(19, 12)
Add-Box $slide 0 0 960 540 (RgbColor 241 245 249) (RgbColor 241 245 249) | Out-Null
Add-Text $slide "阶段总结" 80 78 360 48 34 $navy $true | Out-Null
Add-Box $slide 80 144 800 2 $blue $blue | Out-Null
Add-Bullets $slide @(
    "当前项目已完成登录权限、题库、考试、判分、成绩确认、统计分析和报告导出的核心业务闭环",
    "技术方案采用 Vue + Spring Boot，结构清晰，便于后续扩展数据库、权限认证和报告模板",
    "Demo 完成度可以支撑中期检查，后续重点转向数据持久化、评分优化、测试和最终交付文档"
) 92 190 760 130 20 | Out-Null
Add-Box $slide 190 374 580 58 $blue $blue 1 | Out-Null
Add-Text $slide "请老师批评指正" 365 390 230 28 22 $white $true | Out-Null

if (Test-Path $output) {
    Remove-Item -LiteralPath $output -Force
}
$presentation.SaveAs($output)
$presentation.Close()
$ppt.Quit()

[System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null

Write-Output $output


