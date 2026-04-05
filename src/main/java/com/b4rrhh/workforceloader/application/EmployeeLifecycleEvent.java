package com.b4rrhh.workforceloader.application;

import java.time.LocalDate;

public record EmployeeLifecycleEvent(
        LifecycleEventType eventType,
        LocalDate effectiveDate,
        MutationEventPayload payload
) {

    public EmployeeLifecycleEvent(LifecycleEventType eventType, LocalDate effectiveDate) {
        this(eventType, effectiveDate, null);
    }
}