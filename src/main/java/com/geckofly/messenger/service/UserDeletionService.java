package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.ChatParticipantEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.ChatParticipantRepository;
import com.geckofly.messenger.repository.ChatRepository;
import com.geckofly.messenger.repository.MessageRepository;
import com.geckofly.messenger.repository.RefreshTokenRepository;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteUserData(UserEntity user) {
        refreshTokenRepository.deleteByUser(user);

        messageRepository.deleteAll(messageRepository.findBySender(user));

        List<ChatParticipantEntity> memberships = chatParticipantRepository.findByUser(user);
        Set<ChatEntity> affectedChats = new HashSet<>();
        for (ChatParticipantEntity membership : memberships) {
            affectedChats.add(membership.getChat());
        }
        chatParticipantRepository.deleteAll(memberships);

        for (ChatEntity chat : affectedChats) {
            if (chatParticipantRepository.findByChat(chat).isEmpty()) {
                messageRepository.deleteAll(messageRepository.findByChat(chat));
                chatRepository.delete(chat);
            }
        }

        userRepository.delete(user);
    }
}
