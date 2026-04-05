package com.b4rrhh.workforceloader.domain.model;

public record HireExecutionResult(
        String employeeNumber,
        boolean success,
        String message
) {
}
