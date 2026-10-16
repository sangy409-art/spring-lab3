package com.example.student.repository;

import com.example.student.model.BakingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BakingRequestRepository extends JpaRepository<BakingRequest, Long> {
}