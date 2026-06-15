package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.MessageEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Page<MessageEntity> findByChat(ChatEntity chat, Pageable pageable);

    List<MessageEntity> findByChat(ChatEntity chat);

    List<MessageEntity> findBySender(UserEntity sender);

    Optional<MessageEntity> findTopByChatOrderByCreatedAtDesc(ChatEntity chat);
}
