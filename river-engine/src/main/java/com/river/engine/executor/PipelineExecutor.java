package com.river.engine.executor;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.SourceConnector;
import com.river.core.context.DestinationContext;
import com.river.core.context.SourceContext;
import com.river.core.model.Record;
import com.river.core.registry.ConnectorRegistry;
import com.river.engine.model.Pipeline;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Executor for running data pipelines
 */
@Slf4j
public class PipelineExecutor {

    private final ConnectorRegistry connectorRegistry;

    public PipelineExecutor(ConnectorRegistry connectorRegistry) {
        this.connectorRegistry = connectorRegistry;
    }

    /**
     * Execute a pipeline
     *
     * @param pipeline Pipeline to execute
     * @return Execution result
     * @throws PipelineExecutionException if execution fails
     */
    public PipelineExecutionResult execute(Pipeline pipeline) throws PipelineExecutionException {
        log.info("Starting pipeline execution: {}", pipeline.getName());

        PipelineExecutionResult result = PipelineExecutionResult.builder()
                .pipelineId(pipeline.getId())
                .pipelineName(pipeline.getName())
                .build();

        SourceConnector sourceConnector = null;
        DestinationConnector destinationConnector = null;

        try {
            // Initialize source connector
            sourceConnector = initializeSourceConnector(pipeline);

            // Initialize destination connector
            destinationConnector = initializeDestinationConnector(pipeline);

            // Read data from source
            log.info("Reading data from source: {}", pipeline.getSource().getType());
            Stream<Record> records = sourceConnector.read();

            // Apply transformations (if any)
            // TODO: Implement transformation layer

            // Write data to destination
            log.info("Writing data to destination: {}", pipeline.getDestination().getType());
            destinationConnector.write(records);

            // Update result
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setRecordsProcessed(sourceConnector.getStats().getRecordsProcessed());
            result.setBytesProcessed(sourceConnector.getStats().getBytesProcessed());

            log.info("Pipeline execution completed successfully: {}", pipeline.getName());

        } catch (Exception e) {
            log.error("Pipeline execution failed: {}", pipeline.getName(), e);
            result.setStatus(ExecutionStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            throw new PipelineExecutionException("Pipeline execution failed", e);

        } finally {
            // Cleanup
            if (sourceConnector != null) {
                try {
                    sourceConnector.stop();
                } catch (ConnectorException e) {
                    log.error("Error stopping source connector", e);
                }
            }
            if (destinationConnector != null) {
                try {
                    destinationConnector.stop();
                } catch (ConnectorException e) {
                    log.error("Error stopping destination connector", e);
                }
            }
            result.complete();
        }

        return result;
    }

    private SourceConnector initializeSourceConnector(Pipeline pipeline) throws Exception {
        String sourceType = pipeline.getSource().getType();
        SourceConnector connector = connectorRegistry.getSource(sourceType);

        // Configure connector
        Map<String, Object> config = new HashMap<>(pipeline.getSource().getConnection());
        config.putAll(pipeline.getSource().getProperties());
        connector.configure(config);

        // Create context
        SourceContext context = SourceContext.builder()
                .pipelineId(pipeline.getId())
                .taskId(pipeline.getId() + "-task")
                .offset(getLastOffset(pipeline))
                .build();

        // Start connector
        connector.start(context);

        return connector;
    }

    private DestinationConnector initializeDestinationConnector(Pipeline pipeline) throws Exception {
        String destinationType = pipeline.getDestination().getType();
        DestinationConnector connector = connectorRegistry.getDestination(destinationType);

        // Configure connector
        Map<String, Object> config = new HashMap<>(pipeline.getDestination().getConnection());
        config.putAll(pipeline.getDestination().getProperties());
        connector.configure(config);

        // Create context
        DestinationContext context = DestinationContext.builder()
                .pipelineId(pipeline.getId())
                .taskId(pipeline.getId() + "-task")
                .batchSize(pipeline.getDestination().getBatchSize())
                .transactional(pipeline.getDestination().getTransactional())
                .build();

        // Start connector
        connector.start(context);

        return connector;
    }

    private Long getLastOffset(Pipeline pipeline) {
        // TODO: Retrieve from metadata store
        return 0L;
    }

    /**
     * Exception thrown during pipeline execution
     */
    public static class PipelineExecutionException extends Exception {
        public PipelineExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Execution status
     */
    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
