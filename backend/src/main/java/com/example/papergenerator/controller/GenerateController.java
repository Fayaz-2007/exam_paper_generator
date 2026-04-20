package com.example.papergenerator.controller;

import com.example.papergenerator.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/generate")
@PreAuthorize("hasRole('HOD')")
public class GenerateController {

    private final QuestionService questionService;

    public GenerateController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Generates a paper per CORE RULES and returns the structure as JSON.
     * Optional `difficulty` query param: easy | medium | hard (default medium).
     */
    @GetMapping
    public ResponseEntity<?> generate(@RequestParam("subject") String subject,
                                      @RequestParam("exam") String exam,
                                      @RequestParam(value = "difficulty", required = false,
                                              defaultValue = "medium") String difficulty) {
        try {
            // Normalize to lowercase codes to match storage format
            subject    = subject.trim().toLowerCase();
            exam       = exam.trim().toLowerCase();
            difficulty = difficulty.trim().toLowerCase();
            Map<String, Object> paper = questionService.generatePaper(subject, exam, difficulty);
            return ResponseEntity.ok(paper);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
