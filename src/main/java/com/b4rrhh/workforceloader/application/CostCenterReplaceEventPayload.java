package com.b4rrhh.workforceloader.application;

import java.util.List;

public record CostCenterReplaceEventPayload(
        List<SimulationCostCenterAllocation> allocations
) implements MutationEventPayload {
}
