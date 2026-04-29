package com.example.assessment.service;

import com.example.assessment.dto.ExamPaperSaveRequest;
import com.example.assessment.dto.ExamSubmitRequest;
import com.example.assessment.model.CourseObjective;
import com.example.assessment.model.ExamPaperType;
import com.example.assessment.model.QuestionType;
import com.example.assessment.model.UserRole;
import com.example.assessment.persistence.entity.ExamPaperEntity;
import com.example.assessment.persistence.entity.QuestionEntity;
import com.example.assessment.persistence.entity.StudentEntity;
import com.example.assessment.persistence.entity.TeachingAssignmentEntity;
import com.example.assessment.persistence.entity.UserAccountEntity;
import com.example.assessment.persistence.repository.ExamPaperRepository;
import com.example.assessment.persistence.repository.ExamResultRepository;
import com.example.assessment.persistence.repository.QuestionRepository;
import com.example.assessment.persistence.repository.StudentRepository;
import com.example.assessment.persistence.repository.TeachingAssignmentRepository;
import com.example.assessment.persistence.repository.UserAccountRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final ExamPaperRepository examPaperRepository;
    private final QuestionRepository questionRepository;
    private final ExamResultRepository examResultRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final AssessmentService assessmentService;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(StudentRepository studentRepository,
                               UserAccountRepository userAccountRepository,
                               ExamPaperRepository examPaperRepository,
                               QuestionRepository questionRepository,
                               ExamResultRepository examResultRepository,
                               TeachingAssignmentRepository teachingAssignmentRepository,
                               AssessmentService assessmentService,
                               JdbcTemplate jdbcTemplate) {
        this.studentRepository = studentRepository;
        this.userAccountRepository = userAccountRepository;
        this.examPaperRepository = examPaperRepository;
        this.questionRepository = questionRepository;
        this.examResultRepository = examResultRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.assessmentService = assessmentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ExamPaperEntity questionBank = ensureQuestionBank();
        ensureStudents();
        migrateLegacyTeachingAssignmentLinks();
        assessmentService.synchronizeExistingStudentData();
        ensureQuestions(questionBank);
        ensureAccounts();
        TeachingAssignmentEntity assignment = ensureTeachingAssignment();
        assessmentService.synchronizeExistingStudentData();
        ExamPaperEntity exam = ensureDemoExam(questionBank, assignment);
        ensureResults(exam);
    }

    private ExamPaperEntity ensureQuestionBank() {
        ExamPaperEntity paper = examPaperRepository.findByCode(AssessmentService.QUESTION_BANK_CODE)
                .orElseGet(() -> examPaperRepository.findByCode("SEPM-2018").orElseGet(ExamPaperEntity::new));
        paper.setCode(AssessmentService.QUESTION_BANK_CODE);
        paper.setCourseName("软件项目策划与管理");
        paper.setPaperName("课程题库");
        paper.setDescription("系统默认题库");
        paper.setPaperType(ExamPaperType.QUESTION_BANK);
        paper.setDurationMinutes(90);
        paper.setStartTime(null);
        paper.setTeachingAssignment(null);
        paper.setActive(true);
        return examPaperRepository.save(paper);
    }

    private void ensureStudents() {
        if (studentRepository.count() > 0) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("templates/score-analysis-template.xls");
        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 3; rowIndex <= 101; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String studentNo = cellText(row, 0);
                String name = cellText(row, 1);
                String className = cellText(row, 3);
                if (studentNo.isBlank() || name.isBlank()) {
                    continue;
                }
                StudentEntity student = new StudentEntity();
                student.setStudentNo(studentNo);
                student.setName(name);
                student.setClassName(className);
                studentRepository.save(student);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("读取示例学生数据失败", ex);
        }
    }

    private void ensureQuestions(ExamPaperEntity paper) {
        if (questionRepository.count() > 0) {
            return;
        }
        saveQuestion(paper, 1, "下列哪一项属于项目范围管理的主要产出？",
                QuestionType.SINGLE_CHOICE, CourseObjective.OBJECTIVE_1, 10,
                List.of("A. 工作分解结构", "B. 数据库索引", "C. 操作系统内核", "D. UI 配色表"),
                "A", "考查项目范围管理的基础概念。");
        saveQuestion(paper, 2, "风险应对策略包括规避、转移、减轻和____。",
                QuestionType.FILL_BLANK, CourseObjective.OBJECTIVE_2, 10,
                List.of(), "接受", "考查风险管理基本术语。");
        saveQuestion(paper, 3, "简述挣值管理中 PV、EV、AC 三个指标的含义。",
                QuestionType.SHORT_ANSWER, CourseObjective.OBJECTIVE_2, 20,
                List.of(), "PV EV AC 计划价值 挣值 实际成本", "考查项目成本与进度综合控制。");
        saveQuestion(paper, 4, "为一个在线考试系统设计主要模块并说明模块关系。",
                QuestionType.DESIGN, CourseObjective.OBJECTIVE_3, 30,
                List.of(), "用户 题库 考试 判分 统计 报告 权限 数据流", "考查系统分析与设计能力。");
        saveQuestion(paper, 5, "结合某项目延期案例，分析原因并给出改进措施。",
                QuestionType.COMPREHENSIVE, CourseObjective.OBJECTIVE_4, 30,
                List.of(), "需求 计划 风险 沟通 资源 监控 改进", "考查综合分析和持续改进能力。");
    }

    private void ensureAccounts() {
        if (userAccountRepository.count() > 0) {
            return;
        }
        List<StudentEntity> students = studentRepository.findAllByOrderByIdAsc();
        createAccount("admin", "123456", "管理员", UserRole.ADMIN, null);
        createAccount("teacher", "123456", "课程教师", UserRole.TEACHER, null);
        createAccount("student", "123456", students.isEmpty() ? "学生1" : students.get(0).getName(),
                UserRole.STUDENT, students.isEmpty() ? null : students.get(0));
        createAccount("student2", "123456", students.size() < 2 ? "学生2" : students.get(1).getName(),
                UserRole.STUDENT, students.size() < 2 ? null : students.get(1));
    }

    private ExamPaperEntity ensureDemoExam(ExamPaperEntity questionBank, TeachingAssignmentEntity assignment) {
        if (assignment == null) {
            return null;
        }
        List<ExamPaperEntity> existingExams = examPaperRepository.findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(
                ExamPaperType.EXAM,
                assignment.getId()
        );
        if (!existingExams.isEmpty()) {
            return existingExams.get(0);
        }
        UserAccountEntity teacher = assignment.getTeacherAccount();
        if (teacher == null) {
            return null;
        }
        List<Long> questionIds = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(questionBank.getId()).stream()
                .map(QuestionEntity::getId)
                .toList();
        if (questionIds.isEmpty()) {
            return null;
        }
        assessmentService.createExam(teacher, new ExamPaperSaveRequest(
                assignment.getId(),
                assignment.getCourseName() + " - 期中考试",
                "演示考试，支持组卷、答题与阅卷。",
                LocalDateTime.now().minusHours(1),
                120,
                questionIds
        ));
        return examPaperRepository.findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(
                ExamPaperType.EXAM,
                assignment.getId()
        ).stream().findFirst().orElse(null);
    }

    private void ensureResults(ExamPaperEntity exam) {
        if (exam == null || examResultRepository.count() > 0) {
            return;
        }
        List<QuestionEntity> examQuestions = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(exam.getId());
        List<StudentEntity> students = studentRepository.findAllByTeachingAssignmentsIdOrderByIdAsc(exam.getTeachingAssignment().getId());
        for (int index = 0; index < Math.min(6, students.size()); index++) {
            StudentEntity student = students.get(index);
            assessmentService.submit(new ExamSubmitRequest(exam.getId(), student.getId(), buildDemoAnswers(examQuestions, index)));
        }
    }

    private Map<Long, String> buildDemoAnswers(List<QuestionEntity> questions, int studentIndex) {
        Map<Long, String> answers = new LinkedHashMap<>();
        List<String> subjectiveSamples = List.of(
                "需要围绕范围、进度、成本和风险建立统一跟踪机制。",
                "应明确里程碑、资源分配，并持续复盘偏差。",
                "通过需求澄清、风险识别和沟通机制提升交付质量。"
        );

        for (int index = 0; index < questions.size(); index++) {
            QuestionEntity question = questions.get(index);
            String answer;
            if (question.getType() == QuestionType.SINGLE_CHOICE && question.getOptions() != null && !question.getOptions().isEmpty()) {
                if ((studentIndex + index) % 4 == 0 && question.getOptions().size() > 1) {
                    answer = optionKey(question.getOptions().get(1));
                } else {
                    answer = question.getAnswer();
                }
            } else if (question.getType() == QuestionType.FILL_BLANK) {
                answer = studentIndex % 3 == 0 ? question.getAnswer() : "接受";
            } else {
                answer = subjectiveSamples.get((studentIndex + index) % subjectiveSamples.size());
            }
            answers.put(question.getId(), answer);
        }
        return answers;
    }

    private String optionKey(String option) {
        if (option == null || option.isBlank()) {
            return "";
        }
        return option.substring(0, 1);
    }

    private void saveQuestion(ExamPaperEntity paper,
                              int sortOrder,
                              String title,
                              QuestionType type,
                              CourseObjective objective,
                              int score,
                              List<String> options,
                              String answer,
                              String analysis) {
        QuestionEntity question = new QuestionEntity();
        question.setPaper(paper);
        question.setCourseName(paper.getCourseName());
        question.setTitle(title);
        question.setType(type);
        question.setObjective(objective);
        question.setScore(score);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setAnalysis(analysis);
        question.setSortOrder(sortOrder);
        questionRepository.save(question);
    }

    private void createAccount(String username,
                               String password,
                               String displayName,
                               UserRole role,
                               StudentEntity student) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUsername(username);
        account.setPassword(password);
        account.setDisplayName(displayName);
        account.setRole(role);
        account.setStudent(student);
        account.setPasswordChangeRequired(false);
        userAccountRepository.save(account);
    }

    private TeachingAssignmentEntity ensureTeachingAssignment() {
        UserAccountEntity teacher = userAccountRepository.findByUsername("teacher")
                .orElseGet(() -> userAccountRepository.findAllByRoleOrderByIdAsc(UserRole.TEACHER).stream().findFirst().orElse(null));
        if (teacher != null) {
            List<TeachingAssignmentEntity> ownAssignments = teachingAssignmentRepository.findAllByTeacherAccountUsernameOrderByIdAsc(teacher.getUsername());
            if (!ownAssignments.isEmpty()) {
                return ownAssignments.get(0);
            }
        }
        List<TeachingAssignmentEntity> allAssignments = teachingAssignmentRepository.findAllByOrderByIdAsc();
        if (!allAssignments.isEmpty() && teacher == null) {
            return allAssignments.get(0);
        }
        if (teacher == null) {
            return null;
        }
        TeachingAssignmentEntity entity = new TeachingAssignmentEntity();
        entity.setCourseName("软件项目策划与管理");
        entity.setClassName("软件工程2018级");
        entity.setTeacherAccount(teacher);
        return teachingAssignmentRepository.save(entity);
    }

    private void migrateLegacyTeachingAssignmentLinks() {
        if (!tableExists("teaching_assignment_students") || !columnExists("students", "teaching_assignment_id")) {
            return;
        }
        jdbcTemplate.update("""
                insert into teaching_assignment_students (student_id, teaching_assignment_id)
                select s.id, s.teaching_assignment_id
                from students s
                where s.teaching_assignment_id is not null
                  and not exists (
                    select 1
                    from teaching_assignment_students tas
                    where tas.student_id = s.id
                      and tas.teaching_assignment_id = s.teaching_assignment_id
                  )
                """);
    }

    private boolean tableExists(String tableName) {
        return !jdbcTemplate.queryForList("SHOW TABLES LIKE ?", tableName).isEmpty();
    }

    private boolean columnExists(String tableName, String columnName) {
        return !jdbcTemplate.queryForList("SHOW COLUMNS FROM " + tableName + " LIKE ?", columnName).isEmpty();
    }

    private String cellText(Row row, int cellIndex) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case FORMULA -> cell.getCellFormula();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
