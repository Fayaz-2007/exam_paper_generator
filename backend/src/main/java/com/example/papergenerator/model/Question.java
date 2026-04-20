package com.example.papergenerator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    private String id;
    private String subject;
    private String exam;
    private String part;

    @JsonProperty("question_text")
    private String questionText;

    private String co;
    private String rbt;
    private int unit;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getExam() { return exam; }
    public void setExam(String exam) { this.exam = exam; }

    public String getPart() { return part; }
    public void setPart(String part) { this.part = part; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCo() { return co; }
    public void setCo(String co) { this.co = co; }

    public String getRbt() { return rbt; }
    public void setRbt(String rbt) { this.rbt = rbt; }

    public int getUnit() { return unit; }
    public void setUnit(int unit) { this.unit = unit; }
}
