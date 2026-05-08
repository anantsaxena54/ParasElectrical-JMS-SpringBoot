package com.paraselectricals.jms_backend.entity;

import com.paraselectricals.jms_backend.enums.JobStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_checklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStage stage;

    @Column(nullable = false)
    private String taskDescription;

    private boolean completed = false;
}
