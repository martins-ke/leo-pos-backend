package com.leo.pos.shop;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import com.leo.pos.staff.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shops")
public class Shop {

	@Id private String id;
	@Column(unique = true, nullable = false)
	private String name;
	private String type;
	private String location;
	private String description;
	@CreationTimestamp
	private LocalDate created_on;
	@ManyToOne @JoinColumn(name = "owner_id")
	private User user; 

	public Shop() {}
	public Shop(String id, String name, String type, String location, String description, LocalDate created_on, User user) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.location = location;
		this.description = description;
		this.created_on = created_on;
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalDate getCreated_on() {
		return created_on;
	}

	public void setCreated_on(LocalDate created_on) {
		this.created_on = created_on;
	}

	public User getOwnerId() {
		return user;
	}
	
	public void setOwnerId(User user) {
		this.user = user;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Shop shop = (Shop) o;
		return Objects.equals(id, shop.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
