package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class WorkCenterMutationGenerator {

    public WorkCenterChangeEventPayload generate(
            ResolvedHireReferencePools pools,
            EmployeeExecutionState state,
            Random random
    ) {
        CatalogOption selected = pickDifferentOrAny(pools.workCenters(), state.getCurrentWorkCenterCode(), random);
        return new WorkCenterChangeEventPayload(selected.code());
    }

    private CatalogOption pickDifferentOrAny(List<CatalogOption> options, String currentCode, Random random) {
        if (options.size() == 1 || currentCode == null || currentCode.isBlank()) {
            return RandomSelector.pickRandom(options, random);
        }

        List<CatalogOption> filtered = options.stream()
                .filter(option -> !option.code().equalsIgnoreCase(currentCode))
                .toList();

        if (filtered.isEmpty()) {
            return RandomSelector.pickRandom(options, random);
        }

        return RandomSelector.pickRandom(filtered, random);
    }
}
