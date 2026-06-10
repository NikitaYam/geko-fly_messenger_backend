package com.ultima.messenger.controller;

import com.ultima.messenger.model.entity.UserEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path FILES_DIR = Paths.get("storage", "files").toAbsolutePath().normalize();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @AuthenticationPrincipal UserEntity currentUser,
            @RequestParam("file") MultipartFile file) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            Files.createDirectories(FILES_DIR);

            String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                ext = original.substring(dot);
            }

            String storedName = UUID.randomUUID() + ext;
            Path target = FILES_DIR.resolve(storedName).toAbsolutePath().normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", original);
            response.put("fileUrl", "/api/files/" + storedName);
            response.put("fileType", file.getContentType());
            response.put("fileSize", file.getSize());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        Path target = FILES_DIR.resolve(fileName).toAbsolutePath().normalize();
        if (!Files.exists(target) || !target.startsWith(FILES_DIR)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        Resource resource = new FileSystemResource(target);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
