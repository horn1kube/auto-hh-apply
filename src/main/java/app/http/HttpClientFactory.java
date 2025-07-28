package app.http;

import app.config.Env;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HttpClientFactory {
    
    private final Env env;
    
    public HttpClientFactory(Env env) {
        this.env = env;
    }
    
    public HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(env.getHttpConnectTimeoutMs()))
                .build();
    }
} 