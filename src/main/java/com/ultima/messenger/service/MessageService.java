package com.ultima.messenger.service;

import com.ultima.messenger.model.dto.message.AttachmentDto;
import com.ultima.messenger.model.dto.message.MessageResponse;
import com.ultima.messenger.model.dto.message.SendMessageRequest;
import com.ultima.messenger.model.dto.message.SendMessageResponse;
import com.ultima.messenger.model.entity.AttachmentEntity;
import com.ultima.messenger.model.dto.user.UserSummary;
import com.ultima.messenger.model.entity.ChatEntity;
import com.ultima.messenger.model.entity.ChatParticipantEntity;
import com.ultima.messenger.model.entity.MessageEntity;
import com.ultima.messenger.model.entity.UserEntity;
import com.ultima.messenger.model.enums.MessageType;
import com.ultima.messenger.repository.ChatParticipantRepository;
import com.ultima.messenger.repository.MessageRepository;
import com.ultima.messenger.websocket.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SendMessageResponse sendMessage(SendMessageRequest request, UserEntity currentUser) {
        MessageEntity saved = saveMessage(
                request.getChatId(),
                request.getContent(),
                request.getType(),
                request.getAttachments(),
                currentUser
        );
        broadcastMessage(saved);

        return SendMessageResponse.builder()
                .message("Message sent successfully")
                .build();
    }

    public MessageEntity saveMessage(Long chatId,
                                     String content,
                                     MessageType type,
                                     List<AttachmentDto> attachments,
                                     UserEntity currentUser) {

        ChatEntity chat = chatService.getChatOrThrow(chatId);
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
    public Page<MessageResponse> getMessages(Long chatId, Pageable pageable, UserEntity currentUser) {

        ChatEntity chat = chatService.getChatOrThrow(chatId);
        chatService.checkAccess(chat, currentUser);

        return messageRepository.findByChat(chat, pageable)
                .map(m -> {

                    UserEntity sender = m.getSender();

                    return MessageResponse.builder()
                            .id(m.getId())
                            .chatId(chat.getId())
                            .sender(toUserSummary(sender))
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

        UserSummary senderSummary = toUserSummary(sender);

        for (ChatParticipantEntity participant : participants) {
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageId(saved.getId())
                    .chatId(saved.getChat().getId())
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

    private UserSummary toUserSummary(UserEntity user) {
        return UserSummary.builder()
                .uuid(user.getUuid())
                .login(user.getLogin())
                .displayName(user.getDisplayName())
                .avatarUrl(null)
                .build();
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