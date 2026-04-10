package com.b4rrhh.workforceloader.infrastructure.generator;

import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class WorkingTimePercentageResolver {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal FULL_TIME_WEEKLY_HOURS = new BigDecimal("40");
    private static final BigDecimal FULL_TIME_MONTHLY_HOURS = new BigDecimal("166.6666666667");
    private static final BigDecimal FULL_TIME_DAILY_HOURS = new BigDecimal("8");

    BigDecimal resolve(LoaderProperties.Generation generation) {
        if (generation.getWorkingTimePercentage() != null) {
            return normalize(generation.getWorkingTimePercentage());
        }
        if (generation.getWeeklyHours() != null) {
            return fromHours(generation.getWeeklyHours(), FULL_TIME_WEEKLY_HOURS);
        }
        if (generation.getMonthlyHours() != null) {
            return fromHours(generation.getMonthlyHours(), FULL_TIME_MONTHLY_HOURS);
        }
        if (generation.getDailyHours() != null) {
            return fromHours(generation.getDailyHours(), FULL_TIME_DAILY_HOURS);
        }
        return null;
    }

    private static BigDecimal fromHours(BigDecimal hours, BigDecimal fullTimeHours) {
        BigDecimal percentage = hours
                .multiply(ONE_HUNDRED)
                .divide(fullTimeHours, 4, RoundingMode.HALF_UP);
        return normalize(percentage);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}