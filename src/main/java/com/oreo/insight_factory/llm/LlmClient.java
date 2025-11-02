package com.oreo.insight_factory.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class LlmClient {

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.model-id}")
    private String modelId;

    @Value("${llm.token}")
    private String token;

    private final ObjectMapper mapper = new ObjectMapper();
    public String summarizeWeekly(int totalUnits, double totalRevenue, String topSku, String topBranch) throws Exception {
        String payload = """
        {
          "model": "%s",
          "messages": [
            {"role": "system", "content": "Eres un analista que escribe resúmenes breves y claros para emails corporativos."},
            {"role": "user", "content": "Con estos datos: totalUnits=%d, totalRevenue=%.2f, topSku=%s, topBranch=%s. Devuelve un resumen \u2264120 palabras, en español, claro y sin alucinaciones, para email."}
          ],
          "max_tokens": 200
        }
        """.formatted(modelId, totalUnits, totalRevenue, jsonSafe(topSku), jsonSafe(topBranch));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            throw new RuntimeException("LLM error " + res.statusCode() + ": " + res.body());
        }

        JsonNode root = mapper.readTree(res.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? "" : content.asText();
    }

    private String jsonSafe(String s) {
        return s == null ? "null" : "\"" + s.replace("\"", "\\\"") + "\"";
    }
}
