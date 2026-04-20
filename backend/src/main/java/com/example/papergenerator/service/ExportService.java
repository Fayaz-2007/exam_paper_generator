package com.example.papergenerator.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH  = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LINE_H = 16f;
    private static final float GAP   = 10f;

    /**
     * Renders the generated paper data as an A4 PDF and returns the bytes.
     * paperData keys: subject, exam, partA:[{q,unit}], partB:[{a,b,unit}]
     */
    public byte[] generatePdf(Map<String, Object> paper, String subject, String exam) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> partA = (List<Map<String, Object>>) paper.get("partA");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> partB = (List<Map<String, Object>>) paper.get("partB");

            State s = new State(doc);
            s.newPage();

            writeHeader(s, bold, regular, subject, exam);
            writePartA(s, bold, regular, partA);
            writePartB(s, bold, regular, partB);

            // PDFBox requires all content streams to be closed before save()
            if (s.stream != null) { s.stream.close(); s.stream = null; }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void writeHeader(State s, PDType1Font bold, PDType1Font regular,
                             String subject, String exam) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        centred(s, bold, 13f, "SMART QUESTION PAPER GENERATOR");
        s.y -= 4;
        centred(s, regular, 10f, "Subject: " + subject + "   |   Exam: " + exam + "   |   Date: " + date);
        s.y -= 4;
        centred(s, regular, 10f, "Time: 3 Hours                              Max. Marks: 50");
        s.y -= GAP;
        hline(s);
        s.y -= GAP;
    }

    private void writePartA(State s, PDType1Font bold, PDType1Font regular,
                            List<Map<String, Object>> questions) throws Exception {
        line(s, bold, 11f, "PART A — Answer ALL Questions  (10 × 2 = 20 Marks)");
        s.y -= GAP / 2;
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            String text = (i + 1) + ". " + q.get("q");
            List<String> wrapped = wrap(text, regular, 9f, USABLE_WIDTH - 40);
            for (int li = 0; li < wrapped.size(); li++) {
                ensure(s, LINE_H);
                String lineText = wrapped.get(li);
                s.stream.beginText();
                s.stream.setFont(li == 0 ? bold : regular, 9f);
                s.stream.newLineAtOffset(MARGIN, s.y);
                s.stream.showText(li == 0 ? lineText : "   " + lineText);
                s.stream.endText();
                if (li == 0) writeAt(s, regular, 9f, "[2]", PAGE_WIDTH - MARGIN - 20);
                s.y -= LINE_H;
            }
            s.y -= 3;
        }
        s.y -= GAP;
        hline(s);
        s.y -= GAP;
    }

    private void writePartB(State s, PDType1Font bold, PDType1Font regular,
                            List<Map<String, Object>> sets) throws Exception {
        line(s, bold, 11f, "PART B — Answer ONE from each set  (3 × 10 = 30 Marks)");
        s.y -= GAP;
        for (int i = 0; i < sets.size(); i++) {
            Map<String, Object> set = sets.get(i);
            ensure(s, LINE_H * 4);
            line(s, bold, 10f, "Set " + (i + 1) + "  [10 Marks]");
            s.y -= 4;
            writeQuestion(s, bold, regular, "(a)", (String) set.get("a"));
            centred(s, bold, 9f, "— OR —");
            s.y -= 4;
            writeQuestion(s, bold, regular, "(b)", (String) set.get("b"));
            s.y -= GAP;
            if (i < sets.size() - 1) { hline(s); s.y -= GAP; }
        }
    }

    private void writeQuestion(State s, PDType1Font bold, PDType1Font regular,
                               String label, String text) throws Exception {
        if (text == null) return;
        List<String> wrapped = wrap(text, regular, 10f, USABLE_WIDTH - 60);
        for (int li = 0; li < wrapped.size(); li++) {
            ensure(s, LINE_H);
            s.stream.beginText();
            if (li == 0) {
                s.stream.setFont(bold, 10f);
                s.stream.newLineAtOffset(MARGIN, s.y);
                s.stream.showText(label + " ");
                s.stream.setFont(regular, 10f);
                s.stream.showText(wrapped.get(li));
            } else {
                s.stream.setFont(regular, 10f);
                s.stream.newLineAtOffset(MARGIN + 30, s.y);
                s.stream.showText(wrapped.get(li));
            }
            s.stream.endText();
            s.y -= LINE_H;
        }
        s.y -= 6;
    }

    private void centred(State s, PDType1Font font, float size, String text) throws Exception {
        ensure(s, LINE_H);
        float w = font.getStringWidth(text) / 1000 * size;
        float x = MARGIN + (USABLE_WIDTH - w) / 2;
        s.stream.beginText();
        s.stream.setFont(font, size);
        s.stream.newLineAtOffset(x, s.y);
        s.stream.showText(text);
        s.stream.endText();
        s.y -= LINE_H;
    }

    private void line(State s, PDType1Font font, float size, String text) throws Exception {
        ensure(s, LINE_H);
        s.stream.beginText();
        s.stream.setFont(font, size);
        s.stream.newLineAtOffset(MARGIN, s.y);
        s.stream.showText(text);
        s.stream.endText();
        s.y -= LINE_H;
    }

    private void writeAt(State s, PDType1Font font, float size, String text, float x) throws Exception {
        float w = font.getStringWidth(text) / 1000 * size;
        s.stream.beginText();
        s.stream.setFont(font, size);
        s.stream.newLineAtOffset(x - w, s.y + LINE_H);
        s.stream.showText(text);
        s.stream.endText();
    }

    private void hline(State s) throws Exception {
        s.stream.setLineWidth(0.5f);
        s.stream.moveTo(MARGIN, s.y);
        s.stream.lineTo(PAGE_WIDTH - MARGIN, s.y);
        s.stream.stroke();
        s.y -= 4;
    }

    private void ensure(State s, float need) throws Exception {
        if (s.y - need < MARGIN) s.newPage();
    }

    private List<String> wrap(String text, PDType1Font font, float size, float maxW) throws Exception {
        List<String> result = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) { result.add(""); return result; }
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.isEmpty() ? w : cur + " " + w;
            if (font.getStringWidth(candidate) / 1000 * size > maxW && !cur.isEmpty()) {
                result.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (!cur.isEmpty()) result.add(cur.toString());
        return result;
    }

    private class State {
        final PDDocument doc;
        PDPageContentStream stream;
        float y;
        State(PDDocument doc) { this.doc = doc; }
        void newPage() throws Exception {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PAGE_HEIGHT - MARGIN;
        }
    }
}
