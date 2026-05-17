package com.paraselectricals.jms_backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private Cloudinary cloudinary;

    // ─── 1. Upload Photo ──────────────────────────────────────────────────────

    @PostMapping("/photo/{jobId}")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long jobId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam String bucketType,
                                         @RequestParam(required = false) JobStage stageCaptured,
                                         @RequestParam(defaultValue = "admin") String uploader) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            // Upload to Cloudinary under folder "jms/photos/<jobId>"
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "jms/photos/" + jobId,
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            // Store the permanent Cloudinary URL and public_id
            String secureUrl  = (String) uploadResult.get("secure_url");
            String publicId   = (String) uploadResult.get("public_id");

            Photo photo = new Photo();
            photo.setJob(jobOpt.get());
            photo.setBucketType(bucketType);
            photo.setFilePath(secureUrl);      // permanent HTTPS URL
            photo.setPublicId(publicId);       // needed for deletion
            photo.setStageCaptured(stageCaptured);
            photo.setUploader(uploader);
            photoRepository.save(photo);
            return ResponseEntity.ok(photo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload photo: " + e.getMessage());
        }
    }

    // ─── 2. Upload Document ───────────────────────────────────────────────────

    @PostMapping("/document/{jobId}")
    public ResponseEntity<?> uploadDocument(@PathVariable Long jobId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam String documentType) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            // Upload raw file (PDF, DOC, etc.) to Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "jms/documents/" + jobId,
                            "resource_type", "raw",   // use "raw" for non-image files
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId  = (String) uploadResult.get("public_id");

            Document doc = new Document();
            doc.setJob(jobOpt.get());
            doc.setDocumentType(documentType);
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setFilePath(secureUrl);   // permanent HTTPS URL
            doc.setPublicId(publicId);    // needed for deletion
            documentRepository.save(doc);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload document: " + e.getMessage());
        }
    }

    // ─── 3. Get Photos for Job ────────────────────────────────────────────────

    @GetMapping("/photo/{jobId}")
    public ResponseEntity<List<Photo>> getPhotos(@PathVariable Long jobId) {
        return ResponseEntity.ok(photoRepository.findByJobId(jobId));
    }

    // ─── 4. Get Documents for Job ─────────────────────────────────────────────

    @GetMapping("/document/{jobId}")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable Long jobId) {
        return ResponseEntity.ok(documentRepository.findByJobId(jobId));
    }

    // ─── 5. Delete Photo ──────────────────────────────────────────────────────

    @DeleteMapping("/photo/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        Optional<Photo> photoOpt = photoRepository.findById(id);
        if (photoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Photo photo = photoOpt.get();
        // Delete from Cloudinary if publicId is available
        if (photo.getPublicId() != null && !photo.getPublicId().isBlank()) {
            try {
                cloudinary.uploader().destroy(photo.getPublicId(), ObjectUtils.emptyMap());
            } catch (IOException e) {
                System.err.println("Failed to delete from Cloudinary: " + e.getMessage());
                // Don't fail the request — still remove from DB
            }
        }
        photoRepository.delete(photo);
        return ResponseEntity.ok().build();
    }

    // ─── 5b. Delete Document ──────────────────────────────────────────────────

    @DeleteMapping("/document/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Document doc = docOpt.get();
        // Delete from Cloudinary if publicId is available
        if (doc.getPublicId() != null && !doc.getPublicId().isBlank()) {
            try {
                cloudinary.uploader().destroy(doc.getPublicId(),
                        ObjectUtils.asMap("resource_type", "raw"));
            } catch (IOException e) {
                System.err.println("Failed to delete from Cloudinary: " + e.getMessage());
            }
        }
        documentRepository.delete(doc);
        return ResponseEntity.ok().build();
    }

    // ─── NOTE ─────────────────────────────────────────────────────────────────
    // The old /files/{filename} endpoint is removed.
    // Files are now served directly from Cloudinary's CDN via the secure_url
    // stored in filePath. The frontend should use photo.filePath / doc.filePath
    // directly as the <img src> or download link.
}
