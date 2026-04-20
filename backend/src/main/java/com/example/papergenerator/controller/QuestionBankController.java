package com.example.papergenerator.controller;

import com.example.papergenerator.model.Question;
import com.example.papergenerator.service.SupabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasAnyRole('TEACHER', 'HOD')")
public class QuestionBankController {

    private final SupabaseService supabaseService;

    public QuestionBankController(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    /** Returns all questions, optionally filtered by subject. */
    @GetMapping
    public ResponseEntity<?> questions(@RequestParam(required = false) String subject) {
        try {
            List<Question> list = supabaseService.fetchQuestionsBySubject(subject);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Returns per-(subject,exam) counts so the HOD can see which banks are available. */
    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        try {
            return ResponseEntity.ok(supabaseService.fetchQuestionSummary());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
