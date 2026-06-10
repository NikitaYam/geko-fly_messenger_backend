package com.ultima.messenger.model.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateChatResponse {
    
    private String message;
    private Long chatId;
}
