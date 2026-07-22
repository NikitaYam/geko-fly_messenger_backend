package com.geckofly.messenger.model.entity;

import com.geckofly.messenger.model.enums.UserChatRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
    name = "chat_participants",
    indexes = {
        @Index(name = "idx_participants_user", columnList = "user_id"),
        @Index(name = "idx_participants_chat", columnList = "chat_id")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_participants_chat_user",
        columnNames = {"chat_id", "user_id"}
    )
)
public class ChatParticipantEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private UserChatRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
}
