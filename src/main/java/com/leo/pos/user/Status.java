package com.leo.pos.user;

public enum Status {
	ACTIVE,
	INACTIVE,
	DISABLED,
	PENDING;

	public static Status fromString(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (Status status : values()) {
			if (status.name().equalsIgnoreCase(value.trim())) {
				return status;
			}
		}
		return null;
	}
}
