package app.util;

import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {
    
    /**
     * Extracts CSRF token from cookie string
     * @param cookieString Full cookie header value
     * @return CSRF token if found
     */
    public static Optional<String> extractCsrfToken(String cookieString) {
        if (cookieString == null || cookieString.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(cookieString.split(";"))
                .map(String::trim)
                .filter(cookie -> cookie.startsWith("_xsrf="))
                .map(cookie -> cookie.substring("_xsrf=".length()))
                .findFirst();
    }
} 