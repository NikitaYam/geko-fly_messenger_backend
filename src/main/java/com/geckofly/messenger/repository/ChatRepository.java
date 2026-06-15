package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    List<ChatEntity> findByTitle(String title);

    Optional<ChatEntity> findByUuid(UUID uuid);
}
