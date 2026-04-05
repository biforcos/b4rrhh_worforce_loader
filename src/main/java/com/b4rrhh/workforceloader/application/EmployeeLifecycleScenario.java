package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;

import java.util.List;

public record EmployeeLifecycleScenario(
        SyntheticEmployee syntheticEmployee,
        List<EmployeeLifecycleEvent> events
) {
}