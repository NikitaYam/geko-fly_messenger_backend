package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.AttachmentDeliveryEntity;
import com.geckofly.messenger.model.entity.AttachmentEntity;
import com.geckofly.messenger.model.entity.ChatEntity;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentDeliveryRepository extends JpaRepository<AttachmentDeliveryEntity, Long> {
    
    boolean existsByAttachmentAndUser(AttachmentEntity attachment, UserEntity user);

    long countByAttachment(AttachmentEntity attachment);

    /** Удалить все отметки доставки вложений чата (при полном удалении чата). */
    @Modifying
    @Query("DELETE FROM AttachmentDeliveryEntity d WHERE d.attachment.message.chat = :chat")
    void deleteByChat(@Param("chat") ChatEntity chat);
}
