
package com.money.draft.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "ThisIsASecretKeyThatIsAtLeast32BytesLongForHS256!";
    private static final long EXPIRATION_MS = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_ShouldCreateValidToken_WithAccountId() {
        String token = jwtService.generateToken("john", "USER", 100L);
        assertNotNull(token);

        Jws<Claims> parsed = jwtService.parse(token);
        assertEquals("john", parsed.getBody().getSubject());
        assertEquals("USER", parsed.getBody().get("role"));
        assertEquals(100, parsed.getBody().get("accountId", Integer.class));
        assertNotNull(parsed.getBody().getIssuedAt());
        assertNotNull(parsed.getBody().getExpiration());
    }

    @Test
    void generateToken_ShouldCreateValidToken_WithoutAccountId() {
        String token = jwtService.generateToken("admin", "ADMIN", null);
        assertNotNull(token);

        Jws<Claims> parsed = jwtService.parse(token);
        assertEquals("admin", parsed.getBody().getSubject());
        assertEquals("ADMIN", parsed.getBody().get("role"));
        assertFalse(parsed.getBody().containsKey("accountId"));
    }

    @Test
    void parse_ShouldThrow_WhenTokenExpired() {
        JwtService shortExpiry = new JwtService(SECRET, -1000);
        String token = shortExpiry.generateToken("john", "USER", null);

        assertThrows(JwtException.class, () -> shortExpiry.parse(token));
    }

    @Test
    void parse_ShouldThrow_WhenTokenMalformed() {
        assertThrows(JwtException.class, () -> jwtService.parse("invalid.token.here"));
    }

    @Test
    void parse_ShouldThrow_WhenTokenSignedWithWrongKey() {
        JwtService otherService = new JwtService("AnotherSecretKeyThatIsAtLeast32BytesLongForHS256!", EXPIRATION_MS);
        String token = otherService.generateToken("john", "USER", null);

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }
}
