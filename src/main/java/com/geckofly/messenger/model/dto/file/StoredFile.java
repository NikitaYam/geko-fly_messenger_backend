package com.geckofly.messenger.model.dto.file;

/**
 * Результат сохранения файла в хранилище.
 * storedName — имя на диске (UUID.ext), нужно для скачивания и очистки в R4.
 * url — публичный путь для клиента.
 */
public record StoredFile(
        String fileName,
        String storedName,
        String url,
        String contentType,
        long size
) {}
