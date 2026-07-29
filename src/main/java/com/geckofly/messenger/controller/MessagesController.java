package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.message.EditMessageRequest;
import com.geckofly.messenger.model.dto.message.SendMessageRequest;
import com.geckofly.messenger.model.dto.message.SendMessageResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessagesController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<SendMessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(request, currentUser));
    }

    // R7: редактирование своего текстового сообщения.
    @PatchMapping("/{messageUuid}")
    public ResponseEntity<Void> editMessage(
            @PathVariable UUID messageUuid,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        messageService.editMessage(messageUuid, request.getContent(), currentUser);
        return ResponseEntity.noContent().build();
    }

    // R7: мягкое удаление (автор или ADMIN чата).
    @DeleteMapping("/{messageUuid}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageUuid,
            @AuthenticationPrincipal UserEntity currentUser) {
        messageService.deleteMessage(messageUuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}
