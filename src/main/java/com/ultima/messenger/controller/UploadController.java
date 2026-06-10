package com.ultima.messenger.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.ultima.messenger.model.entity.UserEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Path AVATAR_DIR = Paths.get("storage", "avatars").toAbsolutePath().normalize();

    @PostMapping(value = "/avatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserEntity currentUser,
            @RequestParam("file") MultipartFile file) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            Files.createDirectories(AVATAR_DIR);

            String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image";
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                ext = original.substring(dot);
            }

            String fileName = UUID.randomUUID() + ext;
            Path target = AVATAR_DIR.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = "/api/uploads/avatars/" + fileName;
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }
    }

    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        Path target = AVATAR_DIR.resolve(fileName).toAbsolutePath().normalize();
        if (!Files.exists(target) || !target.startsWith(AVATAR_DIR)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        Resource resource = new FileSystemResource(target);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
