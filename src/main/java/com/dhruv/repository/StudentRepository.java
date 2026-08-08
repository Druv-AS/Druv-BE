package com.dhruv.repository;

import com.dhruv.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, UUID> {
    Optional<StudentEntity> findByPhoneNumber(String phoneNumber);
    Optional<StudentEntity> findByUserId(String userId);
    java.util.List<StudentEntity> findByParentPhoneNumber(String parentPhoneNumber);
}
