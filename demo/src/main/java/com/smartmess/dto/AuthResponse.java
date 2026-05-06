package com.smartmess.dto;

import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.PlanType;
import com.smartmess.enums.Role;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private PlanType planType;
    private com.smartmess.enums.SubscriptionPreference subscriptionPreference;
    private UUID messId;
    private ApprovalStatus approvalStatus;
}
