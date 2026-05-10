package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.CatalogApiClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    private final LoaderProperties.Filters filters;

    public HireReferenceDataResolver(CatalogApiClient catalogApiClient, LoaderProperties properties) {
        this.catalogApiClient = catalogApiClient;
        this.filters = properties.getFilters();
    }

    public ResolvedHireData resolve(String ruleSystemCode) {
        return resolve(ruleSystemCode, null);
    }

    public ResolvedHireData resolve(String ruleSystemCode, Random random) {
        String normalizedRuleSystemCode = normalizeCode(ruleSystemCode);

        ResolvedHireReferencePools pools = preloadPools(normalizedRuleSystemCode);
        return resolveFromPools(pools, normalizedRuleSystemCode, null, random);
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
                applyAgreementFilter(agreementsWithCategories),
                applyContractFilter(contractTypesWithSubtypes)
        );
    }

    private List<AgreementWithCategories> applyAgreementFilter(List<AgreementWithCategories> source) {
        String code = filters.getAgreementCode();
        if (code == null || code.isBlank()) {
            return source;
        }
        String normalizedCode = code.trim().toUpperCase();
        List<String> categoryFilter = filters.getAgreementCategoryCodes().stream()
                .map(c -> c.trim().toUpperCase())
                .toList();

        return source.stream()
                .filter(a -> a.agreement().code().equalsIgnoreCase(normalizedCode))
                .map(a -> categoryFilter.isEmpty() ? a :
                        new AgreementWithCategories(
                                a.agreement(),
                                a.categories().stream()
                                        .filter(c -> categoryFilter.contains(c.code().toUpperCase()))
                                        .toList()
                        )
                )
                .filter(a -> !a.categories().isEmpty())
                .toList();
    }

    private List<ContractTypeWithSubtypes> applyContractFilter(List<ContractTypeWithSubtypes> source) {
        List<ContractTypeWithSubtypes> result = source;

        if (filters.isNumericContractTypesOnly()) {
            result = result.stream()
                    .filter(c -> c.contractType().code().matches("[0-9]+"))
                    .toList();
        }

        if (filters.isRequireContractSubtype()) {
            result = result.stream()
                    .filter(c -> !c.subtypes().isEmpty())
                    .toList();
        }

        return result;
    }

    public ResolvedHireData resolveFromPools(ResolvedHireReferencePools pools, Random random) {
        return resolveFromPools(pools, null, null, random);
    }

    public ResolvedHireData resolveFromPools(
            ResolvedHireReferencePools pools,
            String ruleSystemCode,
            LocalDate referenceDate,
            Random random
    ) {
        ResolvedCompanyWorkCenter companyAndWorkCenter = resolveCompanyAndWorkCenterFromPools(
            pools.companies(),
            ruleSystemCode,
            referenceDate,
            random
        );
        CatalogOption entryReason = pick(pools.entryReasons(), random);

        AgreementWithCategories agreementWithCategories = pick(pools.agreementsWithCategories(), random);
        CatalogOption agreement = agreementWithCategories.agreement();
        CatalogOption agreementCategory = pick(agreementWithCategories.categories(), random);

        ContractTypeWithSubtypes contractTypeWithSubtypes = pick(pools.contractTypesWithSubtypes(), random);
        CatalogOption contractType = contractTypeWithSubtypes.contractType();
        CatalogOption contractSubtype = pick(contractTypeWithSubtypes.subtypes(), random);

        return new ResolvedHireData(
            companyAndWorkCenter.companyCode(),
            companyAndWorkCenter.workCenterCode(),
                entryReason.code(),
                agreement.code(),
                agreementCategory.code(),
                contractType.code(),
                contractSubtype.code(),
                null
        );
    }

    public String resolveExitReasonFromPools(ResolvedHireReferencePools pools, Random random) {
        CatalogOption exitReason = pick(pools.exitReasons(), random);
        return exitReason.code();
    }

    public String resolveWorkCenterCodeFromPools(ResolvedHireReferencePools pools, Random random) {
        CatalogOption company = pick(pools.companies(), random);
        return resolveWorkCenterCodeForCompany(null, company.code(), null, null, random);
    }

    public String resolveWorkCenterCodeForCompany(
            String ruleSystemCode,
            String companyCode,
            LocalDate referenceDate,
            String currentWorkCenterCode,
            Random random
    ) {
        List<CatalogOption> workCenters = catalogApiClient.getWorkCentersByCompany(ruleSystemCode, companyCode, referenceDate);
        CatalogOption selected = pickDifferentOrAny(workCenters, currentWorkCenterCode, random);
        return selected.code();
    }

    public String tryResolveWorkCenterCodeForCompany(
            String ruleSystemCode,
            String companyCode,
            LocalDate referenceDate,
            String currentWorkCenterCode,
            Random random
    ) {
        try {
            return resolveWorkCenterCodeForCompany(ruleSystemCode, companyCode, referenceDate, currentWorkCenterCode, random);
        } catch (IllegalStateException ex) {
            return null;
        }
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

    private ResolvedCompanyWorkCenter resolveCompanyAndWorkCenterFromPools(
            List<CatalogOption> companies,
            String ruleSystemCode,
            LocalDate referenceDate,
            Random random
    ) {
        if (companies == null || companies.isEmpty()) {
            throw new IllegalStateException("Cannot resolve company and work center from empty company pool");
        }

        int startIndex = random == null ? 0 : random.nextInt(companies.size());
        IllegalStateException lastError = null;

        for (int offset = 0; offset < companies.size(); offset++) {
            CatalogOption company = companies.get((startIndex + offset) % companies.size());
            try {
                String workCenterCode = resolveWorkCenterCodeForCompany(
                        ruleSystemCode,
                        company.code(),
                        referenceDate,
                        null,
                        random
                );
                return new ResolvedCompanyWorkCenter(company.code(), workCenterCode);
            } catch (IllegalStateException ex) {
                lastError = ex;
            }
        }

        throw new IllegalStateException(
                "No work centers available for any company in ruleSystemCode="
                        + normalizeCode(ruleSystemCode)
                        + ", referenceDate="
                        + referenceDate,
                lastError
        );
    }

    private static CatalogOption pickDifferentOrAny(List<CatalogOption> options, String currentCode, Random random) {
        if (options.size() == 1 || currentCode == null || currentCode.isBlank()) {
            return pick(options, random);
        }

        List<CatalogOption> filtered = options.stream()
                .filter(option -> !option.code().equalsIgnoreCase(currentCode))
                .toList();

        if (filtered.isEmpty()) {
            return pick(options, random);
        }

        return pick(filtered, random);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private record ResolvedCompanyWorkCenter(String companyCode, String workCenterCode) {
    }
}
