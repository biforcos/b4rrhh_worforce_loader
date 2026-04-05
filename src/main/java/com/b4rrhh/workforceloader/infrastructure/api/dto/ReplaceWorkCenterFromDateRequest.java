package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;

public record ReplaceWorkCenterFromDateRequest(
        LocalDate effectiveDate,
        String workCenterCode
) {
}