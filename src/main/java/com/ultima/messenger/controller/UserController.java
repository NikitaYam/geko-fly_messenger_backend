package com.ultima.messenger.controller;

import com.ultima.messenger.model.dto.user.DeleteUserRequest;
import com.ultima.messenger.model.dto.user.UserResponse;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.service.UserService;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(userService.getCurrentUser(currentUser));
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
