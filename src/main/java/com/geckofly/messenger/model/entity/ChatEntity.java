package com.geckofly.messenger.model.entity;

import org.hibernate.annotations.UuidGenerator;
import com.geckofly.messenger.model.enums.ChatType;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "chats",
    indexes = {
        @Index(name = "idx_chats_uuid", columnList = "uuid")
    }
)
public class ChatEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "chat_type")
    private ChatType type;

    @Column(nullable = true)
    private String title;

    /** Аватар группового чата (R6). У приватных чатов null — показываем аватар собеседника. */
    @Column(name = "avatar_url")
    private String avatarUrl;
}
