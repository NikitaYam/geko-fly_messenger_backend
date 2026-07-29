package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.enums.UserRole;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    /** Поиск по подстроке логина или отображаемого имени (R8), без самого себя. */
    @Query("""
            SELECT u FROM UserEntity u
            WHERE u.id <> :excludeId
              AND u.deleted = false
              AND (LOWER(u.login) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
                   OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\')
            ORDER BY u.displayName
            """)
    List<UserEntity> searchByLoginOrName(@Param("q") String q,
                                         @Param("excludeId") Long excludeId,
                                         Pageable pageable);

    Optional<UserEntity> findByUuid(UUID uuid);

    boolean existsByLogin(String login);

    boolean existsByUserEmail(String email);

    List<UserEntity> findAllByLoginIn(List<String> logins);

    boolean existsByRole(UserRole role);

    long countByRole(UserRole role);
}
