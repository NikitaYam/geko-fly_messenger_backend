package com.geckofly.messenger.model.dto.message;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Builder
public class SendMessageResponse {
    
    private String message;
    private UUID messageUuid;
    private UUID chatUuid;
}
