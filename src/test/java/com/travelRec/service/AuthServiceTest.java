package com.travelRec.service;

import com.travelRec.dto.auth.AuthResponse;
import com.travelRec.dto.auth.LoginRequest;
import com.travelRec.dto.auth.RegisterRequest;
import com.travelRec.entity.User;
import com.travelRec.entity.enums.Role;
import com.travelRec.repository.UserRepository;
import com.travelRec.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("anna@mail.com")
                .passwordHash("$2a$10$hashedpassword")
                .firstName("Anna")
                .lastName("Shevchenko")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register new user and return token")
        void shouldRegister() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .build();

            when(userRepository.existsByEmail("anna@mail.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtUtil.generateToken("anna@mail.com", "USER")).thenReturn("jwt-token-123");

            AuthResponse response = authService.register(request);

            assertEquals("jwt-token-123", response.getToken());
            assertEquals("anna@mail.com", response.getEmail());
            assertEquals("Anna", response.getFirstName());
            assertEquals("USER", response.getRole());
        }

        @Test
        @DisplayName("should create user preferences on register")
        void shouldCreatePreferences() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .build();

            when(userRepository.existsByEmail("anna@mail.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");

            authService.register(request);

            verify(userRepository).save(argThat(u -> u.getPreferences() != null));
        }

        @Test
        @DisplayName("should hash password before saving")
        void shouldHashPassword() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .build();

            when(userRepository.existsByEmail("anna@mail.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");

            authService.register(request);

            verify(userRepository).save(argThat(u ->
                    u.getPasswordHash().equals("$2a$10$encoded")));
        }

        @Test
        @DisplayName("should throw when email already exists")
        void shouldThrowWhenEmailExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .build();

            when(userRepository.existsByEmail("anna@mail.com")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set default role USER")
        void shouldSetDefaultRole() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .build();

            when(userRepository.existsByEmail("anna@mail.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");

            AuthResponse response = authService.register(request);

            assertEquals("USER", response.getRole());
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should login with correct credentials")
        void shouldLogin() {
            LoginRequest request = LoginRequest.builder()
                    .email("anna@mail.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
            when(jwtUtil.generateToken("anna@mail.com", "USER")).thenReturn("jwt-token-123");

            AuthResponse response = authService.login(request);

            assertEquals("jwt-token-123", response.getToken());
            assertEquals("anna@mail.com", response.getEmail());
            assertEquals("Anna", response.getFirstName());
        }

        @Test
        @DisplayName("should throw for wrong email")
        void shouldThrowForWrongEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("wrong@mail.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail("wrong@mail.com")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("should throw for wrong password")
        void shouldThrowForWrongPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("anna@mail.com")
                    .password("wrongpassword")
                    .build();

            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("should not reveal whether email or password is wrong")
        void shouldNotRevealWhichIsWrong() {
            LoginRequest wrongEmail = LoginRequest.builder()
                    .email("wrong@mail.com").password("password123").build();
            LoginRequest wrongPass = LoginRequest.builder()
                    .email("anna@mail.com").password("wrongpassword").build();

            when(userRepository.findByEmail("wrong@mail.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                    () -> authService.login(wrongEmail));
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> authService.login(wrongPass));

            assertEquals(ex1.getMessage(), ex2.getMessage());
        }
    }

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("should return new token for valid token")
        void shouldRefresh() {
            when(jwtUtil.isTokenValid("old-token")).thenReturn(true);
            when(jwtUtil.extractEmail("old-token")).thenReturn("anna@mail.com");
            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken("anna@mail.com", "USER")).thenReturn("new-token");

            AuthResponse response = authService.refresh("old-token");

            assertEquals("new-token", response.getToken());
            assertEquals("anna@mail.com", response.getEmail());
        }

        @Test
        @DisplayName("should throw for invalid token")
        void shouldThrowForInvalidToken() {
            when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-token"));
        }

        @Test
        @DisplayName("should throw when user no longer exists")
        void shouldThrowWhenUserDeleted() {
            when(jwtUtil.isTokenValid("old-token")).thenReturn(true);
            when(jwtUtil.extractEmail("old-token")).thenReturn("deleted@mail.com");
            when(userRepository.findByEmail("deleted@mail.com")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> authService.refresh("old-token"));
        }
    }
}
