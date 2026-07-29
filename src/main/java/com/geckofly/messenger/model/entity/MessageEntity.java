package com.geckofly.messenger.model.entity;

import com.geckofly.messenger.model.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_chat_id",   columnList = "chat_id"),
        @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
        @Index(name = "idx_messages_uuid", columnList = "uuid")
    }
)
public class MessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(name = "content", nullable = false, length = 4096)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

    /** Когда отредактировано (R7); null — не редактировалось. */
    @Column(name = "edited_at")
    private Instant editedAt;

    /** Мягкое удаление (R7): true — контент затёрт, показываем «сообщение удалено». */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<AttachmentEntity> attachments = new java.util.ArrayList<>();
}
