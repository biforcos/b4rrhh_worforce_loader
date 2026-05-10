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
    public SyntheticEmployee withEmployeeNumber(String number) {
        return new SyntheticEmployee(
                ruleSystemCode, employeeTypeCode, number,
                firstName, lastName1, lastName2, preferredName,
                hireDate, workingTimePercentage
        );
    }
}
