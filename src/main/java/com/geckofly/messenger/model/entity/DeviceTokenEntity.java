package com.geckofly.messenger.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** FCM-токен устройства пользователя (R9). У пользователя может быть несколько. */
@Getter
@Setter
@Entity
@Table(
    name = "device_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_token", columnNames = "token")
)
public class DeviceTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "platform", length = 32)
    private String platform;
}
