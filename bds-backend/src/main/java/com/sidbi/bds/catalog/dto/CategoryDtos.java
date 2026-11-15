package com.sidbi.bds.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must contain upper case letters, digits or underscore") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String description,
            @Size(max = 60) String icon,
            @Min(0) @Max(999) Integer displayOrder,
            Boolean active,
            @Size(max = 20) List<String> mandatoryFields,
            @Size(max = 2000) String eligibilityRules
    ) {
    }

    public record SubcategoryRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String description,
            @Min(0) @Max(999) Integer displayOrder,
            Boolean active
    ) {
    }

    public record SubcategoryResponse(Long id, String name, String description, int displayOrder, boolean active, long listingCount) {
    }

    public record CategoryResponse(
            Long id,
            String code,
            String name,
            String description,
            String icon,
            int displayOrder,
            boolean active,
            List<String> mandatoryFields,
            String eligibilityRules,
            long listingCount,
            List<SubcategoryResponse> subcategories
    ) {
    }
}
