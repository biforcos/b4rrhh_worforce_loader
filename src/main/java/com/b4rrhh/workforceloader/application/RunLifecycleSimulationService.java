package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.domain.model.LifecycleEventExecutionResult;
import com.b4rrhh.workforceloader.domain.model.LoaderRunSummary;
import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;
import com.b4rrhh.workforceloader.infrastructure.api.B4rrhhLifecycleClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import com.b4rrhh.workforceloader.infrastructure.generator.SyntheticEmployeeGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RunLifecycleSimulationService implements RunLifecycleSimulationUseCase {

    private final LoaderProperties properties;
    private final SyntheticEmployeeGenerator syntheticEmployeeGenerator;
    private final EmployeeLifecycleScenarioGenerator scenarioGenerator;
    private final B4rrhhLifecycleClient b4rrhhLifecycleClient;
    private final HireReferenceDataResolver hireReferenceDataResolver;

    public RunLifecycleSimulationService(
            LoaderProperties properties,
            SyntheticEmployeeGenerator syntheticEmployeeGenerator,
            EmployeeLifecycleScenarioGenerator scenarioGenerator,
            B4rrhhLifecycleClient b4rrhhLifecycleClient,
            HireReferenceDataResolver hireReferenceDataResolver
    ) {
        this.properties = properties;
        this.syntheticEmployeeGenerator = syntheticEmployeeGenerator;
        this.scenarioGenerator = scenarioGenerator;
        this.b4rrhhLifecycleClient = b4rrhhLifecycleClient;
        this.hireReferenceDataResolver = hireReferenceDataResolver;
    }

    @Override
    public LoaderRunSummary run() {
        validateConfiguration();

        List<SyntheticEmployee> employees = syntheticEmployeeGenerator.generateEmployees();
        List<EmployeeLifecycleScenario> scenarios = scenarioGenerator.generate(employees);

        String ruleSystemCode = normalizeCode(properties.getDefaults().getRuleSystemCode());
        ResolvedHireReferencePools referencePools = hireReferenceDataResolver.preloadPools(ruleSystemCode);
        Random referenceDataRandom = new Random(properties.getGeneration().getSeed() + 2);

        List<LifecycleEventExecutionResult> results = new ArrayList<>();

        int hiresRequested = 0;
        int hiresSuccess = 0;
        int hiresFailed = 0;

        int terminationsRequested = 0;
        int terminationsSuccess = 0;
        int terminationsFailed = 0;

        int rehiresRequested = 0;
        int rehiresSuccess = 0;
        int rehiresFailed = 0;

        for (EmployeeLifecycleScenario scenario : scenarios) {
            SyntheticEmployee employee = scenario.syntheticEmployee();
            // V1 now varies reference data by employee; a future iteration may also vary it per event.
            ResolvedHireData employeeResolvedHireData =
                hireReferenceDataResolver.resolveFromPools(referencePools, referenceDataRandom);
            String employeeExitReasonCode =
                hireReferenceDataResolver.resolveExitReasonFromPools(referencePools, referenceDataRandom);

            boolean scenarioCanContinue = true;

            for (EmployeeLifecycleEvent event : scenario.events()) {
                if (!scenarioCanContinue) {
                    break;
                }

                EventOutcome outcome;
                if (event.eventType() == LifecycleEventType.HIRE) {
                    hiresRequested++;
                    HireEmployeeRequest request = toHireRequest(employee, event, employeeResolvedHireData);
                    outcome = executeHire(request);
                    if (outcome.success()) {
                        hiresSuccess++;
                    } else {
                        hiresFailed++;
                        scenarioCanContinue = false;
                    }
                } else if (event.eventType() == LifecycleEventType.TERMINATE) {
                    terminationsRequested++;
                    TerminateEmployeeRequest request = toTerminateRequest(event, employeeExitReasonCode);
                    outcome = executeTerminate(employee, request);
                    if (outcome.success()) {
                        terminationsSuccess++;
                    } else {
                        terminationsFailed++;
                        scenarioCanContinue = false;
                    }
                } else {
                    rehiresRequested++;
                    RehireEmployeeRequest request = toRehireRequest(event, employeeResolvedHireData);
                    outcome = executeRehire(employee, request);
                    if (outcome.success()) {
                        rehiresSuccess++;
                    } else {
                        rehiresFailed++;
                        scenarioCanContinue = false;
                    }
                }

                results.add(new LifecycleEventExecutionResult(
                        employee.employeeNumber(),
                        event.eventType().name(),
                        event.effectiveDate(),
                        outcome.success(),
                        outcome.message()
                ));
            }
        }

        return new LoaderRunSummary(
                employees.size(),
                hiresRequested,
                hiresSuccess,
                hiresFailed,
                terminationsRequested,
                terminationsSuccess,
                terminationsFailed,
                rehiresRequested,
                rehiresSuccess,
                rehiresFailed,
                results
        );
    }

    private HireEmployeeRequest toHireRequest(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            ResolvedHireData resolvedHireData
    ) {
        return new HireEmployeeRequest(
                normalizeCode(employee.ruleSystemCode()),
                normalizeCode(employee.employeeTypeCode()),
                employee.employeeNumber(),
                employee.firstName(),
                employee.lastName1(),
                employee.lastName2(),
                employee.preferredName(),
                event.effectiveDate(),
                normalizeCode(resolvedHireData.entryReasonCode()),
                normalizeCode(resolvedHireData.companyCode()),
                normalizeCode(resolvedHireData.workCenterCode()),
                new HireEmployeeRequest.Contract(
                        normalizeCode(resolvedHireData.contractTypeCode()),
                        normalizeCode(resolvedHireData.contractSubtypeCode())
                ),
                new HireEmployeeRequest.LaborClassification(
                        normalizeCode(resolvedHireData.agreementCode()),
                        normalizeCode(resolvedHireData.agreementCategoryCode())
                ),
                buildHireCostCenterDistribution()
        );
    }

    private TerminateEmployeeRequest toTerminateRequest(EmployeeLifecycleEvent event, String exitReasonCode) {
        return new TerminateEmployeeRequest(
                event.effectiveDate(),
                normalizeCode(exitReasonCode)
        );
    }

    private RehireEmployeeRequest toRehireRequest(EmployeeLifecycleEvent event, ResolvedHireData resolvedHireData) {
        return new RehireEmployeeRequest(
                event.effectiveDate(),
                normalizeCode(resolvedHireData.entryReasonCode()),
                normalizeCode(resolvedHireData.companyCode()),
                new RehireEmployeeRequest.WorkCenter(normalizeCode(resolvedHireData.workCenterCode())),
                new RehireEmployeeRequest.Contract(
                        normalizeCode(resolvedHireData.contractTypeCode()),
                        normalizeCode(resolvedHireData.contractSubtypeCode())
                ),
                new RehireEmployeeRequest.LaborClassification(
                        normalizeCode(resolvedHireData.agreementCode()),
                        normalizeCode(resolvedHireData.agreementCategoryCode())
                ),
                buildRehireCostCenterDistribution()
        );
    }

    private EventOutcome executeHire(HireEmployeeRequest request) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeHirePayload(request));
        }

        try {
            HireEmployeeResponse response = b4rrhhLifecycleClient.hire(request);
            return EventOutcome.success(summarizeApiResponse("Hire", response.status(), response.message()));
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private EventOutcome executeTerminate(SyntheticEmployee employee, TerminateEmployeeRequest request) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeTerminatePayload(employee, request));
        }

        try {
            TerminateEmployeeResponse response = b4rrhhLifecycleClient.terminate(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success(summarizeApiResponse("Terminate", response.status(), response.message()));
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private EventOutcome executeRehire(SyntheticEmployee employee, RehireEmployeeRequest request) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeRehirePayload(employee, request));
        }

        try {
            RehireEmployeeResponse response = b4rrhhLifecycleClient.rehire(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success(summarizeApiResponse("Rehire", response.status(), response.message()));
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private HireEmployeeRequest.CostCenterDistribution buildHireCostCenterDistribution() {
        if (!properties.getCostCenter().isEnabled()) {
            return null;
        }

        List<HireEmployeeRequest.CostCenterDistribution.Item> items = properties.getCostCenter().getItems().stream()
                .map(item -> new HireEmployeeRequest.CostCenterDistribution.Item(
                        normalizeCode(item.getCostCenterCode()),
                        item.getAllocationPercentage()
                ))
                .toList();

        return new HireEmployeeRequest.CostCenterDistribution(items);
    }

    private RehireEmployeeRequest.CostCenterDistribution buildRehireCostCenterDistribution() {
        if (!properties.getCostCenter().isEnabled()) {
            return null;
        }

        List<RehireEmployeeRequest.CostCenterDistribution.Item> items = properties.getCostCenter().getItems().stream()
                .map(item -> new RehireEmployeeRequest.CostCenterDistribution.Item(
                        normalizeCode(item.getCostCenterCode()),
                        item.getAllocationPercentage()
                ))
                .toList();

        return new RehireEmployeeRequest.CostCenterDistribution(items);
    }

    private void validateConfiguration() {
        LoaderProperties.Generation generation = properties.getGeneration();
        if (generation.getHireDateFrom().isAfter(generation.getHireDateTo())) {
            throw new IllegalArgumentException("Invalid date range: loader.generation.hire-date-from must be <= hire-date-to");
        }

        LoaderProperties.Simulation simulation = properties.getSimulation();
        if (simulation.getTerminationMinDaysAfterHire() > simulation.getTerminationMaxDaysAfterHire()) {
            throw new IllegalArgumentException(
                    "Invalid simulation configuration: termination min days must be <= max days"
            );
        }
        if (simulation.getRehireMinDaysAfterTermination() > simulation.getRehireMaxDaysAfterTermination()) {
            throw new IllegalArgumentException(
                    "Invalid simulation configuration: rehire min days must be <= max days"
            );
        }

        LoaderProperties.CostCenter costCenter = properties.getCostCenter();
        if (costCenter.isEnabled()) {
            if (costCenter.getItems() == null || costCenter.getItems().isEmpty()) {
                throw new IllegalArgumentException("Invalid cost center configuration: items must not be empty when loader.cost-center.enabled=true");
            }
            int allocationSum = costCenter.getItems().stream()
                    .map(LoaderProperties.CostCenter.Item::getAllocationPercentage)
                    .mapToInt(Integer::intValue)
                    .sum();
            if (allocationSum > 100) {
                throw new IllegalArgumentException("Invalid cost center configuration: total allocationPercentage must be <= 100");
            }
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String summarizeHirePayload(HireEmployeeRequest request) {
        return "employeeNumber=" + request.employeeNumber()
                + ", ruleSystemCode=" + request.ruleSystemCode()
                + ", employeeTypeCode=" + request.employeeTypeCode()
                + ", hireDate=" + request.hireDate();
    }

    private static String summarizeTerminatePayload(SyntheticEmployee employee, TerminateEmployeeRequest request) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", terminationDate=" + request.terminationDate()
                + ", exitReasonCode=" + request.exitReasonCode();
    }

    private static String summarizeRehirePayload(SyntheticEmployee employee, RehireEmployeeRequest request) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", rehireDate=" + request.rehireDate();
    }

    private static String summarizeApiResponse(String operation, String status, String message) {
        String responseMessage = operation + " call completed";
        if (status != null && !status.isBlank()) {
            responseMessage = responseMessage + ": " + status;
        }
        if (message != null && !message.isBlank()) {
            responseMessage = responseMessage + " - " + message;
        }
        return responseMessage;
    }

    private record EventOutcome(boolean success, String message) {
        static EventOutcome success(String message) {
            return new EventOutcome(true, message);
        }

        static EventOutcome failure(String message) {
            return new EventOutcome(false, message);
        }
    }
}