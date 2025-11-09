package com.river.api.service;

import com.river.api.dto.CreatePipelineRequest;
import com.river.api.dto.PipelineResponse;
import com.river.engine.model.Pipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing pipelines
 */
@Service
@Slf4j
public class PipelineService {

    private final Map<String, Pipeline> pipelines = new ConcurrentHashMap<>();

    public PipelineResponse createPipeline(CreatePipelineRequest request) {
        log.info("Creating pipeline: {}", request.getName());

        Pipeline pipeline = Pipeline.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .source(request.getSource())
                .destination(request.getDestination())
                .schedule(request.getSchedule())
                .mode(request.getMode())
                .config(request.getConfig())
                .owner(request.getOwner())
                .state(Pipeline.PipelineState.CREATED)
                .build();

        pipelines.put(pipeline.getId(), pipeline);

        return toPipelineResponse(pipeline);
    }

    public List<PipelineResponse> getAllPipelines() {
        log.info("Getting all pipelines");
        return pipelines.values().stream()
                .map(this::toPipelineResponse)
                .toList();
    }

    public PipelineResponse getPipeline(String id) {
        log.info("Getting pipeline: {}", id);
        Pipeline pipeline = pipelines.get(id);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline not found: " + id);
        }
        return toPipelineResponse(pipeline);
    }

    public void startPipeline(String id) {
        log.info("Starting pipeline: {}", id);
        Pipeline pipeline = pipelines.get(id);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline not found: " + id);
        }
        pipeline.setState(Pipeline.PipelineState.RUNNING);
        // TODO: Actually start the pipeline execution
    }

    public void stopPipeline(String id) {
        log.info("Stopping pipeline: {}", id);
        Pipeline pipeline = pipelines.get(id);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline not found: " + id);
        }
        pipeline.setState(Pipeline.PipelineState.STOPPED);
    }

    public void deletePipeline(String id) {
        log.info("Deleting pipeline: {}", id);
        pipelines.remove(id);
    }

    private PipelineResponse toPipelineResponse(Pipeline pipeline) {
        return PipelineResponse.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .description(pipeline.getDescription())
                .mode(pipeline.getMode())
                .state(pipeline.getState())
                .schedule(pipeline.getSchedule())
                .createdAt(pipeline.getCreatedAt())
                .updatedAt(pipeline.getUpdatedAt())
                .owner(pipeline.getOwner())
                .build();
    }
}
