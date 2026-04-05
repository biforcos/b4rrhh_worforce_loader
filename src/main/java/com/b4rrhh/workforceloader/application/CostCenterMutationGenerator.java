package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Component
public class CostCenterMutationGenerator {

    private final LoaderProperties properties;

    public CostCenterMutationGenerator(LoaderProperties properties) {
        this.properties = properties;
    }

    public CostCenterReplaceEventPayload generate(EmployeeExecutionState state, Random random) {
        List<SimulationCostCenterAllocation> current = state.getCurrentCostCenterDistribution();
        List<SimulationCostCenterAllocation> candidate = generateEvolvedDistribution(current, random);

        if (current != null && !current.isEmpty()) {
            for (int attempt = 0; attempt < 4; attempt++) {
                if (!sameDistribution(candidate, current)) {
                    break;
                }
                candidate = generateEvolvedDistribution(current, random);
            }
        }

        return new CostCenterReplaceEventPayload(candidate);
    }

    public List<SimulationCostCenterAllocation> generateDistribution(Random random) {
        return generateEvolvedDistribution(null, random);
    }

    private List<SimulationCostCenterAllocation> generateEvolvedDistribution(
            List<SimulationCostCenterAllocation> current,
            Random random
    ) {
        if (!properties.getCostCenter().isEnabled() || properties.getCostCenter().getItems().isEmpty()) {
            return List.of();
        }

        List<String> availableCodes = properties.getCostCenter().getItems().stream()
                .map(LoaderProperties.CostCenter.Item::getCostCenterCode)
                .map(CostCenterMutationGenerator::normalizeCode)
                .distinct()
                .toList();

        if (current == null || current.isEmpty()) {
            return buildInitialDistribution(availableCodes, random);
        }

        if (current.size() == 1) {
            return evolveFromSingleCenter(current.getFirst(), availableCodes, random);
        }

        if (current.size() == 2) {
            return evolveFromSplit(current, availableCodes, random);
        }

        return buildInitialDistribution(availableCodes, random);
    }

    private List<SimulationCostCenterAllocation> buildInitialDistribution(List<String> availableCodes, Random random) {
        if (availableCodes.size() == 1 || random.nextDouble() < 0.65) {
            return List.of(singleAllocation(RandomSelector.pickRandom(availableCodes, random)));
        }

        String firstCode = RandomSelector.pickRandom(availableCodes, random);

        return splitAllocations(
                firstCode,
                pickDifferentCode(availableCodes, firstCode, random),
                pickSplitTemplate(random)
        );
    }

    private List<SimulationCostCenterAllocation> evolveFromSingleCenter(
            SimulationCostCenterAllocation current,
            List<String> availableCodes,
            Random random
    ) {
        String currentCode = normalizeCode(current.costCenterCode());

        if (availableCodes.size() == 1) {
            return List.of(singleAllocation(currentCode));
        }

        if (random.nextDouble() < 0.60) {
            return List.of(singleAllocation(pickDifferentCode(availableCodes, currentCode, random)));
        }

        return splitAllocations(currentCode, pickDifferentCode(availableCodes, currentCode, random), pickSplitTemplate(random));
    }

    private List<SimulationCostCenterAllocation> evolveFromSplit(
            List<SimulationCostCenterAllocation> current,
            List<String> availableCodes,
            Random random
    ) {
        String firstCode = normalizeCode(current.get(0).costCenterCode());
        String secondCode = normalizeCode(current.get(1).costCenterCode());

        if (random.nextDouble() < 0.45) {
            return List.of(singleAllocation(pickSingleTargetFromSplit(current, random)));
        }

        List<int[]> alternativeSplits = splitTemplates().stream()
                .filter(split -> !sameSplit(split, current))
                .toList();

        if (!alternativeSplits.isEmpty()) {
            int[] split = RandomSelector.pickRandom(alternativeSplits, random);
            if (random.nextDouble() < 0.20 && availableCodes.size() > 2) {
                String newCode = pickDifferentCode(availableCodes, firstCode, secondCode, random);
                return splitAllocations(firstCode, newCode, split);
            }
            return splitAllocations(firstCode, secondCode, split);
        }

        return List.of(singleAllocation(pickSingleTargetFromSplit(current, random)));
    }

    private static SimulationCostCenterAllocation singleAllocation(String costCenterCode) {
        return new SimulationCostCenterAllocation(normalizeCode(costCenterCode), 100);
    }

    private static List<SimulationCostCenterAllocation> splitAllocations(String firstCode, String secondCode, int[] split) {
        if (split[0] >= split[1]) {
            return List.of(
                    new SimulationCostCenterAllocation(normalizeCode(firstCode), split[0]),
                    new SimulationCostCenterAllocation(normalizeCode(secondCode), split[1])
            );
        }

        return List.of(
                new SimulationCostCenterAllocation(normalizeCode(secondCode), split[1]),
                new SimulationCostCenterAllocation(normalizeCode(firstCode), split[0])
        );
    }

    private static int[] pickSplitTemplate(Random random) {
        return RandomSelector.pickRandom(splitTemplates(), random);
    }

    private static List<int[]> splitTemplates() {
        return List.of(
                new int[]{50, 50},
                new int[]{60, 40},
                new int[]{70, 30}
        );
    }

    private static String pickSingleTargetFromSplit(List<SimulationCostCenterAllocation> current, Random random) {
        SimulationCostCenterAllocation dominant = current.get(0).allocationPercentage() >= current.get(1).allocationPercentage()
                ? current.get(0)
                : current.get(1);

        if (random.nextDouble() < 0.70) {
            return dominant.costCenterCode();
        }

        SimulationCostCenterAllocation other = dominant == current.get(0) ? current.get(1) : current.get(0);
        return other.costCenterCode();
    }

    private static boolean sameSplit(int[] split, List<SimulationCostCenterAllocation> current) {
        List<Integer> currentAllocations = current.stream()
                .map(SimulationCostCenterAllocation::allocationPercentage)
                .sorted(Comparator.reverseOrder())
                .toList();

        List<Integer> candidateAllocations = List.of(split[0], split[1]).stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        return currentAllocations.equals(candidateAllocations);
    }

    private static String pickDifferentCode(List<String> availableCodes, String excludedCode, Random random) {
        return pickDifferentCode(availableCodes, excludedCode, null, random);
    }

    private static String pickDifferentCode(
            List<String> availableCodes,
            String excludedCodeA,
            String excludedCodeB,
            Random random
    ) {
        List<String> filtered = availableCodes.stream()
                .filter(code -> excludedCodeA == null || !code.equalsIgnoreCase(excludedCodeA))
                .filter(code -> excludedCodeB == null || !code.equalsIgnoreCase(excludedCodeB))
                .toList();

        if (filtered.isEmpty()) {
            return RandomSelector.pickRandom(availableCodes, random);
        }

        return RandomSelector.pickRandom(filtered, random);
    }

    private static boolean sameDistribution(
            List<SimulationCostCenterAllocation> left,
            List<SimulationCostCenterAllocation> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        List<String> leftKeys = left.stream()
                .map(item -> item.costCenterCode() + ":" + item.allocationPercentage())
                .sorted(Comparator.naturalOrder())
                .toList();

        List<String> rightKeys = right.stream()
                .map(item -> item.costCenterCode() + ":" + item.allocationPercentage())
                .sorted(Comparator.naturalOrder())
                .toList();

        return leftKeys.equals(rightKeys);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
