package com.ProductClientService.ProductClientService.Service.reco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Thin client over reco-serving (Python FastAPI, port 8100 by default).
 * When the service is unreachable or disabled the orchestrator falls back
 * to cold-start logic — this client never blocks the request path for
 * longer than the HTTP timeout.
 *
 * Toggled with:
 *   reco.serving.enabled=false   (default)
 *   reco.serving.baseUrl=http://reco-serving:8100
 *
 * Replace with generated gRPC stubs in Phase 4 once protobuf build is
 * wired in. REST keeps the Java repo free of protoc until the Python side
 * ships a stable contract.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoServingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${reco.serving.enabled:false}")
    private boolean enabled;

    @Value("${reco.serving.baseUrl:http://localhost:8100}")
    private String baseUrl;

    public record Candidate(String productId, double score, String reason) {}
    public record Response(String modelVersion, List<Candidate> candidates) {}

    public Optional<Response> recommend(UUID userId, int k, String context, List<String> sessionClicks) {
        if (!enabled) return Optional.empty();
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId.toString());
            body.put("k", k);
            body.put("context", context);
            body.put("sessionClicks", sessionClicks);

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/recommend",
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), h),
                    String.class);

            JsonNode root = mapper.readTree(resp.getBody());
            String modelVersion = root.path("modelVersion").asText("reco_v?");
            List<Candidate> out = new ArrayList<>();
            for (JsonNode n : root.path("items")) {
                out.add(new Candidate(
                        n.path("productId").asText(),
                        n.path("score").asDouble(),
                        n.path("reason").asText(null)));
            }
            return Optional.of(new Response(modelVersion, out));
        } catch (Exception e) {
            log.warn("reco-serving call failed (circuit opens): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
