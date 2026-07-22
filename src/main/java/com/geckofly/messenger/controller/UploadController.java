package com.geckofly.messenger.controller;

import com.geckofly.messenger.model.dto.file.StoredFile;
import com.geckofly.messenger.service.FileStorageService;
import com.geckofly.messenger.service.FileTypeRules;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/avatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        StoredFile stored = fileStorageService.storeAvatar(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", stored.url()));
    }

    // Аватары остаются публичными осознанно (С6): маленькие, малочувствительные,
    // встречаются в UI повсюду — таскать токен в каждый Image.network дороже риска.
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadAvatar(fileName);
        return ResponseEntity.ok()
                .contentType(FileTypeRules.contentType(fileName))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
