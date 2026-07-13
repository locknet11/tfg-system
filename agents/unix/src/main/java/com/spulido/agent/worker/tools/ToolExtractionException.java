package com.spulido.agent.worker.tools;

/**
 * Thrown when no matching bundled tool variant exists for the current
 * {@code os.name}/{@code os.arch} combination, or when extraction of
 * required tool binaries fails at startup.
 */
public class ToolExtractionException extends RuntimeException {

    public ToolExtractionException(String message) {
        super(message);
    }

    public ToolExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
