package com.dhruv.web;

import java.util.Map;

/**
 * Uniform error envelope for every non-2xx API response.
 *
 * @param code    stable identifier the client branches on, e.g. {@code ACCOUNT_NOT_FOUND}
 * @param message human-readable text, safe to display
 * @param fields  per-field validation messages; null when not a validation failure
 */
public record ApiErrorResponse(String code, String message, Map<String, String> fields) {

    public ApiErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
