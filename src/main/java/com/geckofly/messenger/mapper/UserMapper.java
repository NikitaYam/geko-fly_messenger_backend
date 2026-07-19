package com.geckofly.messenger.mapper;

import com.geckofly.messenger.model.dto.user.ParticipantResponse;
import com.geckofly.messenger.model.dto.user.UserResponse;
import com.geckofly.messenger.model.dto.user.UserSummary;
import com.geckofly.messenger.model.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserSummary toSummary(UserEntity user) {
        return UserSummary.builder()
            .uuid(user.getUuid())
            .login(user.getLogin())
            .displayName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }

    public UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
            .uuid(user.getUuid())
            .login(user.getLogin())
            .displayName(user.getDisplayName())
            .email(user.getUserEmail())
            .role(user.getRole())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }

    public ParticipantResponse toParticipantResponse(UserEntity user){
        return ParticipantResponse.builder()
            .uuid(user.getUuid())
            .login(user.getLogin())
            .displayName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .email(user.getUserEmail())
            .build();
    }
}
