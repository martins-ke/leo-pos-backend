package com.leo.pos.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, String> {

	Optional<Product> findByName(String name);
	Optional<Product> findByNameAndShopId(String name, String shopId);
	List<Product> findByShopId(String shopId);
	List<Product> findByActiveTrue();
	List<Product> findByShopIdAndActiveTrue(String shopId);
	long countByActiveTrue();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") String id);
}
