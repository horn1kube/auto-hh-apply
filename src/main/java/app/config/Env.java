package app.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class Env {
    private static final Logger log = LoggerFactory.getLogger(Env.class);
    
    private Dotenv dotenv;
    
    @PostConstruct
    public void init() {
        dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing()
                .load();
        log.info("Environment configuration loaded");
        
        // Validate required configuration
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        if (getHhResumeId() == null || getHhResumeId().trim().isEmpty()) {
            log.error("HH_RESUME_ID is not set or empty");
        }
        if (getHhCookies() == null || getHhCookies().trim().isEmpty()) {
            log.error("HH_COOKIES is not set or empty");
        }
        if (getHhSearchUrl() == null || getHhSearchUrl().trim().isEmpty()) {
            log.error("HH_SEARCH_URL is not set or empty");
        }
    }
    
    public String get(String key) {
        return dotenv.get(key);
    }
    
    public String get(String key, String defaultValue) {
        return Optional.ofNullable(dotenv.get(key)).orElse(defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    // HH.ru specific getters
    public String getHhBaseUrl() {
        return get("HH_BASE_URL", "https://hh.ru");
    }
    
    public String getHhResumeId() {
        return get("HH_RESUME_ID");
    }
    
    public String getHhCookies() {
        return get("HH_COOKIES");
    }
    
    public String getHhSearchUrl() {
        String url = get("HH_SEARCH_URL");
        if (url != null) {
            String resumeId = getHhResumeId();
            if (resumeId != null) {
                String encodedResumeId = URLEncoder.encode(resumeId, StandardCharsets.UTF_8);
                return url.replace("${HH_RESUME_ID}", encodedResumeId);
            }
        }
        return url;
    }
    
    public String getHhApplyUrlTemplate() {
        return get("HH_APPLY_URL_TEMPLATE");
    }
    
    public String getHhApplyBodyTemplate() {
        String template = get("HH_APPLY_BODY_TEMPLATE");
        if (template != null) {
            return template.replace("${HH_RESUME_ID}", getHhResumeId());
        }
        return template;
    }
    
    public String getHhApplyExtraHeaders() {
        return get("HH_APPLY_EXTRA_HEADERS");
    }
    
    public boolean isHhSendInteraction() {
        return getBoolean("HH_SEND_INTERACTION", false);
    }
    
    public String getHhInteractionUrl() {
        return get("HH_INTERACTION_URL");
    }
    
    public String getUserAgent() {
        return get("USER_AGENT", "Mozilla/5.0 (X11; Linux x86_64) Java21-AutoApply/1.0");
    }
    
    public int getHttpConnectTimeoutMs() {
        return getInt("HTTP_CONNECT_TIMEOUT_MS", 10000);
    }
    
    public int getHttpReadTimeoutMs() {
        return getInt("HTTP_READ_TIMEOUT_MS", 20000);
    }
    
    public int getRateLimitMinDelayMs() {
        return getInt("RATE_LIMIT_MIN_DELAY_MS", 500);
    }
    
    public int getRateLimitMaxDelayMs() {
        return getInt("RATE_LIMIT_MAX_DELAY_MS", 4000);
    }
    
    public String getDbPath() {
        return get("DB_PATH", "data/app.db");
    }
    
    public boolean isApplyDryRun() {
        return getBoolean("APPLY_DRY_RUN", false);
    }
} 