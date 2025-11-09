package com.river.api.dto;

import com.river.engine.model.SourceConfig;
import com.river.engine.model.DestinationConfig;
import com.river.engine.model.Pipeline.PipelineMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for creating a pipeline
 */
@Data
public class CreatePipelineRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private SourceConfig source;

    @NotNull
    private DestinationConfig destination;

    private String schedule;

    private PipelineMode mode = PipelineMode.BATCH;

    private Map<String, Object> config = new HashMap<>();

    private String owner;
}
