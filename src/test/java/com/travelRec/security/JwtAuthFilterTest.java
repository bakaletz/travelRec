package com.travelRec.security;

import com.travelRec.entity.User;
import com.travelRec.entity.enums.Role;
import com.travelRec.repository.UserRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private User user;
    private User adminUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        user = User.builder()
                .id(1L)
                .email("anna@mail.com")
                .firstName("Anna")
                .lastName("Shevchenko")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .email("admin@mail.com")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .build();
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
            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getPrincipal() instanceof CustomUserDetails);

            CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
            assertEquals(1L, details.getId());
            assertEquals("anna@mail.com", details.getEmail());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should set ADMIN authority for admin token")
        void shouldSetAdminAuthority() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
            when(jwtUtil.isTokenValid("admin-token")).thenReturn(true);
            when(jwtUtil.extractEmail("admin-token")).thenReturn("admin@mail.com");
            when(jwtUtil.extractRole("admin-token")).thenReturn("ADMIN");
            when(userRepository.findByEmail("admin@mail.com")).thenReturn(Optional.of(adminUser));

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("should set USER authority for user token")
        void shouldSetUserAuthority() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer user-token");
            when(jwtUtil.isTokenValid("user-token")).thenReturn(true);
            when(jwtUtil.extractEmail("user-token")).thenReturn("anna@mail.com");
            when(jwtUtil.extractRole("user-token")).thenReturn("USER");
            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        }

        @Test
        @DisplayName("should not set authentication when no header")
        void shouldNotSetAuthWhenNoHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should not set authentication when header without Bearer")
        void shouldNotSetAuthWhenNoBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
            verify(jwtUtil, never()).isTokenValid(anyString());
        }

        @Test
        @DisplayName("should not set authentication for invalid token")
        void shouldNotSetAuthForInvalidToken() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("should not set authentication when user no longer exists in DB")
        void shouldNotSetAuthWhenUserDeleted() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractEmail("valid-token")).thenReturn("deleted@mail.com");
            when(jwtUtil.extractRole("valid-token")).thenReturn("USER");
            when(userRepository.findByEmail("deleted@mail.com")).thenReturn(Optional.empty());

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