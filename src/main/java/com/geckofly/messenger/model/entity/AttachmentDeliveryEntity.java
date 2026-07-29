package com.geckofly.messenger.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
    name = "attachment_deliveries",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_attachment_delivery",
        columnNames = {"attachment_id", "user_id"}
    )
)
public class AttachmentDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    private AttachmentEntity attachment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;
    
}
