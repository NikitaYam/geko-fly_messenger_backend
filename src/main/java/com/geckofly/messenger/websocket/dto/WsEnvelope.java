package com.geckofly.messenger.websocket.dto;

/**
 * Универсальный конверт для событий по WebSocket.
 * event — имя типа (WsEventType), payload — полезная нагрузка под этот тип.
 * Клиент разбирает по полю event и приводит payload к нужной модели.
 */
public record WsEnvelope(String event, Object payload) {

    public static WsEnvelope of(WsEventType type, Object payload) {
        return new WsEnvelope(type.name(), payload);
    }
}
