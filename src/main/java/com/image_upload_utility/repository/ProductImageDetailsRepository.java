package com.image_upload_utility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.image_upload_utility.model.ProductImageDetails;

import jakarta.transaction.Transactional;

@Repository
public interface ProductImageDetailsRepository extends JpaRepository<ProductImageDetails, Long>{
	
	@Query("SELECT COALESCE(MAX(p.id), 0) FROM ProductImageDetails p")
	Long findMaxId();

	@Modifying
	@Query("DELETE FROM ProductImageDetails pid WHERE pid.productid IN :productIds")
	int deleteByProductIds(@Param("productIds") List<Long> productIds);
	
	@Query("SELECT COUNT(p) > 0 FROM ProductImageDetails p WHERE p.productid IN :productIds")
	boolean existsByProductIdIn(@Param("productIds") List<Long> productIds);
	


}
