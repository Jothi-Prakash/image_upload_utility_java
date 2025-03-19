package com.image_upload_utility.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
	
	public List<String> getMissingFolderNamesInDB(List<String> folderNames);
	
	public boolean checkImagesExistsInDB(List<String> folderNames);

	public void deleteExistingImages(List<String> folderNames) ;

	public void processMultipartFiles(String subfolderName, List<MultipartFile> imageFiles);

	public void printFinalInsertCounts();
	
	
	
	
}
