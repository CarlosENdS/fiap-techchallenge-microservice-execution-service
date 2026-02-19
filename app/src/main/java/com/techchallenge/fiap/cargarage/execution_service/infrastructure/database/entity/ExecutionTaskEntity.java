package com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for Execution Task.
 */
@Data
@Entity
@Builder
@Table(name = "execution_task")
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "service_order_id", nullable = false)
    private Long serviceOrderId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_license_plate")
    private String vehicleLicensePlate;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "assigned_technician")
    private String assignedTechnician;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
