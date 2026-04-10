package com.b4rrhh.workforceloader.application;

import java.math.BigDecimal;

public record ResolvedHireData(
        String companyCode,
        String workCenterCode,
        String entryReasonCode,
        String agreementCode,
        String agreementCategoryCode,
        String contractTypeCode,
        String contractSubtypeCode,
        BigDecimal workingTimePercentage
) {

    public ResolvedHireData withWorkingTimePercentage(BigDecimal workingTimePercentage) {
        return new ResolvedHireData(
                companyCode,
                workCenterCode,
                entryReasonCode,
                agreementCode,
                agreementCategoryCode,
                contractTypeCode,
                contractSubtypeCode,
                workingTimePercentage
        );
    }
}
