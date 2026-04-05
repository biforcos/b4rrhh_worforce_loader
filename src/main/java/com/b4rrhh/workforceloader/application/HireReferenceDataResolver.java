package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.CatalogApiClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HireReferenceDataResolver {

    private static final String COMPANY = "COMPANY";
    private static final String WORK_CENTER = "WORK_CENTER";
    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";
    private static final String AGREEMENT = "AGREEMENT";
    private static final String CONTRACT = "CONTRACT";

    private final CatalogApiClient catalogApiClient;

    public HireReferenceDataResolver(CatalogApiClient catalogApiClient) {
        this.catalogApiClient = catalogApiClient;
    }

    public ResolvedHireData resolve(String ruleSystemCode) {
        String normalizedRuleSystemCode = normalizeCode(ruleSystemCode);

        CatalogOption company = RandomSelector.pickRandom(
                catalogApiClient.getDirectOptions(normalizedRuleSystemCode, COMPANY)
        );

        CatalogOption workCenter = RandomSelector.pickRandom(
                catalogApiClient.getDirectOptions(normalizedRuleSystemCode, WORK_CENTER)
        );

        CatalogOption entryReason = RandomSelector.pickRandom(resolveEntryReasonOptions(normalizedRuleSystemCode));

        CatalogOption agreement = RandomSelector.pickRandom(
                catalogApiClient.getDirectOptions(normalizedRuleSystemCode, AGREEMENT)
        );

        CatalogOption agreementCategory = RandomSelector.pickRandom(
                catalogApiClient.getAgreementCategories(normalizedRuleSystemCode, agreement.code())
        );

        CatalogOption contractType = RandomSelector.pickRandom(
                catalogApiClient.getDirectOptions(normalizedRuleSystemCode, CONTRACT)
        );

        CatalogOption contractSubtype = RandomSelector.pickRandom(
                catalogApiClient.getContractSubtypes(normalizedRuleSystemCode, contractType.code())
        );

        return new ResolvedHireData(
                company.code(),
                workCenter.code(),
                entryReason.code(),
                agreement.code(),
                agreementCategory.code(),
                contractType.code(),
                contractSubtype.code()
        );
    }

    private List<CatalogOption> resolveEntryReasonOptions(String ruleSystemCode) {
        try {
            return catalogApiClient.getDirectOptions(ruleSystemCode, ENTRY_REASON);
        } catch (IllegalStateException primaryError) {
            return catalogApiClient.getDirectOptions(ruleSystemCode, "ENTRY_REASON");
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
