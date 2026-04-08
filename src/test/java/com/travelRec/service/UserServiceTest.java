package com.travelRec.service;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.dto.user.UserResponse;
import com.travelRec.entity.User;
import com.travelRec.entity.UserPreferences;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.Role;
import com.travelRec.mapper.PreferencesMapper;
import com.travelRec.mapper.UserMapper;
import com.travelRec.repository.UserPreferencesRepository;
import com.travelRec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Spy
    private PreferencesMapper preferencesMapper = new PreferencesMapper();

    @InjectMocks
    private UserService userService;

    private User user;
    private UserPreferences prefs;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("anna@mail.com")
                .firstName("Anna").lastName("Shevchenko")
                .role(Role.USER).build();

        prefs = UserPreferences.builder()
                .id(1L).user(user)
                .cultureWeight(0.9f).foodWeight(0.8f)
                .nightlifeWeight(0.3f).natureWeight(0.7f)
                .safetyWeight(0.2f).budgetWeight(0.6f)
                .beachWeight(0.4f).architectureWeight(0.5f)
                .shoppingWeight(0.1f)
                .build();
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("should return user response")
        void shouldReturn() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(1L);

            assertEquals("Anna", response.getFirstName());
            assertEquals("anna@mail.com", response.getEmail());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.getUserById(99L));
        }
    }

    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmail {

        @Test
        @DisplayName("should return user by email")
        void shouldReturn() {
            when(userRepository.findByEmail("anna@mail.com")).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserByEmail("anna@mail.com");

            assertEquals(1L, response.getId());
        }

        @Test
        @DisplayName("should throw when email not found")
        void shouldThrow() {
            when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.getUserByEmail("nobody@mail.com"));
        }
    }

    @Nested
    @DisplayName("getPreferences()")
    class GetPreferences {

        @Test
        @DisplayName("should return preferences")
        void shouldReturn() {
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            PreferencesResponse response = userService.getPreferences(1L);

            assertEquals(0.9f, response.getCultureWeight());
            assertEquals(0.8f, response.getFoodWeight());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(preferencesRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.getPreferences(99L));
        }
    }

    @Nested
    @DisplayName("updatePreferences()")
    class UpdatePreferences {

        @Test
        @DisplayName("should update existing preferences without explicit save (Dirty Checking)")
        void shouldUpdate() {
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.1f)
                    .preferredCityType(CityType.RESORT)
                    .build();

            prefs.setFoodWeight(0.8f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            PreferencesResponse response = userService.updatePreferences(1L, request);

            assertEquals(0.1f, response.getCultureWeight());
            assertEquals(0.8f, response.getFoodWeight());
            assertEquals(CityType.RESORT, response.getPreferredCityType());

            verify(preferencesRepository, never()).save(any(UserPreferences.class));
        }

        @Test
        @DisplayName("should create preferences if not exist")
        void shouldCreateIfNotExist() {
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.7f).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> {
                UserPreferences p = inv.getArgument(0);
                p.setId(2L);
                return p;
            });

            PreferencesResponse response = userService.updatePreferences(1L, request);

            verify(preferencesRepository, atLeast(1)).save(any(UserPreferences.class));
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("should delete existing user")
        void shouldDelete() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.deleteUser(1L);

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(99L));
        }
    }
}
