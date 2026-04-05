package com.b4rrhh.workforceloader.application;

import com.b4rrhh.workforceloader.infrastructure.api.dto.CatalogOption;

import java.util.List;

public record AgreementWithCategories(
        CatalogOption agreement,
        List<CatalogOption> categories
) {
}