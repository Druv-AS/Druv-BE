package com.dhruv.repository;

import com.dhruv.domain.TimetableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, UUID> {
    List<TimetableEntity> findByStudentId(UUID studentId);
}
