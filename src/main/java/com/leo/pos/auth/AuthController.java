package com.leo.pos.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/setup/status")
	public ResponseEntity<ApiResponse<PlatformSetupStatus>> getPlatformSetupStatus() {
		return authService.getPlatformSetupStatus();
	}

	@PostMapping("/setup")
	public ResponseEntity<ApiResponse<AuthResponse>> setupPlatformOwner(
			@Valid @RequestBody PlatformSetupRequest request,
			HttpServletRequest httpRequest) {
		return authService.setupPlatformOwner(request, httpRequest.getRemoteAddr());
	}
	
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request){
		return authService.signUp(request); 
	}
	
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request){
		return authService.login(request); 
	}
}
