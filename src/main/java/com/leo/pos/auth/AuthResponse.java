package com.leo.pos.auth;

import com.leo.pos.staff.User;

public record AuthResponse(
		User user,
		String token
) {}
