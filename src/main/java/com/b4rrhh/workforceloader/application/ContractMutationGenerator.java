package com.b4rrhh.workforceloader.application;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class ContractMutationGenerator {

    public ContractReplaceEventPayload generate(
            ResolvedHireReferencePools pools,
            EmployeeExecutionState state,
            Random random
    ) {
        ResolvedContractData selected = selectMutation(pools, state.getCurrentContractData(), random);
        return new ContractReplaceEventPayload(selected.contractCode(), selected.contractSubtypeCode());
    }

    private ResolvedContractData selectMutation(
            ResolvedHireReferencePools pools,
            ResolvedContractData current,
            Random random
    ) {
        ResolvedContractData lightChange = tryLightChange(pools, current, random);
        ResolvedContractData strongChange = tryStrongChange(pools, current, random);

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

    private ResolvedContractData tryLightChange(
            ResolvedHireReferencePools pools,
            ResolvedContractData current,
            Random random
    ) {
        if (current == null) {
            return null;
        }

        ContractTypeWithSubtypes currentType = pools.contractTypesWithSubtypes().stream()
                .filter(type -> type.contractType().code().equalsIgnoreCase(current.contractCode()))
                .findFirst()
                .orElse(null);

        if (currentType == null) {
            return null;
        }

        var alternativeSubtypes = currentType.subtypes().stream()
                .filter(subtype -> !subtype.code().equalsIgnoreCase(current.contractSubtypeCode()))
                .toList();

        if (alternativeSubtypes.isEmpty()) {
            return null;
        }

        return new ResolvedContractData(
                currentType.contractType().code(),
                RandomSelector.pickRandom(alternativeSubtypes, random).code()
        );
    }

    private ResolvedContractData tryStrongChange(
            ResolvedHireReferencePools pools,
            ResolvedContractData current,
            Random random
    ) {
        var alternativeTypes = pools.contractTypesWithSubtypes().stream()
                .filter(type -> current == null || !type.contractType().code().equalsIgnoreCase(current.contractCode()))
                .toList();

        if (alternativeTypes.isEmpty()) {
            return null;
        }

        ContractTypeWithSubtypes selectedType = RandomSelector.pickRandom(alternativeTypes, random);
        return new ResolvedContractData(
                selectedType.contractType().code(),
                RandomSelector.pickRandom(selectedType.subtypes(), random).code()
        );
    }

    private ResolvedContractData fallbackSelection(ResolvedHireReferencePools pools, Random random) {
        if (pools.contractTypesWithSubtypes().size() == 1
                && pools.contractTypesWithSubtypes().getFirst().subtypes().size() == 1) {
            ContractTypeWithSubtypes onlyType = pools.contractTypesWithSubtypes().getFirst();
            return new ResolvedContractData(onlyType.contractType().code(), onlyType.subtypes().getFirst().code());
        }

        ContractTypeWithSubtypes fallbackType = RandomSelector.pickRandom(pools.contractTypesWithSubtypes(), random);
        return new ResolvedContractData(
                fallbackType.contractType().code(),
                RandomSelector.pickRandom(fallbackType.subtypes(), random).code()
        );
    }
}
