package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.admin.AdminCreateUserRequest;
import com.geckofly.messenger.model.dto.admin.AdminUpdateUserRoleRequest;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        UserResponse created = adminUserService.createUser(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/users/{userUuid}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userUuid,
            @Valid @RequestBody AdminUpdateUserRoleRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(adminUserService.updateUserRole(userUuid, request, currentUser));
    }

    @DeleteMapping("/users/{userUuid}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userUuid,
            @AuthenticationPrincipal UserEntity currentUser) {
        adminUserService.deleteUser(userUuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}
