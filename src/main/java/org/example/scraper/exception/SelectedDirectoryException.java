package org.example.scraper.exception;

public class SelectedDirectoryException extends RuntimeException {
    public SelectedDirectoryException(String message) {
        super(message);
    }

    public SelectedDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
