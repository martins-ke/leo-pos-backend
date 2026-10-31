package com.leo.pos.auth;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.leo.pos.ApiResponse;
import com.leo.pos.security.JwtService;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;
import com.leo.pos.util.IdGenarator;

@Service
public class AuthService {
	private final UserRepository userRepo; 
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	
	public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public ResponseEntity<ApiResponse<AuthResponse>> signUp(SignUpRequest request) {
		String password = request.password().trim(); 
		String username = request.username().trim();
		String email = request.email();
		
		if (userRepo.findByUsername(username).isPresent()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "User with username " + username + " already exists! Please choose a different username.", false, Instant.now()));
		}
		if (userRepo.findByEmail(email).isPresent()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "User with email " + email + " already exists! Email address must be unique.", false, Instant.now()));
		}
		if (!password.equals(request.confirm_password())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Password mismatch! Please confirm and sign up again.", false, Instant.now())); 
		}
		
		String assignedRole = request.role() != null && !request.role().isBlank() ? request.role().toUpperCase() : null;
		boolean hasOwner = userRepo.findByRole("OWNER").isPresent();
		assignedRole = hasOwner ? "STAFF" : "OWNER"; 
		
		String initialStatus = "OWNER".equalsIgnoreCase(assignedRole) ? "ACTIVE" : "PENDING";
		
		String userId = IdGenarator.generateId(username, userRepo.count());
		String hashedPassword = passwordEncoder.encode(password); 
		
		User user = new User(userId, username, email, hashedPassword, assignedRole, initialStatus, "SELF"); 
		if ("STAFF".equals(assignedRole)) {
			userRepo.findByRole("OWNER").ifPresent(owner -> user.setCreatedBy(owner.getId()));
		}
		User saved = userRepo.save(user);
		
		String token = "ACTIVE".equalsIgnoreCase(saved.getStatus()) ? jwtService.generateToken(saved) : null;
		String successMsg = "OWNER".equalsIgnoreCase(assignedRole)
				? "Registration successful ✅ Welcome, Shop Owner!"
				: "Account created! Awaiting shop owner approval before POS login.";
		return ResponseEntity.status(201).body(new ApiResponse<>(new AuthResponse(saved, token), successMsg, true, Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request) {
		String identifier = request.username().trim();
		String password = request.password().trim();

		User user = userRepo.findByUsername(identifier)
				.orElseGet(() -> userRepo.findByEmail(identifier).orElse(null));

		if (user == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword()); 
		if (!passwordMatches) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		if ("PENDING".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account pending owner approval. Please ask your shop owner to approve your account.", false, Instant.now()));
		}

		if ("INACTIVE".equalsIgnoreCase(user.getStatus()) || "DEACTIVATED".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Your account has been deactivated. Please contact your manager.", false, Instant.now()));
		}
		
		String token = jwtService.generateToken(user);
		return ResponseEntity.status(200).body(new ApiResponse<>(new AuthResponse(user, token), "Login successful", true, Instant.now()));
	}
}
