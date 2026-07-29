package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.user.UpdateUserProfileRequest;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.dto.user.UserSummary;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import com.geckofly.messenger.mapper.UserMapper;
import com.geckofly.messenger.util.UploadPaths;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDeletionService userDeletionService;
    private final UserMapper userMapper;

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserEntity currentUser) {
        return userMapper.toResponse(currentUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByLogin(String login) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + login));
        return userMapper.toResponse(user);
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
            String avatar = blankToNull(request.getAvatarUrl());
            // A2: URL аватара должен указывать на наш загруженный файл, а не на произвольную строку.
            if (avatar != null && !UploadPaths.isValidAvatarUrl(avatar)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar URL");
            }
            currentUser.setAvatarUrl(avatar);
            changed = true;
        }
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return userMapper.toResponse(userRepository.save(currentUser));
    }

    /** Поиск пользователей для создания чата (R8): минимум 2 символа, до 20 результатов, без себя. */
    @Transactional(readOnly = true)
    public List<UserSummary> searchUsers(String query, UserEntity currentUser) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        // A4: экранируем спецсимволы LIKE (\ % _), чтобы "query=%" не возвращал всех.
        String escaped = query.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return userRepository
                .searchByLoginOrName(escaped, currentUser.getId(), PageRequest.of(0, 20))
                .stream()
                .map(userMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAccount(UserEntity currentUser, String password) {
        if (!passwordEncoder.matches(password, currentUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect password");   
        }
        userDeletionService.deleteUserData(currentUser);
    }
}