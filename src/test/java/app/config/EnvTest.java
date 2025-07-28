package app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnvTest {

    @Autowired
    private Env env;

    @Test
    void testEnvLoads() {
        assertNotNull(env);
        assertNotNull(env.getHhBaseUrl());
        assertEquals("https://hh.ru", env.getHhBaseUrl());
    }

    @Test
    void testDefaultValues() {
        assertEquals("Mozilla/5.0 (X11; Linux x86_64) Java21-AutoApply/1.0", env.getUserAgent());
        assertEquals(10000, env.getHttpConnectTimeoutMs());
        assertEquals(20000, env.getHttpReadTimeoutMs());
        assertEquals(500, env.getRateLimitMinDelayMs());
        assertEquals(4000, env.getRateLimitMaxDelayMs());
        assertEquals("data/app.db", env.getDbPath());
        assertFalse(env.isApplyDryRun());
    }
} 