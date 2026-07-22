package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.RefreshTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /** Полная зачистка сессий пользователя — используется при удалении аккаунта. */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") UserEntity user);

    /** Гигиена при логине: убрать только истёкшие токены, живые сессии не трогать. */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.user = :user AND rt.expiresAt < :now")
    void deleteExpiredByUser(@Param("user") UserEntity user, @Param("now") Instant now);
}
