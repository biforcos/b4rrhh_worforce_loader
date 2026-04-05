package com.b4rrhh.workforceloader.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeExecutionState {

    private boolean active;
    private String currentWorkCenterCode;
    private ResolvedContractData currentContractData;
    private ResolvedLaborClassificationData currentLaborClassificationData;
    private List<SimulationCostCenterAllocation> currentCostCenterDistribution;
    private LocalDate lastEffectiveDate;

    public static EmployeeExecutionState inactive() {
        return new EmployeeExecutionState();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCurrentWorkCenterCode() {
        return currentWorkCenterCode;
    }

    public void setCurrentWorkCenterCode(String currentWorkCenterCode) {
        this.currentWorkCenterCode = currentWorkCenterCode;
    }

    public ResolvedContractData getCurrentContractData() {
        return currentContractData;
    }

    public void setCurrentContractData(ResolvedContractData currentContractData) {
        this.currentContractData = currentContractData;
    }

    public ResolvedLaborClassificationData getCurrentLaborClassificationData() {
        return currentLaborClassificationData;
    }

    public void setCurrentLaborClassificationData(ResolvedLaborClassificationData currentLaborClassificationData) {
        this.currentLaborClassificationData = currentLaborClassificationData;
    }

    public List<SimulationCostCenterAllocation> getCurrentCostCenterDistribution() {
        return currentCostCenterDistribution;
    }

    public void setCurrentCostCenterDistribution(List<SimulationCostCenterAllocation> currentCostCenterDistribution) {
        if (currentCostCenterDistribution == null) {
            this.currentCostCenterDistribution = null;
            return;
        }
        this.currentCostCenterDistribution = new ArrayList<>(currentCostCenterDistribution);
    }

    public LocalDate getLastEffectiveDate() {
        return lastEffectiveDate;
    }

    public void setLastEffectiveDate(LocalDate lastEffectiveDate) {
        this.lastEffectiveDate = lastEffectiveDate;
    }
}
