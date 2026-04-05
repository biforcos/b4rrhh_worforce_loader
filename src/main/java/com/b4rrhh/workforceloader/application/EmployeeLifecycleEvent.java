package com.b4rrhh.workforceloader.application;

import java.time.LocalDate;

public record EmployeeLifecycleEvent(
        LifecycleEventType eventType,
        LocalDate effectiveDate
) {
}