package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByJobId(Long jobId);
}
