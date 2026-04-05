package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;

public record CreateWorkCenterRequest(
        String workCenterCode,
        LocalDate startDate,
        LocalDate endDate
) {
}
