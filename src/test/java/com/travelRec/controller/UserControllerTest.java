package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.dto.user.UpdateUserRequest;
import com.travelRec.dto.user.UserResponse;
import com.travelRec.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private RequestPostProcessor authUser;

    @BeforeEach
    void setUp() {
        authUser = TestAuth.user();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserResponse buildUser() {
        return UserResponse.builder()
                .id(1L).email("anna@mail.com")
                .firstName("Anna").lastName("Shevchenko")
                .role("USER").build();
    }

    private PreferencesResponse buildPrefs() {
        return PreferencesResponse.builder()
                .id(1L)
                .cultureWeight(0.9f).foodWeight(0.8f)
                .nightlifeWeight(0.3f).natureWeight(0.7f)
                .safetyWeight(0.2f).budgetWeight(0.6f)
                .beachWeight(0.4f).architectureWeight(0.5f)
                .shoppingWeight(0.1f)
                .preferredCityTypes(Set.of())
                .preferredClimateTypes(Set.of())
                .build();
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user")
        void shouldReturn() throws Exception {
            when(userService.getUserById(1L)).thenReturn(buildUser());

            mockMvc.perform(get("/api/users/me").with(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("anna@mail.com"))
                    .andExpect(jsonPath("$.firstName").value("Anna"));

            verify(userService).getUserById(1L);
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    class UpdateCurrentUser {

        @Test
        @DisplayName("should update and return user")
        void shouldUpdate() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Annette");

            when(userService.updateUser(eq(1L), any(UpdateUserRequest.class)))
                    .thenReturn(buildUser());

            mockMvc.perform(put("/api/users/me")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("should return 400 when firstName too long")
        void shouldRejectTooLong() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("a".repeat(100));

            mockMvc.perform(put("/api/users/me")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetById {

        @Test
        @DisplayName("should return user by id")
        void shouldReturn() throws Exception {
            when(userService.getUserById(1L)).thenReturn(buildUser());

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}/preferences")
    class GetPreferences {

        @Test
        @DisplayName("should return preferences for current user")
        void shouldReturn() throws Exception {
            when(userService.getPreferences(1L)).thenReturn(buildPrefs());

            mockMvc.perform(get("/api/users/1/preferences").with(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cultureWeight").value(0.9));

            // controller passes user.getId(), not the path param — that's the production behavior
            verify(userService).getPreferences(1L);
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}/preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("should update preferences and return 200")
        void shouldUpdate() throws Exception {
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.5f)
                    .preferredCityTypes(Set.of())
                    .preferredClimateTypes(Set.of())
                    .build();

            when(userService.updatePreferences(eq(1L), any(PreferencesRequest.class)))
                    .thenReturn(buildPrefs());

            mockMvc.perform(put("/api/users/1/preferences")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(userService).updatePreferences(eq(1L), any(PreferencesRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 No Content")
        void shouldDelete() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(1L);
        }
    }
}