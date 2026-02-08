package com.money.draft.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final String secret =
            "this-is-a-very-secure-secret-key-32-bytes-min";
    private final long expirationMs = 60_000;

    private final JwtService jwtService =
            new JwtService(secret, expirationMs);

    @Test
    void generateAndParseToken_success() {
        String token = jwtService.generateToken("test1", "USER", 42L);

        assertNotNull(token);

        Jws<Claims> parsed = jwtService.parse(token);

        assertEquals("test1", parsed.getBody().getSubject());
        assertEquals("USER", parsed.getBody().get("role"));
        assertEquals(42L, parsed.getBody().get("accountId", Long.class));
    }

    @Test
    void generateToken_withoutAccountId() {
        String token = jwtService.generateToken("admin", "ADMIN", null);

        Jws<Claims> parsed = jwtService.parse(token);

        assertEquals("admin", parsed.getBody().getSubject());
        assertEquals("ADMIN", parsed.getBody().get("role"));
        assertFalse(parsed.getBody().containsKey("accountId"));
    }
}
