package com.ultima.messenger.model.dto.chat;

import com.ultima.messenger.model.enums.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateChatRequest {

    @NotEmpty(message = "Participant list cannot be empty")
    private List<String> participantLogins;

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @NotNull(message = "Chat type is required")
    private ChatType type;
}
