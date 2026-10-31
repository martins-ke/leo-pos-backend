package com.leo.pos.shop;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ShopRepository extends JpaRepository<Shop, String> {

	Optional<Shop> findByName(String name);
	List<Shop> findByUserId(String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Shop s where s.id = :id")
	Optional<Shop> findByIdForUpdate(@Param("id") String id);
}
