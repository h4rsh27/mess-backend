package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeedbackDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID menuItemId;
    private String menuItemName;
    private UUID messId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
