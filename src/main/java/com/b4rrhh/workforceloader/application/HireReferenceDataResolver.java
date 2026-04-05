package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.CatalogApiClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class HireReferenceDataResolver {

    private static final String COMPANY = "COMPANY";
    private static final String WORK_CENTER = "WORK_CENTER";
    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";
    private static final String EXIT_REASON = "EMPLOYEE_PRESENCE_EXIT_REASON";
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

        List<CatalogOption> companies = catalogApiClient.getDirectOptions(normalizedRuleSystemCode, COMPANY);
        List<CatalogOption> workCenters = catalogApiClient.getDirectOptions(normalizedRuleSystemCode, WORK_CENTER);
        List<CatalogOption> entryReasons = resolveEntryReasonOptions(normalizedRuleSystemCode);
        List<CatalogOption> exitReasons = resolveExitReasonOptions(normalizedRuleSystemCode);

        List<CatalogOption> agreements = catalogApiClient.getDirectOptions(normalizedRuleSystemCode, AGREEMENT);
        List<AgreementWithCategories> agreementsWithCategories = new ArrayList<>(agreements.size());
        for (CatalogOption agreement : agreements) {
            List<CatalogOption> categories = catalogApiClient.getAgreementCategories(normalizedRuleSystemCode, agreement.code());
            agreementsWithCategories.add(new AgreementWithCategories(agreement, categories));
        }

        List<CatalogOption> contractTypes = catalogApiClient.getDirectOptions(normalizedRuleSystemCode, CONTRACT);
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
        try {
            return catalogApiClient.getDirectOptions(ruleSystemCode, ENTRY_REASON);
        } catch (IllegalStateException primaryError) {
            return catalogApiClient.getDirectOptions(ruleSystemCode, "ENTRY_REASON");
        }
    }

    private List<CatalogOption> resolveExitReasonOptions(String ruleSystemCode) {
        try {
            return catalogApiClient.getDirectOptions(ruleSystemCode, EXIT_REASON);
        } catch (IllegalStateException primaryError) {
            return catalogApiClient.getDirectOptions(ruleSystemCode, "EXIT_REASON");
        }
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
