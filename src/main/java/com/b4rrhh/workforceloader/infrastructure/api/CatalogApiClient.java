package com.b4rrhh.workforceloader.infrastructure.api;

import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CatalogApiClient {

    private final WebClient webClient;

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

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

        private record DirectCatalogOptionsResponse(List<RawCatalogItem> items) {
    }

        private record RawCatalogItem(String code, String name, Boolean active) {
    }
}
