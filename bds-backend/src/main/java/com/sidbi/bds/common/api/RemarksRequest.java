package com.sidbi.bds.common.api;

import jakarta.validation.constraints.Size;

public record RemarksRequest(@Size(max = 1000) String remarks) {
}
