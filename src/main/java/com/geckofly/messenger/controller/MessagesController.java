package com.geckofly.messenger.controller;

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
}
