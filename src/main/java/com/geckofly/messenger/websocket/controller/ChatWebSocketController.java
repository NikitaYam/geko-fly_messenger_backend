package com.geckofly.messenger.websocket.controller;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.MessageService;
import com.geckofly.messenger.websocket.dto.ChatMessageRequest;
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
        if (request.getChatUuid() == null || (!hasText && !hasAttachments)) {
            log.warn("sendMessage: invalid request from user={}", sender.getLogin());
            return;
        }

        // save + broadcast в одной транзакции (P2: безопасно для LAZY-связей).
        messageService.handleWebSocketMessage(
                request.getChatUuid(),
                request.getContent(),
                request.getType(),
                request.getAttachments(),
                sender
        );
    }
}