package com.leo.pos.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

@RestController
@RequestMapping("/admin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

	private final SuperAdminService adminService;

	public SuperAdminController(SuperAdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/stats")
	public ResponseEntity<ApiResponse<SuperAdminStatsDto>> getPlatformStats(@AuthenticationPrincipal User actor) {
		requirePlatformAdmin(actor);
		return ResponseEntity.ok(new ApiResponse<>(adminService.getStats(), "Platform metrics loaded", true, Instant.now()));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TenantSummaryDto>>> getAllTenants(@AuthenticationPrincipal User actor) {
		requirePlatformAdmin(actor);
		return ResponseEntity.ok(new ApiResponse<>(adminService.getAllTenants(), "Tenants loaded", true, Instant.now()));
	}

	@PatchMapping("/{ownerId}/toggle-status")
	public ResponseEntity<ApiResponse<User>> toggleTenantStatus(
			@PathVariable String ownerId,
			@AuthenticationPrincipal User actor) {
		requirePlatformAdmin(actor);
		return ResponseEntity.ok(new ApiResponse<>(adminService.toggleTenantStatus(ownerId), "Tenant status updated", true, Instant.now()));
	}

	private void requirePlatformAdmin(User actor) {
		if (actor == null) {
			throw new AccessDeniedException("Authentication required");
		}
		if (!"SUPER_ADMIN".equalsIgnoreCase(actor.getRole())) {
			throw new AccessDeniedException("Access Denied: Super-admin privileges required for SaaS Platform management.");
		}
	}
}
