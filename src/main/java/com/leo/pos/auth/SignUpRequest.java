package com.leo.pos.auth;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
		@NotBlank(message = "Username is required!")
		String username, 
		String email,
		@NotBlank(message = "Password is required!")
		String password, 
		@NotBlank(message = "Confirm password is required!")
		String confirm_password,
		String businessName,
		String businessLocation,
		String businessType
) {}
