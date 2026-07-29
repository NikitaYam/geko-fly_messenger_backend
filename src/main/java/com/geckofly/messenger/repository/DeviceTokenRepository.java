package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.DeviceTokenEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {

    Optional<DeviceTokenEntity> findByToken(String token);

    List<DeviceTokenEntity> findByUser(UserEntity user);

    void deleteByToken(String token);

    void deleteByUser(UserEntity user);
}
