package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.Job;
import com.paraselectricals.jms_backend.enums.JobStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByJobId(String jobId);
    List<Job> findByCurrentStage(JobStage stage);

    /**
     * Fetch all jobs with their photos, notes, and documents in a single query
     * using LEFT JOIN FETCH, eliminating the N+1 problem on the dashboard.
     * DISTINCT prevents duplicate Job rows caused by multiple collection joins.
     */
    @Query("SELECT DISTINCT j FROM Job j " +
           "LEFT JOIN FETCH j.photos " +
           "LEFT JOIN FETCH j.notes " +
           "LEFT JOIN FETCH j.documents")
    List<Job> findAllWithRelations();
}
