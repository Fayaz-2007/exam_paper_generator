package com.example.papergenerator.service;

import com.example.papergenerator.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final SupabaseService supabaseService;

    public QuestionService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    /** Deduplicates against existing DB texts, inserts new questions, returns summary. */
    public String saveQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "0 questions inserted, 0 duplicates skipped.";
        }
        String subject = questions.get(0).getSubject();
        Set<String> existing = supabaseService.fetchExistingTexts(subject);

        List<Question> toInsert = new ArrayList<>();
        int skipped = 0;
        for (Question q : questions) {
            String text = q.getQuestionText() != null ? q.getQuestionText().trim() : "";
            if (existing.contains(text)) { skipped++; }
            else { toInsert.add(q); }
        }
        if (!toInsert.isEmpty()) supabaseService.insertQuestions(toInsert);
        return toInsert.size() + " questions inserted, " + skipped + " duplicates skipped.";
    }

    /** Returns normalized RBT as int (1 or 2) if allowed, else -1. */
    private int normalizeRbt(String rbt) {
        if (rbt == null) return -1;
        String n = rbt.trim().toLowerCase();
        if (n.equals("1") || n.equals("l1") || n.equals("remember")) return 1;
        if (n.equals("2") || n.equals("l2") || n.equals("understand")) return 2;
        return -1;
    }

    /**
     * Backwards-compatible overload — default difficulty = "medium".
     */
    public Map<String, Object> generatePaper(String subject, String exam) {
        return generatePaper(subject, exam, "medium");
    }

    /**
     * Generates a paper following CORE RULES and applies difficulty bias:
     *  - easy   : prefer RBT 1 questions, fall back to RBT 2 if needed
     *  - medium : balanced mix of RBT 1 and 2
     *  - hard   : prefer RBT 2 questions, fall back to RBT 1 if needed
     *
     * Part A: 10 questions × 2 marks. Part B: 3 OR-pairs (6 questions × 10 marks).
     * Questions with RBT > 2 are discarded per CORE RULES.
     *
     * Response shape:
     *   partA: [{q, unit}, ...]
     *   partB: [{a, b, unit}, ...]
     */
    public Map<String, Object> generatePaper(String subject, String exam, String difficulty) {
        List<Question> all = supabaseService.fetchQuestions(subject, exam);
        if (all == null || all.isEmpty()) {
            throw new RuntimeException("No questions found for subject='" + subject + "' exam='" + exam + "'.");
        }

        // Keep only allowed RBT 1 / 2 questions
        List<Question> filtered = all.stream()
                .filter(q -> normalizeRbt(q.getRbt()) > 0)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new RuntimeException("No eligible questions after RBT filter (only RBT 1 & 2 allowed).");
        }

        // Split by part
        List<Question> poolA = filtered.stream()
                .filter(q -> "A".equalsIgnoreCase(q.getPart()))
                .collect(Collectors.toList());
        List<Question> poolB = filtered.stream()
                .filter(q -> "B".equalsIgnoreCase(q.getPart()))
                .collect(Collectors.toList());

        if (poolA.size() < 10) {
            throw new IllegalStateException(
                    "Not enough Part A questions. Found " + poolA.size() + ", need at least 10.");
        }
        if (poolB.size() < 6) {
            throw new IllegalStateException(
                    "Not enough Part B questions. Found " + poolB.size() + ", need at least 6.");
        }

        // Apply difficulty ordering (preferred RBT bucket first, then the rest)
        poolA = orderByDifficulty(poolA, difficulty);
        poolB = orderByDifficulty(poolB, difficulty);

        // Build partA response: [{q, unit}]
        List<Map<String, Object>> partA = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Question q = poolA.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("q", q.getQuestionText());
            row.put("unit", q.getUnit());
            partA.add(row);
        }

        // Build partB response: [{a, b, unit}]
        List<Map<String, Object>> partB = new ArrayList<>();
        for (int i = 0; i < 6; i += 2) {
            Question qa = poolB.get(i);
            Question qb = poolB.get(i + 1);
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("a", qa.getQuestionText());
            pair.put("b", qb.getQuestionText());
            pair.put("unit", qa.getUnit());
            partB.add(pair);
        }

        Map<String, Object> paper = new LinkedHashMap<>();
        paper.put("subject", subject);
        paper.put("exam", exam);
        paper.put("difficulty", difficulty == null ? "medium" : difficulty.toLowerCase());
        paper.put("partA", partA);
        paper.put("partB", partB);
        return paper;
    }

    /**
     * Reorder a pool so the preferred-RBT bucket appears first, then the rest.
     * This keeps the core rules valid while biasing selection toward the requested level.
     */
    private List<Question> orderByDifficulty(List<Question> pool, String difficulty) {
        String d = difficulty == null ? "medium" : difficulty.trim().toLowerCase();

        List<Question> rbt1 = new ArrayList<>();
        List<Question> rbt2 = new ArrayList<>();
        for (Question q : pool) {
            int r = normalizeRbt(q.getRbt());
            if (r == 1) rbt1.add(q);
            else if (r == 2) rbt2.add(q);
        }
        Collections.shuffle(rbt1);
        Collections.shuffle(rbt2);

        List<Question> ordered = new ArrayList<>(pool.size());
        switch (d) {
            case "easy":
                ordered.addAll(rbt1);
                ordered.addAll(rbt2);
                break;
            case "hard":
                ordered.addAll(rbt2);
                ordered.addAll(rbt1);
                break;
            case "medium":
            default:
                // Interleave for a balanced mix
                int a = 0, b = 0;
                while (a < rbt1.size() || b < rbt2.size()) {
                    if (a < rbt1.size()) ordered.add(rbt1.get(a++));
                    if (b < rbt2.size()) ordered.add(rbt2.get(b++));
                }
                break;
        }
        return ordered;
    }
}
