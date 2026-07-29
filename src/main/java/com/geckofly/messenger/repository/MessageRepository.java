package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.MessageEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Page<MessageEntity> findByChat(ChatEntity chat, Pageable pageable);

    List<MessageEntity> findByChat(ChatEntity chat);

    List<MessageEntity> findBySender(UserEntity sender);

    Optional<MessageEntity> findTopByChatOrderByCreatedAtDesc(ChatEntity chat);

    Optional<MessageEntity> findByUuid(UUID uuid);

    /** Непрочитанные: сообщения чата новее afterId, отправленные не самим viewer. */
    @Query("""
            SELECT COUNT(m) FROM MessageEntity m
            WHERE m.chat = :chat AND m.id > :afterId AND m.sender <> :viewer
            """)
    long countUnread(@Param("chat") ChatEntity chat,
                     @Param("afterId") long afterId,
                     @Param("viewer") UserEntity viewer);
}
