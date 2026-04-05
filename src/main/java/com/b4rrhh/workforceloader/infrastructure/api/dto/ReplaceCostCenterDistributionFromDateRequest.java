package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ReplaceCostCenterDistributionFromDateRequest(
        LocalDate effectiveDate,
        List<Item> items
) {
    public record Item(
            String costCenterCode,
            Integer allocationPercentage
    ) {
    }
}
