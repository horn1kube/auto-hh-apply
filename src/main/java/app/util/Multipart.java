package app.util;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class Multipart {
    
    /**
     * Creates multipart/form-data body publisher for HttpClient
     * @param fields Map of field name to value
     * @param boundary Boundary string (if null, generates random)
     * @return HttpRequest.BodyPublisher with multipart content
     */
    public static HttpRequest.BodyPublisher of(Map<String, String> fields, String boundary) {
        String actualBoundary = boundary != null ? boundary : "----" + UUID.randomUUID().toString();
        
        byte[] body = buildMultipartBody(fields, actualBoundary);
        
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }
    
    /**
     * Creates multipart/form-data body publisher with random boundary
     * @param fields Map of field name to value
     * @return HttpRequest.BodyPublisher with multipart content
     */
    public static HttpRequest.BodyPublisher of(Map<String, String> fields) {
        return of(fields, null);
    }
    
    private static byte[] buildMultipartBody(Map<String, String> fields, String boundary) {
        StringBuilder body = new StringBuilder();
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            body.append("--").append(boundary).append("\r\n");
            body.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
            body.append("\r\n");
            body.append(entry.getValue()).append("\r\n");
        }
        
        body.append("--").append(boundary).append("--\r\n");
        
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Gets Content-Type header value for multipart/form-data
     * @param boundary Boundary string
     * @return Content-Type header value
     */
    public static String getContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }
} 