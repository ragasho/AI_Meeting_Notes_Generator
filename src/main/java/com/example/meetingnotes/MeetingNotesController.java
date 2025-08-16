package com.example.meetingnotes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MeetingNotesController {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private PdfService pdfService;

    // Landing page
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // AJAX: upload -> returns HTML fragment (safe)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return "<div style='color:#b00020;'>Please choose an audio file.</div>";
            if (file.getSize() > 25L * 1024 * 1024)
                return "<div style='color:#b00020;'>File too large. Max 25MB.</div>";

            String transcript = openAIService.speechToText(file.getBytes(), file.getOriginalFilename());
            String summary = openAIService.summarizeText(transcript);

            // Display as HTML; keep text version for PDF via JS .innerText
            String safeHtml = summary
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br/>");

            return """
              <div class='summary-card'>
                %s
              </div>
            """.formatted(safeHtml);

        } catch (Exception e) {
            e.printStackTrace();
            return "<div style='color:#b00020;'>Error: " + e.getMessage() + "</div>";
        }
    }

    // Download PDF: expects plain text (we send innerText from the page)
    @PostMapping("/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam("summary") String summary) throws Exception {
        // summary is plain text (no HTML); if HTML sneaks in, normalize:
        String plainText = summary.replaceAll("<br\\s*/?>", "\n");
        byte[] pdf = pdfService.generateMeetingNotesPdf("Meeting Notes", plainText);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meeting-notes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
