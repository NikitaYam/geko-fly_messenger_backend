package com.ultima.messenger.model.entity;

import lombok.Getter;
import lombok.Setter;

import com.ultima.messenger.model.enums.ChatType;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "chats")
public class ChatEntity extends BaseEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "chat_type")
    private ChatType type;

    @Column(nullable = true)
    private String title;


}
