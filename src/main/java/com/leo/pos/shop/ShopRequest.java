package com.leo.pos.shop;

import jakarta.validation.constraints.NotBlank;

public record ShopRequest(
		@NotBlank(message = "Shop name required!")
		String name,
		String type,
		String location,
		String description
		) {

}
