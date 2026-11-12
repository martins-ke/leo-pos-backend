package com.leo.pos.dashboard;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('OWNER')")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
			@RequestParam(required = false) String shopId, @AuthenticationPrincipal User actor) {
		DashboardSummaryResponse summary = dashboardService.getSummary(shopId, actor);
		return ResponseEntity.ok(new ApiResponse<>(summary, "Dashboard summary loaded", true, Instant.now()));
	}
}
