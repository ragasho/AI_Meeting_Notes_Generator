package com.example.meetingnotes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Whisper (Groq) transcription */
    public String speechToText(byte[] audioBytes, String fileName) throws Exception {
        String url = "https://api.groq.com/openai/v1/audio/transcriptions";

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + groqApiKey);

            MultipartEntityBuilder mb = MultipartEntityBuilder.create();
            mb.addBinaryBody("file", audioBytes, ContentType.DEFAULT_BINARY, fileName == null ? "audio.mp3" : fileName);
            mb.addTextBody("model", "whisper-large-v3");
            // Optional: set language if you know it: mb.addTextBody("language", "en");
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

    /** LLaMA3 (Groq) meeting-summary prompt */
    public String summarizeText(String transcript) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        String escaped = transcript.replace("\\", "\\\\").replace("\"", "\\\"");
        String payload = """
          {
            "model": "llama3-8b-8192",
            "messages": [
              { "role": "system", "content": "You turn meeting transcripts into concise, readable notes with sections: Title, Key Points (bullets), Action Items (assignee, due), Decisions, and Next Steps." },
              { "role": "user", "content": "%s" }
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
