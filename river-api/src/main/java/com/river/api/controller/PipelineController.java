package com.river.api.controller;

import com.river.api.dto.CreatePipelineRequest;
import com.river.api.dto.PipelineResponse;
import com.river.api.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for pipeline management
 */
@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipelines", description = "Pipeline management APIs")
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping
    @Operation(summary = "Create a new pipeline")
    public ResponseEntity<PipelineResponse> createPipeline(
            @Valid @RequestBody CreatePipelineRequest request) {
        PipelineResponse response = pipelineService.createPipeline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all pipelines")
    public ResponseEntity<List<PipelineResponse>> getAllPipelines() {
        List<PipelineResponse> pipelines = pipelineService.getAllPipelines();
        return ResponseEntity.ok(pipelines);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pipeline by ID")
    public ResponseEntity<PipelineResponse> getPipeline(@PathVariable String id) {
        PipelineResponse response = pipelineService.getPipeline(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a pipeline")
    public ResponseEntity<Void> startPipeline(@PathVariable String id) {
        pipelineService.startPipeline(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop a pipeline")
    public ResponseEntity<Void> stopPipeline(@PathVariable String id) {
        pipelineService.stopPipeline(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pipeline")
    public ResponseEntity<Void> deletePipeline(@PathVariable String id) {
        pipelineService.deletePipeline(id);
        return ResponseEntity.noContent().build();
    }
}
