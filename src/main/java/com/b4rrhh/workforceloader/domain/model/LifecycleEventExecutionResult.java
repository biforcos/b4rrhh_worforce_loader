package com.b4rrhh.workforceloader.domain.model;

import java.time.LocalDate;

public record LifecycleEventExecutionResult(
        String employeeNumber,
        String eventType,
        LocalDate effectiveDate,
        boolean success,
        String message
) {
}
