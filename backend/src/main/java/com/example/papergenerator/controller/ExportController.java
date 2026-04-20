package com.example.papergenerator.controller;

import com.example.papergenerator.service.ExportService;
import com.example.papergenerator.service.QuestionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@PreAuthorize("hasRole('HOD')")
public class ExportController {

    private final QuestionService questionService;
    private final ExportService exportService;

    public ExportController(QuestionService questionService, ExportService exportService) {
        this.questionService = questionService;
        this.exportService = exportService;
    }

    /**
     * Generates a paper and streams it as a PDF download. Returns 400 on rule violations.
     * Optional `difficulty` query param: easy | medium | hard (default medium).
     */
    @GetMapping
    public ResponseEntity<byte[]> export(@RequestParam("subject") String subject,
                                         @RequestParam("exam") String exam,
                                         @RequestParam(value = "difficulty", required = false,
                                                 defaultValue = "medium") String difficulty) {
        try {
            subject    = subject.trim().toLowerCase();
            exam       = exam.trim().toLowerCase();
            difficulty = difficulty.trim().toLowerCase();
            byte[] pdf = exportService.generatePdf(
                    questionService.generatePaper(subject, exam, difficulty), subject, exam);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "paper_" + subject + "_" + exam + "_" + difficulty + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (RuntimeException e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return ResponseEntity.badRequest().headers(headers).body(e.getMessage().getBytes());
        }
    }
}
