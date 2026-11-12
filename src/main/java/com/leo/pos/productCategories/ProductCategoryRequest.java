package com.leo.pos.productCategories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCategoryRequest(
		@NotBlank(message = "Category name is required.")
		@Size(max = 80, message = "Category name must be 80 characters or fewer.")
		String name
) {}
