package com.river.core.connector;

/**
 * Exception thrown by connectors
 */
public class ConnectorException extends Exception {

    private final ErrorCode errorCode;
    private final boolean retryable;

    public ConnectorException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN;
        this.retryable = false;
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
        this.retryable = false;
    }

    public ConnectorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = errorCode.isRetryable();
    }

    public ConnectorException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = errorCode.isRetryable();
    }

    public ConnectorException(ErrorCode errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Error codes for connector exceptions
     */
    public enum ErrorCode {
        CONFIGURATION_ERROR(false),
        CONNECTION_ERROR(true),
        AUTHENTICATION_ERROR(false),
        AUTHORIZATION_ERROR(false),
        SCHEMA_ERROR(false),
        DATA_ERROR(false),
        TIMEOUT_ERROR(true),
        RESOURCE_EXHAUSTED(true),
        NOT_FOUND(false),
        ALREADY_EXISTS(false),
        UNKNOWN(true);

        private final boolean retryable;

        ErrorCode(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
