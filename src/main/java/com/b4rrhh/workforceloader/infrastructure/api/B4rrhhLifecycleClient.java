package com.b4rrhh.workforceloader.infrastructure.api;

import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.HireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.CreateWorkCenterRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.RehireEmployeeResponse;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceContractFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceCostCenterDistributionFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceLaborClassificationFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.ReplaceWorkCenterFromDateRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeRequest;
import com.b4rrhh.workforceloader.infrastructure.api.dto.TerminateEmployeeResponse;
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

    public TerminateEmployeeResponse terminate(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            TerminateEmployeeRequest request
    ) {
        try {
            TerminateEmployeeResponse response = webClient.post()
                    .uri("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/terminate",
                            ruleSystemCode,
                            employeeTypeCode,
                            employeeNumber)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TerminateEmployeeResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Backend terminate call returned empty response body");
            }

            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "HTTP error during backend terminate call: status=" + ex.getStatusCode() + ", body=" + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            throw new RuntimeException("Connection/runtime error during backend terminate call: " + ex.getMessage(), ex);
        }
    }

    public RehireEmployeeResponse rehire(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            RehireEmployeeRequest request
    ) {
        try {
            RehireEmployeeResponse response = webClient.post()
                    .uri("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/rehire",
                            ruleSystemCode,
                            employeeTypeCode,
                            employeeNumber)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RehireEmployeeResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Backend rehire call returned empty response body");
            }

            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "HTTP error during backend rehire call: status=" + ex.getStatusCode() + ", body=" + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            throw new RuntimeException("Connection/runtime error during backend rehire call: " + ex.getMessage(), ex);
        }
    }

            public void createWorkCenter(
                String ruleSystemCode,
                String employeeTypeCode,
                String employeeNumber,
                CreateWorkCenterRequest request
            ) {
            executePostWithoutResponse(
                "work center create",
                "/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/work-centers",
                request,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
            );
            }

            public void replaceWorkCenterFromDate(
                String ruleSystemCode,
                String employeeTypeCode,
                String employeeNumber,
                ReplaceWorkCenterFromDateRequest request
            ) {
            executePostWithoutResponse(
                "work center replace-from-date",
                "/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/work-centers/replace-from-date",
                request,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
            );
            }

            public void replaceContractFromDate(
                String ruleSystemCode,
                String employeeTypeCode,
                String employeeNumber,
                ReplaceContractFromDateRequest request
            ) {
            executePostWithoutResponse(
                "contract replace-from-date",
                "/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contracts/replace-from-date",
                request,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
            );
            }

            public void replaceLaborClassificationFromDate(
                String ruleSystemCode,
                String employeeTypeCode,
                String employeeNumber,
                ReplaceLaborClassificationFromDateRequest request
            ) {
            executePostWithoutResponse(
                "labor classification replace-from-date",
                "/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/labor-classifications/replace-from-date",
                request,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
            );
            }

            public void replaceCostCenterFromDate(
                String ruleSystemCode,
                String employeeTypeCode,
                String employeeNumber,
                ReplaceCostCenterDistributionFromDateRequest request
            ) {
            executePostWithoutResponse(
                "cost center replace-from-date",
                "/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/replace-from-date",
                request,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
            );
            }

            private void executePostWithoutResponse(String operation, String uri, Object body, Object... uriVariables) {
            try {
                webClient.post()
                    .uri(uri, uriVariables)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            } catch (WebClientResponseException ex) {
                throw new RuntimeException(
                    "HTTP error during " + operation + " call: status=" + ex.getStatusCode() + ", body=" + ex.getResponseBodyAsString(),
                    ex
                );
            } catch (Exception ex) {
                throw new RuntimeException("Connection/runtime error during " + operation + " call: " + ex.getMessage(), ex);
            }
            }
}
