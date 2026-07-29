package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.ChatParticipantEntity;
import com.geckofly.messenger.model.entity.ChatReadStateEntity;
import com.geckofly.messenger.model.entity.MessageEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.model.enums.MessageStatus;
import com.geckofly.messenger.repository.ChatParticipantRepository;
import com.geckofly.messenger.repository.ChatReadStateRepository;
import com.geckofly.messenger.repository.MessageRepository;
import com.geckofly.messenger.websocket.dto.MessageStatusPayload;
import com.geckofly.messenger.websocket.dto.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Статусы сообщений «доставлено/прочитано» через вотермарки на пару (чат, пользователь).
 * Вотермарка — внутренний id сообщения (монотонен = хронология): всё с id <= вотермарки
 * считается доставленным/прочитанным этим пользователем.
 */
@Service
@RequiredArgsConstructor
public class MessageStatusService {

    private final ChatReadStateRepository readStateRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;

    /** Пользователь получил чат целиком (загрузил историю) — двигаем delivered до последнего сообщения. */
    @Transactional
    public void markDeliveredUpToLatest(ChatEntity chat, UserEntity user) {
        MessageEntity newest = messageRepository.findTopByChatOrderByCreatedAtDesc(chat).orElse(null);
        if (newest == null) {
            return;
        }
        ChatReadStateEntity state = stateFor(chat, user);
        if (state.getLastDeliveredMessageId() == null || state.getLastDeliveredMessageId() < newest.getId()) {
            state.setLastDeliveredMessageId(newest.getId());
            readStateRepository.save(state);
            notifyOthers(chat, user, newest, "DELIVERED");
        }
    }

    /** Пользователь прочитал вплоть до сообщения upto (read подразумевает и delivered). */
    @Transactional
    public void markRead(ChatEntity chat, UserEntity user, MessageEntity upto) {
        ChatReadStateEntity state = stateFor(chat, user);
        boolean changed = false;
        if (state.getLastReadMessageId() == null || state.getLastReadMessageId() < upto.getId()) {
            state.setLastReadMessageId(upto.getId());
            changed = true;
        }
        if (state.getLastDeliveredMessageId() == null || state.getLastDeliveredMessageId() < upto.getId()) {
            state.setLastDeliveredMessageId(upto.getId());
            changed = true;
        }
        if (changed) {
            readStateRepository.save(state);
            notifyOthers(chat, user, upto, "READ");
        }
    }

    /** Непрочитанные для viewer: сообщения новее его last_read и не его собственные. */
    @Transactional(readOnly = true)
    public long unreadCount(ChatEntity chat, UserEntity viewer) {
        long afterId = readStateRepository.findByChatAndUser(chat, viewer)
                .map(ChatReadStateEntity::getLastReadMessageId)
                .filter(Objects::nonNull)
                .orElse(0L);
        return messageRepository.countUnread(chat, afterId, viewer);
    }

    /**
     * {minDelivered, minRead} — минимальные вотермарки по всем получателям (кроме viewer).
     * Считается один раз на страницу сообщений; 0, если у кого-то из получателей вотермарки нет.
     */
    @Transactional(readOnly = true)
    public long[] minWatermarks(ChatEntity chat, UserEntity viewer) {
        // A1: удалённые (тумбстоуны) — не получатели; иначе их нулевая вотермарка
        // навечно держала бы статус на SENT.
        List<Long> recipientIds = chatParticipantRepository.findByChat(chat).stream()
                .map(ChatParticipantEntity::getUser)
                .filter(u -> !u.getId().equals(viewer.getId()) && !u.isDeleted())
                .map(UserEntity::getId)
                .toList();
        if (recipientIds.isEmpty()) {
            return new long[]{Long.MAX_VALUE, Long.MAX_VALUE};
        }
        Map<Long, ChatReadStateEntity> byUser = readStateRepository.findByChat(chat).stream()
                .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s, (a, b) -> a));
        long minDelivered = Long.MAX_VALUE;
        long minRead = Long.MAX_VALUE;
        for (Long rid : recipientIds) {
            ChatReadStateEntity s = byUser.get(rid);
            long d = (s != null && s.getLastDeliveredMessageId() != null) ? s.getLastDeliveredMessageId() : 0L;
            long r = (s != null && s.getLastReadMessageId() != null) ? s.getLastReadMessageId() : 0L;
            minDelivered = Math.min(minDelivered, d);
            minRead = Math.min(minRead, r);
        }
        return new long[]{minDelivered, minRead};
    }

    /** Чистая функция: статус сообщения по заранее посчитанным вотермаркам. */
    public static MessageStatus statusFromWatermarks(long messageId, long[] wm) {
        if (messageId <= wm[1]) {
            return MessageStatus.READ;
        }
        if (messageId <= wm[0]) {
            return MessageStatus.DELIVERED;
        }
        return MessageStatus.SENT;
    }

    private ChatReadStateEntity stateFor(ChatEntity chat, UserEntity user) {
        return readStateRepository.findByChatAndUser(chat, user).orElseGet(() -> {
            ChatReadStateEntity s = new ChatReadStateEntity();
            s.setChat(chat);
            s.setUser(user);
            return s;
        });
    }

    private void notifyOthers(ChatEntity chat, UserEntity mover, MessageEntity upto, String status) {
        List<String> logins = chatParticipantRepository.findByChat(chat).stream()
                .map(p -> p.getUser().getLogin())
                .filter(l -> !l.equals(mover.getLogin()))
                .toList();
        if (!logins.isEmpty()) {
            eventPublisher.toUsers(logins, WsEventType.MESSAGE_STATUS,
                    new MessageStatusPayload(chat.getUuid(), mover.getUuid(), upto.getUuid(), status));
        }
    }
}
