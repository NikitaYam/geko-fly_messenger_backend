package com.geckofly.messenger.websocket.dto;

import java.util.UUID;

/** Нагрузка события PRESENCE: кто и в каком статусе. */
public record PresencePayload(UUID userUuid, String login, boolean online) {}
