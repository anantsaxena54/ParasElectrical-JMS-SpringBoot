package com.paraselectricals.jms_backend.controller;

import com.paraselectricals.jms_backend.entity.Document;
import com.paraselectricals.jms_backend.entity.Job;
import com.paraselectricals.jms_backend.entity.Photo;
import com.paraselectricals.jms_backend.enums.JobStage;
import com.paraselectricals.jms_backend.repository.DocumentRepository;
import com.paraselectricals.jms_backend.repository.JobRepository;
import com.paraselectricals.jms_backend.repository.PhotoRepository;
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
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private DocumentRepository documentRepository;

    private final String UPLOAD_DIR = "uploads/";

    // 1. Upload Photo
    @PostMapping("/photo/{jobId}")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long jobId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam String bucketType,
                                         @RequestParam(required = false) JobStage stageCaptured,
                                         @RequestParam(defaultValue = "admin") String uploader) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            String filePath = saveFileLocally(file, "photos_" + jobId);
            Photo photo = new Photo();
            photo.setJob(jobOpt.get());
            photo.setBucketType(bucketType);
            photo.setFilePath(filePath);
            photo.setStageCaptured(stageCaptured);
            photo.setUploader(uploader);
            photoRepository.save(photo);
            return ResponseEntity.ok(photo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload photo");
        }
    }

    // 2. Upload Document
    @PostMapping("/document/{jobId}")
    public ResponseEntity<?> uploadDocument(@PathVariable Long jobId,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam String documentType) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            String filePath = saveFileLocally(file, "docs_" + jobId);
            Document doc = new Document();
            doc.setJob(jobOpt.get());
            doc.setDocumentType(documentType);
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setFilePath(filePath);
            documentRepository.save(doc);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload document");
        }
    }

    // 3. Get Photos for Job
    @GetMapping("/photo/{jobId}")
    public ResponseEntity<List<Photo>> getPhotos(@PathVariable Long jobId) {
        return ResponseEntity.ok(photoRepository.findByJobId(jobId));
    }

    // 4. Get Documents for Job
    @GetMapping("/document/{jobId}")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable Long jobId) {
        return ResponseEntity.ok(documentRepository.findByJobId(jobId));
    }

    // 5. Delete Photo
    @DeleteMapping("/photo/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        Optional<Photo> photoOpt = photoRepository.findById(id);
        if (photoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Photo photo = photoOpt.get();
        try {
            Path path = Paths.get(UPLOAD_DIR + photo.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file from disk: " + e.getMessage());
        }
        photoRepository.delete(photo);
        return ResponseEntity.ok().build();
    }

    // 5b. Delete Document
    @DeleteMapping("/document/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Document doc = docOpt.get();
        try {
            Path path = Paths.get(UPLOAD_DIR + doc.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file from disk: " + e.getMessage());
        }
        documentRepository.delete(doc);
        return ResponseEntity.ok().build();
    }

    // 6. Serve File
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> serveFile(@PathVariable String filename, @RequestParam(required = false, defaultValue = "false") boolean download) {
        try {
            Path file = Paths.get(UPLOAD_DIR).resolve(filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) contentType = "application/octet-stream";
                
                org.springframework.http.ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType);
                        
                if (download) {
                    response.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
                }
                
                return response.body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String saveFileLocally(MultipartFile file, String prefix) throws IOException {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();

        String filename = prefix + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, file.getBytes());
        return filename;
    }
}
