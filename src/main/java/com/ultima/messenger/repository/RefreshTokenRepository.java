package com.ultima.messenger.repository;

import com.ultima.messenger.model.entity.RefreshTokenEntity;
import com.ultima.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") UserEntity user);
}
