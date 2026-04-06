package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.CatalogApiClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class HireReferenceDataResolver {

    private static final String PRESENCE_RESOURCE = "employee.presence";
    private static final String WORK_CENTER_RESOURCE = "employee.work_center";
    private static final String CONTRACT_RESOURCE = "employee.contract";
    private static final String LABOR_CLASSIFICATION_RESOURCE = "employee.labor_classification";

    private static final String COMPANY_FIELD = "companyCode";
    private static final String WORK_CENTER_FIELD = "workCenterCode";
    private static final String ENTRY_REASON_FIELD = "entryReasonCode";
    private static final String EXIT_REASON_FIELD = "exitReasonCode";
    private static final String CONTRACT_TYPE_FIELD = "contractTypeCode";
    private static final String AGREEMENT_FIELD = "agreementCode";

    private static final String COMPANY = "COMPANY";
    private static final String LEGACY_PRESENCE_COMPANY = "EMPLOYEE_PRESENCE_COMPANY";
    private static final String WORK_CENTER = "WORK_CENTER";
    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";
    private static final String LEGACY_ENTRY_REASON = "ENTRY_REASON";
    private static final String EXIT_REASON = "EMPLOYEE_PRESENCE_EXIT_REASON";
    private static final String LEGACY_EXIT_REASON = "EXIT_REASON";
    private static final String AGREEMENT = "AGREEMENT";
    private static final String CONTRACT = "CONTRACT";

    private final CatalogApiClient catalogApiClient;

    public HireReferenceDataResolver(CatalogApiClient catalogApiClient) {
        this.catalogApiClient = catalogApiClient;
    }

    public ResolvedHireData resolve(String ruleSystemCode) {
        return resolve(ruleSystemCode, null);
    }

    public ResolvedHireData resolve(String ruleSystemCode, Random random) {
        String normalizedRuleSystemCode = normalizeCode(ruleSystemCode);

        ResolvedHireReferencePools pools = preloadPools(normalizedRuleSystemCode);
        return resolveFromPools(pools, random);
    }

    public ResolvedHireReferencePools preloadPools(String ruleSystemCode) {
        String normalizedRuleSystemCode = normalizeCode(ruleSystemCode);

        List<CatalogOption> companies = catalogApiClient.getDirectOptionsForField(
            normalizedRuleSystemCode,
            PRESENCE_RESOURCE,
            COMPANY_FIELD,
            COMPANY,
            LEGACY_PRESENCE_COMPANY
        );
        List<CatalogOption> workCenters = catalogApiClient.getDirectOptionsForField(
            normalizedRuleSystemCode,
            WORK_CENTER_RESOURCE,
            WORK_CENTER_FIELD,
            WORK_CENTER
        );
        List<CatalogOption> entryReasons = resolveEntryReasonOptions(normalizedRuleSystemCode);
        List<CatalogOption> exitReasons = resolveExitReasonOptions(normalizedRuleSystemCode);

        List<CatalogOption> agreements = catalogApiClient.getDirectOptionsForField(
            normalizedRuleSystemCode,
            LABOR_CLASSIFICATION_RESOURCE,
            AGREEMENT_FIELD,
            AGREEMENT
        );
        List<AgreementWithCategories> agreementsWithCategories = new ArrayList<>(agreements.size());
        for (CatalogOption agreement : agreements) {
            List<CatalogOption> categories = catalogApiClient.getAgreementCategories(normalizedRuleSystemCode, agreement.code());
            agreementsWithCategories.add(new AgreementWithCategories(agreement, categories));
        }

        List<CatalogOption> contractTypes = catalogApiClient.getDirectOptionsForField(
            normalizedRuleSystemCode,
            CONTRACT_RESOURCE,
            CONTRACT_TYPE_FIELD,
            CONTRACT
        );
        List<ContractTypeWithSubtypes> contractTypesWithSubtypes = new ArrayList<>(contractTypes.size());
        for (CatalogOption contractType : contractTypes) {
            List<CatalogOption> subtypes = catalogApiClient.getContractSubtypes(normalizedRuleSystemCode, contractType.code());
            contractTypesWithSubtypes.add(new ContractTypeWithSubtypes(contractType, subtypes));
        }

        return new ResolvedHireReferencePools(
                companies,
                workCenters,
                entryReasons,
                exitReasons,
                agreementsWithCategories,
                contractTypesWithSubtypes
        );
    }

    public ResolvedHireData resolveFromPools(ResolvedHireReferencePools pools, Random random) {
        CatalogOption company = pick(pools.companies(), random);
        CatalogOption workCenter = pick(pools.workCenters(), random);
        CatalogOption entryReason = pick(pools.entryReasons(), random);

        AgreementWithCategories agreementWithCategories = pick(pools.agreementsWithCategories(), random);
        CatalogOption agreement = agreementWithCategories.agreement();
        CatalogOption agreementCategory = pick(agreementWithCategories.categories(), random);

        ContractTypeWithSubtypes contractTypeWithSubtypes = pick(pools.contractTypesWithSubtypes(), random);
        CatalogOption contractType = contractTypeWithSubtypes.contractType();
        CatalogOption contractSubtype = pick(contractTypeWithSubtypes.subtypes(), random);

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

    public String resolveExitReasonFromPools(ResolvedHireReferencePools pools, Random random) {
        CatalogOption exitReason = pick(pools.exitReasons(), random);
        return exitReason.code();
    }

    public String resolveWorkCenterCodeFromPools(ResolvedHireReferencePools pools, Random random) {
        CatalogOption workCenter = pick(pools.workCenters(), random);
        return workCenter.code();
    }

    public ResolvedContractData resolveContractFromPools(ResolvedHireReferencePools pools, Random random) {
        ContractTypeWithSubtypes contractTypeWithSubtypes = pick(pools.contractTypesWithSubtypes(), random);
        CatalogOption contractType = contractTypeWithSubtypes.contractType();
        CatalogOption contractSubtype = pick(contractTypeWithSubtypes.subtypes(), random);
        return new ResolvedContractData(contractType.code(), contractSubtype.code());
    }

    public ResolvedLaborClassificationData resolveLaborClassificationFromPools(
            ResolvedHireReferencePools pools,
            Random random
    ) {
        AgreementWithCategories agreementWithCategories = pick(pools.agreementsWithCategories(), random);
        CatalogOption agreement = agreementWithCategories.agreement();
        CatalogOption agreementCategory = pick(agreementWithCategories.categories(), random);
        return new ResolvedLaborClassificationData(agreement.code(), agreementCategory.code());
    }

    public String resolveExitReasonCode(String ruleSystemCode, Random random) {
        String normalizedRuleSystemCode = normalizeCode(ruleSystemCode);
        ResolvedHireReferencePools pools = preloadPools(normalizedRuleSystemCode);
        return resolveExitReasonFromPools(pools, random);
    }

    private List<CatalogOption> resolveEntryReasonOptions(String ruleSystemCode) {
        return catalogApiClient.getDirectOptionsForField(
                ruleSystemCode,
                PRESENCE_RESOURCE,
                ENTRY_REASON_FIELD,
                ENTRY_REASON,
                LEGACY_ENTRY_REASON
        );
    }

    private List<CatalogOption> resolveExitReasonOptions(String ruleSystemCode) {
        return catalogApiClient.getDirectOptionsForField(
                ruleSystemCode,
                PRESENCE_RESOURCE,
                EXIT_REASON_FIELD,
                EXIT_REASON,
                LEGACY_EXIT_REASON
        );
    }

    private static <T> T pick(List<T> values, Random random) {
        if (random == null) {
            return RandomSelector.pickRandom(values);
        }
        return RandomSelector.pickRandom(values, random);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
