package com.leo.pos.staff;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.pos.shop.Shop;
import com.leo.pos.user.Role;
import com.leo.pos.user.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private String id;

	@PrePersist void ensureId() { if (id == null) id = UUID.randomUUID().toString(); }

	@Column(unique = true, nullable = false)
	private String username;

	@Column(unique = true, nullable = false)
	private String email;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)                                    // ← added nullable=false
	private Status status;

	@JsonProperty("created_by")
	private String createdBy;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "user_shops",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "shop_id")
	)
	@JsonIgnore
	private Set<Shop> shops = new HashSet<>();

	// ─── Constructors ────────────────────────────────────────────────────────
	public User() {}

	public User(String id, String username, String email, String password,
				Role role, Status status, String createdBy) {          // ← String → Status
		this.id = id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = role;
		this.status = status;
		this.createdBy = createdBy;
	}

	// ─── Getters & Setters ───────────────────────────────────────────────────
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role; }

	public Status getStatus() { return status; }                   // ← returns Status
	public void setStatus(Status status) { this.status = status; } // ← takes Status

	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	public Set<Shop> getShops() { return shops; }
	public void setShops(Set<Shop> shops) { this.shops = shops; }
}