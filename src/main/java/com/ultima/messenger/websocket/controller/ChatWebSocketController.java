package com.ultima.messenger.websocket.controller;

import com.ultima.messenger.model.entity.MessageEntity;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.service.MessageService;
import com.ultima.messenger.websocket.dto.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest request,
                            SimpMessageHeaderAccessor accessor) {

        UserEntity sender = (UserEntity) accessor.getSessionAttributes().get("user");

        if (sender == null) {
            log.error("sendMessage called without authenticated user in session");
            return;
        }

        boolean hasText = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();
        if (request.getChatId() == null || (!hasText && !hasAttachments)) {
            log.warn("sendMessage: invalid request from user={}", sender.getLogin());
            return;
        }

        MessageEntity saved = messageService.saveMessage(
                request.getChatId(),
                request.getContent(),
                request.getType(),
                request.getAttachments(),
                sender
        );
        messageService.broadcastMessage(saved);
    }
}