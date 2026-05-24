package com.interview.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.auth.common.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final String TEST_SECRET = "this-is-a-test-secret-for-unit-tests-only-32bytes!";
    private static final int EXPIRE_HOURS = 1;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(new ObjectMapper(), TEST_SECRET, EXPIRE_HOURS);
    }

    @Test
    void shouldGenerateAndParseToken() {
        String token = tokenService.generateToken(1L, "test@example.com", "testuser");

        assertNotNull(token);
        assertTrue(token.contains("."));

        Map<String, Object> payload = tokenService.parseToken("Bearer " + token);

        assertEquals(1L, Long.parseLong(String.valueOf(payload.get("userId"))));
        assertEquals("test@example.com", payload.get("email"));
        assertEquals("testuser", payload.get("username"));
        assertNotNull(payload.get("exp"));
    }

    @Test
    void shouldParseTokenWithoutBearerPrefix() {
        String token = tokenService.generateToken(42L, "user@test.com", "john");

        Map<String, Object> payload = tokenService.parseToken(token);

        assertEquals(42L, Long.parseLong(String.valueOf(payload.get("userId"))));
    }

    @Test
    void shouldRejectNullToken() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken(null)
        );
        assertEquals(401, ex.getCode());
    }

    @Test
    void shouldRejectEmptyToken() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken("")
        );
        assertEquals(401, ex.getCode());
    }

    @Test
    void shouldRejectTokenWithoutDot() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken("Bearer invalidTokenWithoutDot")
        );
        assertEquals(401, ex.getCode());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = tokenService.generateToken(1L, "test@example.com", "testuser");
        String tampered = token + "x";

        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken(tampered)
        );
        assertEquals(401, ex.getCode());
    }

    @Test
    void shouldRejectTokenWithWrongSignature() {
        // Generate a token with one secret, parse with another
        TokenService otherService = new TokenService(new ObjectMapper(), "different-secret-key-for-testing-purposes", EXPIRE_HOURS);
        String tokenFromOtherKey = otherService.generateToken(1L, "a@b.com", "user");

        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken(tokenFromOtherKey)
        );
        assertEquals(401, ex.getCode());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        // Create a token service with -1 hour expiry (always expired)
        TokenService expiredService = new TokenService(new ObjectMapper(), TEST_SECRET, -1);
        String expiredToken = expiredService.generateToken(1L, "test@example.com", "user");

        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken(expiredToken)
        );
        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void shouldGenerateDifferentTokensForSameUser() {
        String token1 = tokenService.generateToken(1L, "test@example.com", "testuser");
        String token2 = tokenService.generateToken(1L, "test@example.com", "testuser");

        // Tokens should differ due to different exp timestamps
        assertTrue(token1.startsWith(token1.split("\\.")[0]));
        assertTrue(token2.startsWith(token2.split("\\.")[0]));
        assert !token1.equals(token2) : "Tokens should be different due to timestamp";
    }

    @Test
    void shouldHandleSpecialCharactersInUsername() {
        String token = tokenService.generateToken(1L, "test@example.com", "用户🔥");

        Map<String, Object> payload = tokenService.parseToken(token);
        assertEquals("用户🔥", payload.get("username"));
    }

    @Test
    void shouldRejectTokenWithOnlyBearerPrefix() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            tokenService.parseToken("Bearer ")
        );
        assertEquals(401, ex.getCode());
    }
}
