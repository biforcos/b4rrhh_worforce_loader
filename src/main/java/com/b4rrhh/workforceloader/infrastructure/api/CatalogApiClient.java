package com.b4rrhh.workforceloader.infrastructure.api;

import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CatalogApiClient {

    private final WebClient webClient;
    private final Map<String, Map<String, CatalogFieldBinding>> bindingsCache = new ConcurrentHashMap<>();

    public CatalogApiClient(LoaderProperties properties, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(properties.getBackend().getBaseUrl())
                .build();
    }

    public List<CatalogOption> getDirectOptions(String ruleSystemCode, String entityTypeCode) {
        DirectCatalogOptionsResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/catalog-options/direct")
                        .queryParam("ruleSystemCode", normalize(ruleSystemCode))
                        .queryParam("ruleEntityTypeCode", normalize(entityTypeCode))
                        .build())
                .retrieve()
                .bodyToMono(DirectCatalogOptionsResponse.class)
                .block();

        List<RawCatalogItem> items = response == null ? List.of() : response.items();
        return mapDirectOptions(items, entityTypeCode);
    }

    public List<CatalogOption> getDirectOptionsForField(
            String ruleSystemCode,
            String resourceCode,
            String fieldCode,
            String... fallbackEntityTypeCodes
    ) {
        LinkedHashSet<String> candidateEntityTypeCodes = new LinkedHashSet<>();

        resolveBinding(resourceCode, fieldCode)
                .map(CatalogFieldBinding::ruleEntityTypeCode)
                .map(CatalogApiClient::normalize)
                .filter(value -> value != null && !value.isBlank())
                .ifPresent(candidateEntityTypeCodes::add);

        if (fallbackEntityTypeCodes != null) {
            for (String fallbackEntityTypeCode : fallbackEntityTypeCodes) {
                String normalizedFallback = normalize(fallbackEntityTypeCode);
                if (normalizedFallback != null && !normalizedFallback.isBlank()) {
                    candidateEntityTypeCodes.add(normalizedFallback);
                }
            }
        }

        IllegalStateException lastError = null;
        for (String candidateEntityTypeCode : candidateEntityTypeCodes) {
            try {
                return getDirectOptions(ruleSystemCode, candidateEntityTypeCode);
            } catch (IllegalStateException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        throw new IllegalStateException(
                "No catalog options found for resource=" + resourceCode + ", field=" + fieldCode
        );
    }

    public List<CatalogOption> getAgreementCategories(String ruleSystemCode, String agreementCode) {
        List<RawCatalogItem> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/labor-classification-catalog/agreement-categories")
                        .queryParam("ruleSystemCode", normalize(ruleSystemCode))
                        .queryParam("agreementCode", normalize(agreementCode))
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RawCatalogItem>>() { })
                .block();

        return mapDependentOptions(response, "AGREEMENT_CATEGORY");
    }

    public List<CatalogOption> getContractSubtypes(String ruleSystemCode, String contractTypeCode) {
        List<RawCatalogItem> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/contract-catalog/contract-subtypes")
                        .queryParam("ruleSystemCode", normalize(ruleSystemCode))
                        .queryParam("contractTypeCode", normalize(contractTypeCode))
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RawCatalogItem>>() { })
                .block();

        return mapDependentOptions(response, "CONTRACT_SUBTYPE");
    }

    public List<CatalogOption> getWorkCentersByCompany(
            String ruleSystemCode,
            String companyCode,
            LocalDate referenceDate
    ) {
        WorkCentersByCompanyResponse response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/catalog-options/work-centers-by-company")
                            .queryParam("ruleSystemCode", normalize(ruleSystemCode))
                            .queryParam("companyCode", normalize(companyCode));
                    if (referenceDate != null) {
                        builder.queryParam("referenceDate", referenceDate);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(WorkCentersByCompanyResponse.class)
                .block();

            List<WorkCenterByCompanyItem> items = response == null ? List.of() : response.items();
            return mapWorkCentersByCompany(items);
    }

    private List<CatalogOption> mapDirectOptions(List<RawCatalogItem> rawItems, String entityTypeCode) {
        List<CatalogOption> options = (rawItems == null ? List.<RawCatalogItem>of() : rawItems).stream()
                .filter(option -> option != null
                        && option.code() != null
                        && option.name() != null
                        && Boolean.TRUE.equals(option.active()))
                .map(option -> new CatalogOption(normalize(option.code()), option.name().trim()))
                .toList();

        if (options.isEmpty()) {
            throw new IllegalStateException("No catalog options found for " + entityTypeCode);
        }

        return options;
    }

    private List<CatalogOption> mapDependentOptions(List<RawCatalogItem> rawItems, String entityTypeCode) {
        List<CatalogOption> options = (rawItems == null ? List.<RawCatalogItem>of() : rawItems).stream()
                .filter(option -> option != null
                        && option.code() != null
                        && option.name() != null)
                .map(option -> new CatalogOption(normalize(option.code()), option.name().trim()))
                .toList();

        if (options.isEmpty()) {
            throw new IllegalStateException("No catalog options found for " + entityTypeCode);
        }

        return options;
    }

    private List<CatalogOption> mapWorkCentersByCompany(List<WorkCenterByCompanyItem> rawItems) {
        List<CatalogOption> options = (rawItems == null ? List.<WorkCenterByCompanyItem>of() : rawItems).stream()
                .filter(option -> option != null
                        && option.code() != null
                        && option.name() != null)
                .map(option -> new CatalogOption(normalize(option.code()), option.name().trim()))
                .toList();

        if (options.isEmpty()) {
            throw new IllegalStateException("No catalog options found for WORK_CENTER_BY_COMPANY");
        }

        return options;
    }

    private Optional<CatalogFieldBinding> resolveBinding(String resourceCode, String fieldCode) {
        String normalizedResourceCode = normalizeResourceCode(resourceCode);
        String normalizedFieldCode = normalize(fieldCode);
        if (normalizedResourceCode == null || normalizedFieldCode == null) {
            return Optional.empty();
        }

        Map<String, CatalogFieldBinding> bindingsByField = bindingsCache.computeIfAbsent(
                normalizedResourceCode,
                this::loadBindingsByResource
        );
        CatalogFieldBinding binding = bindingsByField.get(normalizedFieldCode);
        if (binding == null || !binding.active() || !"DIRECT".equals(normalize(binding.catalogKind()))) {
            return Optional.empty();
        }
        return Optional.of(binding);
    }

    private Map<String, CatalogFieldBinding> loadBindingsByResource(String resourceCode) {
        try {
            CatalogBindingsByResourceResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/catalog-bindings/{resourceCode}")
                            .build(resourceCode))
                    .retrieve()
                    .bodyToMono(CatalogBindingsByResourceResponse.class)
                    .block();

            List<CatalogFieldBinding> fields = response == null ? List.of() : response.fields();
            Map<String, CatalogFieldBinding> bindingsByField = new ConcurrentHashMap<>();
            for (CatalogFieldBinding field : fields) {
                if (field == null || field.fieldCode() == null) {
                    continue;
                }
                bindingsByField.put(normalize(field.fieldCode()), field);
            }
            return bindingsByField;
        } catch (WebClientResponseException.NotFound ex) {
            return Map.of();
        } catch (WebClientResponseException ex) {
            return Map.of();
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String normalizeResourceCode(String value) {
        return value == null ? null : value.trim();
    }

    private record DirectCatalogOptionsResponse(List<RawCatalogItem> items) {
    }

    private record RawCatalogItem(String code, String name, Boolean active) {
    }

    private record CatalogBindingsByResourceResponse(List<CatalogFieldBinding> fields) {
    }

        private record WorkCentersByCompanyResponse(
            String ruleSystemCode,
            String companyCode,
            LocalDate referenceDate,
                List<WorkCenterByCompanyItem> items
        ) {
        }

            private record WorkCenterByCompanyItem(String code, String name) {
            }

    private record CatalogFieldBinding(
            String fieldCode,
            String catalogKind,
            String ruleEntityTypeCode,
            boolean active
    ) {
    }
}
