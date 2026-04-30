package com.example.assessment.controller;

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
import com.example.assessment.model.ExamPaperSummary;
import com.example.assessment.model.ExamResult;
import com.example.assessment.model.MockExamResultGenerationResult;
import com.example.assessment.model.RegularGrade;
import com.example.assessment.model.QuestionImportResult;
import com.example.assessment.model.ProfessionalClassImportResult;
import com.example.assessment.model.ProfessionalClassOption;
import com.example.assessment.model.Question;
import com.example.assessment.model.Student;
import com.example.assessment.model.StudentImportResult;
import com.example.assessment.model.TeacherAccountOption;
import com.example.assessment.model.TeachingAssignment;
import com.example.assessment.model.UserRole;
import com.example.assessment.persistence.entity.UserAccountEntity;
import com.example.assessment.service.AssessmentService;
import com.example.assessment.service.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class AssessmentController {
    private final AssessmentService assessmentService;
    private final AuthService authService;

    public AssessmentController(AssessmentService assessmentService, AuthService authService) {
        this.assessmentService = assessmentService;
        this.authService = authService;
    }

    @GetMapping("/questions")
    public List<Question> questions(@RequestHeader(value = "X-Username", required = false) String username,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.listQuestions();
    }

    @PostMapping("/questions")
    public Question addQuestion(@RequestHeader(value = "X-Username", required = false) String username,
                                @RequestHeader(value = "X-User-Role", required = false) String role,
                                @RequestBody Question question) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.addQuestion(question);
    }

    @PatchMapping("/questions/{questionId}")
    public Question updateQuestion(@RequestHeader(value = "X-Username", required = false) String username,
                                   @RequestHeader(value = "X-User-Role", required = false) String role,
                                   @PathVariable Long questionId,
                                   @RequestBody Question question) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.updateQuestion(questionId, question);
    }

    @DeleteMapping("/questions/{questionId}")
    public void deleteQuestion(@RequestHeader(value = "X-Username", required = false) String username,
                               @RequestHeader(value = "X-User-Role", required = false) String role,
                               @PathVariable Long questionId) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        assessmentService.deleteQuestion(questionId);
    }

    @PostMapping("/questions/import")
    public QuestionImportResult importQuestions(@RequestHeader(value = "X-Username", required = false) String username,
                                                @RequestHeader(value = "X-User-Role", required = false) String role,
                                                @RequestParam("file") MultipartFile file) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.importQuestions(file);
    }

    @GetMapping("/teacher-accounts")
    public List<TeacherAccountOption> teacherAccounts(@RequestHeader(value = "X-Username", required = false) String username,
                                                      @RequestHeader(value = "X-User-Role", required = false) String role) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.listTeacherAccounts();
    }

    @PostMapping("/teacher-accounts")
    public TeacherAccountOption addTeacherAccount(@RequestHeader(value = "X-Username", required = false) String username,
                                                  @RequestHeader(value = "X-User-Role", required = false) String role,
                                                  @RequestBody TeacherAccountCreateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.addTeacherAccount(request);
    }

    @PatchMapping("/teacher-accounts/{teacherAccountId}")
    public TeacherAccountOption updateTeacherAccount(@RequestHeader(value = "X-Username", required = false) String username,
                                                     @RequestHeader(value = "X-User-Role", required = false) String role,
                                                     @PathVariable Long teacherAccountId,
                                                     @RequestBody TeacherAccountUpdateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.updateTeacherAccount(teacherAccountId, request);
    }

    @DeleteMapping("/teacher-accounts/{teacherAccountId}")
    public void deleteTeacherAccount(@RequestHeader(value = "X-Username", required = false) String username,
                                     @RequestHeader(value = "X-User-Role", required = false) String role,
                                     @PathVariable Long teacherAccountId) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        assessmentService.deleteTeacherAccount(teacherAccountId);
    }

    @GetMapping("/teaching-assignments")
    public List<TeachingAssignment> teachingAssignments(@RequestHeader(value = "X-Username", required = false) String username,
                                                        @RequestHeader(value = "X-User-Role", required = false) String role) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.listTeachingAssignments(actor);
    }

    @PostMapping("/teaching-assignments")
    public TeachingAssignment addTeachingAssignment(@RequestHeader(value = "X-Username", required = false) String username,
                                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                                    @RequestBody TeachingAssignmentCreateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.addTeachingAssignment(request);
    }

    @PatchMapping("/teaching-assignments/{teachingAssignmentId}")
    public TeachingAssignment updateTeachingAssignment(@RequestHeader(value = "X-Username", required = false) String username,
                                                       @RequestHeader(value = "X-User-Role", required = false) String role,
                                                       @PathVariable Long teachingAssignmentId,
                                                       @RequestBody TeachingAssignmentUpdateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.updateTeachingAssignment(teachingAssignmentId, request);
    }

    @GetMapping("/professional-classes")
    public List<ProfessionalClassOption> professionalClasses(@RequestHeader(value = "X-Username", required = false) String username,
                                                             @RequestHeader(value = "X-User-Role", required = false) String role) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.listProfessionalClasses();
    }

    @PostMapping("/professional-classes/import")
    public ProfessionalClassImportResult importProfessionalClasses(@RequestHeader(value = "X-Username", required = false) String username,
                                                                   @RequestHeader(value = "X-User-Role", required = false) String role,
                                                                   @RequestParam("file") MultipartFile file) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.importProfessionalClasses(file);
    }

    @DeleteMapping("/teaching-assignments/{teachingAssignmentId}")
    public void deleteTeachingAssignment(@RequestHeader(value = "X-Username", required = false) String username,
                                         @RequestHeader(value = "X-User-Role", required = false) String role,
                                         @PathVariable Long teachingAssignmentId) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        assessmentService.deleteTeachingAssignment(teachingAssignmentId);
    }

    @GetMapping("/teaching-assignments/{teachingAssignmentId}/candidate-students")
    public List<Student> candidateStudents(@RequestHeader(value = "X-Username", required = false) String username,
                                           @RequestHeader(value = "X-User-Role", required = false) String role,
                                           @PathVariable Long teachingAssignmentId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.TEACHER);
        return assessmentService.listCandidateStudents(actor, teachingAssignmentId);
    }

    @PostMapping("/teaching-assignments/{teachingAssignmentId}/students/{studentId}")
    public Student addStudentToTeachingAssignment(@RequestHeader(value = "X-Username", required = false) String username,
                                                  @RequestHeader(value = "X-User-Role", required = false) String role,
                                                  @PathVariable Long teachingAssignmentId,
                                                  @PathVariable Long studentId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.TEACHER);
        return assessmentService.addStudentToTeachingAssignment(actor, teachingAssignmentId, studentId);
    }

    @DeleteMapping("/teaching-assignments/{teachingAssignmentId}/students/{studentId}")
    public void removeStudentFromTeachingAssignment(@RequestHeader(value = "X-Username", required = false) String username,
                                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                                    @PathVariable Long teachingAssignmentId,
                                                    @PathVariable Long studentId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.TEACHER);
        assessmentService.removeStudentFromTeachingAssignment(actor, teachingAssignmentId, studentId);
    }

    @GetMapping("/students")
    public List<Student> students(@RequestHeader(value = "X-Username", required = false) String username,
                                  @RequestHeader(value = "X-User-Role", required = false) String role) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.listStudents(actor);
    }

    @GetMapping("/exams")
    public List<ExamPaperSummary> exams(@RequestHeader(value = "X-Username", required = false) String username,
                                        @RequestHeader(value = "X-User-Role", required = false) String role) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.listExams(actor);
    }

    @PostMapping("/exams")
    public ExamPaperSummary createExam(@RequestHeader(value = "X-Username", required = false) String username,
                                       @RequestHeader(value = "X-User-Role", required = false) String role,
                                       @RequestBody ExamPaperSaveRequest request) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.createExam(actor, request);
    }

    @DeleteMapping("/exams/{examId}")
    public void deleteExam(@RequestHeader(value = "X-Username", required = false) String username,
                           @RequestHeader(value = "X-User-Role", required = false) String role,
                           @PathVariable Long examId) {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        assessmentService.deleteExam(examId);
    }

    @PostMapping("/exams/{examId}/mock-results")
    public MockExamResultGenerationResult generateMockResults(@RequestHeader(value = "X-Username", required = false) String username,
                                                              @RequestHeader(value = "X-User-Role", required = false) String role,
                                                              @PathVariable Long examId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.generateMockResults(actor, examId);
    }

    @GetMapping("/exams/{examId}/questions")
    public List<Question> examQuestions(@RequestHeader(value = "X-Username", required = false) String username,
                                        @RequestHeader(value = "X-User-Role", required = false) String role,
                                        @PathVariable Long examId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.listExamQuestions(actor, examId);
    }

    @PostMapping("/students")
    public Student addStudent(@RequestHeader(value = "X-Username", required = false) String username,
                              @RequestHeader(value = "X-User-Role", required = false) String role,
                              @RequestBody StudentCreateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.addStudent(request);
    }

    @PatchMapping("/students/{studentId}")
    public Student updateStudent(@RequestHeader(value = "X-Username", required = false) String username,
                                 @RequestHeader(value = "X-User-Role", required = false) String role,
                                 @PathVariable Long studentId,
                                 @RequestBody StudentUpdateRequest request) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        return assessmentService.updateStudent(studentId, request);
    }

    @DeleteMapping("/students/{studentId}")
    public void deleteStudent(@RequestHeader(value = "X-Username", required = false) String username,
                              @RequestHeader(value = "X-User-Role", required = false) String role,
                              @PathVariable Long studentId) {
        authService.requireAnyRole(username, role, UserRole.ADMIN);
        assessmentService.deleteStudent(studentId);
    }

    @PostMapping("/students/import")
    public StudentImportResult importStudents(@RequestHeader(value = "X-Username", required = false) String username,
                                              @RequestHeader(value = "X-User-Role", required = false) String role,
                                              @RequestParam("teachingAssignmentId") Long teachingAssignmentId,
                                              @RequestParam("file") MultipartFile file) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.TEACHER);
        return assessmentService.importStudents(actor, teachingAssignmentId, file);
    }

    @GetMapping("/results")
    public List<ExamResult> results(@RequestHeader(value = "X-Username", required = false) String username,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.listResults(actor);
    }

    @PostMapping("/exams/submit")
    public ExamResult submit(@RequestHeader(value = "X-Username", required = false) String username,
                             @RequestHeader(value = "X-User-Role", required = false) String role,
                             @RequestBody ExamSubmitRequest request) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.TEACHER, UserRole.STUDENT);
        return assessmentService.submit(actor, request);
    }

    @PatchMapping("/results/score")
    public ExamResult confirmScore(@RequestHeader(value = "X-Username", required = false) String username,
                                   @RequestHeader(value = "X-User-Role", required = false) String role,
                                   @RequestBody ScoreConfirmRequest request) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.confirmScore(actor, request);
    }

    @GetMapping("/analysis")
    public AnalysisSummary analysis(@RequestHeader(value = "X-Username", required = false) String username,
                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                    @RequestParam(value = "teachingAssignmentId", required = false) Long teachingAssignmentId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.analysis(actor, teachingAssignmentId);
    }

    @GetMapping("/regular-grades")
    public List<RegularGrade> regularGrades(@RequestHeader(value = "X-Username", required = false) String username,
                                            @RequestHeader(value = "X-User-Role", required = false) String role,
                                            @RequestParam(value = "teachingAssignmentId") Long teachingAssignmentId) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.listRegularGrades(actor, teachingAssignmentId);
    }

    @PutMapping("/regular-grades")
    public List<RegularGrade> saveRegularGrades(@RequestHeader(value = "X-Username", required = false) String username,
                                                @RequestHeader(value = "X-User-Role", required = false) String role,
                                                @RequestBody RegularGradeSaveRequest request) {
        UserAccountEntity actor = authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return assessmentService.saveRegularGrades(actor, request);
    }
}
