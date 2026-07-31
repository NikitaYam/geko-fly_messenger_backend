package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.chat.ChatResponse;
import com.geckofly.messenger.model.dto.chat.UpdateChatRequest;
import com.geckofly.messenger.model.entity.*;
import com.geckofly.messenger.model.enums.ChatType;
import com.geckofly.messenger.model.enums.UserChatRole;
import com.geckofly.messenger.repository.*;
import com.geckofly.messenger.util.UploadPaths;
import com.geckofly.messenger.websocket.dto.MembershipPayload;
import com.geckofly.messenger.websocket.dto.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Управление участниками группового чата: добавить/удалить/выйти/переименовать/сменить роль.
 * Приватные чаты не управляются. Роли внутри чата (ADMIN/MEMBER) наконец получают смысл.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMembershipService {

    private final ChatParticipantRepository participantRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ChatReadStateRepository readStateRepository;
    private final AttachmentDeliveryRepository attachmentDeliveryRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    public void addParticipant(UUID chatUuid, String login, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        requireGroup(chat);
        requireChatAdmin(chat, actor);

        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + login));
        if (participantRepository.findByChatAndUser(chat, user).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a participant");
        }
        // A8: предел размера группы.
        if (participantRepository.findByChat(chat).size() >= ChatService.MAX_GROUP_PARTICIPANTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is full");
        }

        saveParticipant(chat, user, UserChatRole.MEMBER);

        // Новичку — CHAT_CREATED (чат появляется в его списке), остальным — PARTICIPANT_ADDED.
        eventPublisher.toUser(user.getLogin(), WsEventType.CHAT_CREATED, chatService.buildChatResponse(chat, user));
        notifyOthers(chat, user.getLogin(), WsEventType.PARTICIPANT_ADDED, payload(chat, user));
    }

    public void removeParticipant(UUID chatUuid, UUID userUuid, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        requireGroup(chat);
        requireChatAdmin(chat, actor);

        UserEntity target = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (target.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the leave endpoint to remove yourself");
        }
        ChatParticipantEntity cp = participantRepository.findByChatAndUser(chat, target)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant"));

        removeMember(chat, cp);
    }

    public void leave(UUID chatUuid, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        requireGroup(chat);
        ChatParticipantEntity cp = participantRepository.findByChatAndUser(chat, actor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant"));

        removeMember(chat, cp);
    }

    public ChatResponse updateChat(UUID chatUuid, UpdateChatRequest request, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        requireGroup(chat);
        requireChatAdmin(chat, actor);

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be empty");
            }
            chat.setTitle(title);
        }
        if (request.getAvatarUrl() != null) {
            String avatar = request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl().trim();
            // A2: аватар группы должен указывать на наш загруженный файл.
            if (avatar != null && !UploadPaths.isValidAvatarUrl(avatar)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar URL");
            }
            chat.setAvatarUrl(avatar);
        }
        chatRepository.save(chat);

        // Всем участникам — обновлённый чат (в группе otherParticipant=null, payload одинаков).
        for (ChatParticipantEntity p : participantRepository.findByChat(chat)) {
            eventPublisher.toUser(p.getUser().getLogin(),
                    WsEventType.CHAT_UPDATED, chatService.buildChatResponse(chat, p.getUser()));
        }
        return chatService.buildChatResponse(chat, actor);
    }

    public void changeRole(UUID chatUuid, UUID userUuid, UserChatRole role, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        requireGroup(chat);
        requireChatAdmin(chat, actor);

        UserEntity target = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ChatParticipantEntity cp = participantRepository.findByChatAndUser(chat, target)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant"));

        cp.setRole(role);
        participantRepository.save(cp);

        for (ChatParticipantEntity p : participantRepository.findByChat(chat)) {
            eventPublisher.toUser(p.getUser().getLogin(),
                    WsEventType.CHAT_UPDATED, chatService.buildChatResponse(chat, p.getUser()));
        }
    }

    // Личное заглушение уведомлений — доступно в любом чате (не только группа),
    // без требования роли ADMIN: это настройка самого себя, не управление чатом.
    public void setMute(UUID chatUuid, boolean muted, UserEntity actor) {
        ChatEntity chat = chatService.getChatByUuidOrThrow(chatUuid);
        ChatParticipantEntity cp = participantRepository.findByChatAndUser(chat, actor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant"));

        cp.setMuted(muted);
        participantRepository.save(cp);
    }

    // --- внутреннее ---

    private void removeMember(ChatEntity chat, ChatParticipantEntity cp) {
        UserEntity removed = cp.getUser();
        participantRepository.delete(cp);
        readStateRepository.deleteByChatAndUser(chat, removed);

        List<ChatParticipantEntity> remaining = participantRepository.findByChat(chat);
        if (remaining.isEmpty()) {
            deleteChatCompletely(chat);
        } else {
            // Гарантируем хотя бы одного ADMIN: если ушёл последний — назначаем старейшего.
            boolean anyAdmin = remaining.stream().anyMatch(p -> p.getRole() == UserChatRole.ADMIN);
            if (!anyAdmin) {
                ChatParticipantEntity oldest = remaining.stream()
                        .min(Comparator.comparing(ChatParticipantEntity::getJoinedAt))
                        .orElseThrow();
                oldest.setRole(UserChatRole.ADMIN);
                participantRepository.save(oldest);
            }
            notifyOthers(chat, removed.getLogin(), WsEventType.PARTICIPANT_REMOVED, payload(chat, removed));
        }
        // Ушедшему — тоже событие, чтобы клиент убрал чат из списка.
        eventPublisher.toUser(removed.getLogin(), WsEventType.PARTICIPANT_REMOVED, payload(chat, removed));
    }

    /** Полное удаление чата: файлы с диска, вложения, доставки, статусы, участники, сам чат. */
    private void deleteChatCompletely(ChatEntity chat) {
        List<MessageEntity> messages = messageRepository.findByChat(chat);
        for (MessageEntity m : messages) {
            for (AttachmentEntity a : m.getAttachments()) {
                if (!a.isFileDeleted()) {
                    fileStorageService.deleteChatFile(a.getStoredName());
                }
            }
        }
        attachmentDeliveryRepository.deleteByChat(chat);
        readStateRepository.deleteByChat(chat);
        messageRepository.deleteAll(messages);   // orphanRemoval удалит вложения
        participantRepository.deleteAll(participantRepository.findByChat(chat));
        chatRepository.delete(chat);
    }

    private void requireGroup(ChatEntity chat) {
        if (chat.getType() != ChatType.GROUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only group chats can be managed");
        }
    }

    private void requireChatAdmin(ChatEntity chat, UserEntity user) {
        ChatParticipantEntity cp = participantRepository.findByChatAndUser(chat, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant"));
        if (cp.getRole() != UserChatRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires chat admin role");
        }
    }

    private void saveParticipant(ChatEntity chat, UserEntity user, UserChatRole role) {
        ChatParticipantEntity cp = new ChatParticipantEntity();
        cp.setChat(chat);
        cp.setUser(user);
        cp.setRole(role);
        cp.setJoinedAt(Instant.now());
        participantRepository.save(cp);
    }

    private void notifyOthers(ChatEntity chat, String excludeLogin, WsEventType type, MembershipPayload payload) {
        for (ChatParticipantEntity p : participantRepository.findByChat(chat)) {
            if (!p.getUser().getLogin().equals(excludeLogin)) {
                eventPublisher.toUser(p.getUser().getLogin(), type, payload);
            }
        }
    }

    private MembershipPayload payload(ChatEntity chat, UserEntity user) {
        return new MembershipPayload(chat.getUuid(), user.getUuid(), user.getLogin(), user.getDisplayName());
    }
}
