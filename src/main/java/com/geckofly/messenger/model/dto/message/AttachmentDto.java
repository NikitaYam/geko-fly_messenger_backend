package com.geckofly.messenger.model.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Boolean fileDeleted;
}
