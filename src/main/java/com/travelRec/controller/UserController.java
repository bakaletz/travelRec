package com.travelRec.controller;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.dto.user.UserResponse;
import com.travelRec.security.CustomUserDetails;
import com.travelRec.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/preferences")
    public ResponseEntity<PreferencesResponse> getPreferences(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.getPreferences(user.getId()));
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<PreferencesResponse> updatePreferences(@AuthenticationPrincipal CustomUserDetails user,
                                                                  @Valid @RequestBody PreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(user.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
