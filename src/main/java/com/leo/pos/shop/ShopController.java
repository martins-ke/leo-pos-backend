package com.leo.pos.shop;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/shops")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class ShopController {
	private  final ShopService shopService;
	
	
	public ShopController(ShopService shopService) {
		this.shopService = shopService;
	}


	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	ResponseEntity<ApiResponse<Shop>> register(
			@Valid @RequestBody ShopRequest req, @AuthenticationPrincipal User actor){
		return shopService.register(req, actor); 
	}
	
	@GetMapping
	ResponseEntity<ApiResponse<List<Shop>>> getAllShops(@AuthenticationPrincipal User actor){
		return shopService.getAllShops(actor); 
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	ResponseEntity<ApiResponse<Shop>> getShopById(@PathVariable String id, @AuthenticationPrincipal User actor){
		return shopService.getShopById(id, actor);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	ResponseEntity<ApiResponse<String>> deleteShop(@PathVariable String id, @AuthenticationPrincipal User actor){
		return shopService.deleteShop(id, actor);
	}
}
