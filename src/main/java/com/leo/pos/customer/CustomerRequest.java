package com.leo.pos.customer;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
		@NotBlank String shopId,
		@NotBlank String name,
		String phone,
		String email
) {}
