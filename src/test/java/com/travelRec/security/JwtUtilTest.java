package com.travelRec.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "TravelRecSecretKeyForJWTTokenGenerationMinimum256Bits!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("should generate non-null token")
        void shouldGenerateToken() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void shouldGenerateDifferentTokens() {
            String token1 = jwtUtil.generateToken("anna@mail.com", "USER");
            String token2 = jwtUtil.generateToken("admin@mail.com", "ADMIN");
            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("should extract correct email")
        void shouldExtractEmail() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            assertEquals("anna@mail.com", jwtUtil.extractEmail(token));
        }
    }

    @Nested
    @DisplayName("extractRole()")
    class ExtractRole {

        @Test
        @DisplayName("should extract USER role")
        void shouldExtractUserRole() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            assertEquals("USER", jwtUtil.extractRole(token));
        }

        @Test
        @DisplayName("should extract ADMIN role")
        void shouldExtractAdminRole() {
            String token = jwtUtil.generateToken("admin@mail.com", "ADMIN");
            assertEquals("ADMIN", jwtUtil.extractRole(token));
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrue() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            assertTrue(jwtUtil.isTokenValid(token));
        }

        @Test
        @DisplayName("should return false for expired token")
        void shouldReturnFalseForExpired() {
            ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            assertFalse(jwtUtil.isTokenValid(token));
        }

        @Test
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTampered() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");
            String tampered = token.substring(0, token.length() - 5) + "xxxxx";
            assertFalse(jwtUtil.isTokenValid(tampered));
        }

        @Test
        @DisplayName("should return false for garbage string")
        void shouldReturnFalseForGarbage() {
            assertFalse(jwtUtil.isTokenValid("not.a.token"));
        }

        @Test
        @DisplayName("should return false for empty string")
        void shouldReturnFalseForEmpty() {
            assertFalse(jwtUtil.isTokenValid(""));
        }

        @Test
        @DisplayName("should return false for token signed with different key")
        void shouldReturnFalseForDifferentKey() {
            String token = jwtUtil.generateToken("anna@mail.com", "USER");

            JwtUtil otherJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(otherJwtUtil, "secret", "CompletelyDifferentSecretKeyThatIsAlso256BitsLong!!");
            ReflectionTestUtils.setField(otherJwtUtil, "expiration", 86400000L);

            assertFalse(otherJwtUtil.isTokenValid(token));
        }
    }

    @Nested
    @DisplayName("full flow")
    class FullFlow {

        @Test
        @DisplayName("should generate and extract correctly")
        void shouldGenerateAndExtract() {
            String token = jwtUtil.generateToken("test@mail.com", "ADMIN");

            assertTrue(jwtUtil.isTokenValid(token));
            assertEquals("test@mail.com", jwtUtil.extractEmail(token));
            assertEquals("ADMIN", jwtUtil.extractRole(token));
        }
    }
}
