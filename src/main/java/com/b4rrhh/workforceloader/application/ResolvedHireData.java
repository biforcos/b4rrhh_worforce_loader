package com.b4rrhh.workforceloader.application;

public record ResolvedHireData(
        String companyCode,
        String workCenterCode,
        String entryReasonCode,
        String agreementCode,
        String agreementCategoryCode,
        String contractTypeCode,
        String contractSubtypeCode
) {
}
