package com.b4rrhh.workforceloader.domain.model;

import java.util.List;

public record LoaderRunSummary(
        int totalEmployeesRequested,
        int hiresRequested,
        int hiresSuccess,
        int hiresFailed,
        int terminationsRequested,
        int terminationsSuccess,
        int terminationsFailed,
        int rehiresRequested,
        int rehiresSuccess,
        int rehiresFailed,
        List<LifecycleEventExecutionResult> results
) {
}
