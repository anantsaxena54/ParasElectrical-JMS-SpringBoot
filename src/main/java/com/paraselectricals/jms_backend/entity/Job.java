package com.paraselectricals.jms_backend.entity;

import com.paraselectricals.jms_backend.enums.JobStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jobId; // e.g., PEW-2026-0001

    @CreationTimestamp
    private LocalDate receivedDate;

    @Column(nullable = false)
    private String clientName;

    private String phoneNumber;
    private String contactPerson;
    
    private String motorType; // HT / LT / AC / DC
    private String pole;
    private String capacity; // kW or MW
    private String voltage;
    private String weight;

    @Column(columnDefinition = "TEXT")
    private String problemReported;

    private String priority; // High / Medium / Low

    private LocalDate expectedDeliveryDate;

    private LocalDate startDate; // when job entered the workshop

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStage currentStage;
    
    private String assignedPerson;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
