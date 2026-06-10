package com.ultima.messenger.service;

import com.ultima.messenger.model.dto.user.UserResponse;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.repository.UserRepository;
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
    public void deleteAccount(UserEntity currentUser, String password) {
        if (!passwordEncoder.matches(password, currentUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect password");
        }
        userRepository.delete(currentUser);
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
}
