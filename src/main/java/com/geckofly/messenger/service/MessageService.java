package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.message.AttachmentDto;
import com.geckofly.messenger.model.dto.message.MessageResponse;
import com.geckofly.messenger.model.dto.message.SendMessageRequest;
import com.geckofly.messenger.model.dto.message.SendMessageResponse;
import com.geckofly.messenger.model.entity.AttachmentEntity;
import com.geckofly.messenger.model.dto.user.UserSummary;
import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.ChatParticipantEntity;
import com.geckofly.messenger.model.entity.MessageEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.model.enums.MessageType;
import com.geckofly.messenger.repository.ChatParticipantRepository;
import com.geckofly.messenger.repository.MessageRepository;
import com.geckofly.messenger.websocket.dto.ChatMessageResponse;
import com.geckofly.messenger.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final ChatParticipantRepository chatParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;

    public SendMessageResponse sendMessage(SendMessageRequest request, UserEntity currentUser) {
        MessageEntity saved = saveMessage(
                request.getChatUuid(),
                request.getContent(),
                request.getType(),
                request.getAttachments(),
                currentUser
        );
        broadcastMessage(saved);

        return SendMessageResponse.builder()
                .message("Message sent successfully")
                .messageUuid(saved.getUuid())
                .chatUuid(request.getChatUuid())
                .build();
    }

    public MessageEntity saveMessage(UUID chatUuid,
                                     String content,
                                     MessageType type,
                                     List<AttachmentDto> attachments,
                                     UserEntity currentUser) {

        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        chatService.checkAccess(chat, currentUser);

        boolean hasText = content != null && !content.isBlank();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasText && !hasAttachments) {
            throw new IllegalArgumentException("Message must contain text or attachments");
        }

        MessageEntity message = new MessageEntity();
        message.setChat(chat);
        message.setSender(currentUser);
        message.setContent(hasText ? content : "");
        message.setType(type != null ? type : MessageType.TEXT);

        if (hasAttachments) {
            for (AttachmentDto attachmentDto : attachments) {
                if (attachmentDto == null || attachmentDto.getFileUrl() == null) {
                    continue;
                }
                AttachmentEntity attachment = new AttachmentEntity();
                attachment.setMessage(message);
                attachment.setFileName(attachmentDto.getFileName());
                attachment.setFileUrl(attachmentDto.getFileUrl());
                attachment.setFileType(attachmentDto.getFileType());
                attachment.setFileSize(attachmentDto.getFileSize());
                message.getAttachments().add(attachment);
            }
        }

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID chatUuid, Pageable pageable, UserEntity currentUser) {

        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        chatService.checkAccess(chat, currentUser);

        return messageRepository.findByChat(chat, pageable)
                .map(m -> {

                    UserEntity sender = m.getSender();

                    return MessageResponse.builder()
                            .uuid(m.getUuid())
                            .chatUuid(chat.getUuid())
                            .sender(userMapper.toSummary(sender))
                            .content(m.getContent())
                            .type(m.getType().name())
                            .createdAt(m.getCreatedAt())
                            .attachments(m.getAttachments().stream()
                                    .map(this::toAttachmentDto)
                                    .collect(Collectors.toList()))
                            .build();
                });
    }

    public void broadcastMessage(MessageEntity saved) {
        UserEntity sender = saved.getSender();
        List<ChatParticipantEntity> participants = chatParticipantRepository.findByChat(saved.getChat());

        UserSummary senderSummary = userMapper.toSummary(sender);

        for (ChatParticipantEntity participant : participants) {
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageUuid(saved.getUuid())
                    .chatUuid(saved.getChat().getUuid())
                    .sender(senderSummary)
                    .content(saved.getContent())
                    .type(saved.getType().name())
                    .createdAt(saved.getCreatedAt())
                    .attachments(saved.getAttachments().stream()
                            .map(this::toAttachmentDto)
                            .collect(Collectors.toList()))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    participant.getUser().getLogin(),
                    "/queue/messages",
                    response
            );
        }
    }

    private AttachmentDto toAttachmentDto(AttachmentEntity attachment) {
        return AttachmentDto.builder()
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .build();
    }
}