package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.chat.ChatResponse;
import com.geckofly.messenger.model.dto.chat.CreateChatRequest;
import com.geckofly.messenger.model.dto.chat.CreateChatResponse;
import com.geckofly.messenger.model.dto.message.MessageResponse;
import com.geckofly.messenger.model.dto.user.ParticipantResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.ChatService;
import com.geckofly.messenger.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatsController {

    private final ChatService chatService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<CreateChatResponse> createChat(
            @Valid @RequestBody CreateChatRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createChat(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getChats(
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(chatService.getChats(currentUser));
    }

    @GetMapping("/{chatUuid}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable UUID chatUuid,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(chatService.getParticipants(chatUuid, currentUser));
    }

    @GetMapping("/{chatUuid}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable UUID chatUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserEntity currentUser) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(messageService.getMessages(chatUuid, pageable, currentUser));
    }

}
