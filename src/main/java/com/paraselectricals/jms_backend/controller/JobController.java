package com.paraselectricals.jms_backend.controller;

import com.paraselectricals.jms_backend.entity.*;
import com.paraselectricals.jms_backend.enums.JobStage;
import com.paraselectricals.jms_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobStageHistoryRepository stageHistoryRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private DocumentRepository documentRepository;

    private final String UPLOAD_DIR = "uploads/";

    // 1. Get all jobs (Dashboard)
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    // 2. Get single job details
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Optional<Job> job = jobRepository.findById(id);
        return job.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 3. Create new Job
    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        // Auto-generate Job ID (Simplified for MVP: PEW-YYYY-ID)
        job.setJobId("PEW-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis() % 10000);
        job.setCurrentStage(JobStage.RECEIVED);
        
        Job savedJob = jobRepository.save(job);
        
        // Log initial stage history
        JobStageHistory history = new JobStageHistory();
        history.setJob(savedJob);
        history.setStage(JobStage.RECEIVED);
        history.setNotes("Job created.");
        stageHistoryRepository.save(history);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
    }

    // 4. Update Stage
    @PutMapping("/{id}/stage")
    public ResponseEntity<?> updateStage(@PathVariable Long id, @RequestParam JobStage newStage, @RequestParam(required = false) String notes) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        Job job = jobOpt.get();
        job.setCurrentStage(newStage);
        jobRepository.save(job);

        // Record history
        JobStageHistory history = new JobStageHistory();
        history.setJob(job);
        history.setStage(newStage);
        history.setNotes(notes != null ? notes : "Moved to " + newStage.getDisplayName());
        stageHistoryRepository.save(history);

        return ResponseEntity.ok(job);
    }

    // 5. Add Note
    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id, @RequestBody Note note) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        note.setJob(jobOpt.get());
        if (note.getAuthor() == null) note.setAuthor("admin");
        noteRepository.save(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    // 5b. Update Note
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<?> updateNote(@PathVariable Long noteId, @RequestBody Note updatedNote) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) return ResponseEntity.notFound().build();

        Note note = noteOpt.get();
        note.setContent(updatedNote.getContent());
        noteRepository.save(note);
        return ResponseEntity.ok(note);
    }

    // 5c. Delete Note
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) return ResponseEntity.notFound().build();

        noteRepository.delete(noteOpt.get());
        return ResponseEntity.ok().build();
    }

    // 6. Get Notes
    @GetMapping("/{id}/notes")
    public ResponseEntity<List<Note>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(noteRepository.findByJobIdOrderByTimestampDesc(id));
    }

    // 7. Get History
    @GetMapping("/{id}/history")
    public ResponseEntity<List<JobStageHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(stageHistoryRepository.findByJobIdOrderByCompletedDateAsc(id));
    }
}
