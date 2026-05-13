package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.auth.AuthResponse;
import com.travelRec.dto.auth.LoginRequest;
import com.travelRec.dto.auth.RegisterRequest;
import com.travelRec.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.travelRec.repository.UserRepository;
import com.travelRec.security.JwtUtil;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .token("jwt-token-123")
                .id(1L)
                .email("anna@mail.com")
                .firstName("Anna")
                .lastName("Shevchenko")
                .role("USER")
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 CREATED with token on valid request")
        void shouldRegister() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com").password("password123")
                    .firstName("Anna").lastName("Shevchenko").build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt-token-123"))
                    .andExpect(jsonPath("$.email").value("anna@mail.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldRejectInvalidEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("not-an-email").password("password123")
                    .firstName("Anna").lastName("Shevchenko").build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldRejectShortPassword() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com").password("123")
                    .firstName("Anna").lastName("Shevchenko").build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when first name is blank")
        void shouldRejectBlankFirstName() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com").password("password123")
                    .firstName("").lastName("Shevchenko").build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 OK with token on valid login")
        void shouldLogin() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("anna@mail.com").password("password123").build();

            when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-123"))
                    .andExpect(jsonPath("$.email").value("anna@mail.com"));
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void shouldRejectBlankEmail() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("").password("password123").build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 200 OK with new token")
        void shouldRefresh() throws Exception {
            when(authService.refresh("old-token")).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/auth/refresh")
                            .header("Authorization", "Bearer old-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-123"));
        }

        @Test
        @DisplayName("should strip Bearer prefix from header")
        void shouldStripBearer() throws Exception {
            when(authService.refresh("plain-token")).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/auth/refresh")
                            .header("Authorization", "Bearer plain-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when Authorization header is missing")
        void shouldRejectMissingHeader() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest());
        }
    }
}