package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.admin.AdminCreateUserRequest;
import com.geckofly.messenger.model.dto.admin.AdminUpdateUserRoleRequest;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.model.enums.UserRole;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.geckofly.messenger.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDeletionService userDeletionService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
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
        
        UserRole roleToAssign = resolveRoleOnCreate(request.getRole(), actor);

        UserEntity user = new UserEntity();
        user.setLogin(request.getLogin().trim());
        user.setDisplayName(request.getDisplayName().trim());
        user.setUserEmail(blankToNull(request.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(roleToAssign);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(
        UUID userUuid,
        AdminUpdateUserRoleRequest request,
        UserEntity actor) {
            if (!actor.isSuperAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only super-admin can change roles");
            }

            UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            
                if (user.getId().equals(actor.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
                }
                if (user.isSuperAdmin()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Super-admin role cannot be changed");
                }
                UserRole newRole = request.getRole();
                if (newRole != UserRole.USER && newRole != UserRole.ADMIN) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
                }
                user.setRole(newRole);
                return userMapper.toResponse(userRepository.save(user));
            }

    @Transactional
    public void deleteUser(UUID userUuid, UserEntity actor) {
        if (!actor.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only super-admin can delete users");
        }

        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete yourself");
        }
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Super-admin cannot be deleted");
        }

        userDeletionService.deleteUserData(user);
    }

    private UserRole resolveRoleOnCreate(UserRole requestedRole, UserEntity actor) {
        if (requestedRole == null || requestedRole == UserRole.USER) {
            return UserRole.USER;
        }
        if (requestedRole == UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "SUPER_ADMIN cannot be created via API");
        }
        if (requestedRole == UserRole.ADMIN) {
            if (!actor.isSuperAdmin()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Only super-admin can create admin users");
            }
            return UserRole.ADMIN;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
