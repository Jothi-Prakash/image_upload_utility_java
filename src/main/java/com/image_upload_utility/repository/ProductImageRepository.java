package com.image_upload_utility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.image_upload_utility.model.ProductImage;

import jakarta.transaction.Transactional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	
	@Query("SELECT COALESCE(MAX(p.id), 0) FROM ProductImage p")
	Long findMaxId();
	
	@Modifying
	@Query("DELETE FROM ProductImage pi WHERE pi.productid IN :productIds")
	int deleteByProductIds(@Param("productIds") List<Long> productIds);
	
	@Query("SELECT COUNT(pi) > 0 FROM ProductImage pi WHERE pi.productid IN :productIds")
	boolean existsByProductIdIn(@Param("productIds") List<Long> productIds);



}
