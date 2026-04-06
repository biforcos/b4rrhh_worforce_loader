package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.CatalogApiClient;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HireReferenceDataResolverTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void preloadPoolsUsesLegacyBindingReturnedByBackend() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        registerCatalogBindingsContexts(true);
        registerCatalogOptionsContext(Map.of(
                "EMPLOYEE_PRESENCE_COMPANY", directOptions("ES02", "Spain Company 02"),
                "WORK_CENTER", directOptions("MAD", "Madrid HQ"),
                "EMPLOYEE_PRESENCE_ENTRY_REASON", directOptions("HIRING", "Hiring"),
                "EMPLOYEE_PRESENCE_EXIT_REASON", directOptions("TERMINATION", "Termination"),
                "AGREEMENT", directOptions("AGR01", "General Agreement"),
                "CONTRACT", directOptions("PERM", "Permanent")
        ));
        registerAgreementCategoriesContext();
        registerContractSubtypesContext();
        server.start();

        HireReferenceDataResolver resolver = newResolver();

        ResolvedHireReferencePools pools = resolver.preloadPools("ESP");

        assertEquals(List.of(new CatalogOption("ES02", "Spain Company 02")), pools.companies());
        assertEquals(List.of(new CatalogOption("HIRING", "Hiring")), pools.entryReasons());
    }

    @Test
    void preloadPoolsFallsBackToCanonicalCodesWhenBindingsEndpointIsUnavailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        registerCatalogOptionsContext(Map.of(
                "COMPANY", directOptions("ES02", "Spain Company 02"),
                "WORK_CENTER", directOptions("MAD", "Madrid HQ"),
                "EMPLOYEE_PRESENCE_ENTRY_REASON", directOptions("HIRING", "Hiring"),
                "EMPLOYEE_PRESENCE_EXIT_REASON", directOptions("TERMINATION", "Termination"),
                "AGREEMENT", directOptions("AGR01", "General Agreement"),
                "CONTRACT", directOptions("PERM", "Permanent")
        ));
        registerAgreementCategoriesContext();
        registerContractSubtypesContext();
        server.start();

        HireReferenceDataResolver resolver = newResolver();

        ResolvedHireReferencePools pools = resolver.preloadPools("ESP");

        assertEquals(List.of(new CatalogOption("ES02", "Spain Company 02")), pools.companies());
        assertEquals(List.of(new CatalogOption("TERMINATION", "Termination")), pools.exitReasons());
    }

    private HireReferenceDataResolver newResolver() {
        LoaderProperties properties = new LoaderProperties();
        properties.getBackend().setBaseUrl("http://localhost:" + server.getAddress().getPort());
        CatalogApiClient catalogApiClient = new CatalogApiClient(properties, WebClient.builder());
        return new HireReferenceDataResolver(catalogApiClient);
    }

    private void registerCatalogBindingsContexts(boolean legacyCompanyBinding) {
        server.createContext("/catalog-bindings/employee.presence", exchange -> writeJson(
                exchange,
                200,
                """
                {
                  "resourceCode": "employee.presence",
                  "fields": [
                    {
                      "fieldCode": "companyCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "%s",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    },
                    {
                      "fieldCode": "entryReasonCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "EMPLOYEE_PRESENCE_ENTRY_REASON",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    },
                    {
                      "fieldCode": "exitReasonCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "EMPLOYEE_PRESENCE_EXIT_REASON",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    }
                  ]
                }
                """.formatted(legacyCompanyBinding ? "EMPLOYEE_PRESENCE_COMPANY" : "COMPANY")
        ));
        server.createContext("/catalog-bindings/employee.work_center", exchange -> writeJson(
                exchange,
                200,
                """
                {
                  "resourceCode": "employee.work_center",
                  "fields": [
                    {
                      "fieldCode": "workCenterCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "WORK_CENTER",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    }
                  ]
                }
                """
        ));
        server.createContext("/catalog-bindings/employee.contract", exchange -> writeJson(
                exchange,
                200,
                """
                {
                  "resourceCode": "employee.contract",
                  "fields": [
                    {
                      "fieldCode": "contractTypeCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "CONTRACT",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    }
                  ]
                }
                """
        ));
        server.createContext("/catalog-bindings/employee.labor_classification", exchange -> writeJson(
                exchange,
                200,
                """
                {
                  "resourceCode": "employee.labor_classification",
                  "fields": [
                    {
                      "fieldCode": "agreementCode",
                      "catalogKind": "DIRECT",
                      "ruleEntityTypeCode": "AGREEMENT",
                      "dependsOnFieldCode": null,
                      "customResolverCode": null,
                      "active": true
                    }
                  ]
                }
                """
        ));
    }

    private void registerCatalogOptionsContext(Map<String, String> responsesByEntityType) {
        server.createContext("/catalog-options/direct", exchange -> {
            String entityTypeCode = queryParams(exchange.getRequestURI()).get("ruleEntityTypeCode");
            String response = responsesByEntityType.getOrDefault(entityTypeCode, "{\"items\":[]}");
            writeJson(exchange, 200, response);
        });
    }

    private void registerAgreementCategoriesContext() {
        server.createContext("/labor-classification-catalog/agreement-categories", exchange -> writeJson(
                exchange,
                200,
                """
                [
                  {
                    "code": "CAT01",
                    "name": "Category 01",
                    "active": true
                  }
                ]
                """
        ));
    }

    private void registerContractSubtypesContext() {
        server.createContext("/contract-catalog/contract-subtypes", exchange -> writeJson(
                exchange,
                200,
                """
                [
                  {
                    "code": "SUB01",
                    "name": "Subtype 01",
                    "active": true
                  }
                ]
                """
        ));
    }

    private static String directOptions(String code, String name) {
        return """
                {
                  "items": [
                    {
                      "code": "%s",
                      "name": "%s",
                      "active": true
                    }
                  ]
                }
                """.formatted(code, name);
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return params;
        }

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }
}
