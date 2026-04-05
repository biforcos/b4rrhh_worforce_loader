package com.b4rrhh.workforceloader.application;

public record ContractReplaceEventPayload(
        String contractCode,
        String contractSubtypeCode
) implements MutationEventPayload {
}
