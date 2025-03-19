package com.image_upload_utility.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	
	private int totalSingleInsertCount = 0;
	private int totalMultipleInsertCount = 0;

    public List<String> getFolderNames(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid folder: " + folderPath);
            return List.of();
        }
        return Arrays.stream(folder.listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public List<String> getSubFolderNames(MultipartFile[] files) {
        if (files.length == 0) {
            return List.of();
        }

        Set<String> subfolderNames = new HashSet<>();
        for (MultipartFile file : files) {
            String relativePath = file.getOriginalFilename();
            if (relativePath == null || !relativePath.contains("/")) continue;

            String[] pathParts = relativePath.split("/");
            if (pathParts.length > 2) {
                subfolderNames.add(pathParts[1]);
            }
        }

        return new ArrayList<>(subfolderNames);
    }


    @Override
    public List<String> getMissingFolderNamesInDB(List<String> folderNames) {
        if (folderNames == null || folderNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> existingFolders = productRepository.findExistingProductDeptCodes(folderNames);

        return folderNames.stream()
                .filter(folder -> !existingFolders.contains(folder))
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkImagesExistsInDB(List<String> folderNames) {
        if (folderNames == null || folderNames.isEmpty()) {
            return false;
        }

        List<Long> productIds = productRepository.findProductIdsByProductDeptCodes(folderNames);

        if (productIds == null || productIds.isEmpty()) {
            return false;
        }

        boolean imagesExistInMain = productImageRepository.existsByProductIdIn(productIds);
        boolean imagesExistInDetails = productImageDetailsRepository.existsByProductIdIn(productIds);

        return imagesExistInMain || imagesExistInDetails;
    }

    @Override
    @Transactional
    public void deleteExistingImages(List<String> folderNames) {
        if (folderNames == null || folderNames.isEmpty()) {
            return;
        }

        List<Long> productIds = productRepository.findProductIdsByProductDeptCodes(folderNames);

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        int detailsDeletedCount = productImageDetailsRepository.deleteByProductIds(productIds);
        int imagesDeletedCount = productImageRepository.deleteByProductIds(productIds);

        System.out.println("Deleted from ProductImageDetails: " + detailsDeletedCount + " rows");
        System.out.println("Deleted from ProductImage: " + imagesDeletedCount + " rows");
    }

	@Override
	public void processMultipartFiles(String subfolderName, List<MultipartFile> imageFiles) {

		List<Long> productIds = productRepository
				.findProductIdsByProductDeptCodes(Collections.singletonList(subfolderName));
		if (productIds.isEmpty()) {
			return;
		}

		Long productId = productIds.get(0); // Assuming one productId per folder

		int singleInsertCount = 0;
		int multipleInsertCount = 0;

		for (int i = 0; i < imageFiles.size(); i++) {
			MultipartFile file = imageFiles.get(i);
			try {
				long fileSizeKB = file.getSize() / 1024;
				String originalFilename = file.getOriginalFilename();
				String extension = getFileExtension(originalFilename);

				byte[] imageBytes = file.getBytes();

				if (fileSizeKB > 2048 && !originalFilename.toLowerCase().endsWith("_h.jpg")) {
					imageBytes = compressImageBytes(imageBytes, extension);
					fileSizeKB = imageBytes.length / 1024;
				}

				byte[] thumbnailBytes = createThumbnailBytes(imageBytes, extension);

				String base64EncodedString = Base64.getEncoder().encodeToString(imageBytes);
				String thumbBase64EncodedString = Base64.getEncoder().encodeToString(thumbnailBytes);

				ProductDTO productDTO = new ProductDTO();
				productDTO.setPid(productId);
				productDTO.setFilename(originalFilename);
				productDTO.setBased64(base64EncodedString);
				productDTO.setThumbbase64(thumbBase64EncodedString);
				productDTO.setProductcode(subfolderName);
				productDTO.setMimetype(extension);
				productDTO.setImagesize((double) fileSizeKB);

				// First image only → singleinsertprocess
				if (i == 0) {
					singleinsertprocess(productDTO);
					singleInsertCount++;
				}

				insertprocess(productDTO);
				multipleInsertCount++;

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		totalSingleInsertCount += singleInsertCount;
		totalMultipleInsertCount += multipleInsertCount;

	}

	@Override
	public void printFinalInsertCounts() {
		System.out.println("=== FINAL TOTAL INSERTS ===");
		System.out.println("Total image table Inserts: " + totalSingleInsertCount);
		System.out.println("Total  image details table Inserts: " + totalMultipleInsertCount);
	}

    private byte[] createThumbnailBytes(byte[] imageBytes, String extension) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(bais)
                .size(200, 200)
                .outputFormat(extension)
                .toOutputStream(baos);

        return baos.toByteArray();
    }

    private byte[] compressImageBytes(byte[] imageBytes, String extension) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(bais)
                .scale(1.0)
                .outputQuality(0.6)
                .outputFormat(extension)
                .toOutputStream(baos);

        return baos.toByteArray();
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }

    public void singleinsertprocess(ProductDTO productDTO) {
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



	
	

