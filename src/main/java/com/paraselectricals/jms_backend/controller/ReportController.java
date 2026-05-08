package com.paraselectricals.jms_backend.controller;

import com.paraselectricals.jms_backend.entity.Job;
import com.paraselectricals.jms_backend.enums.JobStage;
import com.paraselectricals.jms_backend.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getReportDashboard() {
        List<Job> allJobs = jobRepository.findAll();
        Map<String, Object> metrics = new HashMap<>();

        // 1. Cycle Time (Average days for dispatched jobs)
        List<Job> dispatchedJobs = allJobs.stream()
                .filter(j -> j.getCurrentStage() == JobStage.DISPATCHED)
                .collect(Collectors.toList());
        
        double avgCycleTime = 0;
        if (!dispatchedJobs.isEmpty()) {
            long totalDays = 0;
            for (Job j : dispatchedJobs) {
                // Simplified: using current date as completion for MVP if no history checked
                // Assuming it was dispatched today for calculation if exact date isn't easily pulled
                totalDays += ChronoUnit.DAYS.between(j.getReceivedDate(), LocalDate.now());
            }
            avgCycleTime = (double) totalDays / dispatchedJobs.size();
        }
        metrics.put("averageCycleTimeDays", Math.round(avgCycleTime * 10.0) / 10.0);

        // 2. On-Time Delivery Percentage
        if (dispatchedJobs.isEmpty()) {
            metrics.put("onTimeDeliveryPercentage", 100.0);
        } else {
            long onTimeCount = dispatchedJobs.stream()
                    .filter(j -> j.getExpectedDeliveryDate() != null && !LocalDate.now().isAfter(j.getExpectedDeliveryDate()))
                    .count();
            double percentage = ((double) onTimeCount / dispatchedJobs.size()) * 100;
            metrics.put("onTimeDeliveryPercentage", Math.round(percentage * 10.0) / 10.0);
        }

        // 3. Technician Workload
        Map<String, Long> workload = allJobs.stream()
                .filter(j -> j.getCurrentStage() != JobStage.DISPATCHED)
                .filter(j -> j.getAssignedPerson() != null && !j.getAssignedPerson().isEmpty())
                .collect(Collectors.groupingBy(Job::getAssignedPerson, Collectors.counting()));
        metrics.put("technicianWorkload", workload);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportJobsCsv() {
        List<Job> allJobs = jobRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Job ID,Client Name,Motor Type,Capacity,Stage,Priority,Expected Delivery\n");
        
        for (Job j : allJobs) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(j.getJobId()),
                    escapeCsv(j.getClientName()),
                    escapeCsv(j.getMotorType()),
                    escapeCsv(j.getCapacity()),
                    j.getCurrentStage().getDisplayName(),
                    j.getPriority(),
                    j.getExpectedDeliveryDate() != null ? j.getExpectedDeliveryDate().toString() : ""
            ));
        }

        byte[] output = csv.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "jobs_export.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok().headers(headers).body(output);
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        data = data.replace("\"", "\"\"");
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data + "\"";
        }
        return data;
    }
}
