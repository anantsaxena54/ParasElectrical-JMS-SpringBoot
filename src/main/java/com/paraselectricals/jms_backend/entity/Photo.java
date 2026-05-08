package com.paraselectricals.jms_backend.entity;

import com.paraselectricals.jms_backend.enums.JobStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String bucketType; // BEFORE_REPAIR, DURING_WORK, FINAL_COMPLETION

    @Column(nullable = false)
    private String filePath;

    private String uploader;

    @Enumerated(EnumType.STRING)
    private JobStage stageCaptured;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
