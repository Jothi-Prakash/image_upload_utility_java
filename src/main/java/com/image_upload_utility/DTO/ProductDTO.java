package com.image_upload_utility.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
	
	private Long id;
	
	private Long pid;
	
    private String productcode;
	
	private String filename;
	
	private String filesize;
	
	private String mimetype;
	
	private String based64;
	
	private String thumbbase64;
	
	private Double imagesize;



}
