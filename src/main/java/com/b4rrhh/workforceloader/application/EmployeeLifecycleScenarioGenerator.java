package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class EmployeeLifecycleScenarioGenerator {

    private final LoaderProperties properties;

    public EmployeeLifecycleScenarioGenerator(LoaderProperties properties) {
        this.properties = properties;
    }

    public List<EmployeeLifecycleScenario> generate(List<SyntheticEmployee> employees) {
        LoaderProperties.Simulation simulation = properties.getSimulation();
        Random random = new Random(properties.getGeneration().getSeed() + 1);

        List<EmployeeLifecycleScenario> scenarios = new ArrayList<>(employees.size());
        for (SyntheticEmployee employee : employees) {
            List<EmployeeLifecycleEvent> events = new ArrayList<>();
            events.add(new EmployeeLifecycleEvent(LifecycleEventType.HIRE, employee.hireDate()));

            LocalDate terminationDate = null;
            if (random.nextDouble() < simulation.getTerminateRate()) {
                terminationDate = employee.hireDate().plusDays(randomDaysInRange(
                        simulation.getTerminationMinDaysAfterHire(),
                        simulation.getTerminationMaxDaysAfterHire(),
                        random
                ));
                events.add(new EmployeeLifecycleEvent(LifecycleEventType.TERMINATE, terminationDate));
            }

            if (terminationDate != null && random.nextDouble() < simulation.getRehireRateOfTerminated()) {
                LocalDate rehireDate = terminationDate.plusDays(randomDaysInRange(
                        simulation.getRehireMinDaysAfterTermination(),
                        simulation.getRehireMaxDaysAfterTermination(),
                        random
                ));
                events.add(new EmployeeLifecycleEvent(LifecycleEventType.REHIRE, rehireDate));
            }

            scenarios.add(new EmployeeLifecycleScenario(employee, events));
        }

        return scenarios;
    }

    private static int randomDaysInRange(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }
}