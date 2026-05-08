package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.Job;
import com.paraselectricals.jms_backend.enums.JobStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByJobId(String jobId);
    List<Job> findByCurrentStage(JobStage stage);
    // Add custom queries later if needed for search/filter
}
