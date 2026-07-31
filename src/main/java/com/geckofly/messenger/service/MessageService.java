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
import com.geckofly.messenger.model.enums.UserChatRole;
import com.geckofly.messenger.websocket.dto.ChatMessageResponse;
import com.geckofly.messenger.websocket.dto.MessageMutationPayload;
import com.geckofly.messenger.websocket.dto.WsEventType;
import com.geckofly.messenger.mapper.UserMapper;
import com.geckofly.messenger.util.UploadPaths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final ChatParticipantRepository chatParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;
    private final MessageStatusService messageStatusService;
    private final FileStorageService fileStorageService;
    private final ActiveChatService activeChatService;
    private final PushService pushService;
    private final RateLimitService rateLimitService;

    // Для превращения относительного /api/uploads/avatars/... в абсолютный URL
    // в push-уведомлении (FCM image требует полный адрес). Пусто — картинки не будет.
    @Value("${messenger.public-url:}")
    private String publicUrl;

    public SendMessageResponse sendMessage(SendMessageRequest request, UserEntity currentUser) {
        // A3: не более 30 сообщений за 10 секунд на пользователя.
        rateLimitService.check("msg:" + currentUser.getLogin(), 30, Duration.ofSeconds(10));
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

    /**
     * Полный путь отправки через WebSocket: сохранить и разослать в ОДНОЙ транзакции.
     * Важно для LAZY-связей (P2): иначе broadcast обращался бы к отсоединённой сущности.
     */
    @Transactional
    public void handleWebSocketMessage(UUID chatUuid, String content, MessageType type,
                                       List<AttachmentDto> attachments, UserEntity sender) {
        MessageEntity saved = saveMessage(chatUuid, content, type, attachments, sender);
        broadcastMessage(saved);
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
                // A2: вложение должно ссылаться на наш загруженный файл.
                if (!UploadPaths.isValidFileUrl(attachmentDto.getFileUrl())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment URL");
                }
                AttachmentEntity attachment = new AttachmentEntity();
                attachment.setMessage(message);
                attachment.setFileName(attachmentDto.getFileName());
                attachment.setFileUrl(attachmentDto.getFileUrl());
                attachment.setFileType(attachmentDto.getFileType());
                attachment.setFileSize(attachmentDto.getFileSize());
                attachment.setStoredName(extractStoredName(attachmentDto.getFileUrl()));
                message.getAttachments().add(attachment);
            }
        }

        return messageRepository.save(message);
    }

    @Transactional
    public Page<MessageResponse> getMessages(UUID chatUuid, Pageable pageable, UserEntity currentUser) {

        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        chatService.checkAccess(chat, currentUser);

        // Загрузка истории = сообщения доставлены на устройство пользователя.
        messageStatusService.markDeliveredUpToLatest(chat, currentUser);

        // Вотермарки получателей считаем один раз на страницу (не на каждое сообщение).
        long[] watermarks = messageStatusService.minWatermarks(chat, currentUser);

        return messageRepository.findByChat(chat, pageable)
                .map(m -> {

                    UserEntity sender = m.getSender();
                    boolean own = sender.getId().equals(currentUser.getId());

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
                            // Статус рисуем только у своих сообщений (у входящих — null).
                            .status(own
                                    ? MessageStatusService.statusFromWatermarks(m.getId(), watermarks).name()
                                    : null)
                            .editedAt(m.getEditedAt())
                            .deleted(m.isDeleted())
                            .build();
                });
    }

    /** Редактирование: только автор, только текстовое сообщение без вложений. */
    @Transactional
    public void editMessage(UUID messageUuid, String content, UserEntity actor) {
        MessageEntity msg = messageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!msg.getSender().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can edit only your own messages");
        }
        if (msg.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit a deleted message");
        }
        if (msg.getType() != MessageType.TEXT || !msg.getAttachments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only text messages can be edited");
        }
        msg.setContent(content);
        msg.setEditedAt(Instant.now());
        messageRepository.save(msg);
        broadcastMutation(msg, WsEventType.MESSAGE_EDITED);
    }

    /** Удаление (мягкое): автор всегда; ADMIN чата — чужие в своей группе. Файлы вложений удаляются с диска. */
    @Transactional
    public void deleteMessage(UUID messageUuid, UserEntity actor) {
        MessageEntity msg = messageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        ChatEntity chat = msg.getChat();
        chatService.checkAccess(chat, actor);

        boolean isAuthor = msg.getSender().getId().equals(actor.getId());
        boolean isChatAdmin = chatParticipantRepository.findByChatAndUser(chat, actor)
                .map(p -> p.getRole() == UserChatRole.ADMIN)
                .orElse(false);
        if (!isAuthor && !isChatAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can delete only your own messages");
        }
        if (msg.isDeleted()) {
            return; // идемпотентно
        }
        // Сообщение удалено — файлы вложений больше не нужны, убираем с диска.
        for (AttachmentEntity a : msg.getAttachments()) {
            if (!a.isFileDeleted()) {
                fileStorageService.deleteChatFile(a.getStoredName());
                a.setFileDeleted(true);
            }
        }
        msg.setDeleted(true);
        msg.setContent("");
        messageRepository.save(msg);
        broadcastMutation(msg, WsEventType.MESSAGE_DELETED);
    }

    private void broadcastMutation(MessageEntity msg, WsEventType type) {
        MessageMutationPayload payload = new MessageMutationPayload(
                msg.getChat().getUuid(), msg.getUuid(), msg.getContent(), msg.getEditedAt(), msg.isDeleted());
        for (ChatParticipantEntity p : chatParticipantRepository.findByChat(msg.getChat())) {
            eventPublisher.toUser(p.getUser().getLogin(), type, payload);
        }
    }

    /** Отметить прочтение вплоть до messageUuid (для POST /api/chats/{uuid}/read). */
    @Transactional
    public void markRead(UUID chatUuid, UUID messageUuid, UserEntity currentUser) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        chatService.checkAccess(chat, currentUser);
        MessageEntity message = messageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Message not found"));
        if (!message.getChat().getId().equals(chat.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Message does not belong to this chat");
        }
        messageStatusService.markRead(chat, currentUser, message);
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

            // Новый формат — тот же контент в конверте. /queue/messages уберём,
            // когда мобильный клиент переедет на /queue/events.
            eventPublisher.toUser(participant.getUser().getLogin(), WsEventType.MESSAGE_NEW, response);

            // Push — всегда, КРОМЕ случая, когда получатель прямо сейчас смотрит
            // именно этот чат (там сообщение и так видно по WebSocket) или
            // заглушил этот чат. Раньше гейтом было "получатель вообще онлайн" —
            // из-за этого, пока приложение открыто хоть на списке чатов, push не
            // приходил вообще ни по одному чату. Теперь по всем чатам, кроме
            // открытого на экране прямо сейчас.
            UserEntity recipient = participant.getUser();
            boolean isSelf = recipient.getId().equals(sender.getId());
            boolean viewingThisChat = activeChatService.isViewing(recipient.getLogin(), saved.getChat().getUuid());
            boolean shouldPush = !isSelf && !viewingThisChat && !participant.isMuted();
            log.info("push decision for {}: self={} viewingThisChat={} muted={} -> send={}",
                    recipient.getLogin(), isSelf, viewingThisChat, participant.isMuted(), shouldPush);
            if (shouldPush) {
                                String body = recipient.isHidePushPreviews()
                        ? "Новое сообщение"
                        : (saved.getContent() != null && !saved.getContent().isBlank())
                                ? saved.getContent() : "Вложение";
                pushService.pushToUser(recipient, sender.getDisplayName(), body, resolveAvatarUrl(sender), Map.of(
                        "type", "MESSAGE_NEW",
                        "chatUuid", saved.getChat().getUuid().toString(),
                        "messageUuid", saved.getUuid().toString()));
            }
        }
    }

    /** null, если аватара нет или публичный адрес сервера не настроен (messenger.public-url). */
    private String resolveAvatarUrl(UserEntity user) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || publicUrl.isBlank()) {
            return null;
        }
        return publicUrl + avatarUrl;
    }

    private AttachmentDto toAttachmentDto(AttachmentEntity attachment) {
        return AttachmentDto.builder()
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .fileDeleted(attachment.isFileDeleted())
                .build();
    }

    private static String extractStoredName(String fileUrl) {
        int slash = fileUrl.lastIndexOf('/');
        return slash >= 0 ? fileUrl.substring(slash + 1) : fileUrl;
    }
}