package com.dhruv.repository;

import com.dhruv.domain.ParentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<ParentEntity, UUID> {
    Optional<ParentEntity> findByPhoneNumber(String phoneNumber);
    Optional<ParentEntity> findByUserId(String userId);
}
