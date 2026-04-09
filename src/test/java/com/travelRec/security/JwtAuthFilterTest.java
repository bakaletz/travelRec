package com.travelRec.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal()")
    class DoFilter {

        @Test
        @DisplayName("should set authentication for valid token")
        void shouldSetAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractEmail("valid-token")).thenReturn("anna@mail.com");
            when(jwtUtil.extractRole("valid-token")).thenReturn("USER");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("anna@mail.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should set ADMIN authority for admin token")
        void shouldSetAdminAuthority() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
            when(jwtUtil.isTokenValid("admin-token")).thenReturn(true);
            when(jwtUtil.extractEmail("admin-token")).thenReturn("admin@mail.com");
            when(jwtUtil.extractRole("admin-token")).thenReturn("ADMIN");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("should not set authentication when no header")
        void shouldNotSetAuthWhenNoHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should not set authentication when header without Bearer")
        void shouldNotSetAuthWhenNoBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should not set authentication for invalid token")
        void shouldNotSetAuthForInvalidToken() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should always call filterChain regardless of auth result")
        void shouldAlwaysCallFilterChain() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);

            reset(filterChain);
            when(request.getHeader("Authorization")).thenReturn("Bearer bad");
            when(jwtUtil.isTokenValid("bad")).thenReturn(false);
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }
}