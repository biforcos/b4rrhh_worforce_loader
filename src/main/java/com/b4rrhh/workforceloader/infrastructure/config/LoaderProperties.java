package com.b4rrhh.workforceloader.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
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
    private Generation generation = new Generation();

    @Valid
    @NotNull
    private Defaults defaults = new Defaults();

    @Valid
    @NotNull
    private CostCenter costCenter = new CostCenter();

    @Valid
    @NotNull
    private Simulation simulation = new Simulation();

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

    public Generation getGeneration() {
        return generation;
    }

    public void setGeneration(Generation generation) {
        this.generation = generation;
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

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public static class Backend {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String hirePath = "/employees/hire";

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
        private RunMode mode = RunMode.LIFECYCLE;

        private boolean dryRun = false;

        public RunMode getMode() {
            return mode;
        }

        public void setMode(RunMode mode) {
            this.mode = mode;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }

    public static class Generation {

        @Min(1)
        private int count = 10;

        private long seed = 12345L;

        @NotBlank
        private String employeeNumberPrefix = "MAS";

        @Min(1)
        private int employeeNumberPadding = 6;

        @NotNull
        private LocalDate hireDateFrom = LocalDate.now().minusMonths(3);

        @NotNull
        private LocalDate hireDateTo = LocalDate.now();

        private BigDecimal workingTimePercentage;

        private BigDecimal weeklyHours;

        private BigDecimal monthlyHours;

        private BigDecimal dailyHours;

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

        public BigDecimal getWorkingTimePercentage() {
            return workingTimePercentage;
        }

        public void setWorkingTimePercentage(BigDecimal workingTimePercentage) {
            this.workingTimePercentage = workingTimePercentage;
        }

        public BigDecimal getWeeklyHours() {
            return weeklyHours;
        }

        public void setWeeklyHours(BigDecimal weeklyHours) {
            this.weeklyHours = weeklyHours;
        }

        public BigDecimal getMonthlyHours() {
            return monthlyHours;
        }

        public void setMonthlyHours(BigDecimal monthlyHours) {
            this.monthlyHours = monthlyHours;
        }

        public BigDecimal getDailyHours() {
            return dailyHours;
        }

        public void setDailyHours(BigDecimal dailyHours) {
            this.dailyHours = dailyHours;
        }
    }

    public static class Defaults {

        @NotBlank
        private String ruleSystemCode;

        @NotBlank
        private String employeeTypeCode;

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

    public static class Simulation {

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double terminateRate = 0.30;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double rehireRateOfTerminated = 0.40;

        @Min(1)
        private int terminationMinDaysAfterHire = 30;

        @Min(1)
        private int terminationMaxDaysAfterHire = 180;

        @Min(1)
        private int rehireMinDaysAfterTermination = 15;

        @Min(1)
        private int rehireMaxDaysAfterTermination = 120;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double workCenterChangeRate = 0.35;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double contractReplaceRate = 0.25;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double laborClassificationReplaceRate = 0.25;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double costCenterReplaceRate = 0.20;

        public double getTerminateRate() {
            return terminateRate;
        }

        public void setTerminateRate(double terminateRate) {
            this.terminateRate = terminateRate;
        }

        public double getRehireRateOfTerminated() {
            return rehireRateOfTerminated;
        }

        public void setRehireRateOfTerminated(double rehireRateOfTerminated) {
            this.rehireRateOfTerminated = rehireRateOfTerminated;
        }

        public int getTerminationMinDaysAfterHire() {
            return terminationMinDaysAfterHire;
        }

        public void setTerminationMinDaysAfterHire(int terminationMinDaysAfterHire) {
            this.terminationMinDaysAfterHire = terminationMinDaysAfterHire;
        }

        public int getTerminationMaxDaysAfterHire() {
            return terminationMaxDaysAfterHire;
        }

        public void setTerminationMaxDaysAfterHire(int terminationMaxDaysAfterHire) {
            this.terminationMaxDaysAfterHire = terminationMaxDaysAfterHire;
        }

        public int getRehireMinDaysAfterTermination() {
            return rehireMinDaysAfterTermination;
        }

        public void setRehireMinDaysAfterTermination(int rehireMinDaysAfterTermination) {
            this.rehireMinDaysAfterTermination = rehireMinDaysAfterTermination;
        }

        public int getRehireMaxDaysAfterTermination() {
            return rehireMaxDaysAfterTermination;
        }

        public void setRehireMaxDaysAfterTermination(int rehireMaxDaysAfterTermination) {
            this.rehireMaxDaysAfterTermination = rehireMaxDaysAfterTermination;
        }

        public double getWorkCenterChangeRate() {
            return workCenterChangeRate;
        }

        public void setWorkCenterChangeRate(double workCenterChangeRate) {
            this.workCenterChangeRate = workCenterChangeRate;
        }

        public double getContractReplaceRate() {
            return contractReplaceRate;
        }

        public void setContractReplaceRate(double contractReplaceRate) {
            this.contractReplaceRate = contractReplaceRate;
        }

        public double getLaborClassificationReplaceRate() {
            return laborClassificationReplaceRate;
        }

        public void setLaborClassificationReplaceRate(double laborClassificationReplaceRate) {
            this.laborClassificationReplaceRate = laborClassificationReplaceRate;
        }

        public double getCostCenterReplaceRate() {
            return costCenterReplaceRate;
        }

        public void setCostCenterReplaceRate(double costCenterReplaceRate) {
            this.costCenterReplaceRate = costCenterReplaceRate;
        }
    }
}
