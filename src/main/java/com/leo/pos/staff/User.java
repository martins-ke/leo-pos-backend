package com.leo.pos.staff;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private String id;

	@Column(unique = true, nullable = false)
	private String username;

	@Column(unique = true)
	private String email;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	private String role;
	private String status;

	@JsonProperty("created_by")
	private String createdBy;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "user_shops",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "shop_id")
	)
	private Set<Shop> shops = new HashSet<>();

	// ─── Constructors ────────────────────────────────────────────────────────
	public User() {}

	public User(String id, String username, String email, String password,
				String role, String status, String createdBy) {
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

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	public Set<Shop> getShops() { return shops; }
	public void setShops(Set<Shop> shops) { this.shops = shops; }
}
