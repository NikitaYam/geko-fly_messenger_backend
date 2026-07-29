package com.geckofly.messenger.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Вотермарки прочтения/доставки на пару (чат, пользователь).
 * Хранит внутренний id последнего доставленного/прочитанного сообщения.
 */
@Getter
@Setter
@Entity
@Table(
    name = "chat_read_state",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_read_state",
        columnNames = {"chat_id", "user_id"}
    )
)
public class ChatReadStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "last_delivered_message_id")
    private Long lastDeliveredMessageId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}
