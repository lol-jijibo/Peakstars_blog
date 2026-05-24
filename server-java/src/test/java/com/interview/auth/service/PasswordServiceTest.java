package com.interview.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    void shouldEncodeAndMatchWithBCrypt() {
        String raw = "MySecureP@ss123";
        String encoded = passwordService.encode(raw);

        assertNotEquals(raw, encoded);
        assertTrue(encoded.startsWith("$2"));
        assertTrue(passwordService.matches(raw, encoded));
    }

    @Test
    void shouldRejectWrongPassword() {
        String encoded = passwordService.encode("correctPassword");

        assertFalse(passwordService.matches("wrongPassword", encoded));
    }

    @Test
    void shouldRejectNullEncodedPassword() {
        assertFalse(passwordService.matches("anything", null));
    }

    @Test
    void shouldDetectLegacyHashFormat() {
        String legacyHash = passwordService.generateSalt() + ":"
            + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        assertTrue(passwordService.isLegacyHash(legacyHash));
    }

    @Test
    void shouldNotDetectBCryptAsLegacy() {
        String encoded = passwordService.encode("test");

        assertFalse(passwordService.isLegacyHash(encoded));
    }

    @Test
    void shouldMatchLegacyHash() {
        String salt = passwordService.generateSalt();
        String raw = "legacyPassword";
        // SHA-256(salt + raw)
        String hash = sha256(salt + raw);
        String legacyHash = salt + ":" + hash;

        assertTrue(passwordService.matches(raw, legacyHash));
    }

    @Test
    void shouldRejectWrongLegacyPassword() {
        String salt = passwordService.generateSalt();
        String hash = sha256(salt + "correctPassword");
        String legacyHash = salt + ":" + hash;

        assertFalse(passwordService.matches("wrongPassword", legacyHash));
    }

    @Test
    void shouldRejectMalformedLegacyHash() {
        assertFalse(passwordService.matches("anything", "invalidhash"));
    }

    @Test
    void shouldRejectLegacyHashWithWrongParts() {
        // More than 2 parts
        assertFalse(passwordService.matches("anything", "a:b:c"));
    }

    @Test
    void shouldGenerateUniqueSalts() {
        String salt1 = passwordService.generateSalt();
        String salt2 = passwordService.generateSalt();

        assertNotEquals(salt1, salt2);
        // Salt should be 16 bytes, hex-encoded = 32 chars
        assert salt1.length() == 32;
    }

    @Test
    void shouldGenerateUniqueBCryptHashesForSamePassword() {
        String raw = "samePassword";
        String encoded1 = passwordService.encode(raw);
        String encoded2 = passwordService.encode(raw);

        // BCrypt generates unique salt each time
        assertNotEquals(encoded1, encoded2);
        // Both should match
        assertTrue(passwordService.matches(raw, encoded1));
        assertTrue(passwordService.matches(raw, encoded2));
    }

    @Test
    void shouldHandleEmptyPassword() {
        String encoded = passwordService.encode("");

        assertTrue(passwordService.matches("", encoded));
        assertFalse(passwordService.matches(" ", encoded));
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String raw = "密码测试🔥🎉日本語パスワード";
        String encoded = passwordService.encode(raw);

        assertTrue(passwordService.matches(raw, encoded));
    }

    private String sha256(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
