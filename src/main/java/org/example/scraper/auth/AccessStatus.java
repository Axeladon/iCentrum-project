package org.example.scraper.auth;

public enum AccessStatus {
    SUCCESS,                 // Access granted successfully
    LOGIN_REQUIRED,          // Login is required to access the resource
    TWO_FACTOR_REQUIRED,     // Two-factor authentication is required
    INVALID_CREDENTIALS,     // Invalid login or password
    ERROR                    // General error during access attempt
}

