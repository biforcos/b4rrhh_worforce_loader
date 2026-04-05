package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;
import java.util.List;

public record RehireEmployeeRequest(
        LocalDate rehireDate,
        String entryReasonCode,
        String companyCode,
        WorkCenter workCenter,
        Contract contract,
        LaborClassification laborClassification,
        CostCenterDistribution costCenterDistribution
) {

    public record WorkCenter(
            String workCenterCode
    ) {
    }

    public record Contract(
            String contractTypeCode,
            String contractSubtypeCode
    ) {
    }

    public record LaborClassification(
            String agreementCode,
            String agreementCategoryCode
    ) {
    }

    public record CostCenterDistribution(
            List<Item> items
    ) {
        public record Item(
                String costCenterCode,
                Integer allocationPercentage
        ) {
        }
    }
}