package com.geckofly.messenger.websocket.controller;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.ActiveChatService;
import com.geckofly.messenger.service.MessageService;
import com.geckofly.messenger.websocket.dto.ActiveChatRequest;
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
    private final ActiveChatService activeChatService;

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

    // Клиент шлёт при открытии/закрытии экрана чата (chatUuid=null — закрыт).
    // Пока чат "активен" — push по нему не шлём (см. MessageService.broadcastMessage),
    // сообщение и так видно на экране по этому же WS-соединению.
    @MessageMapping("/chat.active")
    public void setActiveChat(@Payload ActiveChatRequest request,
                              SimpMessageHeaderAccessor accessor) {
        UserEntity user = (UserEntity) accessor.getSessionAttributes().get("user");
        if (user == null) {
            log.error("chat.active called without authenticated user in session");
            return;
        }
        log.info("chat.active received: user={} chatUuid={}", user.getLogin(), request.getChatUuid());
        activeChatService.setActiveChat(user.getLogin(), request.getChatUuid());
    }
}