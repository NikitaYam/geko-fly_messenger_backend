package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.chat.ChatResponse;
import com.geckofly.messenger.model.dto.chat.CreateChatRequest;
import com.geckofly.messenger.model.dto.chat.CreateChatResponse;
import com.geckofly.messenger.model.dto.user.ParticipantResponse;
import com.geckofly.messenger.model.entity.*;
import com.geckofly.messenger.model.enums.ChatType;
import com.geckofly.messenger.model.enums.UserChatRole;
import com.geckofly.messenger.repository.*;
import com.geckofly.messenger.websocket.dto.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.geckofly.messenger.model.dto.user.UserSummary;
import com.geckofly.messenger.mapper.UserMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;
    private final PresenceService presenceService;
    private final MessageStatusService messageStatusService;

    /** A8: верхний предел участников группы. */
    public static final int MAX_GROUP_PARTICIPANTS = 200;

    public CreateChatResponse createChat(CreateChatRequest request, UserEntity currentUser) {
        List<String> participantLogins = request.getParticipantLogins();

        Set<String> uniqueLogins = new HashSet<>(participantLogins);
        if (uniqueLogins.size() != participantLogins.size()) {
            throw new IllegalArgumentException("Duplicate participants are not allowed");
        }

        if (uniqueLogins.size() == 1 && uniqueLogins.contains(currentUser.getLogin())) {
            throw new IllegalArgumentException("Cannot create a chat with yourself");
        }

        List<UserEntity> participants = userRepository.findAllByLoginIn(participantLogins);
        if (participants.size() != participantLogins.size()) {
            List<String> foundLogins = participants.stream().map(UserEntity::getLogin).toList();
            List<String> notFound = participantLogins.stream()
                    .filter(l -> !foundLogins.contains(l)).toList();
            throw new IllegalArgumentException("Users not found: " + notFound);
        }

        if (request.getType() == ChatType.GROUP
                && (request.getTitle() == null || request.getTitle().isBlank())) {
            throw new IllegalArgumentException("Group chat requires a title");
        }

        if (request.getType() == ChatType.GROUP && participants.size() > MAX_GROUP_PARTICIPANTS) {
            throw new IllegalArgumentException("Group is too large (max " + MAX_GROUP_PARTICIPANTS + ")");
        }

        if (request.getType() == ChatType.PRIVATE) {
            List<UserEntity> others = participants.stream()
                    .filter(p -> !p.getLogin().equals(currentUser.getLogin()))
                    .toList();

            if (others.size() != 1) {
                throw new IllegalArgumentException("Private chat must have exactly one other participant");
            }

            UserEntity otherUser = others.getFirst();

            if (chatParticipantRepository.existsChatBetweenUsersWithType(
                    currentUser, otherUser, ChatType.PRIVATE)) {
                throw new IllegalArgumentException(
                        "Private chat with user '" + otherUser.getLogin() + "' already exists");
            }
        }

        ChatEntity chat = new ChatEntity();
        chat.setType(request.getType());
        chat.setTitle(request.getTitle());
        chatRepository.save(chat);

        saveParticipant(chat, currentUser, UserChatRole.ADMIN);

        for (UserEntity participant : participants) {
            if (!participant.getLogin().equals(currentUser.getLogin())) {
                saveParticipant(chat, participant, UserChatRole.MEMBER);
            }
        }

        // П4: real-time «вас добавили в чат» — всем участникам, кроме создателя.
        // Payload строим под каждого: в приватном чате otherParticipant у каждого свой.
        for (UserEntity participant : participants) {
            if (!participant.getLogin().equals(currentUser.getLogin())) {
                eventPublisher.toUser(participant.getLogin(),
                        WsEventType.CHAT_CREATED, buildChatResponse(chat, participant));
            }
        }

        return CreateChatResponse.builder()
                .message("Chat created successfully")
                .uuid(chat.getUuid())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getChats(UserEntity currentUser) {
        return chatParticipantRepository.findByUser(currentUser).stream()
                .map(p -> buildChatResponse(p.getChat(), currentUser))
                .collect(Collectors.toList());
    }

    /** ChatResponse с точки зрения конкретного зрителя (otherParticipant в приватном чате — «второй» для него). */
    ChatResponse buildChatResponse(ChatEntity chat, UserEntity viewer) {
        Optional<MessageEntity> lastMsg =
                messageRepository.findTopByChatOrderByCreatedAtDesc(chat);

        String lastMessagePreview = lastMsg.map(msg -> {
            String content = msg.getContent();
            if (content != null && !content.isBlank()) {
                return content;
            }
            if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
                return "Вложение";
            }
            return null;
        }).orElse(null);

        UserEntity lastSender = lastMsg.map(MessageEntity::getSender).orElse(null);
        String lastSenderDisplay = lastSender != null ? lastSender.getDisplayName() : null;

        UserSummary otherParticipant = chat.getType() == ChatType.PRIVATE
                ? findOtherParticipant(chat, viewer)
                : null;

        boolean muted = chatParticipantRepository.findByChatAndUser(chat, viewer)
                .map(ChatParticipantEntity::isMuted)
                .orElse(false);

        return ChatResponse.builder()
                .uuid(chat.getUuid())
                .type(chat.getType().name())
                .title(chat.getTitle())
                .avatarUrl(chat.getAvatarUrl())
                .lastMessage(lastMessagePreview)
                .lastMessageTime(lastMsg.map(MessageEntity::getCreatedAt).orElse(null))
                .lastSenderDisplayName(lastSenderDisplay)
                .lastSenderAvatarUrl(null)
                .otherParticipant(otherParticipant)
                .unreadCount(messageStatusService.unreadCount(chat, viewer))
                .muted(muted)
                .build();
    }

    private UserSummary findOtherParticipant(ChatEntity chat, UserEntity currentUser) {
        return chatParticipantRepository.findByChat(chat).stream()
                .map(ChatParticipantEntity::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst()
                .map(u -> {
                    UserSummary s = userMapper.toSummary(u);
                    s.setOnline(presenceService.isOnline(u.getLogin()));
                    return s;
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(UUID chatUuid, UserEntity currentUser) {
        ChatEntity chat = getChatByUuidOrThrow(chatUuid);
        checkAccess(chat, currentUser);

        return chatParticipantRepository.findByChat(chat).stream()
                .map(p -> {
                    ParticipantResponse r = userMapper.toParticipantResponse(p.getUser());
                    r.setOnline(presenceService.isOnline(p.getUser().getLogin()));
                    r.setChatRole(p.getRole().name());
                    r.setDeleted(p.getUser().isDeleted());
                    return r;
                })
                .collect(Collectors.toList());
    }

    public ChatEntity getChatByUuidOrThrow(UUID chatUuid) {
        return chatRepository.findByUuid(chatUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found: " + chatUuid));
    }

    void checkAccess(ChatEntity chat, UserEntity user) {
        if (!chatParticipantRepository.existsByChatAndUser(chat, user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }
    }

    private void saveParticipant(ChatEntity chat, UserEntity user, UserChatRole role) {
        ChatParticipantEntity cp = new ChatParticipantEntity();
        cp.setChat(chat);
        cp.setUser(user);
        cp.setRole(role);
        cp.setJoinedAt(Instant.now());
        chatParticipantRepository.save(cp);
    }
}
