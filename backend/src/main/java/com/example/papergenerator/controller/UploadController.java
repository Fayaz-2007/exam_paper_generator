package com.example.papergenerator.controller;

import com.example.papergenerator.model.Question;
import com.example.papergenerator.service.PDFService;
import com.example.papergenerator.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@PreAuthorize("hasRole('TEACHER')")
public class UploadController {

    private final PDFService pdfService;
    private final QuestionService questionService;

    public UploadController(PDFService pdfService, QuestionService questionService) {
        this.pdfService = pdfService;
        this.questionService = questionService;
    }

    /** Accepts a PDF question bank, extracts and persists questions, returns a summary. */
    @PostMapping
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("subject") String subject,
                                         @RequestParam("exam") String exam) {
        try {
            // Normalize to lowercase codes so storage is always consistent
            subject = subject.trim().toLowerCase();
            exam    = exam.trim().toLowerCase();
            List<Question> questions = pdfService.extractQuestions(file, subject, exam);
            if (questions.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    "No questions extracted from the PDF. " +
                    "Ensure the file has 'PART A' and 'PART B' section headers, " +
                    "questions are numbered (e.g. 'Q1.' or '1.'), " +
                    "and CO/RBT tags appear in each line (e.g. 'CO1 RBT1'). " +
                    "Questions with RBT > 2 are automatically discarded.");
            }
            String summary = questionService.saveQuestions(questions);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }
}
