package com.b4rrhh.workforceloader.infrastructure.generator;

import com.b4rrhh.workforceloader.domain.model.SyntheticEmployee;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SyntheticEmployeeGenerator {

    private static final List<String> FIRST_NAMES = List.of(
            "Ana", "Luis", "Marta", "Carlos", "Elena", "David", "Lucia", "Pablo"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Garcia", "Fernandez", "Lopez", "Sanchez", "Martinez", "Gonzalez", "Ruiz", "Navarro"
    );

    private final LoaderProperties properties;

    public SyntheticEmployeeGenerator(LoaderProperties properties) {
        this.properties = properties;
    }

    public List<SyntheticEmployee> generateEmployees() {
        LoaderProperties.Defaults defaults = properties.getDefaults();
        LoaderProperties.Generation generation = properties.getGeneration();

        Random random = new Random(generation.getSeed());
        List<SyntheticEmployee> employees = new ArrayList<>(generation.getCount());

        for (int i = 1; i <= generation.getCount(); i++) {
            String firstName = pick(FIRST_NAMES, random);
            String lastName1 = pick(LAST_NAMES, random);
            String lastName2 = random.nextBoolean() ? pick(LAST_NAMES, random) : null;
            String employeeNumber = buildEmployeeNumber(generation, i);
            LocalDate hireDate = randomDateBetween(generation.getHireDateFrom(), generation.getHireDateTo(), random);

            employees.add(new SyntheticEmployee(
                    normalizeCode(defaults.getRuleSystemCode()),
                    normalizeCode(defaults.getEmployeeTypeCode()),
                    employeeNumber,
                    firstName,
                    lastName1,
                    lastName2,
                    firstName,
                    hireDate
            ));
        }

        return employees;
    }

    private static String pick(List<String> source, Random random) {
        return source.get(random.nextInt(source.size()));
    }

    private static String buildEmployeeNumber(LoaderProperties.Generation generation, int sequence) {
        String format = "%s%0" + generation.getEmployeeNumberPadding() + "d";
        return String.format(format, normalizeCode(generation.getEmployeeNumberPrefix()), sequence);
    }

    private static LocalDate randomDateBetween(LocalDate from, LocalDate to, Random random) {
        long fromEpochDay = from.toEpochDay();
        long toEpochDay = to.toEpochDay();
        long randomEpochDay = fromEpochDay + random.nextLong(toEpochDay - fromEpochDay + 1);
        return LocalDate.ofEpochDay(randomEpochDay);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
