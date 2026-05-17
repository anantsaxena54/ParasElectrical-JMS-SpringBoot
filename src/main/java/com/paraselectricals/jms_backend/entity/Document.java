package com.paraselectricals.jms_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String documentType; // Test Reports, Drawings, Quotations, Delivery Challan, Inspection Reports

    @Column(nullable = false)
    private String filePath;   // Cloudinary secure_url (permanent HTTPS CDN URL)

    private String publicId;   // Cloudinary public_id — used for deletion

    @Column(nullable = false)
    private String originalFilename;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
