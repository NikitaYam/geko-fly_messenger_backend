package com.geckofly.messenger.service;

import com.geckofly.messenger.config.StorageProperties;
import com.geckofly.messenger.model.entity.AttachmentEntity;
import com.geckofly.messenger.model.entity.ChatParticipantEntity;
import com.geckofly.messenger.repository.AttachmentDeliveryRepository;
import com.geckofly.messenger.repository.AttachmentRepository;
import com.geckofly.messenger.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Периодическая очистка файлов (relay). Три пути:
 *  1. все получатели скачали → файл больше не нужен;
 *  2. вышел срок хранения (получатель мог уйти навсегда);
 *  3. аварийно — диск заканчивается, удаляем самые старые независимо от доставки.
 * Текст сообщений НЕ трогаем — он остаётся в Postgres, удаляется только файл с диска.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageSweepService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentDeliveryRepository deliveryRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties props;

    // Интервал настраивается messenger.storage.sweep-interval (ISO-8601 duration),
    // по умолчанию раз в час. Отсчёт от конца прошлого прогона — не наслаивается.
    @Scheduled(fixedDelayString = "${messenger.storage.sweep-interval:PT1H}")
    @Transactional
    public void sweep() {
        purgeDeliveredAndExpired();
        emergencyPurgeIfLow();
    }

    private void purgeDeliveredAndExpired() {
        Instant cutoff = Instant.now().minus(props.getRetentionDays(), ChronoUnit.DAYS);
        for (AttachmentEntity att : attachmentRepository.findByFileDeletedFalse()) {
            boolean expired = att.getCreatedAt() != null && att.getCreatedAt().isBefore(cutoff);
            if (expired) {
                purge(att, "expired");
            } else if (isDeliveredToAllRecipients(att)) {
                purge(att, "all delivered");
            }
        }
    }

    private void emergencyPurgeIfLow() {
        long reserve = props.getEmergencyFreeSpace().toBytes();
        if (fileStorageService.usableBytes() >= reserve) {
            return;
        }
        log.warn("Emergency purge: free space below {} bytes", reserve);
        for (AttachmentEntity att : attachmentRepository.findByFileDeletedFalseOrderByCreatedAtAsc()) {
            if (fileStorageService.usableBytes() >= reserve) {
                break;
            }
            purge(att, "emergency low space");
        }
    }

    private boolean isDeliveredToAllRecipients(AttachmentEntity att) {
        long senderId = att.getMessage().getSender().getId();
        List<ChatParticipantEntity> participants =
                chatParticipantRepository.findByChat(att.getMessage().getChat());
        long recipients = participants.stream()
                .filter(p -> !p.getUser().getId().equals(senderId))
                .count();
        if (recipients == 0) {
            return true;   // получателей нет (все вышли) — файл можно удалять
        }
        return deliveryRepository.countByAttachment(att) >= recipients;
    }

    private void purge(AttachmentEntity att, String reason) {
        fileStorageService.deleteChatFile(att.getStoredName());
        att.setFileDeleted(true);
        attachmentRepository.save(att);
        log.info("Purged file {} ({})", att.getStoredName(), reason);
    }
}