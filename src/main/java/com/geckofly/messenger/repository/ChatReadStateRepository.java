package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.ChatReadStateEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatReadStateRepository extends JpaRepository<ChatReadStateEntity, Long> {

    Optional<ChatReadStateEntity> findByChatAndUser(ChatEntity chat, UserEntity user);

    List<ChatReadStateEntity> findByChat(ChatEntity chat);

    void deleteByChat(ChatEntity chat);

    void deleteByChatAndUser(ChatEntity chat, UserEntity user);
}
