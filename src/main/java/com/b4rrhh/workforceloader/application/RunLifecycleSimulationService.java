package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.domain.model.LifecycleEventExecutionResult;
import com.b4rrhh.workforceloader.domain.model.LoaderRunSummary;
import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;
import com.b4rrhh.workforceloader.infrastructure.api.B4rrhhLifecycleClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CreateWorkCenterRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceContractFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceCostCenterDistributionFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceLaborClassificationFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import com.b4rrhh.workforceloader.infrastructure.generator.SyntheticEmployeeGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RunLifecycleSimulationService implements RunLifecycleSimulationUseCase {

    private final LoaderProperties properties;
    private final SyntheticEmployeeGenerator syntheticEmployeeGenerator;
    private final EmployeeLifecycleScenarioGenerator scenarioGenerator;
    private final B4rrhhLifecycleClient b4rrhhLifecycleClient;
    private final CostCenterMutationGenerator costCenterMutationGenerator;

    public RunLifecycleSimulationService(
            LoaderProperties properties,
            SyntheticEmployeeGenerator syntheticEmployeeGenerator,
            EmployeeLifecycleScenarioGenerator scenarioGenerator,
            B4rrhhLifecycleClient b4rrhhLifecycleClient,
            CostCenterMutationGenerator costCenterMutationGenerator
    ) {
        this.properties = properties;
        this.syntheticEmployeeGenerator = syntheticEmployeeGenerator;
        this.scenarioGenerator = scenarioGenerator;
        this.b4rrhhLifecycleClient = b4rrhhLifecycleClient;
        this.costCenterMutationGenerator = costCenterMutationGenerator;
    }

    @Override
    public LoaderRunSummary run() {
        validateConfiguration();

        List<SyntheticEmployee> employees = syntheticEmployeeGenerator.generateEmployees();
        List<EmployeeLifecycleScenario> scenarios = scenarioGenerator.generate(employees);

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

        int workCenterChangesRequested = 0;
        int workCenterChangesSuccess = 0;
        int workCenterChangesFailed = 0;

        int contractReplacementsRequested = 0;
        int contractReplacementsSuccess = 0;
        int contractReplacementsFailed = 0;

        int laborClassificationReplacementsRequested = 0;
        int laborClassificationReplacementsSuccess = 0;
        int laborClassificationReplacementsFailed = 0;

        int costCenterReplacementsRequested = 0;
        int costCenterReplacementsSuccess = 0;
        int costCenterReplacementsFailed = 0;

        for (EmployeeLifecycleScenario scenario : scenarios) {
            SyntheticEmployee employee = scenario.syntheticEmployee();
            ResolvedHireData employeeResolvedHireData = scenario.resolvedHireData();
            ResolvedHireData employeeRehireResolvedHireData = scenario.rehireResolvedHireData();
            String employeeExitReasonCode = scenario.exitReasonCode();
            EmployeeExecutionState executionState = EmployeeExecutionState.inactive();

            boolean scenarioCanContinue = true;

            for (EmployeeLifecycleEvent event : scenario.events()) {
                if (!scenarioCanContinue) {
                    break;
                }

                EventOutcome outcome = EventOutcome.failure("Unsupported lifecycle event: " + event.eventType());
                switch (event.eventType()) {
                    case HIRE -> {
                        hiresRequested++;
                        HireEmployeeRequest request = toHireRequest(employee, event, employeeResolvedHireData);
                        outcome = executeHire(request);
                        if (outcome.success()) {
                            hiresSuccess++;
                            applyInitialHireState(executionState, employeeResolvedHireData, event.effectiveDate(), request.costCenterDistribution());
                        } else {
                            hiresFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case TERMINATE -> {
                        terminationsRequested++;
                        TerminateEmployeeRequest request = toTerminateRequest(event, employeeExitReasonCode);
                        outcome = executeTerminate(employee, request);
                        if (outcome.success()) {
                            terminationsSuccess++;
                            applyTerminationState(executionState, event.effectiveDate());
                        } else {
                            terminationsFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case REHIRE -> {
                        rehiresRequested++;
                        RehireEmployeeRequest request = toRehireRequest(employee, event, employeeRehireResolvedHireData);
                        outcome = executeRehire(employee, request);
                        if (outcome.success()) {
                            rehiresSuccess++;
                            applyRehireState(executionState, employeeRehireResolvedHireData, event.effectiveDate(), request.costCenterDistribution());
                        } else {
                            rehiresFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case CHANGE_WORK_CENTER -> {
                        workCenterChangesRequested++;
                        outcome = executePlannedWorkCenterChange(employee, event, executionState);
                        if (outcome.success()) {
                            workCenterChangesSuccess++;
                        } else {
                            workCenterChangesFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case REPLACE_CONTRACT -> {
                        contractReplacementsRequested++;
                        outcome = executePlannedContractReplace(employee, event, executionState);
                        if (outcome.success()) {
                            contractReplacementsSuccess++;
                        } else {
                            contractReplacementsFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case REPLACE_LABOR_CLASSIFICATION -> {
                        laborClassificationReplacementsRequested++;
                        outcome = executePlannedLaborClassificationReplace(employee, event, executionState);
                        if (outcome.success()) {
                            laborClassificationReplacementsSuccess++;
                        } else {
                            laborClassificationReplacementsFailed++;
                            scenarioCanContinue = false;
                        }
                    }
                    case REPLACE_COST_CENTER -> {
                        costCenterReplacementsRequested++;
                        outcome = executePlannedCostCenterReplace(employee, event, executionState);
                        if (outcome.success()) {
                            costCenterReplacementsSuccess++;
                        } else {
                            costCenterReplacementsFailed++;
                            scenarioCanContinue = false;
                        }
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
                workCenterChangesRequested,
                workCenterChangesSuccess,
                workCenterChangesFailed,
                contractReplacementsRequested,
                contractReplacementsSuccess,
                contractReplacementsFailed,
                laborClassificationReplacementsRequested,
                laborClassificationReplacementsSuccess,
                laborClassificationReplacementsFailed,
                costCenterReplacementsRequested,
                costCenterReplacementsSuccess,
                costCenterReplacementsFailed,
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
                buildHireCostCenterDistribution(employee)
        );
    }

    private TerminateEmployeeRequest toTerminateRequest(EmployeeLifecycleEvent event, String exitReasonCode) {
        return new TerminateEmployeeRequest(
                event.effectiveDate(),
                normalizeCode(exitReasonCode)
        );
    }

    private RehireEmployeeRequest toRehireRequest(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            ResolvedHireData resolvedHireData
    ) {
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
                buildRehireCostCenterDistribution(employee)
        );
    }

    private CreateWorkCenterRequest toCreateWorkCenterRequest(
            EmployeeLifecycleEvent event,
            WorkCenterChangeEventPayload payload
    ) {
        return new CreateWorkCenterRequest(
                normalizeCode(payload.workCenterCode()),
                event.effectiveDate(),
                null
        );
    }

    private ReplaceContractFromDateRequest toReplaceContractRequest(
            EmployeeLifecycleEvent event,
            ContractReplaceEventPayload payload
    ) {
        return new ReplaceContractFromDateRequest(
                event.effectiveDate(),
                normalizeCode(payload.contractCode()),
                normalizeCode(payload.contractSubtypeCode())
        );
    }

    private ReplaceLaborClassificationFromDateRequest toReplaceLaborClassificationRequest(
            EmployeeLifecycleEvent event,
            LaborClassificationReplaceEventPayload payload
    ) {
        return new ReplaceLaborClassificationFromDateRequest(
                event.effectiveDate(),
                normalizeCode(payload.agreementCode()),
                normalizeCode(payload.agreementCategoryCode())
        );
    }

    private ReplaceCostCenterDistributionFromDateRequest toReplaceCostCenterRequest(
            EmployeeLifecycleEvent event,
            CostCenterReplaceEventPayload payload
    ) {
        return new ReplaceCostCenterDistributionFromDateRequest(
                event.effectiveDate(),
                toApiReplaceItems(payload.allocations())
        );
    }

    private EventOutcome executePlannedWorkCenterChange(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            EmployeeExecutionState state
    ) {
        if (!state.isActive()) {
            return EventOutcome.failure("Cannot change work center on inactive employee state");
        }
        if (!(event.payload() instanceof WorkCenterChangeEventPayload payload)) {
            return EventOutcome.failure("Missing WorkCenterChangeEventPayload for CHANGE_WORK_CENTER");
        }

        CreateWorkCenterRequest request = toCreateWorkCenterRequest(event, payload);
        EventOutcome outcome = executeWorkCenterChange(employee, request);
        if (outcome.success()) {
            state.setCurrentWorkCenterCode(payload.workCenterCode());
            state.setLastEffectiveDate(event.effectiveDate());
        }
        return outcome;
    }

    private EventOutcome executePlannedContractReplace(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            EmployeeExecutionState state
    ) {
        if (!state.isActive()) {
            return EventOutcome.failure("Cannot replace contract on inactive employee state");
        }
        if (!(event.payload() instanceof ContractReplaceEventPayload payload)) {
            return EventOutcome.failure("Missing ContractReplaceEventPayload for REPLACE_CONTRACT");
        }

        ReplaceContractFromDateRequest request = toReplaceContractRequest(event, payload);
        EventOutcome outcome = executeContractReplace(employee, request);
        if (outcome.success()) {
            state.setCurrentContractData(new ResolvedContractData(payload.contractCode(), payload.contractSubtypeCode()));
            state.setLastEffectiveDate(event.effectiveDate());
        }
        return outcome;
    }

    private EventOutcome executePlannedLaborClassificationReplace(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            EmployeeExecutionState state
    ) {
        if (!state.isActive()) {
            return EventOutcome.failure("Cannot replace labor classification on inactive employee state");
        }
        if (!(event.payload() instanceof LaborClassificationReplaceEventPayload payload)) {
            return EventOutcome.failure("Missing LaborClassificationReplaceEventPayload for REPLACE_LABOR_CLASSIFICATION");
        }

        ReplaceLaborClassificationFromDateRequest request = toReplaceLaborClassificationRequest(event, payload);
        EventOutcome outcome = executeLaborClassificationReplace(employee, request);
        if (outcome.success()) {
            state.setCurrentLaborClassificationData(
                    new ResolvedLaborClassificationData(payload.agreementCode(), payload.agreementCategoryCode())
            );
            state.setLastEffectiveDate(event.effectiveDate());
        }
        return outcome;
    }

    private EventOutcome executePlannedCostCenterReplace(
            SyntheticEmployee employee,
            EmployeeLifecycleEvent event,
            EmployeeExecutionState state
    ) {
        if (!state.isActive()) {
            return EventOutcome.failure("Cannot replace cost center on inactive employee state");
        }
        if (!(event.payload() instanceof CostCenterReplaceEventPayload payload)) {
            return EventOutcome.failure("Missing CostCenterReplaceEventPayload for REPLACE_COST_CENTER");
        }

        ReplaceCostCenterDistributionFromDateRequest request = toReplaceCostCenterRequest(event, payload);
        EventOutcome outcome = executeCostCenterReplace(employee, request);
        if (outcome.success()) {
            state.setCurrentCostCenterDistribution(payload.allocations());
            state.setLastEffectiveDate(event.effectiveDate());
        }
        return outcome;
    }

    private void applyInitialHireState(
            EmployeeExecutionState state,
            ResolvedHireData resolvedHireData,
            LocalDate effectiveDate,
            HireEmployeeRequest.CostCenterDistribution costCenterDistribution
    ) {
        state.setActive(true);
        state.setCurrentWorkCenterCode(normalizeCode(resolvedHireData.workCenterCode()));
        state.setCurrentContractData(new ResolvedContractData(
                normalizeCode(resolvedHireData.contractTypeCode()),
                normalizeCode(resolvedHireData.contractSubtypeCode())
        ));
        state.setCurrentLaborClassificationData(new ResolvedLaborClassificationData(
                normalizeCode(resolvedHireData.agreementCode()),
                normalizeCode(resolvedHireData.agreementCategoryCode())
        ));
        state.setCurrentCostCenterDistribution(toSimulationAllocations(costCenterDistribution));
        state.setLastEffectiveDate(effectiveDate);
    }

    private void applyRehireState(
            EmployeeExecutionState state,
            ResolvedHireData resolvedHireData,
            LocalDate effectiveDate,
            RehireEmployeeRequest.CostCenterDistribution costCenterDistribution
    ) {
        state.setActive(true);
        state.setCurrentWorkCenterCode(normalizeCode(resolvedHireData.workCenterCode()));
        state.setCurrentContractData(new ResolvedContractData(
                normalizeCode(resolvedHireData.contractTypeCode()),
                normalizeCode(resolvedHireData.contractSubtypeCode())
        ));
        state.setCurrentLaborClassificationData(new ResolvedLaborClassificationData(
                normalizeCode(resolvedHireData.agreementCode()),
                normalizeCode(resolvedHireData.agreementCategoryCode())
        ));
        state.setCurrentCostCenterDistribution(toSimulationAllocations(costCenterDistribution));
        state.setLastEffectiveDate(effectiveDate);
    }

    private void applyTerminationState(EmployeeExecutionState state, LocalDate effectiveDate) {
        state.setActive(false);
        state.setLastEffectiveDate(effectiveDate);
    }

    private static List<SimulationCostCenterAllocation> toSimulationAllocations(
            HireEmployeeRequest.CostCenterDistribution distribution
    ) {
        if (distribution == null || distribution.items() == null) {
            return null;
        }
        return distribution.items().stream()
                .map(item -> new SimulationCostCenterAllocation(
                        normalizeCode(item.costCenterCode()),
                        item.allocationPercentage()
                ))
                .toList();
    }

    private static List<SimulationCostCenterAllocation> toSimulationAllocations(
            RehireEmployeeRequest.CostCenterDistribution distribution
    ) {
        if (distribution == null || distribution.items() == null) {
            return null;
        }
        return distribution.items().stream()
                .map(item -> new SimulationCostCenterAllocation(
                        normalizeCode(item.costCenterCode()),
                        item.allocationPercentage()
                ))
                .toList();
    }

    private static List<ReplaceCostCenterDistributionFromDateRequest.Item> toApiReplaceItems(
            List<SimulationCostCenterAllocation> allocations
    ) {
        if (allocations == null) {
            return List.of();
        }
        return allocations.stream()
                .map(allocation -> new ReplaceCostCenterDistributionFromDateRequest.Item(
                        normalizeCode(allocation.costCenterCode()),
                        allocation.allocationPercentage()
                ))
                .toList();
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

    private EventOutcome executeWorkCenterChange(SyntheticEmployee employee, CreateWorkCenterRequest request) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeWorkCenterChangePayload(employee, request));
        }

        try {
            b4rrhhLifecycleClient.createWorkCenter(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success("Work center create call completed");
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private EventOutcome executeContractReplace(SyntheticEmployee employee, ReplaceContractFromDateRequest request) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeContractReplacePayload(employee, request));
        }

        try {
            b4rrhhLifecycleClient.replaceContractFromDate(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success("Contract replace-from-date call completed");
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private EventOutcome executeLaborClassificationReplace(
            SyntheticEmployee employee,
            ReplaceLaborClassificationFromDateRequest request
    ) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeLaborClassificationReplacePayload(employee, request));
        }

        try {
            b4rrhhLifecycleClient.replaceLaborClassificationFromDate(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success("Labor classification replace-from-date call completed");
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private EventOutcome executeCostCenterReplace(
            SyntheticEmployee employee,
            ReplaceCostCenterDistributionFromDateRequest request
    ) {
        if (properties.getRun().isDryRun()) {
            return EventOutcome.success("DRY-RUN payload -> " + summarizeCostCenterReplacePayload(employee, request));
        }

        try {
            b4rrhhLifecycleClient.replaceCostCenterFromDate(
                    normalizeCode(employee.ruleSystemCode()),
                    normalizeCode(employee.employeeTypeCode()),
                    employee.employeeNumber(),
                    request
            );
            return EventOutcome.success("Cost center replace-from-date call completed");
        } catch (Exception ex) {
            return EventOutcome.failure(ex.getMessage());
        }
    }

    private HireEmployeeRequest.CostCenterDistribution buildHireCostCenterDistribution(SyntheticEmployee employee) {
        if (!properties.getCostCenter().isEnabled()) {
            return null;
        }

        Random random = deterministicEmployeeRandom(employee, 11);
        List<HireEmployeeRequest.CostCenterDistribution.Item> items = costCenterMutationGenerator.generateDistribution(random).stream()
                .map(item -> new HireEmployeeRequest.CostCenterDistribution.Item(
                normalizeCode(item.costCenterCode()),
                item.allocationPercentage()
                ))
                .toList();

        return new HireEmployeeRequest.CostCenterDistribution(items);
    }

    private RehireEmployeeRequest.CostCenterDistribution buildRehireCostCenterDistribution(SyntheticEmployee employee) {
        if (!properties.getCostCenter().isEnabled()) {
            return null;
        }

        Random random = deterministicEmployeeRandom(employee, 23);
        List<RehireEmployeeRequest.CostCenterDistribution.Item> items = costCenterMutationGenerator.generateDistribution(random).stream()
                .map(item -> new RehireEmployeeRequest.CostCenterDistribution.Item(
                normalizeCode(item.costCenterCode()),
                item.allocationPercentage()
                ))
                .toList();

        return new RehireEmployeeRequest.CostCenterDistribution(items);
    }

    private Random deterministicEmployeeRandom(SyntheticEmployee employee, int salt) {
        long seed = properties.getGeneration().getSeed();
        long employeeHash = employee.employeeNumber() == null ? 0L : employee.employeeNumber().hashCode();
        return new Random(seed + employeeHash + salt);
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

    private static String summarizeWorkCenterChangePayload(SyntheticEmployee employee, CreateWorkCenterRequest request) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", workCenterCode=" + request.workCenterCode()
                + ", startDate=" + request.startDate();
    }

    private static String summarizeContractReplacePayload(SyntheticEmployee employee, ReplaceContractFromDateRequest request) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", effectiveDate=" + request.effectiveDate()
                + ", contractCode=" + request.contractCode()
                + ", contractSubtypeCode=" + request.contractSubtypeCode();
    }

    private static String summarizeLaborClassificationReplacePayload(
            SyntheticEmployee employee,
            ReplaceLaborClassificationFromDateRequest request
    ) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", effectiveDate=" + request.effectiveDate()
                + ", agreementCode=" + request.agreementCode()
                + ", agreementCategoryCode=" + request.agreementCategoryCode();
    }

    private static String summarizeCostCenterReplacePayload(
            SyntheticEmployee employee,
            ReplaceCostCenterDistributionFromDateRequest request
    ) {
        return "employeeNumber=" + employee.employeeNumber()
                + ", ruleSystemCode=" + normalizeCode(employee.ruleSystemCode())
                + ", employeeTypeCode=" + normalizeCode(employee.employeeTypeCode())
                + ", effectiveDate=" + request.effectiveDate()
                + ", items=" + request.items().size();
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