package com.b4rrhh.workforceloader.application;

import java.time.LocalDate;

public record ActiveWindow(
        LocalDate startDate,
        LocalDate endDate
) {
}