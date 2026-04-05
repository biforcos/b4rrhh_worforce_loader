package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;

public record TerminateEmployeeRequest(
        LocalDate terminationDate,
        String exitReasonCode
) {
}