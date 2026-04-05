package com.b4rrhh.workforceloader.infrastructure.api.dto;

import java.time.LocalDate;
import java.util.List;

public record HireEmployeeRequest(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        LocalDate hireDate,
        String entryReasonCode,
        String companyCode,
        String workCenterCode,
        Contract contract,
        LaborClassification laborClassification,
        CostCenterDistribution costCenterDistribution
) {

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
