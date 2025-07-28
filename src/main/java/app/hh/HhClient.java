package app.hh;

import app.config.Env;
import app.http.HttpClientFactory;
import app.model.ApplyResult;
import app.util.CookieUtils;
import app.util.Multipart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

@Component
public class HhClient {
    private static final Logger log = LoggerFactory.getLogger(HhClient.class);
    
    private final Env env;
    private final HttpClientFactory httpClientFactory;
    private final ObjectMapper objectMapper;
    
    public HhClient(Env env, HttpClientFactory httpClientFactory) {
        this.env = env;
        this.httpClientFactory = httpClientFactory;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetches search page JSON
     * @return JSON content of the search page
     */
    public String fetchSearchJson() {
        try {
            // Validate required configuration
            String searchUrl = env.getHhSearchUrl();
            String cookies = env.getHhCookies();
            
            if (searchUrl == null || searchUrl.trim().isEmpty()) {
                throw new RuntimeException("HH_SEARCH_URL is not configured");
            }
            if (cookies == null || cookies.trim().isEmpty()) {
                throw new RuntimeException("HH_COOKIES is not configured");
            }
            
            HttpClient client = httpClientFactory.createHttpClient();
            
            log.info("Fetching search page from: {}", searchUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Cookie", env.getHhCookies())
                    .header("User-Agent", env.getUserAgent())
                    .header("Accept", "application/json; charset=utf-8")
                    .header("Accept-Language", "ru,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("x-static-version", "25.31.1.1")
                    .timeout(Duration.ofMillis(env.getHttpReadTimeoutMs()))
                    .GET()
                    .build();
            
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                GZIPInputStream  gzip = new GZIPInputStream(new ByteArrayInputStream(response.body()));
                String body = new String(gzip.readAllBytes());
                log.info("Successfully fetched search JSON, size: {} bytes", body.length());
                log.info(body);
                // Log JSON structure for debugging
                if (body.length() > 0) {
                    log.debug("JSON response preview - first 500 chars: {}", 
                             body.substring(0, Math.min(500, body.length())));
                }
                
                return body;
            } else {
                log.error("Failed to fetch search JSON, status: {}, response: {}", response.statusCode(), response.body());
                throw new RuntimeException("Search JSON fetch failed with status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch search JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Search JSON fetch failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetches popup preparation data (optional)
     * @param vacancyId Vacancy ID
     * @return Optional popup data
     */
    public Optional<String> fetchPopupPrepare(String vacancyId) {
        try {
            HttpClient client = httpClientFactory.createHttpClient();
            
            String url = env.getHhApplyUrlTemplate() + "?vacancyId=" + vacancyId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Cookie", env.getHhCookies())
                    .header("User-Agent", env.getUserAgent())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(env.getHttpReadTimeoutMs()))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (response.statusCode() == 200) {
                log.debug("Successfully fetched popup data for vacancy {}", vacancyId);
                return Optional.of(response.body());
            } else {
                log.warn("Failed to fetch popup data for vacancy {}, status: {}", vacancyId, response.statusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.warn("Failed to fetch popup data for vacancy {}", vacancyId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Applies to vacancy using multipart/form-data
     * @param vacancyId Vacancy ID
     * @return Apply result
     */
    public ApplyResult applyMultipart(String vacancyId) {
        try {
            HttpClient client = httpClientFactory.createHttpClient();
            
            // Extract CSRF token from cookies
            Optional<String> csrfToken = CookieUtils.extractCsrfToken(env.getHhCookies());
            if (csrfToken.isEmpty()) {
                log.error("CSRF token not found in cookies for vacancy {}", vacancyId);
                return ApplyResult.failure(vacancyId, 400, "CSRF token not found");
            }
            
            // Build multipart form data
            Map<String, String> formFields = new HashMap<>();
            String bodyTemplate = env.getHhApplyBodyTemplate();
            
            // Parse template and substitute values
            String[] pairs = bodyTemplate.split(";");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim()
                            .replace("{vacancyId}", vacancyId)
                            .replace("${HH_RESUME_ID}", env.getHhResumeId())
                            .replace("{csrf}", csrfToken.get());
                    formFields.put(key, value);
                }
            }
            
            // Generate boundary and create multipart body
            String boundary = "----" + UUID.randomUUID().toString();
            HttpRequest.BodyPublisher bodyPublisher = Multipart.of(formFields, boundary);
            
            // Build request with proper headers
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(env.getHhApplyUrlTemplate()))
                    .header("Cookie", env.getHhCookies())
                    .header("User-Agent", env.getUserAgent())
                    .header("X-XSRFToken", csrfToken.get())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Origin", env.getHhBaseUrl())
                    .header("Referer", env.getHhBaseUrl() + "/vacancy/" + vacancyId)
                    .header("Accept", "application/json")
                    .header("Content-Type", Multipart.getContentType(boundary))
                    .timeout(Duration.ofMillis(env.getHttpReadTimeoutMs()))
                    .POST(bodyPublisher);
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully applied to vacancy {}", vacancyId);
                return ApplyResult.success(vacancyId);
            } else {
                log.error("Failed to apply to vacancy {}, status: {}, response: {}", 
                         vacancyId, response.statusCode(), response.body());
                return ApplyResult.failure(vacancyId, response.statusCode(), response.body());
            }
            
        } catch (Exception e) {
            log.error("Failed to apply to vacancy {}", vacancyId, e);
            return ApplyResult.failure(vacancyId, 500, e.getMessage());
        }
    }
    
    /**
     * Registers interaction (optional telemetry)
     * @param vacancyId Vacancy ID
     */
    public void registerInteraction(String vacancyId) {
        if (!env.isHhSendInteraction()) {
            return;
        }
        
        try {
            HttpClient client = httpClientFactory.createHttpClient();
            
            // Extract CSRF token
            Optional<String> csrfToken = CookieUtils.extractCsrfToken(env.getHhCookies());
            if (csrfToken.isEmpty()) {
                log.warn("CSRF token not found for interaction registration");
                return;
            }
            
            // Build JSON payload
            Map<String, String> payload = Map.of("vacancyId", vacancyId);
            String jsonBody = objectMapper.writeValueAsString(payload);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(env.getHhInteractionUrl()))
                    .header("Cookie", env.getHhCookies())
                    .header("User-Agent", env.getUserAgent())
                    .header("X-XSRFToken", csrfToken.get())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(env.getHttpReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Successfully registered interaction for vacancy {}", vacancyId);
            } else {
                log.warn("Failed to register interaction for vacancy {}, status: {}", vacancyId, response.statusCode());
            }
            
        } catch (Exception e) {
            log.warn("Failed to register interaction for vacancy {}", vacancyId, e);
        }
    }
} 