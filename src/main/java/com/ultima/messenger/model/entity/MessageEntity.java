package com.ultima.messenger.model.entity;

import com.ultima.messenger.model.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_chat_id",   columnList = "chat_id"),
        @Index(name = "idx_messages_sender_id", columnList = "sender_id")
    }
)
public class MessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(name = "content", nullable = false, length = 4096)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<AttachmentEntity> attachments = new java.util.ArrayList<>();
}
