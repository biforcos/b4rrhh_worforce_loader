package com.b4rrhh.workforceloader.infrastructure.report;

import com.b4rrhh.workforceloader.domain.model.HireExecutionResult;
import com.b4rrhh.workforceloader.domain.model.LoaderRunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RunReportWriter {

    private static final Logger log = LoggerFactory.getLogger(RunReportWriter.class);

    public void printSummary(LoaderRunSummary summary, boolean dryRun) {
        log.info("==========================================");
        log.info("B4RRHH Workforce Loader Summary");
        log.info("Mode: {}", dryRun ? "DRY-RUN" : "LIVE");
        log.info("Total requested: {}", summary.totalRequested());
        log.info("Total success: {}", summary.totalSuccess());
        log.info("Total failed: {}", summary.totalFailed());

        if (dryRun) {
            log.info("Dry-run payload preview:");
            summary.results().stream().limit(5).forEach(result ->
                    log.info("  - {}", result.message())
            );
            if (summary.results().size() > 5) {
                log.info("  ... {} additional payloads", summary.results().size() - 5);
            }
        } else if (summary.totalFailed() == 0) {
            log.info("All hires completed successfully");
        } else if (summary.totalFailed() > 0) {
            log.info("Failed hires:");
            for (HireExecutionResult result : summary.results()) {
                if (!result.success()) {
                    log.info("  - employeeNumber={} error={}", result.employeeNumber(), result.message());
                }
            }
        }

        log.info("==========================================");
    }
}
