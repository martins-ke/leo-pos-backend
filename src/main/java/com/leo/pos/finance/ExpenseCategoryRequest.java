package com.leo.pos.finance;

import jakarta.validation.constraints.NotBlank;

public record ExpenseCategoryRequest(@NotBlank String shopId, @NotBlank String name) {}
