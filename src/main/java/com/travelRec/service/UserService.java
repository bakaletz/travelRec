package com.travelRec.service;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.dto.user.UserResponse;
import com.travelRec.entity.User;
import com.travelRec.entity.UserPreferences;
import com.travelRec.mapper.PreferencesMapper;
import com.travelRec.mapper.UserMapper;
import com.travelRec.repository.UserPreferencesRepository;
import com.travelRec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;
    private final PreferencesMapper preferencesMapper;

    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    public PreferencesResponse getPreferences(Long userId) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));
        return preferencesMapper.toResponse(prefs);
    }

    @Transactional
    public PreferencesResponse updatePreferences(Long userId, PreferencesRequest request) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);

        if (prefs != null) {
            preferencesMapper.updateEntity(prefs, request);

        } else {
            User user = findUserOrThrow(userId);
            prefs = UserPreferences.builder().user(user).build();

            preferencesMapper.updateEntity(prefs, request);

            preferencesRepository.save(prefs);
        }

        return preferencesMapper.toResponse(prefs);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    public User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
}
