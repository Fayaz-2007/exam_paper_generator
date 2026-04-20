package com.example.papergenerator.service;

import com.example.papergenerator.model.Question;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PDFService {

    // Matches question numbering: Q1. Q1) Q1: Q11 (a)  |  1. 1) 1: 1  |  (1)
    private static final Pattern Q_NUM = Pattern.compile(
        "^(?:Q\\d+[.):>\\s]|\\(\\d+\\)|\\d+[.):\\s])", Pattern.CASE_INSENSITIVE);

    // After stripping Q_NUM, if only a sub-label remains (e.g. "(a)", "(b)") the number is standalone
    private static final Pattern SUB_LABEL = Pattern.compile(
        "^\\(?\\s*[a-zA-Z]\\s*\\)?\\s*$");

    // CO tag: "CO1", "CO: 1", "CO:1" — colon is optional
    private static final Pattern CO_PAT = Pattern.compile(
        "\\bCO\\s*:?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);

    // RBT tag: "RBT1", "RBT: 1", "RBTL1", "L1" — colon and L-prefix are optional
    private static final Pattern RBT_PAT = Pattern.compile(
        "\\bRBT\\s*:?\\s*L?\\s*(\\d+)\\b|\\bL(\\d+)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern UNIT_PAT = Pattern.compile(
        "\\bUnit\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Extracts questions from an uploaded PDF.
     * Handles both single-line format ("1. Text CO1 RBT1") and multi-line format
     * where the number, text, and CO/RBT tag each appear on their own lines.
     * Questions with RBT > 2 are discarded per CORE RULES.
     */
    public List<Question> extractQuestions(MultipartFile file, String subject, String exam) {
        List<Question> questions = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            String[] lines = text.split("\\r?\\n");

            String currentPart = "A";
            int unit = 1;

            // Multi-line accumulator state
            StringBuilder pendingText = new StringBuilder();
            String pendingCo = "1";
            String pendingRbt = "1";
            boolean collecting = false;

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isBlank()) continue;

                // Skip "— OR —" separators between Part B question pairs
                if (line.replaceAll("[^a-zA-Z]", "").equalsIgnoreCase("OR")) continue;

                // Detect PART A / PART B section headers
                if (line.toUpperCase().contains("PART A") || line.toUpperCase().contains("PART-A")) {
                    if (collecting && pendingText.length() > 0) {
                        addQuestion(questions, pendingText.toString().trim(), subject, exam,
                                    currentPart, pendingCo, pendingRbt, unit);
                    }
                    pendingText = new StringBuilder(); pendingCo = "1"; pendingRbt = "1"; collecting = false;
                    currentPart = "A"; continue;
                }
                if (line.toUpperCase().contains("PART B") || line.toUpperCase().contains("PART-B")) {
                    if (collecting && pendingText.length() > 0) {
                        addQuestion(questions, pendingText.toString().trim(), subject, exam,
                                    currentPart, pendingCo, pendingRbt, unit);
                    }
                    pendingText = new StringBuilder(); pendingCo = "1"; pendingRbt = "1"; collecting = false;
                    currentPart = "B"; continue;
                }

                // Detect unit markers
                Matcher um = UNIT_PAT.matcher(line);
                if (um.find()) unit = Integer.parseInt(um.group(1));

                // Detect a standalone CO/RBT tag line — this finalizes the pending question.
                // Only treat as a CO/RBT line if it is NOT also a question-number line.
                Matcher coM = CO_PAT.matcher(line);
                Matcher rbtM = RBT_PAT.matcher(line);
                boolean hasCo = coM.find();
                boolean hasRbt = rbtM.find();
                if ((hasCo || hasRbt) && !Q_NUM.matcher(line).find()) {
                    if (hasCo) pendingCo = coM.group(1);
                    if (hasRbt) pendingRbt = rbtM.group(1) != null ? rbtM.group(1) : rbtM.group(2);
                    if (collecting && pendingText.length() > 0) {
                        addQuestion(questions, pendingText.toString().trim(), subject, exam,
                                    currentPart, pendingCo, pendingRbt, unit);
                        pendingText = new StringBuilder(); pendingCo = "1"; pendingRbt = "1";
                        collecting = false;
                    }
                    continue;
                }

                // Detect a question number line
                if (Q_NUM.matcher(line).find()) {
                    // Finalize any in-progress question before starting a new one
                    if (collecting && pendingText.length() > 0) {
                        addQuestion(questions, pendingText.toString().trim(), subject, exam,
                                    currentPart, pendingCo, pendingRbt, unit);
                        pendingText = new StringBuilder(); pendingCo = "1"; pendingRbt = "1";
                    }

                    String remainder = Q_NUM.matcher(line).replaceFirst("").trim();

                    if (remainder.isBlank() || SUB_LABEL.matcher(remainder).matches()) {
                        // Number-only line (e.g. "1." or "Q11 (a)") — text follows on next line(s)
                        collecting = true;
                        continue;
                    }

                    // Remainder contains the question text, possibly with inline CO/RBT tags
                    Matcher cm = CO_PAT.matcher(remainder);
                    Matcher rm = RBT_PAT.matcher(remainder);
                    String co = "1", rbt = "1";
                    if (cm.find()) {
                        co = cm.group(1);
                        remainder = remainder.replaceAll("\\bCO\\s*:?\\s*\\d+\\b", "").trim();
                    }
                    if (rm.find()) {
                        rbt = rm.group(1) != null ? rm.group(1) : rm.group(2);
                        remainder = remainder.replaceAll("\\bRBT\\s*:?\\s*L?\\s*\\d+\\b|\\bL\\d+\\b", "").trim();
                    }
                    if (!remainder.isBlank()) {
                        pendingText = new StringBuilder(remainder);
                        pendingCo = co; pendingRbt = rbt;
                        collecting = true;
                    }
                    continue;
                }

                // Accumulate as continuation of the current question text
                if (collecting) {
                    if (pendingText.length() > 0) pendingText.append(" ");
                    pendingText.append(line);
                }
            }

            // Finalize the last question in the file
            if (collecting && pendingText.length() > 0) {
                addQuestion(questions, pendingText.toString().trim(), subject, exam,
                            currentPart, pendingCo, pendingRbt, unit);
            }

        } catch (Exception e) {
            throw new RuntimeException("PDF parsing failed: " + e.getMessage(), e);
        }
        return questions;
    }

    /** Adds a question after validating text is non-blank and RBT ≤ 2 (CORE RULE). */
    private void addQuestion(List<Question> questions, String qText, String subject, String exam,
                              String part, String co, String rbt, int unit) {
        if (qText.isBlank()) return;
        try { if (Integer.parseInt(rbt) > 2) return; } catch (NumberFormatException ignored) {}

        Question q = new Question();
        q.setQuestionText(qText);
        q.setSubject(subject);
        q.setExam(exam);
        q.setPart(part);
        q.setCo(co);
        q.setRbt(rbt);
        q.setUnit(unit);
        questions.add(q);
    }
}
