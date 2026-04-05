package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;

public record ReplaceLaborClassificationFromDateRequest(
        LocalDate effectiveDate,
        String agreementCode,
        String agreementCategoryCode
) {
}
