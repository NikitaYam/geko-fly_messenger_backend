package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.chat.AddParticipantRequest;
import com.geckofly.messenger.model.dto.chat.ChangeChatRoleRequest;
import com.geckofly.messenger.model.dto.chat.ChatResponse;
import com.geckofly.messenger.model.dto.chat.CreateChatRequest;
import com.geckofly.messenger.model.dto.chat.CreateChatResponse;
import com.geckofly.messenger.model.dto.chat.UpdateChatRequest;
import com.geckofly.messenger.model.dto.message.MarkReadRequest;
import com.geckofly.messenger.model.dto.message.MessageResponse;
import com.geckofly.messenger.model.dto.user.ParticipantResponse;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.ChatMembershipService;
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
    private final ChatMembershipService chatMembershipService;

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

    // R5c: отметить чат прочитанным вплоть до указанного сообщения.
    @PostMapping("/{chatUuid}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID chatUuid,
            @Valid @RequestBody MarkReadRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        messageService.markRead(chatUuid, request.getMessageUuid(), currentUser);
        return ResponseEntity.noContent().build();
    }

    // R6: управление участниками группы (только ADMIN чата).
    @PostMapping("/{chatUuid}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable UUID chatUuid,
            @Valid @RequestBody AddParticipantRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        chatMembershipService.addParticipant(chatUuid, request.getLogin(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Выход из группы — до маппинга {userUuid}, чтобы "me" не пытался парситься как UUID.
    @DeleteMapping("/{chatUuid}/participants/me")
    public ResponseEntity<Void> leave(
            @PathVariable UUID chatUuid,
            @AuthenticationPrincipal UserEntity currentUser) {
        chatMembershipService.leave(chatUuid, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{chatUuid}/participants/{userUuid}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID chatUuid,
            @PathVariable UUID userUuid,
            @AuthenticationPrincipal UserEntity currentUser) {
        chatMembershipService.removeParticipant(chatUuid, userUuid, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{chatUuid}")
    public ResponseEntity<ChatResponse> updateChat(
            @PathVariable UUID chatUuid,
            @Valid @RequestBody UpdateChatRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(chatMembershipService.updateChat(chatUuid, request, currentUser));
    }

    @PatchMapping("/{chatUuid}/participants/{userUuid}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID chatUuid,
            @PathVariable UUID userUuid,
            @Valid @RequestBody ChangeChatRoleRequest request,
            @AuthenticationPrincipal UserEntity currentUser) {
        chatMembershipService.changeRole(chatUuid, userUuid, request.getRole(), currentUser);
        return ResponseEntity.noContent().build();
    }

}
