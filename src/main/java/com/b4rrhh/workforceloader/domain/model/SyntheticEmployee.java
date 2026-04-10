package com.b4rrhh.workforceloader.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SyntheticEmployee(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        LocalDate hireDate,
        BigDecimal workingTimePercentage
) {
}
