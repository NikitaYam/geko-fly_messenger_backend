package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.enums.UserRole;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    Optional<UserEntity> findByUuid(UUID uuid);

    boolean existsByLogin(String login);

    boolean existsByUserEmail(String email);

    List<UserEntity> findAllByLoginIn(List<String> logins);

    boolean existsByRole(UserRole role);

    long countByRole(UserRole role);
}
