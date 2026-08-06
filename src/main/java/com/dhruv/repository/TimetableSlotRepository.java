package com.dhruv.repository;

import com.dhruv.domain.TimetableSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlotEntity, UUID> {
    List<TimetableSlotEntity> findByTimetableIdOrderByDisplayOrderAsc(UUID timetableId);
}
