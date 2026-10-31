package com.leo.pos.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "username/email required!")
		String username,  
		@NotBlank(message = "password required!")
		String password) {

}
