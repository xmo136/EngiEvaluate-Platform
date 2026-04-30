package com.example.assessment.service;

import com.example.assessment.dto.AnalysisSummary;
import com.example.assessment.model.AnswerRecord;
import com.example.assessment.model.CourseObjective;
import com.example.assessment.model.ExamResult;
import com.example.assessment.model.Question;
import com.example.assessment.model.RegularGrade;
import com.example.assessment.model.Student;
import com.example.assessment.persistence.entity.TeachingAssignmentEntity;
import com.example.assessment.persistence.entity.UserAccountEntity;
import com.example.assessment.persistence.repository.RegularGradeRepository;
import com.example.assessment.persistence.repository.TeachingAssignmentRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private static final String SCORE_ANALYSIS_TEMPLATE = "templates/score-analysis-template.xls";
    private static final String OBJECTIVE_REPORT_TEMPLATE = "templates/objective-report-template.docx";
    private static final int FIRST_STUDENT_ROW = 3;
    private static final int FIRST_SCORE_COL = 6;
    private static final int LAST_SCORE_COL = 29;

    private final AssessmentService assessmentService;
    private final OpenAiGradingService openAiGradingService;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final RegularGradeRepository regularGradeRepository;

    public ReportService(AssessmentService assessmentService, OpenAiGradingService openAiGradingService,
                         TeachingAssignmentRepository teachingAssignmentRepository,
                         RegularGradeRepository regularGradeRepository) {
        this.assessmentService = assessmentService;
        this.openAiGradingService = openAiGradingService;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.regularGradeRepository = regularGradeRepository;
    }

    public byte[] buildScoreAnalysisExcel(UserAccountEntity actor, Long teachingAssignmentId) throws IOException {
        try (InputStream inputStream = new ClassPathResource(SCORE_ANALYSIS_TEMPLATE).getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            TeachingAssignmentEntity assignment = teachingAssignmentRepository.findById(teachingAssignmentId).orElse(null);
            List<Student> students = assessmentService.listStudents(actor, teachingAssignmentId);
            List<Question> questions = assessmentService.listAnalysisQuestions(actor, teachingAssignmentId);
            Map<Long, ExamResult> latestResults = latestResultsByStudent(actor, teachingAssignmentId);
            List<RegularGrade> regularGrades = assessmentService.listRegularGrades(actor, teachingAssignmentId);
            Map<Long, RegularGrade> regularGradeMap = regularGrades.stream()
                    .collect(Collectors.toMap(RegularGrade::getStudentId, g -> g, (a, b) -> a));
            String college = assignment != null && assignment.getCollege() != null ? assignment.getCollege() : "计算机科学与技术学院";
            String grade = assignment != null && assignment.getGrade() != null ? assignment.getGrade() : "2018";

            int lastStudentRow = FIRST_STUDENT_ROW + students.size() - 1;
            fillExcelStudents(sheet, students, questions, latestResults, regularGradeMap, college, grade);
            fillExcelQuestionMapping(sheet, questions, lastStudentRow);
            fillExcelSummaryFormulas(sheet, lastStudentRow);
            fillExcelConfigAndAttainment(sheet, questions, lastStudentRow);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] buildObjectiveReportDocx(UserAccountEntity actor, Long teachingAssignmentId) throws IOException {
        try (InputStream inputStream = new ClassPathResource(OBJECTIVE_REPORT_TEMPLATE).getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            fillWordReport(document, actor, teachingAssignmentId);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillExcelStudents(Sheet sheet,
                                   List<Student> students,
                                   List<Question> questions,
                                   Map<Long, ExamResult> latestResults,
                                   Map<Long, RegularGrade> regularGradeMap,
                                   String college,
                                   String grade) {
        String scopedCourseName = questions.stream()
                .map(Question::getCourseName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseGet(() -> latestResults.values().stream()
                        .map(ExamResult::getCourseName)
                        .filter(name -> name != null && !name.isBlank())
                        .findFirst()
                        .orElse(""));

        int maxRow = FIRST_STUDENT_ROW + students.size();
        for (int rowIndex = FIRST_STUDENT_ROW; rowIndex < maxRow; rowIndex++) {
            Row row = getRow(sheet, rowIndex);
            int studentIndex = rowIndex - FIRST_STUDENT_ROW;
            Student student = students.get(studentIndex);
            ExamResult result = latestResults.get(student.getId());
            RegularGrade regularGrade = regularGradeMap.get(student.getId());
            setCell(row, 0, student.getStudentNo());
            setCell(row, 1, student.getName());
            setCell(row, 2, college);
            setCell(row, 3, student.getClassName());
            setCell(row, 4, grade);
            setCell(row, 5, result == null || result.getCourseName() == null || result.getCourseName().isBlank()
                    ? scopedCourseName
                    : result.getCourseName());

            for (int col = FIRST_SCORE_COL; col <= LAST_SCORE_COL; col++) {
                setBlank(row, col);
            }

            if (result == null && regularGrade == null) {
                clearCalculatedCells(row);
                continue;
            }

            if (result != null) {
                Map<Long, AnswerRecord> answerMap = result.getAnswers().stream()
                        .collect(Collectors.toMap(AnswerRecord::getQuestionId, Function.identity(), (a, b) -> a));
                for (int i = 0; i < Math.min(questions.size(), LAST_SCORE_COL - FIRST_SCORE_COL + 1); i++) {
                    AnswerRecord answer = answerMap.get(questions.get(i).getId());
                    setCell(row, FIRST_SCORE_COL + i, answer == null ? 0 : answer.getScore());
                }
            }

            int excelRow = rowIndex + 1;
            if (result != null) {
                setFormula(row, 30, "SUM(G" + excelRow + ":AD" + excelRow + ")");
            } else {
                setCell(row, 30, 0);
            }

            // Regular grades: AF=课堂表现(classScore), AG=实验(labScore), AH=作业(homeworkScore)
            int classScore = regularGrade != null && regularGrade.getClassScore() != null ? regularGrade.getClassScore() : 0;
            int labScore = regularGrade != null && regularGrade.getLabScore() != null ? regularGrade.getLabScore() : 0;
            int homeworkScore = regularGrade != null && regularGrade.getHomeworkScore() != null ? regularGrade.getHomeworkScore() : 0;
            setCell(row, 31, classScore);
            setCell(row, 32, labScore);
            setCell(row, 33, homeworkScore);
            setFormula(row, 34, "AF" + excelRow + "*0.2+AG" + excelRow + "*0.4+AH" + excelRow + "*0.4");
            setFormula(row, 35, "AE" + excelRow + "*0.7+AI" + excelRow + "*0.3");
        }
    }

    private void fillExcelQuestionMapping(Sheet sheet, List<Question> questions, int lastStudentRow) {
        int obj1Row = lastStudentRow + 3;
        int obj2Row = lastStudentRow + 4;
        int obj3Row = lastStudentRow + 5;
        int obj4Row = lastStudentRow + 6;
        int fullScoreRow = lastStudentRow + 7;
        int weightedScoreRow = lastStudentRow + 8;
        int standardScoreRow = lastStudentRow + 9;
        int rateRow = lastStudentRow + 10;

        Row header = getRow(sheet, 2);
        Row obj1 = getRow(sheet, obj1Row);
        Row obj2 = getRow(sheet, obj2Row);
        Row obj3 = getRow(sheet, obj3Row);
        Row obj4 = getRow(sheet, obj4Row);
        Row fullScore = getRow(sheet, fullScoreRow);
        Row weightedScore = getRow(sheet, weightedScoreRow);
        Row standardScore = getRow(sheet, standardScoreRow);
        Row rate = getRow(sheet, rateRow);

        // 普通平均分行 = lastStudentRow + 2
        int plainAvgRow = lastStudentRow + 2;

        for (int col = FIRST_SCORE_COL; col <= LAST_SCORE_COL; col++) {
            int index = col - FIRST_SCORE_COL;
            Question question = index < questions.size() ? questions.get(index) : null;
            setCell(header, col, question == null ? "" : questionNo(index));
            setCell(obj1, col, objectiveFlag(question, CourseObjective.OBJECTIVE_1));
            setCell(obj2, col, objectiveFlag(question, CourseObjective.OBJECTIVE_2));
            setCell(obj3, col, objectiveFlag(question, CourseObjective.OBJECTIVE_3));
            setCell(obj4, col, objectiveFlag(question, CourseObjective.OBJECTIVE_4));
            setCell(fullScore, col, question == null ? 0 : question.getScore());

            // 比例 = 满分 * 0.7
            setFormula(weightedScore, col, cellRef(col, fullScoreRow) + "*0.7");
            // 标准分 = 普通平均分 * 0.7
            setFormula(standardScore, col, cellRef(col, plainAvgRow) + "*0.7");
            // 得分率 = 标准分 / 比例
            setFormula(rate, col, cellRef(col, standardScoreRow) + "/" + cellRef(col, weightedScoreRow));
        }
    }

    private void fillExcelSummaryFormulas(Sheet sheet, int lastStudentRow) {
        // lastStudentRow + 1: 加权平均分（考试列 AVERAGE*0.7，平时成绩列 plain AVERAGE）
        Row weightedAvgRow = getRow(sheet, lastStudentRow + 1);
        for (int col = FIRST_SCORE_COL; col <= LAST_SCORE_COL; col++) {
            String column = columnName(col);
            setFormula(weightedAvgRow, col, "AVERAGE(" + column + (FIRST_STUDENT_ROW + 1) + ":" + column + lastStudentRow + ")*0.7");
        }
        for (int col = 30; col <= 35; col++) {
            String column = columnName(col);
            setFormula(weightedAvgRow, col, "AVERAGE(" + column + (FIRST_STUDENT_ROW + 1) + ":" + column + lastStudentRow + ")");
        }

        // lastStudentRow + 2: 普通平均分（plain AVERAGE）
        Row plainAvgRow = getRow(sheet, lastStudentRow + 2);
        for (int col = FIRST_SCORE_COL; col <= 35; col++) {
            String column = columnName(col);
            setFormula(plainAvgRow, col, "AVERAGE(" + column + (FIRST_STUDENT_ROW + 1) + ":" + column + lastStudentRow + ")");
        }
    }

    private void fillExcelConfigAndAttainment(Sheet sheet, List<Question> questions, int lastStudentRow) {
        // Template rows 3-6 (AX-BA) already have correct per-objective values — preserve them.
        // Only overwrite BB column totals and row 7 normalization.

        // BB column (53): hardcoded totals
        Row row3 = getRow(sheet, 3);
        Row row4 = getRow(sheet, 4);
        Row row5 = getRow(sheet, 5);
        Row row6 = getRow(sheet, 6);
        Row row7 = getRow(sheet, 7);
        setCell(row3, 53, 70);  // BB4 = exam full score total
        setCell(row4, 53, 12);  // BB5 = lab weight total
        setCell(row5, 53, 12);  // BB6 = homework weight total
        setCell(row6, 53, 6);   // BB7 = class weight total
        // Row 7 (AX8-BA8): normalization = lab + homework + class per objective
        setCell(row7, 49, 25);  // AX8
        setCell(row7, 50, 17);  // AY8
        setCell(row7, 51, 20);  // AZ8
        setCell(row7, 52, 38);  // BA8
        setCell(row7, 53, 100); // BB8

        // AQ-AT per-objective attainment formulas (columns 42-45) for each student row
        // =(exam*BB$4 + AG*labWeight + AH*homeworkWeight + AF*classWeight) / (100*normalization)
        for (int rowIndex = FIRST_STUDENT_ROW; rowIndex <= lastStudentRow; rowIndex++) {
            Row row = getRow(sheet, rowIndex);
            int excelRow = rowIndex + 1;
            setFormula(row, 42, "(AM" + excelRow + "*BB$4+AG" + excelRow + "*AX$5+AH" + excelRow + "*AX$6+AF" + excelRow + "*AX$7)/(100*AX$8)");
            setFormula(row, 43, "(AN" + excelRow + "*BB$4+AG" + excelRow + "*AY$5+AH" + excelRow + "*AY$6+AF" + excelRow + "*AY$7)/(100*AY$8)");
            setFormula(row, 44, "(AO" + excelRow + "*BB$4+AG" + excelRow + "*AZ$5+AH" + excelRow + "*AZ$6+AF" + excelRow + "*AZ$7)/(100*AZ$8)");
            setFormula(row, 45, "(AP" + excelRow + "*BB$4+AG" + excelRow + "*BA$5+AH" + excelRow + "*BA$6+AF" + excelRow + "*BA$7)/(100*BA$8)");
        }

        // AW-AY score distribution (rows 10-17, 0-indexed)
        // AE4:AE103 = exam total score range (fixed template range)
        Row aw11 = getRow(sheet, 11);
        Row aw12 = getRow(sheet, 12);
        Row aw13 = getRow(sheet, 13);
        Row aw14 = getRow(sheet, 14);
        Row aw15 = getRow(sheet, 15);
        Row aw16 = getRow(sheet, 16);
        setCell(aw11, 48, 59.9);
        setCell(aw12, 48, 69.9);
        setCell(aw13, 48, 79.9);
        setCell(aw14, 48, 89.9);
        setCell(aw15, 48, 100.0);
        // AX column: COUNTIF formulas
        setFormula(aw11, 49, "COUNTIF(AE4:AE103,\"<=59.9\")");
        setFormula(aw12, 49, "COUNTIF(AE4:AE103,\"<=69.9\")-AX12");
        setFormula(aw13, 49, "COUNTIF(AE4:AE103,\"<=79.9\")-AX13");
        setFormula(aw14, 49, "COUNTIF(AE4:AE103,\"<=89.9\")-AX14");
        setFormula(aw15, 49, "COUNTIF(AE4:AE103,\"<=100\")-AX15");
        // AY column: ratio formulas
        setFormula(aw11, 50, "AX12/AX$17");
        setFormula(aw12, 50, "AX13/AX$17");
        setFormula(aw13, 50, "AX14/AX$17");
        setFormula(aw14, 50, "AX15/AX$17");
        setFormula(aw15, 50, "AX16/AX$17");
        // Row 16 (0-indexed) = Excel row 17: total students
        setFormula(aw16, 49, "SUM(AX12:AX16)");
    }

    private void fillWordReport(XWPFDocument document, UserAccountEntity actor, Long teachingAssignmentId) {
        if (document.getTables().isEmpty()) {
            return;
        }
        XWPFTable table = document.getTables().get(0);
        TeachingAssignmentEntity assignment = teachingAssignmentRepository.findById(teachingAssignmentId).orElse(null);
        List<Student> students = assessmentService.listStudents(actor, teachingAssignmentId);
        List<ExamResult> results = assessmentService.listResults(actor, teachingAssignmentId);
        List<Question> questions = assessmentService.listAnalysisQuestions(actor, teachingAssignmentId);
        List<RegularGrade> regularGrades = assessmentService.listRegularGrades(actor, teachingAssignmentId);
        AnalysisSummary analysis = assessmentService.analysis(actor, teachingAssignmentId);
        Map<CourseObjective, Double> attainment = objectiveAttainment(actor, teachingAssignmentId);

        fillCourseInfo(table, assignment, actor);
        setCellText(table, 4, 1, String.valueOf(students.size()));
        fillObjectiveDescriptions(table, questions);
        fillScoreDistribution(table, results);
        fillRegularGradeSummary(table, regularGrades);
        fillQuestionAttainment(table, questions, results, attainment);

        setCellText(table, 40, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_1)));
        setCellText(table, 51, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_2)));
        setCellText(table, 61, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_3)));
        setCellText(table, 66, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_4)));

        setCellText(table, 75, 0, buildObjectiveSummary(attainment, analysis));

        Map<String, Double> attainmentRatios = new java.util.LinkedHashMap<>();
        for (var entry : attainment.entrySet()) {
            attainmentRatios.put(entry.getKey().getLabel(), entry.getValue());
        }
        Map<String, String> perObjective = openAiGradingService.generatePerObjectiveSuggestions(
                attainmentRatios, analysis.averageScore());
        setCellText(table, 78, 0, perObjective.getOrDefault("课程目标1",
                "评价结果分析：" + objectiveText("课程目标1", attainment.get(CourseObjective.OBJECTIVE_1))
                        + "持续改进：建议加强相关练习。"));
        setCellText(table, 80, 0, perObjective.getOrDefault("课程目标2",
                "评价结果分析：" + objectiveText("课程目标2", attainment.get(CourseObjective.OBJECTIVE_2))
                        + "持续改进：建议加强相关练习。"));
        setCellText(table, 82, 0, perObjective.getOrDefault("课程目标3",
                "评价结果分析：" + objectiveText("课程目标3", attainment.get(CourseObjective.OBJECTIVE_3))
                        + "持续改进：建议加强相关练习。"));
        setCellText(table, 84, 0, perObjective.getOrDefault("课程目标4",
                "评价结果分析：" + objectiveText("课程目标4", attainment.get(CourseObjective.OBJECTIVE_4))
                        + "持续改进：建议加强相关练习。"));
    }

    private void fillCourseInfo(XWPFTable table, TeachingAssignmentEntity assignment, UserAccountEntity actor) {
        String courseCode = assignment != null && assignment.getCourseCode() != null ? assignment.getCourseCode() : "";
        String courseName = assignment != null ? assignment.getCourseName() : "";
        String creditHours = assignment != null && assignment.getCreditHours() != null ? String.valueOf(assignment.getCreditHours()) : "";
        String credits = assignment != null && assignment.getCredits() != null ? String.valueOf(assignment.getCredits()) : "";
        String semester = assignment != null && assignment.getSemester() != null ? assignment.getSemester() : "";
        String teacherName = assignment != null && assignment.getTeacherAccount() != null ? assignment.getTeacherAccount().getDisplayName() : "";
        String className = assignment != null ? assignment.getClassName() : "";

        setCellText(table, 1, 1, courseCode);
        setCellText(table, 1, 3, courseName);
        setCellText(table, 2, 1, creditHours);
        setCellText(table, 2, 3, credits);
        setCellText(table, 2, 5, teacherName);
        setCellText(table, 2, 7, className);
        setCellText(table, 3, 1, semester);
    }

    private void fillObjectiveDescriptions(XWPFTable table, List<Question> questions) {
        Map<CourseObjective, List<Question>> byObjective = questions.stream()
                .filter(q -> q.getObjective() != null)
                .collect(Collectors.groupingBy(Question::getObjective, LinkedHashMap::new, Collectors.toList()));
        StringBuilder sb = new StringBuilder();
        for (CourseObjective obj : CourseObjective.values()) {
            List<Question> qs = byObjective.getOrDefault(obj, List.of());
            if (!qs.isEmpty()) {
                int totalScore = qs.stream().mapToInt(Question::getScore).sum();
                sb.append(obj.getLabel()).append("：共").append(qs.size()).append("题，满分").append(totalScore).append("分。");
                String titles = qs.stream().limit(3).map(q -> q.getTitle().length() > 20 ? q.getTitle().substring(0, 20) + "..." : q.getTitle()).collect(Collectors.joining("；"));
                sb.append(titles).append("\n");
            }
        }
        if (sb.length() > 0) {
            setCellText(table, 6, 0, sb.toString().trim());
        }
    }

    private void fillRegularGradeSummary(XWPFTable table, List<RegularGrade> regularGrades) {
        if (regularGrades.isEmpty()) {
            return;
        }
        long labCount = regularGrades.stream().filter(g -> g.getLabScore() != null).count();
        long hwCount = regularGrades.stream().filter(g -> g.getHomeworkScore() != null).count();
        long clsCount = regularGrades.stream().filter(g -> g.getClassScore() != null).count();
        double labAvg = regularGrades.stream().filter(g -> g.getLabScore() != null).mapToInt(RegularGrade::getLabScore).average().orElse(0);
        double hwAvg = regularGrades.stream().filter(g -> g.getHomeworkScore() != null).mapToInt(RegularGrade::getHomeworkScore).average().orElse(0);
        double clsAvg = regularGrades.stream().filter(g -> g.getClassScore() != null).mapToInt(RegularGrade::getClassScore).average().orElse(0);

        setCellText(table, 11, 1, String.valueOf(labCount));
        setCellText(table, 11, 2, String.format("%.1f", labAvg));
        setCellText(table, 13, 1, String.valueOf(hwCount));
        setCellText(table, 13, 2, String.format("%.1f", hwAvg));
        setCellText(table, 15, 1, String.valueOf(clsCount));
        setCellText(table, 15, 2, String.format("%.1f", clsAvg));
    }

    private void fillQuestionAttainment(XWPFTable table, List<Question> questions, List<ExamResult> results,
                                        Map<CourseObjective, Double> attainment) {
        if (questions.isEmpty() || results.isEmpty()) {
            return;
        }
        Map<CourseObjective, Integer> fullScores = new EnumMap<>(CourseObjective.class);
        for (Question q : questions) {
            fullScores.merge(q.getObjective(), q.getScore(), Integer::sum);
        }
        Map<Long, Double> questionAvgScores = new LinkedHashMap<>();
        for (Question q : questions) {
            double avg = results.stream()
                    .mapToInt(r -> r.getAnswers().stream()
                            .filter(a -> a.getQuestionId().equals(q.getId()))
                            .mapToInt(AnswerRecord::getScore)
                            .findFirst().orElse(0))
                    .average().orElse(0);
            questionAvgScores.put(q.getId(), Math.round(avg * 10.0) / 10.0);
        }
        int rowIdx = 40;
        for (int i = 0; i < questions.size() && rowIdx <= 74; i++) {
            Question q = questions.get(i);
            double avg = questionAvgScores.getOrDefault(q.getId(), 0.0);
            int fullScore = q.getScore();
            double ratio = fullScore > 0 ? Math.round(avg / fullScore * 1000.0) / 1000.0 : 0;
            setCellText(table, rowIdx, 0, String.valueOf(i + 1));
            setCellText(table, rowIdx, 1, q.getTitle().length() > 15 ? q.getTitle().substring(0, 15) + "..." : q.getTitle());
            setCellText(table, rowIdx, 2, q.getObjective() != null ? q.getObjective().getLabel() : "");
            setCellText(table, rowIdx, 3, String.valueOf(fullScore));
            setCellText(table, rowIdx, 4, String.format("%.1f", avg));
            setCellText(table, rowIdx, 5, formatRatio(ratio));
            rowIdx++;
        }
    }

    private Map<Long, ExamResult> latestResultsByStudent(UserAccountEntity actor, Long teachingAssignmentId) {
        Map<Long, ExamResult> map = new LinkedHashMap<>();
        for (ExamResult result : assessmentService.listResults(actor, teachingAssignmentId)) {
            map.putIfAbsent(result.getStudent().getId(), result);
        }
        return map;
    }

    private Map<CourseObjective, Double> objectiveAttainment(UserAccountEntity actor, Long teachingAssignmentId) {
        List<ExamResult> results = assessmentService.listResults(actor, teachingAssignmentId);
        Map<CourseObjective, Integer> fullScores = assessmentService.objectiveFullScores(actor, teachingAssignmentId);
        Map<CourseObjective, Double> attainment = new EnumMap<>(CourseObjective.class);
        for (CourseObjective objective : CourseObjective.values()) {
            int fullScore = Math.max(1, fullScores.getOrDefault(objective, 0));
            double value = results.stream()
                    .mapToDouble(result -> result.getObjectiveScores().getOrDefault(objective, 0) / (double) fullScore)
                    .average()
                    .orElse(0);
            attainment.put(objective, Math.round(value * 1000.0) / 1000.0);
        }
        return attainment;
    }

    private void fillScoreDistribution(XWPFTable table, List<ExamResult> results) {
        int total = Math.max(1, results.size());
        int[] counts = new int[]{
                (int) results.stream().filter(result -> result.getTotalScore() >= 90).count(),
                (int) results.stream().filter(result -> result.getTotalScore() >= 80 && result.getTotalScore() < 90).count(),
                (int) results.stream().filter(result -> result.getTotalScore() >= 70 && result.getTotalScore() < 80).count(),
                (int) results.stream().filter(result -> result.getTotalScore() >= 60 && result.getTotalScore() < 70).count(),
                (int) results.stream().filter(result -> result.getTotalScore() < 60).count()
        };
        for (int i = 0; i < counts.length; i++) {
            setCellText(table, 9, i + 1, String.valueOf(counts[i]));
            setCellText(table, 10, i + 1, String.format("%.1f", counts[i] * 100.0 / total));
        }
    }

    private String buildObjectiveSummary(Map<CourseObjective, Double> attainment, AnalysisSummary analysis) {
        return "课程目标1达成情况=" + formatRatio(attainment.get(CourseObjective.OBJECTIVE_1))
                + "\n课程目标2达成情况=" + formatRatio(attainment.get(CourseObjective.OBJECTIVE_2))
                + "\n课程目标3达成情况=" + formatRatio(attainment.get(CourseObjective.OBJECTIVE_3))
                + "\n课程目标4达成情况=" + formatRatio(attainment.get(CourseObjective.OBJECTIVE_4))
                + "\n成绩总体平均分为" + analysis.averageScore() + "分。"
                + "当前报告数据由平台题库、学生名单和考试成绩自动生成，反映系统当前已有数据。";
    }

    private String objectiveText(String name, Double value) {
        return name + "当前达成评价值为" + formatRatio(value) + "，达成情况" + level(value) + "。";
    }

    private String level(Double value) {
        double actual = value == null ? 0 : value;
        if (actual >= 0.8) {
            return "良好";
        }
        if (actual >= 0.7) {
            return "中等";
        }
        if (actual >= 0.6) {
            return "基本达成";
        }
        return "需改进";
    }

    private String formatRatio(Double value) {
        return String.format("%.3f", value == null ? 0 : value);
    }

    private int objectiveFlag(Question question, CourseObjective objective) {
        return question != null && question.getObjective() == objective ? 1 : 0;
    }

    private String questionNo(int index) {
        int group = index / 5 + 1;
        int sub = index % 5 + 1;
        String[] groups = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十"};
        String groupLabel = group <= groups.length ? groups[group - 1] : String.valueOf(group);
        return groupLabel + "." + sub;
    }

    private Row getRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private void setCell(Row row, int col, String value) {
        Cell cell = getCell(row, col);
        if (cell.getCellType() == CellType.FORMULA) {
            cell.setBlank();
        }
        cell.setCellValue(value);
    }

    private void setCell(Row row, int col, int value) {
        Cell cell = getCell(row, col);
        if (cell.getCellType() == CellType.FORMULA) {
            cell.setBlank();
        }
        cell.setCellValue(value);
    }

    private void setCell(Row row, int col, double value) {
        Cell cell = getCell(row, col);
        if (cell.getCellType() == CellType.FORMULA) {
            cell.setBlank();
        }
        cell.setCellValue(value);
    }

    private void clearFormulaCells(Row row, int fromCol, int toCol) {
        for (int col = fromCol; col <= toCol; col++) {
            Cell cell = row.getCell(col);
            if (cell != null && cell.getCellType() == CellType.FORMULA) {
                cell.setBlank();
            }
        }
    }

    private void setFormula(Row row, int col, String formula) {
        Cell cell = getCell(row, col);
        cell.setCellFormula(formula);
    }

    private void setBlank(Row row, int col) {
        getCell(row, col).setBlank();
    }

    private Cell getCell(Row row, int col) {
        Cell cell = row.getCell(col);
        return cell == null ? row.createCell(col) : cell;
    }

    private void clearCalculatedCells(Row row) {
        for (int col = 30; col <= 45; col++) {
            setBlank(row, col);
        }
    }

    private void clearRow(Row row) {
        for (int col = 0; col <= 45; col++) {
            setBlank(row, col);
        }
    }

    private String cellRef(int col, int row) {
        return columnName(col) + (row + 1);
    }

    private String columnName(int col) {
        StringBuilder name = new StringBuilder();
        int current = col;
        do {
            name.insert(0, (char) ('A' + current % 26));
            current = current / 26 - 1;
        } while (current >= 0);
        return name.toString();
    }

    private void setCellText(XWPFTable table, int rowIndex, int cellIndex, String text) {
        if (rowIndex >= table.getRows().size()) {
            return;
        }
        List<XWPFTableCell> cells = table.getRow(rowIndex).getTableCells();
        if (cellIndex >= cells.size()) {
            return;
        }
        XWPFTableCell cell = cells.get(cellIndex);
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("宋体");
        run.setFontSize(10);
        run.setText(text == null ? "" : text);
    }
}
