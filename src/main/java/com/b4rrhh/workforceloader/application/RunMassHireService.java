package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.domain.model.HireExecutionResult;
import com.b4rrhh.workforceloader.domain.model.LoaderRunSummary;
import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;
import com.b4rrhh.workforceloader.infrastructure.api.B4rrhhLifecycleClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import com.b4rrhh.workforceloader.infrastructure.generator.SyntheticEmployeeGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RunMassHireService implements RunMassHireUseCase {

    private final LoaderProperties properties;
    private final SyntheticEmployeeGenerator syntheticEmployeeGenerator;
    private final B4rrhhLifecycleClient b4rrhhLifecycleClient;
    private final HireReferenceDataResolver hireReferenceDataResolver;

    public RunMassHireService(
            LoaderProperties properties,
            SyntheticEmployeeGenerator syntheticEmployeeGenerator,
            B4rrhhLifecycleClient b4rrhhLifecycleClient,
            HireReferenceDataResolver hireReferenceDataResolver
    ) {
        this.properties = properties;
        this.syntheticEmployeeGenerator = syntheticEmployeeGenerator;
        this.b4rrhhLifecycleClient = b4rrhhLifecycleClient;
        this.hireReferenceDataResolver = hireReferenceDataResolver;
    }

    @Override
    public LoaderRunSummary run() {
        validateConfiguration();

        List<SyntheticEmployee> employees = syntheticEmployeeGenerator.generateEmployees();
        String ruleSystemCode = normalizeCode(properties.getDefaults().getRuleSystemCode());
        // Current behavior resolves one reference set per run; future evolution may resolve per employee.
        ResolvedHireData resolvedHireData = hireReferenceDataResolver.resolve(ruleSystemCode);
        List<HireExecutionResult> results = new ArrayList<>(employees.size());

        int totalSuccess = 0;
        int totalFailed = 0;

        for (SyntheticEmployee employee : employees) {
            HireEmployeeRequest request = toRequest(employee, resolvedHireData);

            if (properties.getRun().isDryRun()) {
                results.add(new HireExecutionResult(
                        employee.employeeNumber(),
                        true,
                        "DRY-RUN payload -> " + summarizePayload(request)
                ));
                totalSuccess++;
                continue;
            }

            try {
                HireEmployeeResponse response = b4rrhhLifecycleClient.hire(request);
                String responseMessage = "Hire call completed";
                if (response.status() != null && !response.status().isBlank()) {
                    responseMessage = "Hire call completed: " + response.status();
                    if (response.message() != null && !response.message().isBlank()) {
                        responseMessage = responseMessage + " - " + response.message();
                    }
                }
                results.add(new HireExecutionResult(employee.employeeNumber(), true, responseMessage));
                totalSuccess++;
            } catch (Exception ex) {
                results.add(new HireExecutionResult(employee.employeeNumber(), false, ex.getMessage()));
                totalFailed++;
            }
        }

        return new LoaderRunSummary(employees.size(), totalSuccess, totalFailed, results);
    }

    private HireEmployeeRequest toRequest(SyntheticEmployee employee, ResolvedHireData resolvedHireData) {
        HireEmployeeRequest.CostCenterDistribution distribution = null;
        if (properties.getCostCenter().isEnabled()) {
            List<HireEmployeeRequest.CostCenterDistribution.Item> items = properties.getCostCenter().getItems().stream()
                    .map(item -> new HireEmployeeRequest.CostCenterDistribution.Item(
                            normalizeCode(item.getCostCenterCode()),
                            item.getAllocationPercentage()
                    ))
                    .toList();
            distribution = new HireEmployeeRequest.CostCenterDistribution(items);
        }

        return new HireEmployeeRequest(
                normalizeCode(employee.ruleSystemCode()),
                normalizeCode(employee.employeeTypeCode()),
                employee.employeeNumber(),
                employee.firstName(),
                employee.lastName1(),
                employee.lastName2(),
                employee.preferredName(),
                employee.hireDate(),
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
                distribution
        );
    }

    private void validateConfiguration() {
        LoaderProperties.Generation generation = properties.getGeneration();
        if (generation.getHireDateFrom().isAfter(generation.getHireDateTo())) {
            throw new IllegalArgumentException("Invalid date range: loader.generation.hire-date-from must be <= hire-date-to");
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

    private static String summarizePayload(HireEmployeeRequest request) {
        return "employeeNumber=" + request.employeeNumber()
                + ", ruleSystemCode=" + request.ruleSystemCode()
                + ", employeeTypeCode=" + request.employeeTypeCode()
                + ", hireDate=" + request.hireDate();
    }
}
