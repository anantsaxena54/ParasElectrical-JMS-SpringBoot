package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.JobStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobStageHistoryRepository extends JpaRepository<JobStageHistory, Long> {
    List<JobStageHistory> findByJobIdOrderByCompletedDateAsc(Long jobId);
}
