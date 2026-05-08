package com.paraselectricals.jms_backend.entity;

import com.paraselectricals.jms_backend.enums.JobStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_stage_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStage stage;

    @CreationTimestamp
    private LocalDateTime completedDate;

    private String assignedTeam;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
