package com.leo.pos.staff;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.leo.pos.ApiResponse;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.util.IdGenarator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

	private final UserRepository userRepo;
	private final ShopRepository shopRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepo, ShopRepository shopRepository, PasswordEncoder passwordEncoder) {
		this.userRepo = userRepo;
		this.shopRepository = shopRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// ─── GET ALL STAFF (filtered to owner's staff) ────────────────────────────
	public ResponseEntity<ApiResponse<List<User>>> getAllStaff(User actor) {
		List<User> list = userRepo.findAll().stream()
				.filter(user -> actor.getId().equals(user.getCreatedBy()))
				.toList();
		return ResponseEntity.ok(new ApiResponse<>(list, "Staff members retrieved", true, Instant.now()));
	}

	// ─── CREATE STAFF (owner creates on behalf of cashier) ───────────────────
	public ResponseEntity<ApiResponse<User>> createStaff(StaffCreateRequest req, String createdById) {
		String username = req.username() != null ? req.username().trim() : "";
		if (userRepo.findByUsername(username).isPresent()) {
			return ResponseEntity.status(400)
					.body(new ApiResponse<>(null, "Username already exists! Please select a different username.", false, Instant.now()));
		}

		String role = "STAFF";
		String status = req.status() != null && !req.status().isBlank()
				? req.status().toUpperCase()
				: "ACTIVE";
		String userId = IdGenarator.generateId(username, userRepo.count());
		String hashedPassword = passwordEncoder.encode(req.password().trim());

		User user = new User(userId, username, req.email(), hashedPassword, role, status,
				createdById != null ? createdById : "OWNER");
		User saved = userRepo.save(user);

		return ResponseEntity.status(201)
				.body(new ApiResponse<>(saved, "Staff account created successfully ✅", true, Instant.now()));
	}

	// ─── APPROVE STAFF (set status ACTIVE, link createdBy to owner) ──────────
	public ResponseEntity<ApiResponse<User>> approveStaff(String id, User actor) {
		User user = userRepo.findById(id).orElse(null);
		if (user == null) {
			return ResponseEntity.status(404)
					.body(new ApiResponse<>(null, "Staff user not found ⚠", false, Instant.now()));
		}
		if (!actor.getId().equals(user.getCreatedBy())) {
			throw new AccessDeniedException("You cannot manage this staff member ⚠");
		}
		user.setRole("STAFF");
		user.setCreatedBy(actor.getId());
		user.setStatus("ACTIVE");
		User saved = userRepo.save(user);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Staff approved and activated ✅", true, Instant.now()));
	}

	// ─── TOGGLE STAFF STATUS (ACTIVE <-> INACTIVE) ───────────────────────────
	public ResponseEntity<ApiResponse<User>> deactivateStaff(String id, User actor) {
		User user = userRepo.findById(id).orElse(null);
		if (user == null) {
			return ResponseEntity.status(404)
					.body(new ApiResponse<>(null, "Staff user not found", false, Instant.now()));
		}
		if (!actor.getId().equals(user.getCreatedBy())) {
			throw new AccessDeniedException("You cannot manage this staff member ⚠");
		}
		user.setStatus(user.getStatus().equalsIgnoreCase("ACTIVE") ? "INACTIVE" : "ACTIVE");
		User saved = userRepo.save(user);
		return ResponseEntity.ok(
				new ApiResponse<>(saved, "Staff status toggled to " + saved.getStatus()+ "✅", true, Instant.now()));
	}

	// ─── ASSIGN SHOP TO STAFF ─────────────────────────────────────────────────
	public ResponseEntity<ApiResponse<User>> assignShopToUser(String userId, String shopId, User actor) {
		User staff = userRepo.findById(userId).orElse(null);
		if (staff == null) {
			return ResponseEntity.status(404)
					.body(new ApiResponse<>(null, "Staff user not found ⚠", false, Instant.now()));
		}
		if (!"STAFF".equalsIgnoreCase(staff.getRole())) {
			return ResponseEntity.status(400)
					.body(new ApiResponse<>(null, "User is not a staff member ⚠", false, Instant.now()));
		}
		if (!actor.getId().equals(staff.getCreatedBy())) {
			throw new AccessDeniedException("You cannot manage this staff member ⚠");
		}
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new EntityNotFoundException("Shop not found ⚠"));
		if (shop.getOwnerId() == null || !actor.getId().equals(shop.getOwnerId().getId())) {
			throw new AccessDeniedException("You do not own this shop ⚠");
		}
		if (staff.getShops() != null) {
			staff.getShops().removeIf(s -> s.getId().equals(shopId));
		}
		staff.getShops().add(shop);
		User saved = userRepo.save(staff);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Shop assigned to staff ✅", true, Instant.now()));
	} 

	// ─── REMOVE SHOP FROM STAFF ───────────────────────────────────────────────
	public ResponseEntity<ApiResponse<User>> removeShopFromUser(String userId, String shopId, User actor) {
		User staff = userRepo.findById(userId).orElse(null);
		if (staff == null) {
			return ResponseEntity.status(404)
					.body(new ApiResponse<>(null, "Staff user not found", false, Instant.now()));
		}
		if (!"STAFF".equalsIgnoreCase(staff.getRole())) {
			return ResponseEntity.status(400)
					.body(new ApiResponse<>(null, "User is not a staff member", false, Instant.now()));
		}
		if (!actor.getId().equals(staff.getCreatedBy())) {
			throw new AccessDeniedException("You cannot manage this staff member");
		}
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new EntityNotFoundException("Shop not found"));
		if (shop.getOwnerId() == null || !actor.getId().equals(shop.getOwnerId().getId())) {
			throw new AccessDeniedException("You do not own this shop");
		}
		if (staff.getShops() != null) {
			staff.getShops().removeIf(s -> s.getId().equals(shopId));
		}
		User saved = userRepo.save(staff);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Shop removed from staff", true, Instant.now()));
	}  

	// ─── DELETE STAFF ─────────────────────────────────────────────────────────
	public ResponseEntity<ApiResponse<String>> deleteStaff(String id, User actor) {
		User user = userRepo.findById(id).orElse(null);
		if (user == null) {
			return ResponseEntity.status(404)
					.body(new ApiResponse<>(null, "Staff member not found", false, Instant.now()));
		}
		if (!actor.getId().equals(user.getCreatedBy())) {
			throw new AccessDeniedException("You cannot manage this staff member");
		}
		userRepo.deleteById(id);
		return ResponseEntity.ok(new ApiResponse<>("Staff member deleted", "Success", true, Instant.now()));
	}
}
