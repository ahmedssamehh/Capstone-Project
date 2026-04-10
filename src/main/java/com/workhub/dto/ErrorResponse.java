package com.workhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard error response DTO
 * 
 * Format:
 * {
 *   "message": "Error message",
 *   "details": [...]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private String message;
    private List<String> details;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
