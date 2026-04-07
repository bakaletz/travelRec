package com.travelRec.mapper;

import com.travelRec.dto.user.UserResponse;
import com.travelRec.entity.User;
import com.travelRec.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all fields")
        void shouldMapAllFields() {
            LocalDateTime now = LocalDateTime.now();
            User user = User.builder()
                    .id(1L)
                    .email("anna@mail.com")
                    .firstName("Anna")
                    .lastName("Shevchenko")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .role(Role.USER)
                    .createdAt(now)
                    .build();

            UserResponse response = mapper.toResponse(user);

            assertEquals(1L, response.getId());
            assertEquals("anna@mail.com", response.getEmail());
            assertEquals("Anna", response.getFirstName());
            assertEquals("Shevchenko", response.getLastName());
            assertEquals("https://example.com/avatar.jpg", response.getAvatarUrl());
            assertEquals("USER", response.getRole());
            assertEquals(now, response.getCreatedAt());
        }

        @Test
        @DisplayName("should map admin role correctly")
        void shouldMapAdminRole() {
            User user = User.builder()
                    .id(2L)
                    .email("admin@mail.com")
                    .firstName("Admin")
                    .lastName("User")
                    .role(Role.ADMIN)
                    .build();

            UserResponse response = mapper.toResponse(user);

            assertEquals("ADMIN", response.getRole());
        }

        @Test
        @DisplayName("should handle null avatar")
        void shouldHandleNullAvatar() {
            User user = User.builder()
                    .id(1L)
                    .email("test@mail.com")
                    .firstName("Test")
                    .lastName("User")
                    .role(Role.USER)
                    .build();

            UserResponse response = mapper.toResponse(user);

            assertNull(response.getAvatarUrl());
        }

        @Test
        @DisplayName("should not expose password hash")
        void shouldNotExposePasswordHash() {
            User user = User.builder()
                    .id(1L)
                    .email("test@mail.com")
                    .passwordHash("$2a$10$secrethash")
                    .firstName("Test")
                    .lastName("User")
                    .role(Role.USER)
                    .build();

            UserResponse response = mapper.toResponse(user);

            assertFalse(response.toString().contains("secrethash"));
        }
    }
}
