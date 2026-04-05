package com.b4rrhh.workforceloader.domain.model;

import java.util.List;

public record LoaderRunSummary(
        int totalRequested,
        int totalSuccess,
        int totalFailed,
        List<HireExecutionResult> results
) {
}
