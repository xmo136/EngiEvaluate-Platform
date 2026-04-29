package com.example.assessment.service;

import com.example.assessment.dto.AnalysisSummary;
import com.example.assessment.model.AnswerRecord;
import com.example.assessment.model.CourseObjective;
import com.example.assessment.model.ExamResult;
import com.example.assessment.model.Question;
import com.example.assessment.model.Student;
import org.apache.poi.ss.usermodel.Cell;
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
    private static final int LAST_STUDENT_ROW = 101;
    private static final int FIRST_SCORE_COL = 6;
    private static final int LAST_SCORE_COL = 29;

    private final AssessmentService assessmentService;

    public ReportService(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    public byte[] buildScoreAnalysisExcel() throws IOException {
        try (InputStream inputStream = new ClassPathResource(SCORE_ANALYSIS_TEMPLATE).getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            fillExcelStudents(sheet);
            fillExcelQuestionMapping(sheet);
            fillExcelSummaryFormulas(sheet);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] buildObjectiveReportDocx() throws IOException {
        try (InputStream inputStream = new ClassPathResource(OBJECTIVE_REPORT_TEMPLATE).getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            fillWordReport(document);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillExcelStudents(Sheet sheet) {
        List<Student> students = assessmentService.listStudents();
        List<Question> questions = assessmentService.listQuestions();
        Map<Long, ExamResult> latestResults = latestResultsByStudent();

        for (int rowIndex = FIRST_STUDENT_ROW; rowIndex <= LAST_STUDENT_ROW; rowIndex++) {
            Row row = getRow(sheet, rowIndex);
            int studentIndex = rowIndex - FIRST_STUDENT_ROW;
            if (studentIndex >= students.size()) {
                clearRow(row);
                continue;
            }

            Student student = students.get(studentIndex);
            ExamResult result = latestResults.get(student.getId());
            setCell(row, 0, student.getStudentNo());
            setCell(row, 1, student.getName());
            setCell(row, 2, "计算机科学与技术学院");
            setCell(row, 3, student.getClassName());
            setCell(row, 4, "2018");
            setCell(row, 5, "软件项目策划与管理");

            for (int col = FIRST_SCORE_COL; col <= LAST_SCORE_COL; col++) {
                setBlank(row, col);
            }

            if (result == null) {
                clearCalculatedCells(row);
                continue;
            }

            Map<Long, AnswerRecord> answerMap = result.getAnswers().stream()
                    .collect(Collectors.toMap(AnswerRecord::getQuestionId, Function.identity(), (a, b) -> a));
            for (int i = 0; i < Math.min(questions.size(), LAST_SCORE_COL - FIRST_SCORE_COL + 1); i++) {
                AnswerRecord answer = answerMap.get(questions.get(i).getId());
                setCell(row, FIRST_SCORE_COL + i, answer == null ? 0 : answer.getScore());
            }

            int excelRow = rowIndex + 1;
            setFormula(row, 30, "SUM(G" + excelRow + ":AD" + excelRow + ")");
            setCell(row, 31, processScore(result.getTotalScore(), 4));
            setCell(row, 32, processScore(result.getTotalScore(), 2));
            setCell(row, 33, processScore(result.getTotalScore(), 0));
            setFormula(row, 34, "AF" + excelRow + "*0.2+AG" + excelRow + "*0.4+AH" + excelRow + "*0.4");
            setFormula(row, 35, "AE" + excelRow + "*0.7+AI" + excelRow + "*0.3");
            setFormula(row, 38, "SUMPRODUCT(G" + excelRow + ":AD" + excelRow + ",G$106:AD$106)");
            setFormula(row, 39, "SUMPRODUCT(G" + excelRow + ":AD" + excelRow + ",G$107:AD$107)");
            setFormula(row, 40, "SUMPRODUCT(G" + excelRow + ":AD" + excelRow + ",G$108:AD$108)");
            setFormula(row, 41, "SUMPRODUCT(G" + excelRow + ":AD" + excelRow + ",G$109:AD$109)");
            setFormula(row, 42, "IFERROR(AM" + excelRow + "/SUMPRODUCT(G$110:AD$110,G$106:AD$106),0)");
            setFormula(row, 43, "IFERROR(AN" + excelRow + "/SUMPRODUCT(G$110:AD$110,G$107:AD$107),0)");
            setFormula(row, 44, "IFERROR(AO" + excelRow + "/SUMPRODUCT(G$110:AD$110,G$108:AD$108),0)");
            setFormula(row, 45, "IFERROR(AP" + excelRow + "/SUMPRODUCT(G$110:AD$110,G$109:AD$109),0)");
        }
    }

    private void fillExcelQuestionMapping(Sheet sheet) {
        List<Question> questions = assessmentService.listQuestions();
        Row header = getRow(sheet, 2);
        Row obj1 = getRow(sheet, 105);
        Row obj2 = getRow(sheet, 106);
        Row obj3 = getRow(sheet, 107);
        Row obj4 = getRow(sheet, 108);
        Row fullScore = getRow(sheet, 109);
        Row weightScore = getRow(sheet, 110);
        Row standardScore = getRow(sheet, 111);
        Row rate = getRow(sheet, 112);

        for (int col = FIRST_SCORE_COL; col <= LAST_SCORE_COL; col++) {
            int index = col - FIRST_SCORE_COL;
            Question question = index < questions.size() ? questions.get(index) : null;
            setCell(header, col, question == null ? "" : questionNo(index));
            setCell(obj1, col, objectiveFlag(question, CourseObjective.OBJECTIVE_1));
            setCell(obj2, col, objectiveFlag(question, CourseObjective.OBJECTIVE_2));
            setCell(obj3, col, objectiveFlag(question, CourseObjective.OBJECTIVE_3));
            setCell(obj4, col, objectiveFlag(question, CourseObjective.OBJECTIVE_4));
            setCell(fullScore, col, question == null ? 0 : question.getScore());
            setFormula(weightScore, col, cellRef(col, 110) + "*0.7");
            setFormula(standardScore, col, cellRef(col, 104) + "*0.7");
            setFormula(rate, col, "IFERROR(" + cellRef(col, 111) + "/" + cellRef(col, 110) + ",0)");
        }
    }

    private void fillExcelSummaryFormulas(Sheet sheet) {
        for (int col = FIRST_SCORE_COL; col <= 35; col++) {
            String column = columnName(col);
            setFormula(getRow(sheet, 102), col, "AVERAGE(" + column + "4:" + column + "102)*0.7");
            setFormula(getRow(sheet, 104), col, "AVERAGE(" + column + "4:" + column + "102)");
        }
        Row attainment = getRow(sheet, 104);
        setCell(attainment, 36, "达成度");
        setFormula(getRow(sheet, 105), 36, "SUMPRODUCT(G$105:AD$105,G106:AD106)");
        setFormula(getRow(sheet, 106), 36, "SUMPRODUCT(G$105:AD$105,G107:AD107)");
        setFormula(getRow(sheet, 107), 36, "SUMPRODUCT(G$105:AD$105,G108:AD108)");
        setFormula(getRow(sheet, 108), 36, "SUMPRODUCT(G$105:AD$105,G109:AD109)");
        setFormula(getRow(sheet, 109), 36, "SUM(AK106:AK109)");
    }

    private void fillWordReport(XWPFDocument document) {
        if (document.getTables().isEmpty()) {
            return;
        }
        XWPFTable table = document.getTables().get(0);
        List<Student> students = assessmentService.listStudents();
        List<ExamResult> results = assessmentService.listResults();
        AnalysisSummary analysis = assessmentService.analysis();
        Map<CourseObjective, Double> attainment = objectiveAttainment();

        setCellText(table, 4, 1, String.valueOf(students.size()));
        fillScoreDistribution(table, results);
        setCellText(table, 40, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_1)));
        setCellText(table, 51, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_2)));
        setCellText(table, 61, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_3)));
        setCellText(table, 66, 5, formatRatio(attainment.get(CourseObjective.OBJECTIVE_4)));

        setCellText(table, 75, 0, buildObjectiveSummary(attainment, analysis));
        List<String> suggestions = analysis.improvementSuggestions();
        setCellText(table, 78, 0, "评价结果分析：" + objectiveText("课程目标1", attainment.get(CourseObjective.OBJECTIVE_1))
                + "持续改进：" + suggestions.get(0));
        setCellText(table, 80, 0, "评价结果分析：" + objectiveText("课程目标2", attainment.get(CourseObjective.OBJECTIVE_2))
                + "持续改进：加强风险识别、风险概率分析和风险应对策略练习。");
        setCellText(table, 82, 0, "评价结果分析：" + objectiveText("课程目标3", attainment.get(CourseObjective.OBJECTIVE_3))
                + "持续改进：增加挣值管理、成本监控和项目状态分析题目的讲解。");
        setCellText(table, 84, 0, "评价结果分析：" + objectiveText("课程目标4", attainment.get(CourseObjective.OBJECTIVE_4))
                + "持续改进：强化项目活动分解、资源分配和关键路径分析训练。");
    }

    private Map<Long, ExamResult> latestResultsByStudent() {
        Map<Long, ExamResult> map = new LinkedHashMap<>();
        for (ExamResult result : assessmentService.listResults()) {
            map.putIfAbsent(result.getStudent().getId(), result);
        }
        return map;
    }

    private Map<CourseObjective, Double> objectiveAttainment() {
        List<ExamResult> results = assessmentService.listResults();
        Map<CourseObjective, Integer> fullScores = assessmentService.objectiveFullScores();
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

    private int processScore(int totalScore, int offset) {
        return Math.max(60, Math.min(98, totalScore + offset));
    }

    private int objectiveFlag(Question question, CourseObjective objective) {
        return question != null && question.getObjective() == objective ? 1 : 0;
    }

    private String questionNo(int index) {
        return switch (index) {
            case 0 -> "一.1";
            case 1 -> "一.2";
            case 2 -> "二.1";
            case 3 -> "三.1";
            case 4 -> "三.2";
            default -> "";
        };
    }

    private Row getRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private void setCell(Row row, int col, String value) {
        Cell cell = getCell(row, col);
        cell.setCellValue(value);
    }

    private void setCell(Row row, int col, int value) {
        Cell cell = getCell(row, col);
        cell.setCellValue(value);
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
