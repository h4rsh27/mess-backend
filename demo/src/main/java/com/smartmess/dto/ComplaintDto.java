package com.smartmess.dto;

import com.smartmess.enums.ComplaintStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ComplaintDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID messId;
    private String title;
    private String description;
    private ComplaintStatus status;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
