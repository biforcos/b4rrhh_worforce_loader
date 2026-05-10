package com.b4rrhh.workforceloader.infrastructure.api.dto;

public record HireEmployeeResponse(
        String status,
        String message,
        String employeeNumber
) {
}
