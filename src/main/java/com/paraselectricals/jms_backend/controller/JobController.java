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
    @Autowired
    private ChecklistItemRepository checklistRepository;
    @Autowired
    private com.paraselectricals.jms_backend.service.ExcelService excelService;

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
        
        // Add to central Excel sheet
        excelService.appendJobToExcel(savedJob);
        
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

    // 8. Get Checklist
    @GetMapping("/{id}/checklist")
    public ResponseEntity<List<ChecklistItem>> getChecklist(@PathVariable Long id, @RequestParam String stage) {
        try {
            JobStage jobStage = JobStage.valueOf(stage);
            return ResponseEntity.ok(checklistRepository.findByJobIdAndStage(id, jobStage));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 9. Bulk Create/Update Checklist
    @PutMapping("/{id}/checklist")
    public ResponseEntity<List<ChecklistItem>> updateChecklist(
            @PathVariable Long id,
            @RequestParam String stage,
            @RequestBody List<java.util.Map<String, Object>> items) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();
        Job job = jobOpt.get();
        JobStage jobStage;
        try {
            jobStage = JobStage.valueOf(stage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        // Clear existing items for this job+stage and recreate
        List<ChecklistItem> existing = checklistRepository.findByJobIdAndStage(id, jobStage);
        
        List<ChecklistItem> toSave = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> dto : items) {
            String taskDescription = (String) dto.get("taskDescription");
            boolean completed = Boolean.TRUE.equals(dto.get("completed"));
            Object rawId = dto.get("id");
            
            ChecklistItem item = null;
            if (rawId != null) {
                Long itemId = ((Number) rawId).longValue();
                item = existing.stream().filter(e -> e.getId().equals(itemId)).findFirst().orElse(null);
            }
            if (item == null) {
                item = new ChecklistItem();
                item.setJob(job);
                item.setStage(jobStage);
            }
            item.setTaskDescription(taskDescription);
            item.setCompleted(completed);
            toSave.add(item);
        }

        return ResponseEntity.ok(checklistRepository.saveAll(toSave));
    }

    @GetMapping("/master-sheet")
    public ResponseEntity<org.springframework.core.io.Resource> downloadMasterSheet() {
        try {
            Path path = Paths.get("reports/JobMasterSheet.xlsx");
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"JobMasterSheet.xlsx\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 9b. Update Job Details
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job jobDetails) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        Job job = jobOpt.get();
        // Update fields (except ID and system fields)
        job.setClientName(jobDetails.getClientName());
        job.setPhoneNumber(jobDetails.getPhoneNumber());
        job.setContactPerson(jobDetails.getContactPerson());
        job.setMotorType(jobDetails.getMotorType());
        job.setPole(jobDetails.getPole());
        job.setCapacity(jobDetails.getCapacity());
        job.setVoltage(jobDetails.getVoltage());
        job.setWeight(jobDetails.getWeight());
        job.setProblemReported(jobDetails.getProblemReported());
        job.setPriority(jobDetails.getPriority());
        job.setExpectedDeliveryDate(jobDetails.getExpectedDeliveryDate());
        job.setStartDate(jobDetails.getStartDate());
        job.setAssignedPerson(jobDetails.getAssignedPerson());

        Job updatedJob = jobRepository.save(job);
        return ResponseEntity.ok(updatedJob);
    }

    // 10. Delete Job
    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        Job job = jobOpt.get();

        // 1. Delete Photos and physical files
        List<Photo> photos = photoRepository.findByJobId(id);
        for (Photo photo : photos) {
            try {
                Path path = Paths.get(UPLOAD_DIR + photo.getFilePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Failed to delete photo file: " + e.getMessage());
            }
        }
        photoRepository.deleteAll(photos);

        // 2. Delete Documents and physical files
        List<Document> docs = documentRepository.findByJobId(id);
        for (Document doc : docs) {
            try {
                Path path = Paths.get(UPLOAD_DIR + doc.getFilePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Failed to delete document file: " + e.getMessage());
            }
        }
        documentRepository.deleteAll(docs);

        // 3. Delete Notes
        List<Note> notes = noteRepository.findByJobIdOrderByTimestampDesc(id);
        noteRepository.deleteAll(notes);

        // 4. Delete Stage History
        List<JobStageHistory> history = stageHistoryRepository.findByJobIdOrderByCompletedDateAsc(id);
        stageHistoryRepository.deleteAll(history);

        // 5. Delete Checklist Items
        // findByJobId is not directly in the repo, so we use a custom stream filter if necessary
        // or just add it to the repo. Let's add it to the repo for cleanliness if possible.
        // For now, let's use the stream filter.
        checklistRepository.deleteAll(checklistRepository.findAll().stream().filter(i -> i.getJob().getId().equals(id)).collect(java.util.stream.Collectors.toList()));

        // 6. Delete Job
        jobRepository.delete(job);

        return ResponseEntity.ok().build();
    }
}
