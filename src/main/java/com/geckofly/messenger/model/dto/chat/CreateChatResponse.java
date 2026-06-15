package com.geckofly.messenger.model.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CreateChatResponse {
    
    private String message;
    private UUID uuid;
}
