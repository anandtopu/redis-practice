package com.example.api.jobs;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobsController {

  private final JobStatusService jobStatusService;

  public JobsController(JobStatusService jobStatusService) {
    this.jobStatusService = jobStatusService;
  }

  @PostMapping("/jobs/start")
  public ResponseEntity<?> start(@Valid @RequestBody StartJobRequest request) {
    return ResponseEntity.ok(jobStatusService.startJob(request));
  }

  @GetMapping("/jobs/{jobId}")
  public ResponseEntity<?> get(@PathVariable("jobId") String jobId) {
    JobStatus status = jobStatusService.getJob(jobId);
    if (status == null) {
      return ResponseEntity.status(404).body(Map.of("error", "job_not_found"));
    }
    return ResponseEntity.ok(status);
  }
}
