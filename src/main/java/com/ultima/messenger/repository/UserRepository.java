package com.ultima.messenger.repository;

import com.ultima.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    Optional<UserEntity> findByUserEmail(String email);

    Optional<UserEntity> findByUuid(UUID uuid);

    boolean existsByLogin(String login);

    boolean existsByUserEmail(String email);

    List<UserEntity> findAllByLoginIn(List<String> logins);

    boolean existsByAdminTrue();
}
