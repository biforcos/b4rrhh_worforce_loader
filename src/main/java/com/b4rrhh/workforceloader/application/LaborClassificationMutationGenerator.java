package com.b4rrhh.workforceloader.application;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class LaborClassificationMutationGenerator {

    public LaborClassificationReplaceEventPayload generate(
            ResolvedHireReferencePools pools,
            EmployeeExecutionState state,
            Random random
    ) {
        ResolvedLaborClassificationData selected = selectMutation(pools, state.getCurrentLaborClassificationData(), random);
        return new LaborClassificationReplaceEventPayload(selected.agreementCode(), selected.agreementCategoryCode());
    }

    private ResolvedLaborClassificationData selectMutation(
            ResolvedHireReferencePools pools,
            ResolvedLaborClassificationData current,
            Random random
    ) {
        ResolvedLaborClassificationData lightChange = tryLightChange(pools, current, random);
        ResolvedLaborClassificationData strongChange = tryStrongChange(pools, current, random);

        if (lightChange != null && strongChange != null) {
            return random.nextDouble() < 0.75 ? lightChange : strongChange;
        }
        if (lightChange != null) {
            return lightChange;
        }
        if (strongChange != null) {
            return strongChange;
        }

        return fallbackSelection(pools, random);
    }

    private ResolvedLaborClassificationData tryLightChange(
            ResolvedHireReferencePools pools,
            ResolvedLaborClassificationData current,
            Random random
    ) {
        if (current == null) {
            return null;
        }

        AgreementWithCategories currentAgreement = pools.agreementsWithCategories().stream()
                .filter(agreement -> agreement.agreement().code().equalsIgnoreCase(current.agreementCode()))
                .findFirst()
                .orElse(null);

        if (currentAgreement == null) {
            return null;
        }

        var alternativeCategories = currentAgreement.categories().stream()
                .filter(category -> !category.code().equalsIgnoreCase(current.agreementCategoryCode()))
                .toList();

        if (alternativeCategories.isEmpty()) {
            return null;
        }

        return new ResolvedLaborClassificationData(
                currentAgreement.agreement().code(),
                RandomSelector.pickRandom(alternativeCategories, random).code()
        );
    }

    private ResolvedLaborClassificationData tryStrongChange(
            ResolvedHireReferencePools pools,
            ResolvedLaborClassificationData current,
            Random random
    ) {
        var alternativeAgreements = pools.agreementsWithCategories().stream()
                .filter(agreement -> current == null || !agreement.agreement().code().equalsIgnoreCase(current.agreementCode()))
                .toList();

        if (alternativeAgreements.isEmpty()) {
            return null;
        }

        AgreementWithCategories selectedAgreement = RandomSelector.pickRandom(alternativeAgreements, random);
        return new ResolvedLaborClassificationData(
                selectedAgreement.agreement().code(),
                RandomSelector.pickRandom(selectedAgreement.categories(), random).code()
        );
    }

    private ResolvedLaborClassificationData fallbackSelection(ResolvedHireReferencePools pools, Random random) {
        if (pools.agreementsWithCategories().size() == 1
                && pools.agreementsWithCategories().getFirst().categories().size() == 1) {
            AgreementWithCategories onlyAgreement = pools.agreementsWithCategories().getFirst();
            return new ResolvedLaborClassificationData(
                    onlyAgreement.agreement().code(),
                    onlyAgreement.categories().getFirst().code()
            );
        }

        AgreementWithCategories fallbackAgreement = RandomSelector.pickRandom(pools.agreementsWithCategories(), random);
        return new ResolvedLaborClassificationData(
                fallbackAgreement.agreement().code(),
                RandomSelector.pickRandom(fallbackAgreement.categories(), random).code()
        );
    }
}
