package com.example.meetingnotes;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generateMeetingNotesPdf(String title, String notes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Title
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph pTitle = new Paragraph(title, titleFont);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingAfter(16f);
        doc.add(pTitle);

        // Body (plain text; preserve lines)
        Font bodyFont = new Font(Font.HELVETICA, 12);
        for (String line : notes.split("\\r?\\n")) {
            Paragraph p = new Paragraph(line, bodyFont);
            p.setSpacingAfter(4f);
            doc.add(p);
        }

        doc.close();
        return out.toByteArray();
    }
}
