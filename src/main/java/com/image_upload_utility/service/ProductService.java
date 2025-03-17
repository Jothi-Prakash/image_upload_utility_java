package com.image_upload_utility.service;

import java.util.List;
import java.util.Optional;

public interface ProductService {

	public long getFolderSize(String folderPath);
	
	public List<String> validateSubfolderImageCount(String folderPath);
	
	public List<String> getMissingFolderNamesInDB(String folderPath);
	
	public boolean checkImagesExistsInDB(String folderPath);

	public void deleteExistingImages(String path);

	public Optional<String> findFolderWithMultipleHImages(String parentFolderPath);

	public void processImages(String path);
	
	
	
	
}
