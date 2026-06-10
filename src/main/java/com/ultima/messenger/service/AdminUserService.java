package com.ultima.messenger.service;

import com.ultima.messenger.model.dto.admin.AdminCreateUserRequest;
import com.ultima.messenger.model.dto.admin.AdminUpdateUserRoleRequest;
import com.ultima.messenger.model.dto.user.UserResponse;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request, UserEntity actor) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login is already taken");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByUserEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (request.isAdmin() && !actor.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can grant admin role");
        }

        UserEntity user = new UserEntity();
        user.setLogin(request.getLogin());
        user.setDisplayName(request.getDisplayName());
        user.setUserEmail(blankToNull(request.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAdmin(request.isAdmin());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(UUID userUuid, AdminUpdateUserRoleRequest request, UserEntity actor) {
        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getId().equals(actor.getId()) && Boolean.FALSE.equals(request.getAdmin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove your own admin role");
        }

        user.setAdmin(Boolean.TRUE.equals(request.getAdmin()));
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .uuid(user.getUuid())
                .login(user.getLogin())
                .displayName(user.getDisplayName())
                .email(user.getUserEmail())
                .admin(user.isAdmin())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
