package com.leo.pos.staff;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByUsername(String username); 
	Optional<User> findByEmail(String username);
	Optional<User> findByRole(String role);
}
