package com.geckofly.messenger.service;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Белый список типов файлов и правила их отдачи.
 *
 * Живёт в коде, а не в StorageProperties, намеренно: это доменная логика
 * (какое расширение к какой категории, что рендерить inline, какой MIME отдать),
 * а не тумблеры конфигурации. Пихать ext→mime карту в yml — вредить читаемости.
 *
 * SVG в списке НЕТ намеренно: он исполняет скрипты в браузере (stored-XSS).
 */
public final class FileTypeRules {

    private FileTypeRules() {}

    private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "gif", "webp", "heic");
    private static final Set<String> VIDEO = Set.of("mp4", "mov", "webm", "mkv");
    private static final Set<String> AUDIO = Set.of("mp3", "m4a", "aac", "ogg", "wav", "opus");
    private static final Set<String> DOCUMENT =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");
    private static final Set<String> ARCHIVE = Set.of("zip", "rar", "7z", "tar", "gz");

    /** Всё, что разрешено как вложение в чат. */
    private static final Set<String> ALLOWED = union(IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE);

    /** Только эти категории отдаём inline (браузер показывает). Остальное — attachment (скачивание). */
    private static final Set<String> INLINE = union(IMAGE, VIDEO);

    /** MIME для типов, где важна точность (image/* для isImage на клиенте). Остальное — через MediaTypeFactory. */
    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"), Map.entry("heic", "image/heic"),
            Map.entry("mp4", "video/mp4"), Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"), Map.entry("mkv", "video/x-matroska"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("m4a", "audio/mp4"),
            Map.entry("aac", "audio/aac"), Map.entry("ogg", "audio/ogg"),
            Map.entry("wav", "audio/wav"), Map.entry("opus", "audio/opus")
    );

    /** Расширение в нижнем регистре без точки; "" если его нет. */
    public static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedForChat(String ext) {
        return ALLOWED.contains(ext);
    }

    public static boolean isImage(String ext) {
        return IMAGE.contains(ext);
    }

    public static boolean isInline(String ext) {
        return INLINE.contains(ext);
    }

    /** MIME по расширению: сперва наша карта, затем Spring, в конце — октет-стрим. */
    public static MediaType contentType(String filename) {
        String mime = MIME.get(extension(filename));
        if (mime != null) {
            return MediaType.parseMediaType(mime);
        }
        return MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> result = new HashSet<>();
        for (Set<String> set : sets) {
            result.addAll(set);
        }
        return Set.copyOf(result);
    }
}
