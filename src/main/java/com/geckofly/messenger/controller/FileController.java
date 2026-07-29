package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.file.StoredFile;
import com.geckofly.messenger.service.FileStorageService;
import com.geckofly.messenger.service.FileTypeRules;
import com.geckofly.messenger.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.service.FileRelayService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRelayService fileRelayService;
    private final RateLimitService rateLimitService;

    // Загрузка требует аутентификации (SecurityConfig: /api/files/** уже не permitAll).
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserEntity currentUser) {
        // A3: не более 20 загрузок в минуту на пользователя.
        rateLimitService.check("upload:" + currentUser.getLogin(), 20, Duration.ofMinutes(1));
        StoredFile stored = fileStorageService.storeChatFile(file);

        // Форма ответа сохранена как раньше — мобильный клиент завязан на эти поля.
        Map<String, Object> body = new HashMap<>();
        body.put("fileName", stored.fileName());
        body.put("fileUrl", stored.url());
        body.put("fileType", stored.contentType());
        body.put("fileSize", stored.size());
        return ResponseEntity.ok(body);
    }

    // С6: скачивание теперь авторизованное. Утёкшая ссылка не работает для чужих/анонимов.
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName,
                                            @AuthenticationPrincipal UserEntity currentUser) {
        Resource resource = fileRelayService.download(fileName, currentUser);
        MediaType mediaType = FileTypeRules.contentType(fileName);

        String disposition = FileTypeRules.isInline(FileTypeRules.extension(fileName))
                ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(resource);
    }
}
