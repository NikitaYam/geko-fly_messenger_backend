package com.ultima.messenger.controller;

import com.ultima.messenger.model.dto.chat.ChatResponse;
import com.ultima.messenger.model.dto.chat.CreateChatRequest;
import com.ultima.messenger.model.dto.chat.CreateChatResponse;
import com.ultima.messenger.model.dto.message.MessageResponse;
import com.ultima.messenger.model.dto.user.PartisipantResponse;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.service.ChatService;
import com.ultima.messenger.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{chatId}/participants")
    public ResponseEntity<List<PartisipantResponse>> getParticipants(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(chatService.getParticipants(chatId, currentUser));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserEntity currentUser) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(messageService.getMessages(chatId, pageable, currentUser));
    }
}
