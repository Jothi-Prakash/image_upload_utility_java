package com.image_upload_utility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.image_upload_utility.model.ProductDetails;

public interface ProductRepository extends JpaRepository<ProductDetails, Long> {

	@Query(value = "SELECT productdeptcode FROM gepnic_general_auction_product_details "
			+ "WHERE productdeptcode IN (:folderNames)", nativeQuery = true)
	List<String> findExistingProductDeptCodes(@Param("folderNames") List<String> folderNames);
	
	@Query("SELECT p.id FROM ProductDetails p WHERE p.productdeptcode IN :folderNames")
	List<Long> findProductIdsByProductDeptCodes(@Param("folderNames") List<String> folderNames);
	



}
