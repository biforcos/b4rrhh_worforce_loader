package com.b4rrhh.workforceloader.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "loader")
public class LoaderProperties {

    @Valid
    @NotNull
    private Backend backend = new Backend();

    @Valid
    @NotNull
    private Run run = new Run();

    @Valid
    @NotNull
    private Defaults defaults = new Defaults();

    @Valid
    @NotNull
    private CostCenter costCenter = new CostCenter();

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = costCenter;
    }

    public static class Backend {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String hirePath;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getHirePath() {
            return hirePath;
        }

        public void setHirePath(String hirePath) {
            this.hirePath = hirePath;
        }
    }

    public static class Run {

        @NotNull
        private RunMode mode;

        @Min(1)
        private int count;

        private long seed;

        private boolean dryRun;

        public RunMode getMode() {
            return mode;
        }

        public void setMode(RunMode mode) {
            this.mode = mode;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public long getSeed() {
            return seed;
        }

        public void setSeed(long seed) {
            this.seed = seed;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }

    public static class Defaults {

        @NotBlank
        private String ruleSystemCode;

        @NotBlank
        private String employeeTypeCode;

        @NotBlank
        private String employeeNumberPrefix;

        @Min(1)
        private int employeeNumberPadding;

        @NotNull
        private LocalDate hireDateFrom;

        @NotNull
        private LocalDate hireDateTo;

        @NotBlank
        private String entryReasonCode;

        @NotBlank
        private String companyCode;

        @NotBlank
        private String workCenterCode;

        @NotBlank
        private String contractTypeCode;

        @NotBlank
        private String contractSubtypeCode;

        @NotBlank
        private String agreementCode;

        @NotBlank
        private String agreementCategoryCode;

        public String getRuleSystemCode() {
            return ruleSystemCode;
        }

        public void setRuleSystemCode(String ruleSystemCode) {
            this.ruleSystemCode = ruleSystemCode;
        }

        public String getEmployeeTypeCode() {
            return employeeTypeCode;
        }

        public void setEmployeeTypeCode(String employeeTypeCode) {
            this.employeeTypeCode = employeeTypeCode;
        }

        public String getEmployeeNumberPrefix() {
            return employeeNumberPrefix;
        }

        public void setEmployeeNumberPrefix(String employeeNumberPrefix) {
            this.employeeNumberPrefix = employeeNumberPrefix;
        }

        public int getEmployeeNumberPadding() {
            return employeeNumberPadding;
        }

        public void setEmployeeNumberPadding(int employeeNumberPadding) {
            this.employeeNumberPadding = employeeNumberPadding;
        }

        public LocalDate getHireDateFrom() {
            return hireDateFrom;
        }

        public void setHireDateFrom(LocalDate hireDateFrom) {
            this.hireDateFrom = hireDateFrom;
        }

        public LocalDate getHireDateTo() {
            return hireDateTo;
        }

        public void setHireDateTo(LocalDate hireDateTo) {
            this.hireDateTo = hireDateTo;
        }

        public String getEntryReasonCode() {
            return entryReasonCode;
        }

        public void setEntryReasonCode(String entryReasonCode) {
            this.entryReasonCode = entryReasonCode;
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public void setCompanyCode(String companyCode) {
            this.companyCode = companyCode;
        }

        public String getWorkCenterCode() {
            return workCenterCode;
        }

        public void setWorkCenterCode(String workCenterCode) {
            this.workCenterCode = workCenterCode;
        }

        public String getContractTypeCode() {
            return contractTypeCode;
        }

        public void setContractTypeCode(String contractTypeCode) {
            this.contractTypeCode = contractTypeCode;
        }

        public String getContractSubtypeCode() {
            return contractSubtypeCode;
        }

        public void setContractSubtypeCode(String contractSubtypeCode) {
            this.contractSubtypeCode = contractSubtypeCode;
        }

        public String getAgreementCode() {
            return agreementCode;
        }

        public void setAgreementCode(String agreementCode) {
            this.agreementCode = agreementCode;
        }

        public String getAgreementCategoryCode() {
            return agreementCategoryCode;
        }

        public void setAgreementCategoryCode(String agreementCategoryCode) {
            this.agreementCategoryCode = agreementCategoryCode;
        }
    }

    public static class CostCenter {

        private boolean enabled;

        @Valid
        private List<Item> items = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }

        public static class Item {

            @NotBlank
            private String costCenterCode;

            @NotNull
            @Min(1)
            private Integer allocationPercentage;

            public String getCostCenterCode() {
                return costCenterCode;
            }

            public void setCostCenterCode(String costCenterCode) {
                this.costCenterCode = costCenterCode;
            }

            public Integer getAllocationPercentage() {
                return allocationPercentage;
            }

            public void setAllocationPercentage(Integer allocationPercentage) {
                this.allocationPercentage = allocationPercentage;
            }
        }
    }
}
