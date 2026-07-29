package com.geckofly.messenger.websocket.dto;

import java.util.UUID;

/** Нагрузка событий PARTICIPANT_ADDED / PARTICIPANT_REMOVED. */
public record MembershipPayload(UUID chatUuid, UUID userUuid, String login, String displayName) {}
