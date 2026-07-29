package com.geckofly.messenger.model.enums;

/** Статус сообщения с точки зрения отправителя. */
public enum MessageStatus {
    SENT,       // сервер принял
    DELIVERED,  // дошло до устройств всех получателей
    READ        // прочитано всеми получателями
}
