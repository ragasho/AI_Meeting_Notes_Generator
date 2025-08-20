package com.example.meetingnotes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    // AJAX: upload (file or link) -> returns HTML fragment (safe)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String upload(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "audioLink", required = false) String audioLink
    ) {
        try {
            byte[] audioBytes;
            String fileName;

            if (file != null && !file.isEmpty()) {
                // 🔹 File upload path
                if (file.getSize() > 25L * 1024 * 1024) {
                    return "<div style='color:#b00020;'>File too large. Max 25MB.</div>";
                }
                audioBytes = file.getBytes();
                fileName = file.getOriginalFilename();
            } else if (audioLink != null && !audioLink.isBlank()) {
                // 🔹 Meeting link path
                audioBytes = openAIService.downloadAudioFromLink(audioLink);
                fileName = "downloaded_audio.mp3";
            } else {
                return "<div style='color:#b00020;'>Please choose an audio file or provide a meeting link.</div>";
            }

            // 1. Transcribe
            String transcript = openAIService.speechToText(audioBytes, fileName);

            // 2. Summarize
            String summary = openAIService.summarizeText(transcript);

            // Display as safe HTML (keep plain text version for PDF)
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

    // Download PDF
    @PostMapping("/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam("summary") String summary) throws Exception {
        String plainText = summary.replaceAll("<br\\s*/?>", "\n");
        byte[] pdf = pdfService.generateMeetingNotesPdf("Meeting Notes", plainText);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meeting-notes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
