package com.smartmess.repository;

import com.smartmess.entity.Complaint;
import com.smartmess.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Complaint> findByMessIdOrderByCreatedAtDesc(UUID messId);

    List<Complaint> findByMessIdAndStatus(UUID messId, ComplaintStatus status);

    long countByMessIdAndStatus(UUID messId, ComplaintStatus status);
}
