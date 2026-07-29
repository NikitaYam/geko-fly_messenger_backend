package com.geckofly.messenger.service;

import com.geckofly.messenger.model.entity.AttachmentEntity;
import com.geckofly.messenger.model.entity.MessageEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import com.geckofly.messenger.repository.DeviceTokenRepository;
import com.geckofly.messenger.repository.MessageRepository;
import com.geckofly.messenger.repository.RefreshTokenRepository;
import com.geckofly.messenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Удаление пользователя = анонимизация (R10), а не каскадное стирание.
 * Сообщения и история чатов остаются целыми (в групповых чатах у друзей не появляется дыр),
 * но строка превращается в «тумбстоун»: имя «Удалённый пользователь», войти нельзя.
 * По-настоящему удаляются только личные данные: сессии, токены устройств, загруженные файлы.
 */
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void deleteUserData(UserEntity user) {
        // 1. Сессии и push-токены — обрубаем немедленно.
        refreshTokenRepository.deleteByUser(user);
        deviceTokenRepository.deleteByUser(user);

        // 2. Файлы, загруженные пользователем, — с диска (личные данные). Сами сообщения остаются.
        for (MessageEntity message : messageRepository.findBySender(user)) {
            for (AttachmentEntity attachment : message.getAttachments()) {
                if (!attachment.isFileDeleted()) {
                    fileStorageService.deleteChatFile(attachment.getStoredName());
                    attachment.setFileDeleted(true);
                }
            }
        }

        // 3. Анонимизация. Логин делаем уникальным «мёртвым», пароль — невалидным.
        user.setLogin("deleted_" + user.getUuid());
        user.setDisplayName("Удалённый пользователь");
        user.setUserEmail(null);
        user.setAvatarUrl(null);
        user.setPasswordHash("DELETED");
        user.setDeleted(true);
        userRepository.save(user);
    }
}
