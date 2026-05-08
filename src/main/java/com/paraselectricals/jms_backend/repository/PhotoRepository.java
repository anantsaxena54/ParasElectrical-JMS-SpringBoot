package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByJobId(Long jobId);
}
