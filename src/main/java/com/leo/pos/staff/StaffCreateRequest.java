package com.leo.pos.staff;

import jakarta.validation.constraints.NotBlank;

public record StaffCreateRequest(
		@NotBlank(message = "Username is required")
		String username,
		String email,
		@NotBlank(message = "Password is required")
		String password,
		String role,
		String status
) {}
