package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.ChecklistItem;
import com.paraselectricals.jms_backend.enums.JobStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByJobIdAndStage(Long jobId, JobStage stage);
}
