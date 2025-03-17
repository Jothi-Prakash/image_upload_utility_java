package com.image_upload_utility.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.image_upload_utility.DTO.ProductDTO;
import com.image_upload_utility.model.ProductImage;
import com.image_upload_utility.model.ProductImageDetails;
import com.image_upload_utility.repository.ProductImageDetailsRepository;
import com.image_upload_utility.repository.ProductImageRepository;
import com.image_upload_utility.repository.ProductRepository;

import jakarta.transaction.Transactional;
import net.coobird.thumbnailator.Thumbnails;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	ProductImageRepository productImageRepository;

	@Autowired
	ProductImageDetailsRepository productImageDetailsRepository;

	// list out folder names
	private List<String> getFolderNames(String folderPath) {
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			System.out.println("Invalid folder: " + folderPath);
			return List.of();
		}

		return Arrays.stream(folder.listFiles()).filter(File::isDirectory).map(File::getName)
				.collect(Collectors.toList());
	}
	
	private List<String> getSubFolderNames(MultipartFile[] files) {

	    if (files.length == 0) {
	        return List.of();
	    }

	    Map<String, List<MultipartFile>> subfolderMap = new HashMap<>();

	    for (MultipartFile file : files) {
	        String relativePath = file.getOriginalFilename();
	        if (relativePath == null || !relativePath.contains("/")) continue;

	        String[] pathParts = relativePath.split("/");

	        String subFolder = pathParts.length > 2 ? pathParts[1] : "Unknown";

	        subfolderMap.computeIfAbsent(subFolder, z -> new ArrayList<>()).add(file);
	    }

	    // Collect and return subfolder names
	    List<String> subfolderNames = new ArrayList<>(subfolderMap.keySet());
	    return subfolderNames;
	}

	@Override
	public long getFolderSize(String folderPath) {
	    File folder = new File(folderPath);

	    if (!folder.exists() || !folder.isDirectory()) {
	        System.out.println("Error: Folder does not exist or is not a directory - " + folder.getAbsolutePath());
	        return -1;
	    }

	    long totalSize = calculateSize(folder);
	    System.out.println("Total folder size: " + (totalSize / (1024 * 1024)) + " MB");
	    return totalSize;
	}
	
	private long calculateSize(File folder) {
	    File[] files = folder.listFiles();
	    if (files == null) return 0;

	    long totalSize = 0;
	    for (File file : files) {
	        if (file.isFile()) {
	            totalSize += file.length();
	        } else if (file.isDirectory()) {
	            totalSize += calculateSize(file); //  cal subfolders
	        }
	    }
	    return totalSize;
	}
	
	@Override
	public List<String> validateSubfolderImageCount(String folderPath) {
		File parentFolder = new File(folderPath);

	    if (!parentFolder.exists() || !parentFolder.isDirectory()) {
	        System.out.println("Error: Invalid folder path - " + folderPath);
	        return List.of("Invalid folder path.");
	    }

	    File[] subfolders = parentFolder.listFiles(File::isDirectory);
	    if (subfolders == null || subfolders.length == 0) {
	        System.out.println("Error: No subfolders found inside the folder.");
	        return List.of("No subfolders found.");
	    }

	    List<String> invalidSubfolders = new ArrayList<>();

	    for (File subfolder : subfolders) {
	        File[] imageFiles = subfolder.listFiles(file -> file.isFile() && file.getName().matches("(?i).+\\.(jpg|jpeg|png)$"));

	        int imageCount = (imageFiles != null) ? imageFiles.length : 0;

	        if (imageCount < 3 || imageCount > 5) {
	            invalidSubfolders.add(subfolder.getName() + " (contains " + imageCount + " images)");
	        }
	    }

	    return invalidSubfolders;
	}



	@Override
	public List<String> getMissingFolderNamesInDB(String folderPath) {
	    List<String> folderNames = getFolderNames(folderPath);
	    if (folderNames.isEmpty())
	        return Collections.emptyList(); 

	    List<String> existingFolders = productRepository.findExistingProductDeptCodes(folderNames);

	    return folderNames.stream()
	        .filter(f -> !existingFolders.contains(f))
	        .collect(Collectors.toList());
	}

	@Override
	public boolean checkImagesExistsInDB(String folderPath) {
	    List<String> folderNames = getFolderNames(folderPath);
	    
	    if (folderNames.isEmpty()) {
	        System.out.println("No folder names found. Skipping image check.");
	        return false;
	    }

	    List<Long> productIds = productRepository.findProductIdsByProductDeptCodes(folderNames);

	    if (productIds == null || productIds.isEmpty()) {
	        System.out.println("No matching productIds found in database.");
	        return false;
	    }

	    Set<Long> distinctProductIds = new HashSet<>(productIds);
	    System.out.println("Checking images for distinct product IDs: " + distinctProductIds);

	    boolean imagesExist = productImageRepository.existsByProductIdIn(new ArrayList<>(distinctProductIds));

	    return imagesExist;
	}




	@Override
	@Transactional
	public void deleteExistingImages(String path) {
	    List<String> folderNames = getFolderNames(path);
	    if (folderNames.isEmpty()) {
	        System.out.println("No folder names found. Skipping delete.");
	        return;
	    }

	    // Find productIds 
	    List<Long> productIds = productRepository.findProductIdsByProductDeptCodes(folderNames);
	    
	    if (productIds.isEmpty()) {
	        System.out.println("No matching productIds found in database. Skipping delete.");
	        return;
	    }

	    System.out.println("Deleting images for productIds: " + productIds);
        int deletedDetails = productImageDetailsRepository.deleteByProductIds(productIds);
	    System.out.println("Deleted " + deletedDetails + " records from ProductImageDetails.");

	    int deletedImages = productImageRepository.deleteByProductIds(productIds);
	    System.out.println("Deleted " + deletedImages + " records from ProductImage.");
	}

	@Override
	public Optional<String> findFolderWithMultipleHImages(String parentFolderPath) {
        File parentFolder = new File(parentFolderPath);
        if (!parentFolder.exists() || !parentFolder.isDirectory()) {
            System.out.println("Invalid folder: " + parentFolderPath);
            return Optional.empty();
        }

        File[] subfolders = parentFolder.listFiles(File::isDirectory);
        if (subfolders == null) return Optional.empty();

        for (File subfolder : subfolders) {
            long hImageCount = Arrays.stream(subfolder.listFiles())
                    .filter(file -> file.isFile() && file.getName().toLowerCase().endsWith("_h.jpg"))
                    .count();

            if (hImageCount == 0) {
                System.out.println("Folder missing _h.jpg image: " + subfolder.getName());
                return Optional.of("Missing _h.jpg in folder: " + subfolder.getName());
            }

            if (hImageCount > 1) {
                System.out.println("Folder contains multiple _h.jpg images: " + subfolder.getName());
                return Optional.of("Multiple _h.jpg in folder: " + subfolder.getName());
            }
        }

        return Optional.empty(); 
    }

	@Override
	public void processImages(String folderPath) {
	    File folder = new File(folderPath);
	    if (!folder.exists() || !folder.isDirectory()) {
	        System.out.println("Invalid folder: " + folderPath);
	        return;
	    }

	    File[] subfolders = folder.listFiles(File::isDirectory);
	    if (subfolders == null) return;

	    long totalImages = 0;
	    long singleImageCount = 0;

	    for (File subfolder : subfolders) {
	        File[] images = subfolder.listFiles(File::isFile);

	        if (images == null || images.length == 0) continue;

	        // Find product IDs
	        List<Long> productIds = productRepository.findProductIdsByProductDeptCodes(Collections.singletonList(subfolder.getName()));
	        if (productIds.isEmpty()) {
	            System.out.println("No matching product IDs found for folder: " + subfolder.getName());
	            continue; // Skip processing if no productId found
	        }

	        int count = 0;
	        for (File image : images) {
	            long fileSizeKB = image.length() / 1024;

	            // only non-_h.jpg images if > 2MB
	            if (fileSizeKB > 2048 && !image.getName().toLowerCase().endsWith("_h.jpg")) {
	                System.out.println("Compressing image: " + image.getName());
	                image = compressImage(image);
	            }

	            System.out.println("Creating thumbnail for: " + image.getName());
	            File thumbnail = createThumbnail(image);
	            
	            String base64EncodedString = encodeToBase64(image);
	            String thumbBase64EncodedString = encodeToBase64(thumbnail);

	            for (Long productId : productIds) {
	                ProductDTO productDTO = new ProductDTO();
	                productDTO.setPid(productId);
	                productDTO.setFilename(image.getName());
	                productDTO.setBased64(base64EncodedString);
	                productDTO.setThumbbase64(thumbBase64EncodedString);
	                productDTO.setProductcode(subfolder.getName());
	                productDTO.setMimetype(getFileExtension(image));
	                productDTO.setImagesize((double) fileSizeKB);

	                if (count == 0) { // First image go to the single table
	                    singleImageCount++;
	                    singleinsertprocess(productDTO);
	                }
	                insertprocess(productDTO);
	                count++;
	                totalImages++;
	            }
	        }
	    }

	    System.out.println("Total images processed: " + totalImages);
	    System.out.println("Total single product images: " + singleImageCount);
	}


    
    private File createThumbnail(File imageFile) {
        try {
            File thumbnail = new File(imageFile.getParent(), "thumb_" + imageFile.getName());

            Thumbnails.of(imageFile)
                    .size(200, 200)
                    .toFile(thumbnail);

            return thumbnail;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File compressImage(File imageFile) {
        try {
            File compressedImage = new File(imageFile.getParent(), "compressed_" + imageFile.getName());

            Thumbnails.of(imageFile)
                    .scale(1.0)
                    .outputQuality(0.7)
                    .toFile(compressedImage);

            return compressedImage;
        } catch (IOException e) {
            e.printStackTrace();
            return imageFile;
        }
    }

    private String encodeToBase64(File image) {
        try (FileInputStream fileInputStream = new FileInputStream(image)) {
            byte[] imageBytes = fileInputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileExtension(File image) {
        String fileName = image.getName();
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase(); // Extract extension
        }
        return "unknown";
    }


    public void singleinsertprocess(ProductDTO productDTO) {
        System.out.println("---Insertion Main Table----->" + productDTO.getProductcode() + "------------" + productDTO.getPid() + "----" + productDTO.getFilename());

        // find the max Id and increment it
        Long newId = productImageRepository.findMaxId() + 1;
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        ProductImage singleImage = new ProductImage();
        
        singleImage.setId(newId);
        singleImage.setProductid(productDTO.getPid());
        singleImage.setImagename(productDTO.getFilename());
        singleImage.setMimetype(productDTO.getMimetype());
        singleImage.setActualimage(productDTO.getBased64());
        singleImage.setImagesize(productDTO.getImagesize());
        singleImage.setSigningcertid(40801L);
        singleImage.setThumbnailimage(productDTO.getThumbbase64());
        singleImage.setUploadedby(70681L);
        singleImage.setUploadeddate(timestamp);
        singleImage.setVerifiedby(70681L);
        singleImage.setVerifieddate(timestamp);

        productImageRepository.save(singleImage);
    }

    public void insertprocess(ProductDTO productDTO) {
        System.out.println("" + productDTO.getProductcode() + "------------" + productDTO.getPid() + "----" + productDTO.getFilename());

        // Fetch the max ID and increment it
        Long newId = productImageDetailsRepository.findMaxId() + 1;
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        ProductImageDetails multipleImages = new ProductImageDetails();
        
        multipleImages.setId(newId);
        multipleImages.setProductid(productDTO.getPid());
        multipleImages.setImagename(productDTO.getFilename());
        multipleImages.setMimetype(productDTO.getMimetype());
        multipleImages.setActualimage(productDTO.getBased64());
        multipleImages.setImagesize(productDTO.getImagesize());
        multipleImages.setSigningcertid(40801L);
        multipleImages.setThumbnailimage(productDTO.getThumbbase64());
        multipleImages.setUploadedby(70681L);
        multipleImages.setUploadeddate(timestamp);
        multipleImages.setVerifiedby(70681L);
        multipleImages.setVerifieddate(timestamp);

        productImageDetailsRepository.save(multipleImages);
    }


	
	
}
