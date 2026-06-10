package com.ultima.messenger.repository;

import com.ultima.messenger.model.entity.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    List<ChatEntity> findByTitle(String title);
}
