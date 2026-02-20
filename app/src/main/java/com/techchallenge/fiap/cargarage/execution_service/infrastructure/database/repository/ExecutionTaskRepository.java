package com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.entity.ExecutionTaskEntity;

/**
 * JPA repository for Execution Task entities.
 */
@Repository
public interface ExecutionTaskRepository
        extends JpaRepository<ExecutionTaskEntity, Long> {

    Optional<ExecutionTaskEntity> findByServiceOrderId(Long serviceOrderId);

    Page<ExecutionTaskEntity> findByStatus(String status, Pageable pageable);
}
