package com.b4rrhh.workforceloader.infrastructure.generator;

import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimePercentageResolverTest {

    private final WorkingTimePercentageResolver resolver = new WorkingTimePercentageResolver();

    @Test
    void shouldPreferExplicitWorkingTimePercentage() {
        LoaderProperties.Generation generation = new LoaderProperties.Generation();
        generation.setWorkingTimePercentage(new BigDecimal("87.5"));
        generation.setWeeklyHours(new BigDecimal("20"));

        BigDecimal result = resolver.resolve(generation);

        assertThat(result).isEqualByComparingTo("87.5");
    }

    @Test
    void shouldDeriveWorkingTimePercentageFromWeeklyHours() {
        LoaderProperties.Generation generation = new LoaderProperties.Generation();
        generation.setWeeklyHours(new BigDecimal("30"));

        BigDecimal result = resolver.resolve(generation);

        assertThat(result).isEqualByComparingTo("75");
    }

    @Test
    void shouldDeriveWorkingTimePercentageFromMonthlyHours() {
        LoaderProperties.Generation generation = new LoaderProperties.Generation();
        generation.setMonthlyHours(new BigDecimal("125"));

        BigDecimal result = resolver.resolve(generation);

        assertThat(result).isEqualByComparingTo("75");
    }

    @Test
    void shouldDeriveWorkingTimePercentageFromDailyHours() {
        LoaderProperties.Generation generation = new LoaderProperties.Generation();
        generation.setDailyHours(new BigDecimal("6"));

        BigDecimal result = resolver.resolve(generation);

        assertThat(result).isEqualByComparingTo("75");
    }

    @Test
    void shouldReturnNullWhenNoWorkingTimeSourceExists() {
        LoaderProperties.Generation generation = new LoaderProperties.Generation();

        BigDecimal result = resolver.resolve(generation);

        assertThat(result).isNull();
    }
}