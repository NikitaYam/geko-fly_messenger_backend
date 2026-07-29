package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.user.DeleteUserRequest;
import com.geckofly.messenger.model.dto.user.UpdateUserProfileRequest;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.dto.user.UserSummary;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(userService.getCurrentUser(currentUser));
    }

    // R8: поиск пользователей для создания чата.
    @GetMapping
    public ResponseEntity<List<UserSummary>> search(
            @RequestParam String query,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(userService.searchUsers(query, currentUser));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(userService.updateProfile(currentUser, request));
    }

    @GetMapping("/{login}")
    public ResponseEntity<UserResponse> getByLogin(@PathVariable String login) {
        return ResponseEntity.ok(userService.getUserByLogin(login));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @Valid @RequestBody DeleteUserRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        userService.deleteAccount(currentUser, request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
