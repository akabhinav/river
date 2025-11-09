package com.river.api.dto;

import com.river.engine.model.Pipeline.PipelineMode;
import com.river.engine.model.Pipeline.PipelineState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for pipeline
 */
@Data
@Builder
public class PipelineResponse {

    private String id;
    private String name;
    private String description;
    private PipelineMode mode;
    private PipelineState state;
    private String schedule;
    private Instant createdAt;
    private Instant updatedAt;
    private String owner;
}
