package com.sidbi.bds.common.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequiredRemarksRequest(@NotBlank @Size(max = 1000) String remarks) {
}
