package com.paraselectricals.jms_backend.repository;

import com.paraselectricals.jms_backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByJobIdOrderByTimestampDesc(Long jobId);
}
