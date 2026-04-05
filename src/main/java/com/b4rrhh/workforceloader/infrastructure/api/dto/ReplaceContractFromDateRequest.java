package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;

public record ReplaceContractFromDateRequest(
        LocalDate effectiveDate,
        String contractCode,
        String contractSubtypeCode
) {
}
