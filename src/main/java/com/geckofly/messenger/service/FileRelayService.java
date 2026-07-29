package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.AttachmentDeliveryEntity;
import com.geckofly.messenger.model.entity.AttachmentEntity;
import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.AttachmentDeliveryRepository;
import com.geckofly.messenger.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

/**
 * Логика скачивания вложений в relay-модели:
 *  - удалённый файл → 410 Gone (клиент берёт из локального кэша);
 *  - доступ только участнику чата;
 *  - сам факт скачивания = доставка (учитывается для последующей очистки).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRelayService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentDeliveryRepository deliveryRepository;
    private final FileStorageService fileStorageService;
    private final ChatService chatService;

    @Transactional
    public Resource download(String storedName, UserEntity user) {
        Optional<AttachmentEntity> found = attachmentRepository.findByStoredName(storedName);

        if (found.isPresent()) {
            AttachmentEntity attachment = found.get();

            if (attachment.isFileDeleted()) {
                throw new ResponseStatusException(HttpStatus.GONE, "File deleted from server");
            }

            ChatEntity chat = attachment.getMessage().getChat();
            chatService.checkAccess(chat, user);   // 403, если не участник

            recordDelivery(attachment, user);
        }
        // Вложения нет в БД — это свежезагруженный, ещё не отправленный файл
        // (его качает сам загрузивший). Имя — случайный UUID, доступ уже под auth.

        return fileStorageService.loadChatFile(storedName);   // 404, если файла нет на диске
    }

    /** Отправитель уже имеет файл локально; доставку отмечаем только получателям, один раз. */
    private void recordDelivery(AttachmentEntity attachment, UserEntity user) {
        if (attachment.getMessage().getSender().getId().equals(user.getId())) {
            return;
        }
        if (deliveryRepository.existsByAttachmentAndUser(attachment, user)) {
            return;
        }
        AttachmentDeliveryEntity delivery = new AttachmentDeliveryEntity();
        delivery.setAttachment(attachment);
        delivery.setUser(user);
        delivery.setDeliveredAt(Instant.now());
        deliveryRepository.save(delivery);
        log.debug("Delivery recorded: attachment={} user={}", attachment.getStoredName(), user.getLogin());
    }
} 
