package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.file.StoredFile;
import com.geckofly.messenger.service.FileStorageService;
import com.geckofly.messenger.service.FileTypeRules;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    // Загрузка требует аутентификации (SecurityConfig: /api/files/** уже не permitAll).
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
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
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadChatFile(fileName);
        MediaType mediaType = FileTypeRules.contentType(fileName);

        // Не-картинки/видео отдаём как attachment: браузер скачает, а не исполнит (С5).
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
