package com.smartmess.repository;

import com.smartmess.entity.User;
import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByMessIdAndRole(UUID messId, Role role);

    List<User> findByMessIdAndApprovalStatus(UUID messId, ApprovalStatus approvalStatus);

    long countByMessIdAndRole(UUID messId, Role role);

    long countByMessIdAndRoleAndPlanType(UUID messId, Role role, com.smartmess.enums.PlanType planType);

    @Query("SELECT u FROM User u WHERE u.messId = :messId AND u.role = 'ROLE_STUDENT' AND u.approvalStatus = 'APPROVED'")
    List<User> findApprovedStudentsByMess(UUID messId);
}