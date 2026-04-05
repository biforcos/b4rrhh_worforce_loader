package com.b4rrhh.workforceloader.application;

public record LaborClassificationReplaceEventPayload(
        String agreementCode,
        String agreementCategoryCode
) implements MutationEventPayload {
}
