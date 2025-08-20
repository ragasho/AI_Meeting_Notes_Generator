package com.example.meetingnotes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class OpenAIService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 🔹 Utility: Download audio from a link */
    public byte[] downloadAudioFromLink(String audioUrl) throws Exception {
        try (InputStream in = new URL(audioUrl).openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    /** 🔹 Whisper (Groq) transcription */
    public String speechToText(byte[] audioBytes, String fileName) throws Exception {
        String url = "https://api.groq.com/openai/v1/audio/transcriptions";

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + groqApiKey);

            MultipartEntityBuilder mb = MultipartEntityBuilder.create();
            mb.addBinaryBody("file", audioBytes, ContentType.DEFAULT_BINARY,
                    fileName == null ? "audio.mp3" : fileName);
            mb.addTextBody("model", "whisper-large-v3");
            // Optional: set language
            // mb.addTextBody("language", "en");
            post.setEntity(mb.build());

            try (CloseableHttpResponse resp = http.execute(post)) {
                String body = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                int code = resp.getCode();
                if (code != 200) throw new RuntimeException("Transcription failed: " + body);
                JsonNode json = mapper.readTree(body);
                return json.path("text").asText();
            }
        }
    }

    /** 🔹 LLaMA3 (Groq) meeting-summary prompt */
    public String summarizeText(String transcript) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        String escaped = transcript.replace("\\", "\\\\").replace("\"", "\\\"");
        String payload = """
{
  "model": "llama3-8b-8192",
  "messages": [
    {
      "role": "system",
      "content": "You turn meeting transcripts into concise, professional notes. Include only meaningful and actionable content. Organize notes in these sections:\\n\\n1. Meeting Details (Date, Time, Participants, Organizer, Platform)\\n2. Agenda (key topics)\\n3. Discussions (insights, challenges, suggestions)\\n4. Decisions Made\\n5. Action Items (task, assignee, due date)\\n6. Follow-ups / Next Steps\\n\\nRequirements:\\n- Skip trivial items like introductions/greetings.\\n- Use concise, clear sentences.\\n- Use headings & bullet points.\\n- If info missing, write 'Not mentioned'."
    },
    {
      "role": "user",
      "content": "%s"
    }
  ],
  "temperature": 0.2
}
""".formatted(escaped);

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + groqApiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

            try (CloseableHttpResponse resp = http.execute(post)) {
                String body = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                int code = resp.getCode();
                if (code != 200) throw new RuntimeException("Summarization failed: " + body);
                JsonNode json = mapper.readTree(body);
                return json.path("choices").get(0).path("message").path("content").asText();
            }
        }
    }
}
