package com.dhruv.repository;

import com.dhruv.domain.ParentReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentReportRepository extends JpaRepository<ParentReportEntity, UUID> {
    List<ParentReportEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
