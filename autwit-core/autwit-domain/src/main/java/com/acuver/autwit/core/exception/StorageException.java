package com.acuver.autwit.core.domain;

/**
 * Exception thrown when API context storage operations fail.
 *
 * <h2>USAGE</h2>
 * Thrown by adapters implementing {@link com.acuver.autwit.core.ports.ApiContextPort}
 * when persistence operations encounter errors.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(Throwable cause) {
        super(cause);
    }
}