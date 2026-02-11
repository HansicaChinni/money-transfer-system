
package com.money.draft.dto;

import java.time.Instant;

/**
 * Standard error payload returned by GlobalExceptionHandler.
 * Use with BusinessException hierarchy.
 */
public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp
) {}

