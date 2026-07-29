package com.geckofly.messenger.repository;

import com.geckofly.messenger.model.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {
    
    Optional<AttachmentEntity> findByStoredName(String storedName);

    List<AttachmentEntity> findByFileDeletedFalse();

    List<AttachmentEntity> findByFileDeletedFalseOrderByCreatedAtAsc();
}
