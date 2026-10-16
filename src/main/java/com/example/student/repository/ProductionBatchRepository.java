package com.example.student.repository;

import com.example.student.model.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
}