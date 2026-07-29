package com.geckofly.messenger.util;

import java.util.regex.Pattern;

/**
 * Валидация URL загруженных файлов (A2 из AUDIT.md).
 * Клиент присылает URL аватара/вложения строкой — проверяем, что это путь к нашему
 * загруженному файлу (/api/uploads/avatars/... или /api/files/...), а не произвольная
 * строка (чужой путь, внешний адрес, мусор).
 */
public final class UploadPaths {

    private UploadPaths() {}

    // Имя файла на диске — UUID.ext; допускаем безопасный набор символов, без слэшей.
    private static final Pattern AVATAR_URL = Pattern.compile("^/api/uploads/avatars/[A-Za-z0-9._-]+$");
    private static final Pattern FILE_URL = Pattern.compile("^/api/files/[A-Za-z0-9._-]+$");

    public static boolean isValidAvatarUrl(String url) {
        return url != null && AVATAR_URL.matcher(url).matches();
    }

    public static boolean isValidFileUrl(String url) {
        return url != null && FILE_URL.matcher(url).matches();
    }
}
