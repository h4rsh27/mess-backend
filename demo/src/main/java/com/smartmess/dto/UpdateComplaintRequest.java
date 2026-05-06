package com.smartmess.dto;

import com.smartmess.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateComplaintRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    private String resolutionNote;
}
