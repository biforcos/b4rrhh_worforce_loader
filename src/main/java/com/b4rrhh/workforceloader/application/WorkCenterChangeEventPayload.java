package com.b4rrhh.workforceloader.application;

public record WorkCenterChangeEventPayload(
        String workCenterCode
) implements MutationEventPayload {
}
