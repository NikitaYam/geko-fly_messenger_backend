package com.geckofly.messenger.service;

import com.geckofly.messenger.config.StorageProperties;
import com.geckofly.messenger.model.dto.file.StoredFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Единственная точка работы с файловым хранилищем (правило 2: всё IO — вне контроллеров).
 * Сохранение, выдача, удаление, проверка свободного места. Аватары и вложения
 * лежат в разных папках с разными правилами (аватар — только изображение).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageProperties props;

    private Path filesDir;
    private Path avatarsDir;

    @PostConstruct
    void init() throws IOException {
        Path base = Paths.get(props.getBaseDir()).toAbsolutePath().normalize();
        filesDir = base.resolve("files");
        avatarsDir = base.resolve("avatars");
        // tmp — то же дерево, что multipart.location: один том → transferTo делает
        // атомарный rename, а не копирование 5 ГБ между томами.
        Files.createDirectories(filesDir);
        Files.createDirectories(avatarsDir);
        Files.createDirectories(base.resolve("tmp"));
    }

    /** Вложение в чат: разрешены все категории из белого списка. */
    public StoredFile storeChatFile(MultipartFile file) {
        String original = requireNonEmpty(file, "file");
        String ext = FileTypeRules.extension(original);
        if (!FileTypeRules.isAllowedForChat(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed: " + (ext.isEmpty() ? "(no extension)" : ext));
        }
        return save(file, original, ext, filesDir, "/api/files/");
    }

    /** Аватар: только изображение. */
    public StoredFile storeAvatar(MultipartFile file) {
        String original = requireNonEmpty(file, "image");
        String ext = FileTypeRules.extension(original);
        if (!FileTypeRules.isImage(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        return save(file, original, ext, avatarsDir, "/api/uploads/avatars/");
    }

    public Resource loadChatFile(String storedName) {
        return load(filesDir, storedName);
    }

    public Resource loadAvatar(String storedName) {
        return load(avatarsDir, storedName);
    }

    /** Физическое удаление вложения (используется sweep-джобой в R4). */
    public void deleteChatFile(String storedName) {
        try {
            Files.deleteIfExists(resolveSafely(filesDir, storedName));
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", storedName, e.getMessage());
        }
    }

    private StoredFile save(MultipartFile file, String original, String ext, Path dir, String urlPrefix) {
        ensureFreeSpace();
        String storedName = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(storedName);
        try {
            // transferTo(File), не (Path): сервлетный StandardMultipartFile делает
            // part.write() → rename в пределах тома вместо потокового копирования 5 ГБ.
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("Failed to store file at {}", target, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }
        return new StoredFile(original, storedName, urlPrefix + storedName,
                FileTypeRules.contentType(storedName).toString(), file.getSize());
    }

    private Resource load(Path dir, String storedName) {
        Path target = resolveSafely(dir, storedName);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(target);
    }

    /** Защита от path traversal: имя не должно выводить за пределы своей папки. */
    private Path resolveSafely(Path dir, String storedName) {
        Path target = dir.resolve(storedName).toAbsolutePath().normalize();
        if (!target.startsWith(dir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        return target;
    }

    private void ensureFreeSpace() {
        try {
            long usable = Files.getFileStore(filesDir).getUsableSpace();
            if (usable < props.getMinFreeSpace().toBytes()) {
                throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE,
                        "Not enough free space on server");
            }
        } catch (IOException e) {
            log.error("Cannot check free space", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage check failed");
        }
    }

    private static String requireNonEmpty(MultipartFile file, String fallbackName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : fallbackName;
    }
}
