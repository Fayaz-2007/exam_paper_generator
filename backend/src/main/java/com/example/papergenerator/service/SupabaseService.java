package com.example.papergenerator.service;

import com.example.papergenerator.model.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SupabaseService {

    private static final MediaType JSON = MediaType.get("application/json");
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private String url() { return System.getProperty("SUPABASE_URL"); }
    private String key() { return System.getProperty("SUPABASE_KEY"); }

    /** Common headers required by Supabase PostgREST. */
    private Request.Builder base() {
        return new Request.Builder()
                .header("apikey", key())
                .header("Authorization", "Bearer " + key())
                .header("Content-Type", "application/json");
    }

    /** Insert a batch of questions. Throws on Supabase failure so the caller surfaces the real error. */
    public void insertQuestions(List<Question> questions) {
        try {
            String body = mapper.writeValueAsString(questions.stream().map(q -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("subject", q.getSubject());
                m.put("exam", q.getExam());
                m.put("part", q.getPart());
                m.put("question_text", q.getQuestionText());
                // co column is int8 in Supabase — coerce from String; default to 1 if not numeric
                try { m.put("co", Integer.parseInt(q.getCo())); }
                catch (NumberFormatException e) { m.put("co", 1); }
                m.put("rbt", q.getRbt());
                // Note: no "unit" column in the current Supabase schema, so we omit it
                return m;
            }).toList());

            Request req = base()
                    .url(url() + "/rest/v1/questions")
                    .header("Prefer", "resolution=ignore-duplicates")
                    .post(RequestBody.create(body, JSON))
                    .build();

            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) {
                    String errorBody;
                    ResponseBody rb = res.body();
                    if (rb != null) {
                        errorBody = rb.string();
                    } else {
                        errorBody = "(no body)";
                    }
                    throw new RuntimeException("Supabase insert failed (" + res.code() + "): " + errorBody);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert questions: " + e.getMessage(), e);
        }
    }

    /** Fetch all question texts for a subject — used for dedup at upload time. */
    public Set<String> fetchExistingTexts(String subject) {
        try {
            Request req = base()
                    .url(url() + "/rest/v1/questions?select=question_text&subject=eq." + encode(subject))
                    .get().build();

            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) return new HashSet<>();
                List<java.util.Map<String, Object>> rows =
                        mapper.readValue(res.body().string(), new TypeReference<>() {});
                Set<String> texts = new HashSet<>();
                rows.forEach(r -> texts.add((String) r.get("question_text")));
                return texts;
            }
        } catch (Exception e) {
            System.err.println("fetchExistingTexts error: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /** Fetch questions for paper generation (subject + exam filter). */
    public List<Question> fetchQuestions(String subject, String exam) {
        try {
            String url = url() + "/rest/v1/questions?subject=eq." + encode(subject)
                    + "&exam=eq." + encode(exam);
            Request req = base().url(url).get().build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) throw new RuntimeException("Supabase fetch failed: " + res.code());
                return mapper.readValue(res.body().string(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch questions: " + e.getMessage(), e);
        }
    }

    /**
     * Returns per-(subject,exam) question counts. Used by the HOD dashboard to
     * show which banks are available without shipping every question over the wire.
     */
    public List<java.util.Map<String, Object>> fetchQuestionSummary() {
        try {
            Request req = base()
                    .url(url() + "/rest/v1/questions?select=subject,exam")
                    .get().build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) throw new RuntimeException("Supabase fetch failed: " + res.code());
                List<java.util.Map<String, Object>> rows =
                        mapper.readValue(res.body().string(), new TypeReference<>() {});
                java.util.Map<String, int[]> counts = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> r : rows) {
                    String k = r.getOrDefault("subject", "") + "|" + r.getOrDefault("exam", "");
                    counts.computeIfAbsent(k, x -> new int[1])[0]++;
                }
                List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
                for (var e : counts.entrySet()) {
                    String[] parts = e.getKey().split("\\|", -1);
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("subject", parts[0]);
                    m.put("exam", parts.length > 1 ? parts[1] : "");
                    m.put("count", e.getValue()[0]);
                    out.add(m);
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch question summary: " + e.getMessage(), e);
        }
    }

    /** Fetch questions for the question-bank endpoint (optional subject filter). */
    public List<Question> fetchQuestionsBySubject(String subject) {
        try {
            String url = url() + "/rest/v1/questions";
            if (subject != null && !subject.isBlank()) {
                url += "?subject=eq." + encode(subject);
            }
            Request req = base().url(url).get().build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) throw new RuntimeException("Supabase fetch failed: " + res.code());
                return mapper.readValue(res.body().string(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch questions: " + e.getMessage(), e);
        }
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
