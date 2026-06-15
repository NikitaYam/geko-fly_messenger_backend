package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.ChatParticipantEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.model.enums.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, Long> {

    List<ChatParticipantEntity> findByUser(UserEntity user);

    List<ChatParticipantEntity> findByChat(ChatEntity chat);

    boolean existsByChatAndUser(ChatEntity chat, UserEntity user);

    @Query("""
            SELECT CASE WHEN COUNT(cp1) > 0 THEN true ELSE false END
            FROM ChatParticipantEntity cp1
            JOIN ChatParticipantEntity cp2 ON cp1.chat = cp2.chat
            WHERE cp1.user = :user1
              AND cp2.user = :user2
              AND cp1.chat.type = :type
            """)
    boolean existsChatBetweenUsersWithType(
            @Param("user1") UserEntity user1,
            @Param("user2") UserEntity user2,
            @Param("type") ChatType type);
}
