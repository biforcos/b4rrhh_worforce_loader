package com.b4rrhh.workforceloader.infrastructure.runner;

import com.b4rrhh.workforceloader.application.RunMassHireUseCase;
import com.b4rrhh.workforceloader.domain.model.LoaderRunSummary;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import com.b4rrhh.workforceloader.infrastructure.config.RunMode;
import com.b4rrhh.workforceloader.infrastructure.report.RunReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    private final LoaderProperties properties;
    private final RunMassHireUseCase runMassHireUseCase;
    private final RunReportWriter runReportWriter;

    public CliRunner(
            LoaderProperties properties,
            RunMassHireUseCase runMassHireUseCase,
            RunReportWriter runReportWriter
    ) {
        this.properties = properties;
        this.runMassHireUseCase = runMassHireUseCase;
        this.runReportWriter = runReportWriter;
    }

    @Override
    public void run(String... args) {
        if (properties.getRun().getMode() != RunMode.HIRE) {
            log.info("Run mode '{}' is not supported in V1. Nothing to execute.", properties.getRun().getMode());
            return;
        }

        log.info(
            "Starting mass hire run: count={}, dryRun={}",
            properties.getGeneration().getCount(),
            properties.getRun().isDryRun()
        );

        LoaderRunSummary summary = runMassHireUseCase.run();
        runReportWriter.printSummary(summary, properties.getRun().isDryRun());
    }
}
