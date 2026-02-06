
package com.money.draft.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {
    private Instant timestamp = Instant.now();
    private String error;
    private String message;
    private List<String> details; // optional: field errors, etc.

    public ErrorResponse() {}

    public ErrorResponse(String error, String message, List<String> details) {
        this.error = error;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() { return timestamp; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
