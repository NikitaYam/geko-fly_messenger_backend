package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.user.UpdateUserProfileRequest;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDeletionService userDeletionService;

    private UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .uuid(user.getUuid())
                .login(user.getLogin())
                .displayName(user.getDisplayName())
                .email(user.getUserEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserEntity currentUser) {
        return toResponse(currentUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByLogin(String login) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + login));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UserEntity currentUser, UpdateUserProfileRequest request) {
        boolean changed = false;

        if (request.getDisplayName() != null) {
            String name = request.getDisplayName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be empty");
            }
            currentUser.setDisplayName(name);
            changed = true;
        }

        if (request.getEmail() != null) {
            String email = blankToNull(request.getEmail());
            if (email != null
                && userRepository.existsByUserEmail(email)
                && !email.equalsIgnoreCase(currentUser.getUserEmail())){
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
                }
            currentUser.setUserEmail(email);
            changed = true;
        }

        if (request.getAvatarUrl() != null) {
            currentUser.setAvatarUrl(blankToNull(request.getAvatarUrl()));
            changed = true;
        }
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return toResponse(userRepository.save(currentUser));
    }

    @Transactional
    public void deleteAccount(UserEntity currentUser, String password) {
        if (!passwordEncoder.matches(password, currentUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect password");   
        }
        userDeletionService.deleteUserData(currentUser);
    }
}