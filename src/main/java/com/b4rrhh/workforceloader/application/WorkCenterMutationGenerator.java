package com.b4rrhh.workforceloader.application;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Component
public class WorkCenterMutationGenerator {

    private final HireReferenceDataResolver hireReferenceDataResolver;

    public WorkCenterMutationGenerator(HireReferenceDataResolver hireReferenceDataResolver) {
        this.hireReferenceDataResolver = hireReferenceDataResolver;
    }

    public Optional<WorkCenterChangeEventPayload> generate(
            String ruleSystemCode,
            EmployeeExecutionState state,
            LocalDate effectiveDate,
            Random random
    ) {
        String workCenterCode = hireReferenceDataResolver.tryResolveWorkCenterCodeForCompany(
                ruleSystemCode,
                state.getCurrentCompanyCode(),
                effectiveDate,
                state.getCurrentWorkCenterCode(),
                random
        );
        if (workCenterCode == null || workCenterCode.equalsIgnoreCase(state.getCurrentWorkCenterCode())) {
            return Optional.empty();
        }
        return Optional.of(new WorkCenterChangeEventPayload(workCenterCode));
    }
}
