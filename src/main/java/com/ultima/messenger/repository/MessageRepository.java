package com.ultima.messenger.repository;

import com.ultima.messenger.model.entity.ChatEntity;
import com.ultima.messenger.model.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Page<MessageEntity> findByChat(ChatEntity chat, Pageable pageable);

    Optional<MessageEntity> findTopByChatOrderByCreatedAtDesc(ChatEntity chat);
}
