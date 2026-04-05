package com.b4rrhh.workforceloader.infrastructure.api;

import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class B4rrhhLifecycleClient {

    private final LoaderProperties properties;
    private final WebClient webClient;

    public B4rrhhLifecycleClient(LoaderProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBackend().getBaseUrl())
                .build();
    }

    public HireEmployeeResponse hire(HireEmployeeRequest request) {
        try {
            HireEmployeeResponse response = webClient.post()
                    .uri(properties.getBackend().getHirePath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(HireEmployeeResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Backend hire call returned empty response body");
            }

            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "HTTP error during backend hire call: status=" + ex.getStatusCode() + ", body=" + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            throw new RuntimeException("Connection/runtime error during backend hire call: " + ex.getMessage(), ex);
        }
    }
}
