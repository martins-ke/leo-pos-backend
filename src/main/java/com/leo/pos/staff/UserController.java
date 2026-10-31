package com.leo.pos.staff;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;

import jakarta.validation.Valid;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, originPatterns = "*")
@RestController
@RequestMapping("/staff")
@PreAuthorize("hasRole('OWNER')")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	// GET /staff — list all staff belonging to this owner
	@GetMapping
	public ResponseEntity<ApiResponse<List<User>>> getAllStaff(@AuthenticationPrincipal User actor) {
		return userService.getAllStaff(actor);
	}

	// POST /staff — owner creates a new staff account
	@PostMapping
	public ResponseEntity<ApiResponse<User>> createStaff(
			@Valid @RequestBody StaffCreateRequest req,
			@AuthenticationPrincipal User actor) {
		return userService.createStaff(req, actor.getId());
	}

	// PATCH /staff/{id}/approve — approve a pending cashier
	@PatchMapping("/{id}/approve")
	public ResponseEntity<ApiResponse<User>> approveStaff(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return userService.approveStaff(id, actor);
	}

	// PATCH /staff/{id}/deactivate — toggle ACTIVE <-> INACTIVE
	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<ApiResponse<User>> deactivateStaff(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return userService.deactivateStaff(id, actor);
	}

	// PATCH /staff/{id}/shops/{shopId} — assign a shop to a staff member
	@PatchMapping("/{id}/shops/{shopId}")
	public ResponseEntity<ApiResponse<User>> assignShop(
			@PathVariable String id,
			@PathVariable String shopId,
			@AuthenticationPrincipal User actor) {
		return userService.assignShopToUser(id, shopId, actor);
	}

	// DELETE /staff/{id}/shops/{shopId} — remove a shop from a staff member
	@DeleteMapping("/{id}/shops/{shopId}")
	public ResponseEntity<ApiResponse<User>> removeShop(
			@PathVariable String id,
			@PathVariable String shopId,
			@AuthenticationPrincipal User actor) {
		return userService.removeShopFromUser(id, shopId, actor);
	}

	// DELETE /staff/{id} — delete a staff account
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<String>> deleteStaff(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return userService.deleteStaff(id, actor);
	}
}
