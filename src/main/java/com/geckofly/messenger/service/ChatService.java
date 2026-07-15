package com.geckofly.messenger.service;

import com.geckofly.messenger.model.dto.chat.ChatResponse;
import com.geckofly.messenger.model.dto.chat.CreateChatRequest;
import com.geckofly.messenger.model.dto.chat.CreateChatResponse;
import com.geckofly.messenger.model.dto.user.PartisipantResponse;
import com.geckofly.messenger.model.entity.*;
import com.geckofly.messenger.model.enums.ChatType;
import com.geckofly.messenger.model.enums.UserChatRole;
import com.geckofly.messenger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.geckofly.messenger.model.dto.user.UserSummary;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

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

        return CreateChatResponse.builder()
                .message("Chat created successfully")
                .uuid(chat.getUuid())
                .build();
    }

        @Transactional(readOnly = true)
    public List<ChatResponse> getChats(UserEntity currentUser) {

        List<ChatParticipantEntity> participations =
                chatParticipantRepository.findByUser(currentUser);

        return participations.stream()
                .map(p -> {

                    ChatEntity chat = p.getChat();

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
                            ? findOtherParticipant(chat, currentUser)
                            : null;

                    return ChatResponse.builder()
                            .uuid(chat.getUuid())
                            .type(chat.getType().name())
                            .title(chat.getTitle())
                            .lastMessage(lastMessagePreview)
                            .lastMessageTime(lastMsg.map(MessageEntity::getCreatedAt).orElse(null))
                            .lastSenderDisplayName(lastSenderDisplay)
                            .lastSenderAvatarUrl(null)
                            .otherParticipant(otherParticipant)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private UserSummary findOtherParticipant(ChatEntity chat, UserEntity currentUser) {
        return chatParticipantRepository.findByChat(chat).stream()
                .map(ChatParticipantEntity::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst()
                .map(u -> UserSummary.builder()
                        .uuid(u.getUuid())
                        .login(u.getLogin())
                        .displayName(u.getDisplayName())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PartisipantResponse> getParticipants(UUID chatUuid, UserEntity currentUser) {

        ChatEntity chat = getChatByUuidOrThrow(chatUuid);
        checkAccess(chat, currentUser);

        return chatParticipantRepository.findByChat(chat).stream()
                .map(p -> {

                    UserEntity user = p.getUser();

                    return PartisipantResponse.builder()
                            .uuid(user.getUuid())
                            .login(user.getLogin())
                            .displayName(user.getDisplayName())
                            .avatarUrl(null)
                            .email(user.getUserEmail())
                            .build();
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
        cp.setJoinedAt(LocalDateTime.now());
        chatParticipantRepository.save(cp);
    }
}