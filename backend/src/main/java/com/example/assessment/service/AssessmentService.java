package com.example.assessment.service;

import com.example.assessment.dto.AnalysisSummary;
import com.example.assessment.dto.ExamPaperSaveRequest;
import com.example.assessment.dto.ExamSubmitRequest;
import com.example.assessment.dto.RegularGradeSaveRequest;
import com.example.assessment.dto.ScoreConfirmRequest;
import com.example.assessment.dto.StudentCreateRequest;
import com.example.assessment.dto.StudentUpdateRequest;
import com.example.assessment.dto.TeacherAccountCreateRequest;
import com.example.assessment.dto.TeacherAccountUpdateRequest;
import com.example.assessment.dto.TeachingAssignmentCreateRequest;
import com.example.assessment.dto.TeachingAssignmentUpdateRequest;
import com.example.assessment.model.AnswerRecord;
import com.example.assessment.model.CourseObjective;
import com.example.assessment.model.ExamPaperSummary;
import com.example.assessment.model.ExamPaperType;
import com.example.assessment.model.ExamResult;
import com.example.assessment.model.MockExamResultGenerationResult;
import com.example.assessment.model.QuestionImportResult;
import com.example.assessment.model.ProfessionalClassImportResult;
import com.example.assessment.model.ProfessionalClassOption;
import com.example.assessment.model.Question;
import com.example.assessment.model.QuestionType;
import com.example.assessment.model.RegularGrade;
import com.example.assessment.model.Student;
import com.example.assessment.model.StudentImportResult;
import com.example.assessment.model.TeacherAccountOption;
import com.example.assessment.model.TeachingAssignment;
import com.example.assessment.model.UserRole;
import com.example.assessment.persistence.entity.AnswerRecordEntity;
import com.example.assessment.persistence.entity.ExamPaperEntity;
import com.example.assessment.persistence.entity.ExamResultEntity;
import com.example.assessment.persistence.entity.ProfessionalClassEntity;
import com.example.assessment.persistence.entity.RegularGradeEntity;
import com.example.assessment.persistence.entity.QuestionEntity;
import com.example.assessment.persistence.entity.StudentEntity;
import com.example.assessment.persistence.entity.TeachingAssignmentEntity;
import com.example.assessment.persistence.entity.UserAccountEntity;
import com.example.assessment.persistence.repository.AnswerRecordRepository;
import com.example.assessment.persistence.repository.ExamPaperRepository;
import com.example.assessment.persistence.repository.ExamResultRepository;
import com.example.assessment.persistence.repository.ProfessionalClassRepository;
import com.example.assessment.persistence.repository.QuestionRepository;
import com.example.assessment.persistence.repository.RegularGradeRepository;
import com.example.assessment.persistence.repository.StudentRepository;
import com.example.assessment.persistence.repository.TeachingAssignmentRepository;
import com.example.assessment.persistence.repository.UserAccountRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssessmentService {
    public static final String QUESTION_BANK_CODE = "QUESTION_BANK";
    private static final String DEFAULT_STUDENT_PASSWORD = "123456";
    private static final String DEFAULT_TEACHER_PASSWORD = "123456";

    private final AnswerRecordRepository answerRecordRepository;
    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamResultRepository examResultRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ProfessionalClassRepository professionalClassRepository;
    private final UserAccountRepository userAccountRepository;
    private final RegularGradeRepository regularGradeRepository;
    private final OpenAiGradingService openAiGradingService;

    public AssessmentService(AnswerRecordRepository answerRecordRepository,
                             StudentRepository studentRepository,
                             QuestionRepository questionRepository,
                             ExamPaperRepository examPaperRepository,
                             ExamResultRepository examResultRepository,
                             TeachingAssignmentRepository teachingAssignmentRepository,
                             ProfessionalClassRepository professionalClassRepository,
                             UserAccountRepository userAccountRepository,
                             RegularGradeRepository regularGradeRepository,
                             OpenAiGradingService openAiGradingService) {
        this.answerRecordRepository = answerRecordRepository;
        this.studentRepository = studentRepository;
        this.questionRepository = questionRepository;
        this.examPaperRepository = examPaperRepository;
        this.examResultRepository = examResultRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.professionalClassRepository = professionalClassRepository;
        this.userAccountRepository = userAccountRepository;
        this.regularGradeRepository = regularGradeRepository;
        this.openAiGradingService = openAiGradingService;
    }

    @Transactional(readOnly = true)
    public List<Question> listQuestions() {
        ExamPaperEntity paper = questionBankPaper();
        return questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(paper.getId()).stream()
                .map(this::toQuestion)
                .toList();
    }

    @Transactional
    public Question addQuestion(Question question) {
        ExamPaperEntity paper = questionBankPaper();
        QuestionEntity entity = new QuestionEntity();
        applyQuestion(entity, paper, question);
        entity.setSortOrder(questionRepository.findMaxSortOrderByPaperId(paper.getId()) + 1);
        return toQuestion(questionRepository.save(entity));
    }

    @Transactional
    public Question updateQuestion(Long questionId, Question question) {
        if (questionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择题目");
        }
        ExamPaperEntity paper = questionBankPaper();
        QuestionEntity entity = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
        ensureQuestionBelongsToPaper(entity, paper);
        applyQuestion(entity, paper, question);
        return toQuestion(questionRepository.save(entity));
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        if (questionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择题目");
        }
        QuestionEntity entity = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
        ensureQuestionBelongsToPaper(entity, questionBankPaper());
        if (answerRecordRepository.countByQuestionId(questionId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该题目已有学生作答记录，无法删除");
        }
        questionRepository.delete(entity);
    }

    @Transactional
    public QuestionImportResult importQuestions(MultipartFile file) {
        List<QuestionImportRow> rows = parseQuestionImportRows(file);
        if (rows.isEmpty()) {
            return new QuestionImportResult(0, 0, List.of("未在导入文件中读取到有效题目"));
        }

        ExamPaperEntity paper = questionBankPaper();
        int nextSortOrder = questionRepository.findMaxSortOrderByPaperId(paper.getId()) + 1;
        int createdCount = 0;
        int skippedCount = 0;
        List<String> messages = new ArrayList<>();

        for (int index = 0; index < rows.size(); index++) {
            QuestionImportRow row = rows.get(index);
            try {
                QuestionEntity entity = new QuestionEntity();
                entity.setPaper(paper);
                entity.setCourseName(isBlank(row.courseName()) ? paper.getCourseName() : row.courseName().trim());
                entity.setTitle(requireText(row.title(), "题干"));
                entity.setType(parseQuestionType(row.type()));
                entity.setObjective(parseObjective(row.objective()));
                entity.setScore(parseScore(row.score()));
                entity.setOptions(parseOptions(row.options()));
                entity.setAnswer(requireText(row.answer(), "标准答案"));
                entity.setAnalysis(isBlank(row.analysis()) ? "" : row.analysis().trim());
                entity.setSortOrder(nextSortOrder++);
                validateQuestionImport(entity);
                questionRepository.save(entity);
                createdCount++;
            } catch (ResponseStatusException ex) {
                skippedCount++;
                messages.add("第 " + (index + 2) + " 行跳过：" + ex.getReason());
            }
        }

        messages.add("成功导入 " + createdCount + " 道题，跳过 " + skippedCount + " 行");
        return new QuestionImportResult(createdCount, skippedCount, messages);
    }

    @Transactional(readOnly = true)
    public List<TeachingAssignment> listTeachingAssignments(UserAccountEntity actor) {
        List<TeachingAssignmentEntity> entities = actor.getRole() == UserRole.TEACHER
                ? teachingAssignmentRepository.findAllByTeacherAccountUsernameOrderByIdAsc(actor.getUsername())
                : teachingAssignmentRepository.findAllByOrderByIdAsc();
        return entities.stream()
                .map(this::toTeachingAssignment)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfessionalClassOption> listProfessionalClasses() {
        return professionalClassRepository.findAllByOrderByNameAsc().stream()
                .map(item -> new ProfessionalClassOption(
                        item.getId(),
                        item.getName(),
                        studentRepository.countByProfessionalClassId(item.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherAccountOption> listTeacherAccounts() {
        return userAccountRepository.findAllByRoleOrderByIdAsc(UserRole.TEACHER).stream()
                .map(account -> new TeacherAccountOption(account.getId(), account.getUsername(), account.getDisplayName()))
                .toList();
    }

    @Transactional
    public TeacherAccountOption addTeacherAccount(TeacherAccountCreateRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.displayName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师账号和姓名不能为空");
        }
        String username = request.username().trim();
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setUsername(username);
        account.setDisplayName(request.displayName().trim());
        account.setPassword(isBlank(request.password()) ? DEFAULT_TEACHER_PASSWORD : request.password().trim());
        account.setRole(UserRole.TEACHER);
        account.setStudent(null);
        account.setPasswordChangeRequired(false);
        UserAccountEntity saved = userAccountRepository.save(account);
        return new TeacherAccountOption(saved.getId(), saved.getUsername(), saved.getDisplayName());
    }

    @Transactional
    public TeacherAccountOption updateTeacherAccount(Long teacherAccountId, TeacherAccountUpdateRequest request) {
        if (teacherAccountId == null || request == null || isBlank(request.username()) || isBlank(request.displayName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师账号和姓名不能为空");
        }
        UserAccountEntity account = userAccountRepository.findById(teacherAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教师账号不存在"));
        if (account.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选账号不是教师角色");
        }
        String username = request.username().trim();
        userAccountRepository.findByUsername(username)
                .filter(other -> !Objects.equals(other.getId(), teacherAccountId))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
                });

        account.setUsername(username);
        account.setDisplayName(request.displayName().trim());
        if (!isBlank(request.password())) {
            account.setPassword(request.password().trim());
        }
        UserAccountEntity saved = userAccountRepository.save(account);
        return new TeacherAccountOption(saved.getId(), saved.getUsername(), saved.getDisplayName());
    }

    @Transactional
    public void deleteTeacherAccount(Long teacherAccountId) {
        if (teacherAccountId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择教师账号");
        }
        UserAccountEntity account = userAccountRepository.findById(teacherAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教师账号不存在"));
        if (account.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选账号不是教师角色");
        }
        if (teachingAssignmentRepository.countByTeacherAccountId(teacherAccountId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该教师已绑定教学班，无法删除");
        }
        userAccountRepository.delete(account);
    }

    @Transactional
    public TeachingAssignment addTeachingAssignment(TeachingAssignmentCreateRequest request) {
        if (request == null || isBlank(request.courseName()) || isBlank(request.className()) || request.teacherAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程名称、教学班和任课老师不能为空");
        }
        UserAccountEntity teacher = userAccountRepository.findById(request.teacherAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师账号不存在"));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选账号不是教师角色");
        }
        Set<ProfessionalClassEntity> professionalClasses = loadProfessionalClasses(request.professionalClassIds());

        TeachingAssignmentEntity entity = new TeachingAssignmentEntity();
        entity.setCourseName(request.courseName().trim());
        entity.setClassName(request.className().trim());
        entity.setCourseCode(request.courseCode());
        entity.setCreditHours(request.creditHours());
        entity.setCredits(request.credits());
        entity.setSemester(request.semester());
        entity.setCollege(request.college());
        entity.setGrade(request.grade());
        entity.setTeacherAccount(teacher);
        entity.setProfessionalClasses(professionalClasses);
        TeachingAssignmentEntity saved = teachingAssignmentRepository.save(entity);
        syncAssignmentStudents(saved);
        return toTeachingAssignment(saved);
    }

    @Transactional
    public TeachingAssignment updateTeachingAssignment(Long teachingAssignmentId, TeachingAssignmentUpdateRequest request) {
        if (teachingAssignmentId == null || request == null || isBlank(request.courseName()) || isBlank(request.className()) || request.teacherAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程名称、教学班和任课老师不能为空");
        }
        TeachingAssignmentEntity entity = teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        UserAccountEntity teacher = userAccountRepository.findById(request.teacherAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "教师账号不存在"));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选账号不是教师角色");
        }
        Set<ProfessionalClassEntity> professionalClasses = loadProfessionalClasses(request.professionalClassIds());

        entity.setCourseName(request.courseName().trim());
        entity.setClassName(request.className().trim());
        entity.setCourseCode(request.courseCode());
        entity.setCreditHours(request.creditHours());
        entity.setCredits(request.credits());
        entity.setSemester(request.semester());
        entity.setCollege(request.college());
        entity.setGrade(request.grade());
        entity.setTeacherAccount(teacher);
        entity.setProfessionalClasses(professionalClasses);
        TeachingAssignmentEntity saved = teachingAssignmentRepository.save(entity);
        syncAssignmentStudents(saved);
        return toTeachingAssignment(saved);
    }

    @Transactional
    public void deleteTeachingAssignment(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择教学班安排");
        }
        TeachingAssignmentEntity entity = teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        // 检查是否有关联的考试
        List<ExamPaperEntity> exams = examPaperRepository.findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(
                ExamPaperType.EXAM, teachingAssignmentId);
        if (!exams.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该教学班已关联 " + exams.size() + " 场考试，无法删除。请先删除相关考试。");
        }
        List<StudentEntity> students = studentRepository.findAllByTeachingAssignmentsIdOrderByIdAsc(teachingAssignmentId);
        for (StudentEntity student : students) {
            student.getTeachingAssignments().removeIf(item -> Objects.equals(item.getId(), teachingAssignmentId));
        }
        if (!students.isEmpty()) {
            studentRepository.saveAll(students);
        }
        teachingAssignmentRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents(UserAccountEntity actor) {
        List<StudentEntity> entities = switch (actor.getRole()) {
            case ADMIN -> studentRepository.findAllByOrderByIdAsc();
            case TEACHER -> studentRepository.findAllByTeachingAssignmentsTeacherAccountUsernameOrderByIdAsc(actor.getUsername());
            case STUDENT -> {
                StudentEntity currentStudent = loadStudentForActor(actor);
                yield currentStudent == null ? List.of() : List.of(currentStudent);
            }
        };

        Map<Long, UserAccountEntity> accountsByStudentId = studentAccountsByStudentId(entities);
        return entities.stream()
                .map(student -> toStudent(student, accountsByStudentId.get(student.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents(UserAccountEntity actor, Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listStudents(actor);
        }
        loadAssignmentForActor(teachingAssignmentId, actor);
        return listStudents(actor).stream()
                .filter(student -> student.getTeachingAssignmentIds() != null && student.getTeachingAssignmentIds().contains(teachingAssignmentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents() {
        List<StudentEntity> entities = studentRepository.findAllByOrderByIdAsc();
        Map<Long, UserAccountEntity> accountsByStudentId = studentAccountsByStudentId(entities);
        return entities.stream()
                .map(student -> toStudent(student, accountsByStudentId.get(student.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listStudents();
        }
        teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        return listStudents().stream()
                .filter(student -> student.getTeachingAssignmentIds() != null && student.getTeachingAssignmentIds().contains(teachingAssignmentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> listCandidateStudents(UserAccountEntity actor, Long teachingAssignmentId) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(teachingAssignmentId, actor);
        List<StudentEntity> entities = studentRepository.findAllByOrderByIdAsc().stream()
                .filter(student -> student.getTeachingAssignments().stream().noneMatch(item -> Objects.equals(item.getId(), assignment.getId())))
                .sorted(Comparator
                        .comparing(StudentEntity::getClassName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(StudentEntity::getStudentNo, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<Long, UserAccountEntity> accountsByStudentId = studentAccountsByStudentId(entities);
        return entities.stream()
                .map(student -> toStudent(student, accountsByStudentId.get(student.getId())))
                .toList();
    }

    @Transactional
    public ProfessionalClassImportResult importProfessionalClasses(MultipartFile file) {
        List<ProfessionalClassImportRow> rows = parseProfessionalClassImportRows(file);
        Map<String, ProfessionalClassEntity> classCache = new LinkedHashMap<>();
        int createdClassCount = 0;
        int createdStudentCount = 0;
        int updatedStudentCount = 0;
        int skippedCount = 0;
        List<String> messages = new ArrayList<>();

        for (ProfessionalClassImportRow row : rows) {
            String studentNo = row.studentNo().trim();
            String name = row.name().trim();
            String className = row.className().trim();
            if (studentNo.isBlank() || name.isBlank() || className.isBlank()) {
                skippedCount++;
                messages.add("Skipped a row because student number, name or class is empty");
                continue;
            }

            ProfessionalClassEntity professionalClass = classCache.get(className);
            if (professionalClass == null) {
                boolean existed = professionalClassRepository.findByName(className).isPresent();
                professionalClass = resolveProfessionalClass(className);
                classCache.put(className, professionalClass);
                if (!existed) {
                    createdClassCount++;
                }
            }

            StudentEntity student = studentRepository.findByStudentNo(studentNo).orElse(null);
            if (student == null) {
                StudentEntity entity = new StudentEntity();
                entity.setStudentNo(studentNo);
                entity.setName(name);
                entity.setClassName(className);
                entity.setProfessionalClass(professionalClass);
                StudentEntity savedStudent = studentRepository.save(entity);
                createStudentAccount(savedStudent);
                createdStudentCount++;
                continue;
            }

            student.setName(name);
            student.setClassName(className);
            student.setProfessionalClass(professionalClass);
            StudentEntity savedStudent = studentRepository.save(student);
            ensureStudentAccount(savedStudent);
            updatedStudentCount++;
        }

        if (rows.isEmpty()) {
            messages.add("No valid rows were found in the uploaded file");
        } else {
            messages.add("创建了 " + createdClassCount + " 个专业班级，新增 " + createdStudentCount
                    + " 名学生，更新 " + updatedStudentCount + " 名学生，跳过 " + skippedCount + " 行");
        }
        return new ProfessionalClassImportResult(createdClassCount, createdStudentCount, updatedStudentCount, skippedCount, messages);
    }

    @Transactional
    public Student addStudent(StudentCreateRequest request) {
        if (request == null || isBlank(request.studentNo()) || isBlank(request.name()) || isBlank(request.className())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号、姓名和班级不能为空");
        }
        String studentNo = request.studentNo().trim();
        if (studentRepository.findByStudentNo(studentNo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该学号已存在");
        }
        if (userAccountRepository.findByUsername(studentNo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
        }

        StudentEntity entity = new StudentEntity();
        entity.setStudentNo(studentNo);
        entity.setName(request.name().trim());
        String className = request.className().trim();
        entity.setClassName(className);
        entity.setProfessionalClass(resolveProfessionalClass(className));

        StudentEntity savedStudent = studentRepository.save(entity);
        UserAccountEntity account = createStudentAccount(savedStudent);
        return toStudent(savedStudent, account);
    }

    @Transactional
    public Student updateStudent(Long studentId, StudentUpdateRequest request) {
        if (studentId == null || request == null || isBlank(request.studentNo()) || isBlank(request.name()) || isBlank(request.className())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号、姓名和班级不能为空");
        }
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));

        String studentNo = request.studentNo().trim();
        studentRepository.findByStudentNo(studentNo)
                .filter(other -> !Objects.equals(other.getId(), studentId))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "该学号已存在");
                });

        UserAccountEntity account = userAccountRepository.findByStudentId(studentId).orElse(null);
        if (account != null) {
            Long accountId = account.getId();
            userAccountRepository.findByUsername(studentNo)
                    .filter(other -> !Objects.equals(other.getId(), accountId))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
                    });
        } else if (userAccountRepository.findByUsername(studentNo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
        }

        student.setStudentNo(studentNo);
        student.setName(request.name().trim());
        String className = request.className().trim();
        ProfessionalClassEntity professionalClass = resolveProfessionalClass(className);
        student.setClassName(className);
        student.setProfessionalClass(professionalClass);
        StudentEntity savedStudent = studentRepository.save(student);

        if (account == null) {
            account = createStudentAccount(savedStudent);
        } else {
            account.setUsername(studentNo);
            account.setDisplayName(savedStudent.getName());
            account.setStudent(savedStudent);
            account = userAccountRepository.save(account);
        }
        return toStudent(savedStudent, account);
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学生");
        }
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));

        for (TeachingAssignmentEntity assignment : teachingAssignmentRepository.findAllByOrderByIdAsc()) {
            assignment.getExcludedStudents().removeIf(item -> Objects.equals(item.getId(), studentId));
        }
        student.getTeachingAssignments().clear();
        studentRepository.save(student);

        userAccountRepository.findByStudentId(studentId).ifPresent(userAccountRepository::delete);
        if (examResultRepository.countByStudentId(studentId) > 0) {
            examResultRepository.deleteByStudentId(studentId);
        }
        studentRepository.delete(student);
    }

    @Transactional
    public StudentImportResult importStudents(UserAccountEntity actor, Long teachingAssignmentId, MultipartFile file) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(teachingAssignmentId, actor);
        List<ImportRow> rows = parseImportRows(file);
        Set<Long> allowedClassIds = assignment.getProfessionalClasses().stream()
                .map(ProfessionalClassEntity::getId)
                .collect(Collectors.toSet());

        int importedCount = 0;
        int skippedCount = 0;
        List<String> messages = new ArrayList<>();
        for (ImportRow row : rows) {
            String studentNo = row.studentNo().trim();
            String name = row.name() == null ? "" : row.name().trim();
            if (studentNo.isBlank()) {
                skippedCount++;
                messages.add("Skipped a row because student number is empty");
                continue;
            }

            StudentEntity student = studentRepository.findByStudentNo(studentNo).orElse(null);
            if (student == null) {
                skippedCount++;
                messages.add("Skipped " + studentNo + " because the student profile does not exist yet");
                continue;
            }

            if (!allowedClassIds.isEmpty()) {
                Long professionalClassId = student.getProfessionalClass() == null ? null : student.getProfessionalClass().getId();
                if (professionalClassId == null || !allowedClassIds.contains(professionalClassId)) {
                    skippedCount++;
                    messages.add("Skipped " + studentNo + " because the student class is not part of the teaching assignment");
                    continue;
                }
            } else if (!student.getClassName().trim().equalsIgnoreCase(assignment.getClassName().trim())) {
                skippedCount++;
                messages.add("Skipped " + studentNo + " because the student class does not match the teaching assignment");
                continue;
            }

            if (!name.isBlank() && !student.getName().equals(name)) {
                skippedCount++;
                messages.add("Skipped " + studentNo + " because the name does not match the existing student profile");
                continue;
            }

            if (student.getTeachingAssignments().stream().noneMatch(item -> Objects.equals(item.getId(), assignment.getId()))) {
                student.getTeachingAssignments().add(assignment);
                StudentEntity savedStudent = studentRepository.save(student);
                ensureStudentAccount(savedStudent);
                importedCount++;
            } else {
                skippedCount++;
                messages.add("Skipped " + studentNo + " because the student is already in the teaching assignment");
            }
        }

        if (rows.isEmpty()) {
            messages.add("未在上传文件中读取到有效学生行");
        } else {
            messages.add("成功导入 " + importedCount + " 名学生到课程，跳过 " + skippedCount + " 行");
        }
        return new StudentImportResult(importedCount, skippedCount, messages);
    }

    @Transactional
    public Student addStudentToTeachingAssignment(UserAccountEntity actor, Long teachingAssignmentId, Long studentId) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(teachingAssignmentId, actor);
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学生");
        }
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        if (student.getTeachingAssignments().stream().anyMatch(item -> Objects.equals(item.getId(), assignment.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该学生已在当前教学班中");
        }
        student.getTeachingAssignments().add(assignment);
        assignment.getExcludedStudents().removeIf(item -> Objects.equals(item.getId(), studentId));
        StudentEntity savedStudent = studentRepository.save(student);
        UserAccountEntity account = ensureStudentAccount(savedStudent);
        return toStudent(savedStudent, account);
    }

    @Transactional
    public void removeStudentFromTeachingAssignment(UserAccountEntity actor, Long teachingAssignmentId, Long studentId) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(teachingAssignmentId, actor);
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学生");
        }
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        boolean removed = student.getTeachingAssignments().removeIf(item -> Objects.equals(item.getId(), assignment.getId()));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该学生不在当前教学班中");
        }
        Long professionalClassId = student.getProfessionalClass() == null ? null : student.getProfessionalClass().getId();
        if (assignmentContainsProfessionalClass(assignment, professionalClassId)) {
            assignment.getExcludedStudents().add(student);
        } else {
            assignment.getExcludedStudents().removeIf(item -> Objects.equals(item.getId(), studentId));
        }
        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<ExamPaperSummary> listExams(UserAccountEntity actor) {
        List<ExamPaperEntity> exams;
        if (actor.getRole() == UserRole.STUDENT) {
            StudentEntity student = loadStudentForActor(actor);
            if (student == null) {
                return List.of();
            }
            exams = student.getTeachingAssignments().stream()
                    .sorted(Comparator.comparing(TeachingAssignmentEntity::getId))
                    .flatMap(assignment -> examPaperRepository
                            .findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(ExamPaperType.EXAM, assignment.getId())
                            .stream())
                    .filter(ExamPaperEntity::isActive)
                    .distinct()
                    .toList();
            return exams.stream().map(exam -> toExamSummary(exam, actor)).toList();
        }

        exams = actor.getRole() == UserRole.ADMIN
                ? examPaperRepository.findAllByPaperTypeOrderByStartTimeDescIdDesc(ExamPaperType.EXAM)
                : examPaperRepository.findAllByPaperTypeAndTeachingAssignmentTeacherAccountUsernameOrderByStartTimeDescIdDesc(
                ExamPaperType.EXAM,
                actor.getUsername()
        );
        return exams.stream().map(exam -> toExamSummary(exam, actor)).toList();
    }

    @Transactional
    public ExamPaperSummary createExam(UserAccountEntity actor, ExamPaperSaveRequest request) {
        if (request == null || request.teachingAssignmentId() == null || isBlank(request.paperName()) || request.durationMinutes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教学班、考试名称和时长不能为空");
        }
        if (request.durationMinutes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试时长必须大于0");
        }
        List<Long> questionIds = request.questionIds() == null ? List.of() : request.questionIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一道题目");
        }

        TeachingAssignmentEntity assignment = teachingAssignmentRepository.findById(request.teachingAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        if (actor.getRole() == UserRole.TEACHER) {
            validateTeacherOwnsAssignment(actor, assignment);
        }

        ExamPaperEntity bankPaper = questionBankPaper();
        List<QuestionEntity> selectedQuestions = loadBankQuestions(bankPaper, questionIds);

        ExamPaperEntity exam = new ExamPaperEntity();
        exam.setCode(generateExamCode(assignment));
        exam.setPaperType(ExamPaperType.EXAM);
        exam.setCourseName(assignment.getCourseName());
        exam.setPaperName(request.paperName().trim());
        exam.setDescription(isBlank(request.description()) ? "" : request.description().trim());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setStartTime(request.startTime());
        exam.setTeachingAssignment(assignment);
        exam.setActive(true);
        ExamPaperEntity savedExam = examPaperRepository.save(exam);

        int sortOrder = 1;
        for (QuestionEntity source : selectedQuestions) {
            QuestionEntity copy = new QuestionEntity();
            copy.setPaper(savedExam);
            copy.setCourseName(savedExam.getCourseName());
            copy.setTitle(source.getTitle());
            copy.setType(source.getType());
            copy.setObjective(source.getObjective());
            copy.setScore(source.getScore());
            copy.setOptions(source.getOptions() == null ? List.of() : new ArrayList<>(source.getOptions()));
            copy.setAnswer(source.getAnswer());
            copy.setAnalysis(source.getAnalysis());
            copy.setSortOrder(sortOrder++);
            questionRepository.save(copy);
        }

        return toExamSummary(savedExam, actor);
    }

    @Transactional
    public void deleteExam(Long examId) {
        if (examId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择考试");
        }
        ExamPaperEntity exam = examPaperRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试不存在"));
        if (exam.getPaperType() != ExamPaperType.EXAM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选记录不是考试");
        }
        // 删除关联的答卷
        List<ExamResultEntity> results = examResultRepository.findAllByPaperIdOrderBySubmittedAtDesc(examId);
        if (!results.isEmpty()) {
            examResultRepository.deleteAll(results);
        }
        // 删除关联的题目
        List<QuestionEntity> questions = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(examId);
        if (!questions.isEmpty()) {
            questionRepository.deleteAll(questions);
        }
        examPaperRepository.delete(exam);
    }

    @Transactional
    public MockExamResultGenerationResult generateMockResults(UserAccountEntity actor, Long examId) {
        ExamPaperEntity exam = loadExamPaper(examId);
        if (actor.getRole() == UserRole.TEACHER) {
            validateTeacherOwnsAssignment(actor, exam.getTeachingAssignment());
        }
        if (exam.getTeachingAssignment() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该考试未绑定教学班");
        }

        List<QuestionEntity> questions = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(exam.getId());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选考试没有包含任何题目");
        }

        List<StudentEntity> enrolledStudents = studentRepository.findAllByTeachingAssignmentsIdOrderByIdAsc(exam.getTeachingAssignment().getId());
        if (enrolledStudents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前教学班没有学生");
        }

        Set<Long> submittedStudentIds = examResultRepository.findAllByPaperIdOrderBySubmittedAtDesc(exam.getId()).stream()
                .map(ExamResultEntity::getStudent)
                .filter(Objects::nonNull)
                .map(StudentEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int createdCount = 0;
        int skippedCount = 0;
        int simulationIndex = 0;

        for (StudentEntity student : enrolledStudents) {
            if (student.getId() == null || submittedStudentIds.contains(student.getId())) {
                skippedCount++;
                continue;
            }
            Map<Long, String> mockAnswers = buildMockAnswers(questions, simulationIndex++);
            createExamResult(exam, student, questions, mockAnswers, mockSubmittedAt(exam, simulationIndex), false);
            createdCount++;
        }

        List<String> messages = new ArrayList<>();
        messages.add("已生成 " + createdCount + " 份模拟答卷");
        if (skippedCount > 0) {
            messages.add("跳过 " + skippedCount + " 名已提交答卷的学生");
        }

        return new MockExamResultGenerationResult(exam.getId(), createdCount, skippedCount, messages);
    }

    @Transactional(readOnly = true)
    public List<Question> listExamQuestions(UserAccountEntity actor, Long examId) {
        ExamPaperEntity exam = loadExamPaper(examId);
        validateActorCanAccessExam(actor, exam);
        return questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(exam.getId()).stream()
                .map(this::toQuestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResult> listResults(UserAccountEntity actor) {
        List<ExamResultEntity> entities = switch (actor.getRole()) {
            case ADMIN -> examResultRepository.findAllByOrderBySubmittedAtDesc();
            case TEACHER -> examResultRepository.findAllByPaperTeachingAssignmentTeacherAccountUsernameOrderBySubmittedAtDesc(actor.getUsername());
            case STUDENT -> actor.getStudent() == null || actor.getStudent().getId() == null
                    ? List.of()
                    : examResultRepository.findAllByStudentIdOrderBySubmittedAtDesc(actor.getStudent().getId());
        };
        return entities.stream()
                .map(this::toExamResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResult> listResults(UserAccountEntity actor, Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listResults(actor);
        }
        loadAssignmentForActor(teachingAssignmentId, actor);
        return listResults(actor).stream()
                .filter(result -> Objects.equals(result.getTeachingAssignmentId(), teachingAssignmentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResult> listResults() {
        return examResultRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(this::toExamResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResult> listResults(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listResults();
        }
        teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        return listResults().stream()
                .filter(result -> Objects.equals(result.getTeachingAssignmentId(), teachingAssignmentId))
                .toList();
    }

    @Transactional
    public ExamResult submit(ExamSubmitRequest request) {
        return submit(null, request);
    }

    @Transactional
    public ExamResult submit(UserAccountEntity actor, ExamSubmitRequest request) {
        if (request == null || request.examId() == null || request.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试和学生信息不能为空");
        }
        StudentEntity student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (actor != null && actor.getRole() == UserRole.STUDENT) {
            if (actor.getStudent() == null || !Objects.equals(actor.getStudent().getId(), student.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能提交自己的考试");
            }
        }
        ExamPaperEntity paper = loadExamPaper(request.examId());
        validateStudentCanJoinExam(student, paper);
        if (paper.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = paper.getStartTime().plusMinutes(Math.max(1, paper.getDurationMinutes()));
            if (now.isBefore(paper.getStartTime())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "考试尚未开始");
            }
            if (now.isAfter(endTime)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "考试已结束");
            }
        }
        if (examResultRepository.countByPaperIdAndStudentId(paper.getId(), student.getId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该学生已提交过此考试");
        }
        List<QuestionEntity> questions = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(paper.getId());

        Map<Long, String> submittedAnswers = request.answers() == null ? Map.of() : request.answers();
        return toExamResult(createExamResult(paper, student, questions, submittedAnswers, LocalDateTime.now(), true));
    }

    @Transactional
    public ExamResult confirmScore(ScoreConfirmRequest request) {
        return confirmScore(null, request);
    }

    @Transactional
    public ExamResult confirmScore(UserAccountEntity actor, ScoreConfirmRequest request) {
        ExamResultEntity result = examResultRepository.findById(request.resultId())
                .orElseThrow(() -> new IllegalArgumentException("成绩记录不存在"));
        if (actor != null && actor.getRole() == UserRole.TEACHER) {
            ExamPaperEntity paper = result.getPaper();
            if (paper == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成绩记录不属于有效考试");
            }
            validateTeacherOwnsAssignment(actor, paper.getTeachingAssignment());
        }
        AnswerRecordEntity record = result.getAnswers().stream()
                .filter(item -> Objects.equals(item.getQuestion().getId(), request.questionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Answer record does not exist"));

        int maxScore = record.getQuestion().getScore();
        int confirmedScore = Math.max(0, Math.min(maxScore, request.score()));
        record.setScore(confirmedScore);
        record.setSuggestion("教师已确认最终得分：" + confirmedScore + " 分");
        recalculateResult(result);
        return toExamResult(result);
    }

    @Transactional(readOnly = true)
    public AnalysisSummary analysis() {
        return analysis(null);
    }

    @Transactional(readOnly = true)
    public AnalysisSummary analysis(UserAccountEntity actor) {
        return analysis(actor, null);
    }

    @Transactional(readOnly = true)
    public AnalysisSummary analysis(UserAccountEntity actor, Long teachingAssignmentId) {
        List<Student> students = actor == null
                ? listStudents(teachingAssignmentId)
                : listStudents(actor, teachingAssignmentId);
        List<Question> questions = actor == null
                ? listAnalysisQuestions(teachingAssignmentId)
                : listAnalysisQuestions(actor, teachingAssignmentId);
        List<ExamResult> results = actor == null
                ? listResults(teachingAssignmentId)
                : listResults(actor, teachingAssignmentId);

        double average = results.stream()
                .mapToInt(ExamResult::getTotalScore)
                .average()
                .orElse(0);

        Map<String, Long> scoreBands = new LinkedHashMap<>();
        scoreBands.put("<60", results.stream().filter(result -> result.getTotalScore() < 60).count());
        scoreBands.put("60-69", results.stream().filter(result -> result.getTotalScore() >= 60 && result.getTotalScore() < 70).count());
        scoreBands.put("70-79", results.stream().filter(result -> result.getTotalScore() >= 70 && result.getTotalScore() < 80).count());
        scoreBands.put("80-89", results.stream().filter(result -> result.getTotalScore() >= 80 && result.getTotalScore() < 90).count());
        scoreBands.put(">=90", results.stream().filter(result -> result.getTotalScore() >= 90).count());

        Map<String, Long> questionTypeCount = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> question.getType().getLabel(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<CourseObjective, Integer> fullScores = objectiveFullScores(actor, teachingAssignmentId);
        Map<String, Double> objectiveAverage = new LinkedHashMap<>();
        Map<String, Double> objectiveRatios = new LinkedHashMap<>();
        for (CourseObjective objective : CourseObjective.values()) {
            double objectiveAvg = results.stream()
                    .mapToInt(result -> result.getObjectiveScores().getOrDefault(objective, 0))
                    .average()
                    .orElse(0);
            objectiveAverage.put(objective.getLabel(), round(objectiveAvg));
            int fullScore = Math.max(1, fullScores.getOrDefault(objective, 0));
            objectiveRatios.put(objective.getLabel(), Math.round(objectiveAvg / fullScore * 1000.0) / 1000.0);
        }

        return new AnalysisSummary(
                students.size(),
                questions.size(),
                round(average),
                scoreBands,
                questionTypeCount,
                objectiveAverage,
                buildSuggestions(objectiveRatios, average)
        );
    }

    @Transactional(readOnly = true)
    public Map<CourseObjective, Integer> objectiveFullScores() {
        return objectiveFullScores(null, null);
    }

    @Transactional(readOnly = true)
    public Map<CourseObjective, Integer> objectiveFullScores(UserAccountEntity actor, Long teachingAssignmentId) {
        Map<CourseObjective, Integer> fullScores = new EnumMap<>(CourseObjective.class);
        List<Question> questions = actor == null
                ? listAnalysisQuestions(teachingAssignmentId)
                : listAnalysisQuestions(actor, teachingAssignmentId);
        for (Question question : questions) {
            fullScores.merge(question.getObjective(), question.getScore(), Integer::sum);
        }
        return fullScores;
    }

    @Transactional
    public void synchronizeExistingStudentData() {
        List<StudentEntity> students = studentRepository.findAllByOrderByIdAsc();
        List<StudentEntity> changedStudents = new ArrayList<>();
        for (StudentEntity student : students) {
            if (isBlank(student.getClassName())) {
                continue;
            }
            ProfessionalClassEntity professionalClass = resolveProfessionalClass(student.getClassName().trim());
            if (student.getProfessionalClass() == null || !Objects.equals(student.getProfessionalClass().getId(), professionalClass.getId())) {
                student.setProfessionalClass(professionalClass);
                changedStudents.add(student);
            }
        }
        if (!changedStudents.isEmpty()) {
            studentRepository.saveAll(changedStudents);
        }
        for (TeachingAssignmentEntity assignment : teachingAssignmentRepository.findAllByOrderByIdAsc()) {
            syncAssignmentStudents(assignment);
        }
    }

    private TeachingAssignmentEntity loadAssignmentForActor(Long teachingAssignmentId, UserAccountEntity actor) {
        if (teachingAssignmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择教学班安排");
        }
        TeachingAssignmentEntity assignment = teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        if (actor.getRole() == UserRole.TEACHER) {
            validateTeacherOwnsAssignment(actor, assignment);
        }
        return assignment;
    }

    private void validateTeacherOwnsAssignment(UserAccountEntity actor, TeachingAssignmentEntity assignment) {
        if (actor == null || actor.getRole() != UserRole.TEACHER) {
            return;
        }
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择教学班安排");
        }
        UserAccountEntity teacherAccount = assignment.getTeacherAccount();
        if (teacherAccount == null || !Objects.equals(teacherAccount.getUsername(), actor.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能管理自己负责的教学班");
        }
    }

    private Set<ProfessionalClassEntity> loadProfessionalClasses(List<Long> professionalClassIds) {
        if (professionalClassIds == null || professionalClassIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一个专业班级");
        }
        List<Long> distinctIds = professionalClassIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一个专业班级");
        }
        List<ProfessionalClassEntity> classes = professionalClassRepository.findAllByIdIn(distinctIds);
        if (classes.size() != distinctIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "部分所选专业班级不存在");
        }
        return new LinkedHashSet<>(classes);
    }

    private void syncAssignmentStudents(TeachingAssignmentEntity assignment) {
        Set<Long> professionalClassIds = assignment.getProfessionalClasses().stream()
                .map(ProfessionalClassEntity::getId)
                .collect(Collectors.toSet());
        Set<Long> excludedStudentIds = assignment.getExcludedStudents().stream()
                .map(StudentEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<StudentEntity> changedStudents = new ArrayList<>();

        for (StudentEntity student : studentRepository.findAllByTeachingAssignmentsIdOrderByIdAsc(assignment.getId())) {
            Long classId = student.getProfessionalClass() == null ? null : student.getProfessionalClass().getId();
            if (classId != null && professionalClassIds.contains(classId) && excludedStudentIds.contains(student.getId())) {
                if (student.getTeachingAssignments().removeIf(item -> Objects.equals(item.getId(), assignment.getId()))) {
                    changedStudents.add(student);
                }
            }
        }

        for (Long professionalClassId : professionalClassIds) {
            for (StudentEntity student : studentRepository.findAllByProfessionalClassIdOrderByIdAsc(professionalClassId)) {
                if (excludedStudentIds.contains(student.getId())) {
                    continue;
                }
                boolean alreadyBound = student.getTeachingAssignments().stream()
                        .anyMatch(item -> Objects.equals(item.getId(), assignment.getId()));
                if (!alreadyBound) {
                    student.getTeachingAssignments().add(assignment);
                    changedStudents.add(student);
                }
            }
        }

        if (!changedStudents.isEmpty()) {
            studentRepository.saveAll(changedStudents);
        }
    }

    private boolean assignmentContainsProfessionalClass(TeachingAssignmentEntity assignment, Long professionalClassId) {
        if (assignment == null || professionalClassId == null) {
            return false;
        }
        return assignment.getProfessionalClasses().stream()
                .map(ProfessionalClassEntity::getId)
                .anyMatch(professionalClassId::equals);
    }

    private Map<Long, UserAccountEntity> studentAccountsByStudentId(Collection<StudentEntity> students) {
        List<Long> studentIds = students.stream()
                .map(StudentEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findAllByStudentIdIn(studentIds).stream()
                .filter(account -> account.getStudent() != null)
                .collect(Collectors.toMap(account -> account.getStudent().getId(), account -> account));
    }

    private UserAccountEntity createStudentAccount(StudentEntity student) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUsername(student.getStudentNo());
        account.setPassword(DEFAULT_STUDENT_PASSWORD);
        account.setDisplayName(student.getName());
        account.setRole(UserRole.STUDENT);
        account.setStudent(student);
        account.setPasswordChangeRequired(true);
        return userAccountRepository.save(account);
    }

    private UserAccountEntity ensureStudentAccount(StudentEntity student) {
        UserAccountEntity account = userAccountRepository.findByStudentId(student.getId()).orElse(null);
        if (account == null) {
            return createStudentAccount(student);
        }
        account.setUsername(student.getStudentNo());
        account.setDisplayName(student.getName());
        account.setStudent(student);
        return userAccountRepository.save(account);
    }

    private ProfessionalClassEntity resolveProfessionalClass(String className) {
        if (isBlank(className)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "班级名称不能为空");
        }
        String normalized = className.trim();
        return professionalClassRepository.findByName(normalized)
                .orElseGet(() -> {
                    ProfessionalClassEntity entity = new ProfessionalClassEntity();
                    entity.setName(normalized);
                    return professionalClassRepository.save(entity);
                });
    }

    private List<QuestionImportRow> parseQuestionImportRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传题目导入文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                return parseQuestionImportRowsFromWorkbook(file);
            }
            return parseQuestionImportRowsFromText(file);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取题目导入文件失败");
        }
    }

    private List<QuestionImportRow> parseQuestionImportRowsFromWorkbook(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<QuestionImportRow> rows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String courseName = formatter.formatCellValue(row.getCell(0)).trim();
                String title = formatter.formatCellValue(row.getCell(1)).trim();
                String type = formatter.formatCellValue(row.getCell(2)).trim();
                String objective = formatter.formatCellValue(row.getCell(3)).trim();
                String score = formatter.formatCellValue(row.getCell(4)).trim();
                String options = formatter.formatCellValue(row.getCell(5)).trim();
                String answer = formatter.formatCellValue(row.getCell(6)).trim();
                String analysis = formatter.formatCellValue(row.getCell(7)).trim();
                if (isQuestionHeaderRow(courseName, title, type, objective, score, options, answer, analysis)) {
                    continue;
                }
                if (courseName.isBlank() && title.isBlank() && type.isBlank() && objective.isBlank() && score.isBlank() && options.isBlank() && answer.isBlank() && analysis.isBlank()) {
                    continue;
                }
                rows.add(new QuestionImportRow(courseName, title, type, objective, score, options, answer, analysis));
            }
            return rows;
        }
    }

    private List<QuestionImportRow> parseQuestionImportRowsFromText(MultipartFile file) throws IOException {
        List<QuestionImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split("[,\t]", -1);
                if (columns.length < 8) {
                    columns = line.split(";", -1);
                }
                if (columns.length < 8) {
                    columns = line.split("，", -1);
                }
                if (columns.length < 8) {
                    columns = line.split("\\|\\|", -1);
                }
                if (columns.length < 8) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目导入文本文件列数不足，请按固定模板填写 8 列");
                }
                String courseName = columns[0].trim();
                String title = columns[1].trim();
                String type = columns[2].trim();
                String objective = columns[3].trim();
                String score = columns[4].trim();
                String options = columns[5].trim();
                String answer = columns[6].trim();
                String analysis = columns[7].trim();
                if (isQuestionHeaderRow(courseName, title, type, objective, score, options, answer, analysis)) {
                    continue;
                }
                rows.add(new QuestionImportRow(courseName, title, type, objective, score, options, answer, analysis));
            }
        }
        return rows;
    }

    private boolean isQuestionHeaderRow(String courseName, String title, String type, String objective, String score, String options, String answer, String analysis) {
        return "课程名称".equalsIgnoreCase(courseName)
                || ("题干".equalsIgnoreCase(title) && "题型".equalsIgnoreCase(type))
                || ("courseName".equalsIgnoreCase(courseName) && "title".equalsIgnoreCase(title));
    }

    private String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能为空");
        }
        return value.trim();
    }

    private QuestionType parseQuestionType(String raw) {
        if (isBlank(raw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题型不能为空");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SINGLE_CHOICE", "选择题", "单选题" -> QuestionType.SINGLE_CHOICE;
            case "FILL_BLANK", "填空题" -> QuestionType.FILL_BLANK;
            case "SHORT_ANSWER", "简答题" -> QuestionType.SHORT_ANSWER;
            case "DESIGN", "设计题" -> QuestionType.DESIGN;
            case "COMPREHENSIVE", "综合分析题", "综合题" -> QuestionType.COMPREHENSIVE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的题型：" + raw);
        };
    }

    private CourseObjective parseObjective(String raw) {
        if (isBlank(raw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程目标不能为空");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace("课程目标", "").replace("OBJECTIVE_", "");
        return switch (normalized) {
            case "1" -> CourseObjective.OBJECTIVE_1;
            case "2" -> CourseObjective.OBJECTIVE_2;
            case "3" -> CourseObjective.OBJECTIVE_3;
            case "4" -> CourseObjective.OBJECTIVE_4;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的课程目标：" + raw);
        };
    }

    private int parseScore(String raw) {
        if (isBlank(raw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分值不能为空");
        }
        try {
            int score = (int) Double.parseDouble(raw.trim());
            if (score <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分值必须大于 0");
            }
            return score;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分值格式不正确：" + raw);
        }
    }

    private List<String> parseOptions(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }
        return List.of(raw.split("\\|"))
                .stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private void validateQuestionImport(QuestionEntity entity) {
        if (entity.getType() == QuestionType.SINGLE_CHOICE && entity.getOptions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选择题必须填写选项");
        }
    }

    private void applyQuestion(QuestionEntity entity, ExamPaperEntity paper, Question question) {
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写题目信息");
        }
        if (question.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择题型");
        }
        if (question.getObjective() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择课程目标");
        }
        if (question.getScore() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分值必须大于0");
        }

        entity.setPaper(paper);
        entity.setCourseName(isBlank(question.getCourseName()) ? paper.getCourseName() : question.getCourseName().trim());
        entity.setTitle(requireText(question.getTitle(), "题干"));
        entity.setType(question.getType());
        entity.setObjective(question.getObjective());
        entity.setScore(question.getScore());
        entity.setOptions(question.getOptions() == null ? List.of() : new ArrayList<>(question.getOptions()));
        entity.setAnswer(requireText(question.getAnswer(), "标准答案"));
        entity.setAnalysis(isBlank(question.getAnalysis()) ? "" : question.getAnalysis().trim());
        validateQuestionImport(entity);
    }

    private void ensureQuestionBelongsToPaper(QuestionEntity entity, ExamPaperEntity paper) {
        if (entity.getPaper() == null || !Objects.equals(entity.getPaper().getId(), paper.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目不属于当前试卷");
        }
    }

    private List<ImportRow> parseImportRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传学生表格文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                return parseDelimitedRows(file);
            }
            return parseWorkbookRows(file);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取导入文件失败");
        }
    }

    private List<ProfessionalClassImportRow> parseProfessionalClassImportRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传班级名单文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                return parseProfessionalClassDelimitedRows(file);
            }
            return parseProfessionalClassWorkbookRows(file);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取班级名单文件失败");
        }
    }

    private List<ImportRow> parseWorkbookRows(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<ImportRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String studentNo = formatter.formatCellValue(row.getCell(0)).trim();
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                if (studentNo.isBlank() && name.isBlank()) {
                    continue;
                }
                if (isHeaderRow(studentNo, name)) {
                    continue;
                }
                rows.add(new ImportRow(studentNo, name));
            }
        }
        return rows;
    }

    private List<ProfessionalClassImportRow> parseProfessionalClassWorkbookRows(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<ProfessionalClassImportRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String studentNo = formatter.formatCellValue(row.getCell(0)).trim();
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                String className = formatter.formatCellValue(row.getCell(2)).trim();
                if (studentNo.isBlank() && name.isBlank() && className.isBlank()) {
                    continue;
                }
                if (isProfessionalClassHeaderRow(studentNo, name, className)) {
                    continue;
                }
                rows.add(new ProfessionalClassImportRow(studentNo, name, className));
            }
        }
        return rows;
    }

    private List<ImportRow> parseDelimitedRows(MultipartFile file) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    continue;
                }
                String[] columns = trimmed.split("[,\t;，；]");
                String studentNo = columns.length > 0 ? columns[0].trim() : "";
                String name = columns.length > 1 ? columns[1].trim() : "";
                if (isHeaderRow(studentNo, name)) {
                    continue;
                }
                rows.add(new ImportRow(studentNo, name));
            }
        }
        return rows;
    }

    private List<ProfessionalClassImportRow> parseProfessionalClassDelimitedRows(MultipartFile file) throws IOException {
        List<ProfessionalClassImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    continue;
                }
                String[] columns = trimmed.split("[,\t;，；]");
                String studentNo = columns.length > 0 ? columns[0].trim() : "";
                String name = columns.length > 1 ? columns[1].trim() : "";
                String className = columns.length > 2 ? columns[2].trim() : "";
                if (isProfessionalClassHeaderRow(studentNo, name, className)) {
                    continue;
                }
                rows.add(new ProfessionalClassImportRow(studentNo, name, className));
            }
        }
        return rows;
    }

    private boolean isHeaderRow(String studentNo, String name) {
        String left = studentNo.toLowerCase(Locale.ROOT);
        String right = name.toLowerCase(Locale.ROOT);
        return left.contains("学号")
                || left.contains("student")
                || right.contains("姓名")
                || right.contains("name");
    }

    private boolean isProfessionalClassHeaderRow(String studentNo, String name, String className) {
        String left = studentNo.toLowerCase(Locale.ROOT);
        String middle = name.toLowerCase(Locale.ROOT);
        String right = className.toLowerCase(Locale.ROOT);
        return left.contains("学号")
                || left.contains("student")
                || middle.contains("姓名")
                || middle.contains("name")
                || right.contains("班级")
                || right.contains("class");
    }

    private ExamResultEntity createExamResult(ExamPaperEntity paper,
                                              StudentEntity student,
                                              List<QuestionEntity> questions,
                                              Map<Long, String> submittedAnswers,
                                              LocalDateTime submittedAt,
                                              boolean allowAiGrading) {
        ExamResultEntity result = new ExamResultEntity();
        result.setPaper(paper);
        result.setStudent(student);
        result.setCourseName(paper.getCourseName());
        result.setSubmittedAt(submittedAt == null ? LocalDateTime.now() : submittedAt);

        List<AnswerRecordEntity> records = new ArrayList<>();
        int total = 0;
        for (QuestionEntity question : questions) {
            String answer = submittedAnswers.getOrDefault(question.getId(), "");
            GradingDecision gradingDecision = gradeAnswer(question, answer, allowAiGrading);
            total += gradingDecision.score();

            AnswerRecordEntity record = new AnswerRecordEntity();
            record.setResult(result);
            record.setQuestion(question);
            record.setStudentAnswer(answer);
            record.setScore(gradingDecision.score());
            record.setSuggestion(gradingDecision.suggestion());
            records.add(record);
        }

        result.setAnswers(records);
        result.setTotalScore(total);
        return examResultRepository.save(result);
    }

    private Map<Long, String> buildMockAnswers(List<QuestionEntity> questions, int variantSeed) {
        Map<Long, String> answers = new LinkedHashMap<>();
        List<String> subjectiveSamples = List.of(
                "先明确范围、进度、成本和风险，再通过里程碑跟踪与复盘机制保证交付。",
                "建议补充需求澄清、沟通机制、资源分配和过程监控，减少项目延期风险。",
                "需要从需求分析、任务分解、风险应对和质量控制几个方面形成闭环。"
        );

        for (int index = 0; index < questions.size(); index++) {
            QuestionEntity question = questions.get(index);
            String answer;
            if (question.getType() == QuestionType.SINGLE_CHOICE && question.getOptions() != null && !question.getOptions().isEmpty()) {
                if ((variantSeed + index) % 5 == 0 && question.getOptions().size() > 1) {
                    answer = optionKey(question.getOptions().get(1));
                } else {
                    answer = normalizeObjectiveAnswer(question);
                }
            } else if (question.getType() == QuestionType.FILL_BLANK) {
                answer = (variantSeed + index) % 4 == 0 ? "接受" : normalizeObjectiveAnswer(question);
            } else {
                String base = subjectiveSamples.get((variantSeed + index) % subjectiveSamples.size());
                String keyPoints = question.getAnswer() == null ? "" : question.getAnswer().replaceAll("\\s+", "、");
                answer = keyPoints.isBlank() ? base : base + " 关键点包括：" + keyPoints + "。";
            }
            answers.put(question.getId(), answer);
        }
        return answers;
    }

    private String optionKey(String option) {
        if (option == null || option.isBlank()) {
            return "";
        }
        return option.substring(0, 1).trim();
    }

    private String normalizeObjectiveAnswer(QuestionEntity question) {
        if (question.getAnswer() == null) {
            return "";
        }
        return question.getAnswer().trim();
    }

    private LocalDateTime mockSubmittedAt(ExamPaperEntity exam, int variantSeed) {
        if (exam.getStartTime() == null) {
            return LocalDateTime.now().minusMinutes((variantSeed % 45) + 5L);
        }
        LocalDateTime baseTime = exam.getStartTime().plusMinutes((variantSeed % Math.max(10, exam.getDurationMinutes())) + 5L);
        return baseTime.isAfter(LocalDateTime.now()) ? LocalDateTime.now().minusMinutes(1) : baseTime;
    }

    private GradingDecision gradeAnswer(QuestionEntity question, String answer, boolean allowAiGrading) {
        String normalized = normalize(answer);
        String expected = normalize(question.getAnswer());

        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            int score = normalized.equals(expected) ? question.getScore() : 0;
            return new GradingDecision(score, suggestion(question, score));
        }

        if (question.getType() == QuestionType.FILL_BLANK && normalized.equals(expected)) {
            return new GradingDecision(question.getScore(), "系统判定：与标准答案完全匹配，给满分");
        }

        if (normalized.isBlank()) {
            return new GradingDecision(0, "学生未作答，本题记 0 分");
        }

        int heuristicScore = grade(question, answer);
        String heuristicSuggestion = suggestion(question, heuristicScore);

        if (!allowAiGrading) {
            return new GradingDecision(heuristicScore, heuristicSuggestion);
        }

        return openAiGradingService.gradeQuestion(question, answer)
                .map(decision -> new GradingDecision(
                        Math.max(0, Math.min(question.getScore(), decision.score())),
                        decision.suggestion()
                ))
                .orElseGet(() -> new GradingDecision(heuristicScore, heuristicSuggestion));
    }

    private void recalculateResult(ExamResultEntity result) {
        int total = result.getAnswers().stream()
                .mapToInt(AnswerRecordEntity::getScore)
                .sum();
        result.setTotalScore(total);
    }

    private int grade(QuestionEntity question, String answer) {
        String normalized = normalize(answer);
        String expected = normalize(question.getAnswer());
        if (question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.FILL_BLANK) {
            return normalized.equals(expected) ? question.getScore() : 0;
        }
        if (normalized.isBlank()) {
            return 0;
        }
        long matchedKeywords = List.of(expected.split("\\s+")).stream()
                .filter(keyword -> !keyword.isBlank())
                .filter(normalized::contains)
                .count();
        double ratio = Math.min(1.0, matchedKeywords / Math.max(1.0, expected.split("\\s+").length));
        return (int) Math.round(question.getScore() * Math.max(0.45, ratio));
    }

    private String suggestion(QuestionEntity question, int score) {
        if (score == question.getScore()) {
            return "系统判定：答案满足得分要求，给满分";
        }
        if (question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.FILL_BLANK) {
            return "系统判定：与标准答案不一致，按规则记分";
        }
        return "系统判定：按规则估算得分 " + score + " 分。参考要点：" + question.getAnswer();
    }

    private List<String> buildSuggestions(Map<String, Double> objectiveAverage, double average) {
        return openAiGradingService.generateImprovementSuggestions(objectiveAverage, average);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('\uFF0C', ' ')
                .replace('\u3001', ' ')
                .replace('\u3002', ' ')
                .replace('\uFF1A', ' ')
                .replace(',', ' ')
                .replace('.', ' ')
                .replace(':', ' ')
                .replace(';', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ");
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record GradingDecision(int score, String suggestion) {
    }

    private ExamPaperEntity questionBankPaper() {
        return examPaperRepository.findByCode(QUESTION_BANK_CODE)
                .or(() -> examPaperRepository.findFirstByPaperTypeOrderByIdAsc(ExamPaperType.QUESTION_BANK))
                .orElseThrow(() -> new IllegalStateException("No question bank paper found"));
    }

    private ExamPaperEntity loadExamPaper(Long examId) {
        if (examId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择考试");
        }
        ExamPaperEntity exam = examPaperRepository.findWithTeachingAssignmentById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试不存在"));
        if (exam.getPaperType() != ExamPaperType.EXAM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选记录不是考试");
        }
        return exam;
    }

    @Transactional(readOnly = true)
    public List<Question> listAnalysisQuestions(UserAccountEntity actor, Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listQuestions();
        }
        loadAssignmentForActor(teachingAssignmentId, actor);
        return listAnalysisQuestions(teachingAssignmentId);
    }

    @Transactional(readOnly = true)
    public List<Question> listAnalysisQuestions(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return listQuestions();
        }
        teachingAssignmentRepository.findById(teachingAssignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教学班安排不存在"));
        List<ExamPaperEntity> exams = examPaperRepository.findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(
                ExamPaperType.EXAM,
                teachingAssignmentId
        );
        if (exams.isEmpty()) {
            return List.of();
        }
        return questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(exams.get(0).getId()).stream()
                .map(this::toQuestion)
                .toList();
    }

    private List<QuestionEntity> loadBankQuestions(ExamPaperEntity bankPaper, List<Long> questionIds) {
        List<QuestionEntity> loaded = questionRepository.findAllByIdIn(questionIds);
        Map<Long, QuestionEntity> bankQuestions = loaded.stream()
                .filter(question -> question.getPaper() != null && Objects.equals(question.getPaper().getId(), bankPaper.getId()))
                .collect(Collectors.toMap(QuestionEntity::getId, question -> question));
        List<QuestionEntity> ordered = new ArrayList<>();
        for (Long questionId : questionIds) {
            QuestionEntity question = bankQuestions.get(questionId);
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选题目在题库中不存在：" + questionId);
            }
            ordered.add(question);
        }
        return ordered;
    }

    private String generateExamCode(TeachingAssignmentEntity assignment) {
        String course = assignment.getCourseName() == null ? "EXAM" : assignment.getCourseName()
                .replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "")
                .toUpperCase(Locale.ROOT);
        return course + "-" + System.currentTimeMillis();
    }

    private void validateActorCanAccessExam(UserAccountEntity actor, ExamPaperEntity exam) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.TEACHER) {
            validateTeacherOwnsAssignment(actor, exam.getTeachingAssignment());
            return;
        }
        validateStudentCanJoinExam(loadStudentForActor(actor), exam);
    }

    private StudentEntity loadStudentForActor(UserAccountEntity actor) {
        if (actor == null || actor.getStudent() == null || actor.getStudent().getId() == null) {
            return null;
        }
        return studentRepository.findDetailedById(actor.getStudent().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
    }

    private void validateStudentCanJoinExam(StudentEntity student, ExamPaperEntity exam) {
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学生信息缺失");
        }
        TeachingAssignmentEntity assignment = exam.getTeachingAssignment();
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该考试未绑定教学班");
        }
        boolean enrolled = student.getTeachingAssignments().stream()
                .anyMatch(item -> Objects.equals(item.getId(), assignment.getId()));
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该学生不在目标教学班中");
        }
    }

    private ExamPaperSummary toExamSummary(ExamPaperEntity entity, UserAccountEntity actor) {
        int questionCount = (int) questionRepository.countByPaperId(entity.getId());
        int totalScore = questionRepository.findAllByPaperIdOrderBySortOrderAscIdAsc(entity.getId()).stream()
                .mapToInt(QuestionEntity::getScore)
                .sum();
        boolean submitted = false;
        if (actor.getRole() == UserRole.STUDENT && actor.getStudent() != null) {
            submitted = examResultRepository.countByPaperIdAndStudentId(entity.getId(), actor.getStudent().getId()) > 0;
        }
        return new ExamPaperSummary(
                entity.getId(),
                entity.getTeachingAssignment() == null ? null : entity.getTeachingAssignment().getId(),
                entity.getCourseName(),
                entity.getTeachingAssignment() == null ? null : entity.getTeachingAssignment().getClassName(),
                entity.getPaperName(),
                entity.getDescription(),
                entity.getStartTime(),
                entity.getDurationMinutes(),
                questionCount,
                totalScore,
                entity.isActive(),
                (int) examResultRepository.countByPaperId(entity.getId()),
                submitted
        );
    }

    private Question toQuestion(QuestionEntity entity) {
        return new Question(
                entity.getId(),
                entity.getCourseName(),
                entity.getTitle(),
                entity.getType(),
                entity.getObjective(),
                entity.getScore(),
                entity.getOptions() == null ? List.of() : List.copyOf(entity.getOptions()),
                entity.getAnswer(),
                entity.getAnalysis()
        );
    }

    private Student toStudent(StudentEntity entity, UserAccountEntity account) {
        List<TeachingAssignmentEntity> assignments = entity.getTeachingAssignments().stream()
                .sorted(Comparator
                        .comparing(TeachingAssignmentEntity::getCourseName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(TeachingAssignmentEntity::getClassName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(TeachingAssignmentEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        TeachingAssignmentEntity assignment = assignments.isEmpty() ? null : assignments.get(0);
        return new Student(
                entity.getId(),
                entity.getStudentNo(),
                entity.getName(),
                entity.getClassName(),
                entity.getProfessionalClass() == null ? null : entity.getProfessionalClass().getId(),
                assignment == null ? null : assignment.getCourseName(),
                teacherName(assignment),
                account == null ? null : account.getUsername(),
                account != null && account.isPasswordChangeRequired(),
                assignment == null ? null : assignment.getId(),
                assignments.stream().map(TeachingAssignmentEntity::getId).toList(),
                assignments.stream().map(this::assignmentLabel).toList()
        );
    }

    private String assignmentLabel(TeachingAssignmentEntity assignment) {
        String teacher = teacherName(assignment);
        if (teacher == null || teacher.isBlank()) {
            return assignment.getCourseName() + " / " + assignment.getClassName();
        }
        return assignment.getCourseName() + " / " + assignment.getClassName() + " / " + teacher;
    }

    private String teacherName(TeachingAssignmentEntity assignment) {
        if (assignment == null || assignment.getTeacherAccount() == null) {
            return null;
        }
        return assignment.getTeacherAccount().getDisplayName();
    }

    private ExamResult toExamResult(ExamResultEntity entity) {
        List<AnswerRecord> answers = entity.getAnswers().stream()
                .sorted(Comparator.comparing(item -> item.getQuestion().getSortOrder()))
                .map(item -> new AnswerRecord(
                        item.getQuestion().getId(),
                        item.getQuestion().getTitle(),
                        item.getQuestion().getScore(),
                        item.getStudentAnswer(),
                        item.getScore(),
                        item.getSuggestion()
                ))
                .toList();

        return new ExamResult(
                entity.getId(),
                entity.getPaper() == null ? null : entity.getPaper().getId(),
                entity.getPaper() == null || entity.getPaper().getTeachingAssignment() == null ? null : entity.getPaper().getTeachingAssignment().getId(),
                toStudent(entity.getStudent(), null),
                entity.getCourseName(),
                entity.getPaper() == null ? null : entity.getPaper().getPaperName(),
                entity.getPaper() == null || entity.getPaper().getTeachingAssignment() == null ? null : entity.getPaper().getTeachingAssignment().getClassName(),
                entity.getTotalScore(),
                entity.getSubmittedAt(),
                answers,
                objectiveScores(entity)
        );
    }

    private Map<CourseObjective, Integer> objectiveScores(ExamResultEntity entity) {
        Map<CourseObjective, Integer> scores = new EnumMap<>(CourseObjective.class);
        for (AnswerRecordEntity answer : entity.getAnswers()) {
            scores.merge(answer.getQuestion().getObjective(), answer.getScore(), Integer::sum);
        }
        return scores;
    }

    private TeachingAssignment toTeachingAssignment(TeachingAssignmentEntity entity) {
        UserAccountEntity teacher = entity.getTeacherAccount();
        List<ProfessionalClassEntity> professionalClasses = entity.getProfessionalClasses().stream()
                .sorted(Comparator.comparing(ProfessionalClassEntity::getName))
                .toList();
        return new TeachingAssignment(
                entity.getId(),
                entity.getCourseName(),
                entity.getClassName(),
                entity.getCourseCode(),
                entity.getCreditHours(),
                entity.getCredits(),
                entity.getSemester(),
                entity.getCollege(),
                entity.getGrade(),
                teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getUsername(),
                teacher == null ? null : teacher.getDisplayName(),
                professionalClasses.stream().map(ProfessionalClassEntity::getId).toList(),
                professionalClasses.stream().map(ProfessionalClassEntity::getName).toList()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional(readOnly = true)
    public List<RegularGrade> listRegularGrades(UserAccountEntity actor, Long teachingAssignmentId) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(teachingAssignmentId, actor);
        List<Student> students = listStudents(actor, teachingAssignmentId);
        Map<Long, RegularGradeEntity> gradeMap = regularGradeRepository.findAllByTeachingAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(g -> g.getStudent().getId(), g -> g, (a, b) -> a, LinkedHashMap::new));

        // 获取期末考试成绩
        Map<Long, Integer> examScoreMap = new LinkedHashMap<>();
        List<ExamResult> examResults = listResults(actor, teachingAssignmentId);
        for (ExamResult result : examResults) {
            examScoreMap.putIfAbsent(result.getStudent().getId(), result.getTotalScore());
        }

        List<RegularGrade> result = new ArrayList<>();
        for (Student student : students) {
            RegularGradeEntity entity = gradeMap.get(student.getId());
            Integer examScore = examScoreMap.get(student.getId());
            Integer labScore = entity != null ? entity.getLabScore() : null;
            Integer homeworkScore = entity != null ? entity.getHomeworkScore() : null;
            Integer classScore = entity != null ? entity.getClassScore() : null;
            Double totalScore = calculateTotalScore(labScore, homeworkScore, classScore, examScore);

            result.add(new RegularGrade(
                    entity != null ? entity.getId() : null,
                    student.getId(), student.getStudentNo(), student.getName(),
                    teachingAssignmentId, labScore, homeworkScore, classScore,
                    examScore, totalScore,
                    entity != null ? entity.getUpdatedAt() : null));
        }
        return result;
    }

    private Double calculateTotalScore(Integer labScore, Integer homeworkScore, Integer classScore, Integer examScore) {
        if (labScore == null && homeworkScore == null && classScore == null && examScore == null) {
            return null;
        }
        double lab = labScore != null ? labScore : 0;
        double hw = homeworkScore != null ? homeworkScore : 0;
        double cls = classScore != null ? classScore : 0;
        double exam = examScore != null ? examScore : 0;
        // 平时成绩: 上机12% + 作业12% + 课堂表现6% = 30%
        // 期末成绩: 70%
        return Math.round((lab * 0.12 + hw * 0.12 + cls * 0.06 + exam * 0.70) * 100.0) / 100.0;
    }

    @Transactional
    public List<RegularGrade> saveRegularGrades(UserAccountEntity actor, RegularGradeSaveRequest request) {
        TeachingAssignmentEntity assignment = loadAssignmentForActor(request.teachingAssignmentId(), actor);
        for (RegularGradeSaveRequest.RegularGradeEntry entry : request.grades()) {
            StudentEntity student = studentRepository.findById(entry.studentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "学生不存在"));
            RegularGradeEntity entity = regularGradeRepository
                    .findByStudentIdAndTeachingAssignmentId(student.getId(), assignment.getId())
                    .orElseGet(RegularGradeEntity::new);
            if (entity.getStudent() == null) entity.setStudent(student);
            if (entity.getTeachingAssignment() == null) entity.setTeachingAssignment(assignment);
            entity.setLabScore(clampScore(entry.labScore()));
            entity.setHomeworkScore(clampScore(entry.homeworkScore()));
            entity.setClassScore(clampScore(entry.classScore()));
            entity.setUpdatedAt(LocalDateTime.now());
            regularGradeRepository.save(entity);
        }
        return listRegularGrades(actor, request.teachingAssignmentId());
    }

    private Integer clampScore(Integer score) {
        if (score == null) return null;
        return Math.max(0, Math.min(100, score));
    }

    private record ImportRow(String studentNo, String name) {
    }

    private record ProfessionalClassImportRow(String studentNo, String name, String className) {
    }

    private record QuestionImportRow(String courseName, String title, String type, String objective, String score,
                                     String options, String answer, String analysis) {
    }
}

