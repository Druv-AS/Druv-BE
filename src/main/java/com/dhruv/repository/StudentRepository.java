package com.dhruv.repository;

import com.dhruv.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, UUID> {

    Optional<StudentEntity> findByPhoneNumber(String phoneNumber);

    Optional<StudentEntity> findByUserId(String userId);

    /**
     * Matches any historical spelling of the parent's number in one query.
     * Rows written before phone canonicalisation may still hold {@code 9876543210} or
     * {@code 919876543210} rather than {@code +919876543210}.
     */
    List<StudentEntity> findByParentPhoneNumberIn(Collection<String> parentPhoneNumbers);
}
