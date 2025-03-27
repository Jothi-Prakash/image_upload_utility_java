package com.image_upload_utility.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.image_upload_utility.repository.ProductRepository;
import com.image_upload_utility.service.ProductService;

import ch.qos.logback.core.model.Model;

@Controller
public class ProductController_v2 {
	
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	ProductService productService;
	
	private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
	
	@PostMapping("/folderUpload")
	public String handleFolderUpload(@RequestParam("files") List<MultipartFile[]> files,
	                                 @RequestParam(value = "confirmUpdate", required = false) Boolean confirmUpdate,
	                                 RedirectAttributes redirectAttributes,
	                                 Model model) {

	    List<String> errorMessages = new ArrayList<>();

	    // 🔄 Flatten the list of arrays into a single list
	    List<MultipartFile> allFiles = files.stream()
	                                        .flatMap(Arrays::stream)
	                                        .collect(Collectors.toList());

	    // Step 1: Check no files are selected
	    if (allFiles.isEmpty()) {
	        errorMessages.add("No files selected.");
	    }

	    // Step 2: Calculate total size
	    long totalSize = allFiles.stream()
	                             .mapToLong(MultipartFile::getSize)
	                             .sum();

	    if (totalSize > MAX_FILE_SIZE) {
	        redirectAttributes.addFlashAttribute("errorMessage",
	                "The file size is too large. Only folders up to 50MB are allowed!");
	        return "redirect:/upload";
	    }

	    // Step 3: Group files by subfolder
	    Map<String, List<MultipartFile>> subfolderMap = new HashMap<>();
	    Set<String> allFolderNames = new HashSet<>();

	    for (MultipartFile file : allFiles) {
	        String relativePath = file.getOriginalFilename();
	        if (relativePath == null || !relativePath.contains("/")) continue;

	        String[] pathParts = relativePath.split("/");
	        if (pathParts.length < 2) continue;

	        String folderName = pathParts[1].trim();
	        allFolderNames.add(folderName);

	        subfolderMap.computeIfAbsent(folderName, k -> new ArrayList<>()).add(file);
	    }

	    // Step 4: Normalize and detect duplicates
	    Map<String, List<String>> normalizedFolderMap = new HashMap<>();
	    for (String folderName : allFolderNames) {
	        String normalizedName = folderName.replaceAll(" - Copy( \\(\\d+\\))?$", "").trim();
	        normalizedFolderMap.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(folderName);
	    }

	    Set<String> duplicateFolders = new HashSet<>();
	    for (Map.Entry<String, List<String>> entry : normalizedFolderMap.entrySet()) {
	        List<String> variants = entry.getValue();
	        if (variants.size() > 1) {
	            duplicateFolders.addAll(variants);
	        }
	    }

	    // Step 5: Image count and _h.jpg validations
	    List<String> invalidImageCountFolders = new ArrayList<>();
	    List<String> multipleHImageFolders = new ArrayList<>();
	    List<String> doesnotExistHImageFolders = new ArrayList<>();

	    for (Map.Entry<String, List<MultipartFile>> entry : subfolderMap.entrySet()) {
	        String subfolderName = entry.getKey();
	        List<MultipartFile> imageFiles = entry.getValue();

	        if (imageFiles.size() < 3 || imageFiles.size() > 5) {
	            invalidImageCountFolders.add(subfolderName);
	        }

	        long hImageCount = imageFiles.stream()
	                .filter(f -> f.getOriginalFilename() != null && f.getOriginalFilename().toLowerCase().endsWith("_h.jpg"))
	                .count();

	        if (hImageCount > 1) {
	            multipleHImageFolders.add(subfolderName);
	        }

	        if (hImageCount == 0) {
	            doesnotExistHImageFolders.add(subfolderName);
	        }
	    }

	    // Step 6: Collect error messages
	    if (!invalidImageCountFolders.isEmpty()) {
	        errorMessages.add("The following folders have an invalid image count (3 to 5 images required): " + invalidImageCountFolders);
	    }

	    if (!multipleHImageFolders.isEmpty()) {
	        errorMessages.add("Only one _h.jpg is allowed per folder. Found multiple in: " + multipleHImageFolders);
	    }

	    if (!duplicateFolders.isEmpty()) {
	        errorMessages.add("Duplicate folder names found: " + duplicateFolders + ". Please remove duplicates and upload again.");
	    }

	    // Step 7: Check if folders exist in DB
	    List<String> folderNames = new ArrayList<>(subfolderMap.keySet());
	    List<String> missingFolders = productService.getMissingFolderNamesInDB(folderNames);
	    if (!missingFolders.isEmpty()) {
	        errorMessages.add("Missing productDeptCode(s) in DB: " + String.join(", ", missingFolders));
	    }

	    // Step 8: If there are any errors, return
	    if (!errorMessages.isEmpty()) {
	        String formattedError = "<ul>" + errorMessages.stream().map(msg -> "<li>" + msg + "</li>").collect(Collectors.joining()) + "</ul>";
	        redirectAttributes.addFlashAttribute("errorMessage", formattedError);
	        return "redirect:/upload";
	    }

	    // Step 9: Existing image check and delete
	    boolean existingImagesFound = productService.checkImagesExistsInDB(folderNames);
	    if (existingImagesFound) {
	        productService.deleteExistingImages(folderNames);
	        redirectAttributes.addFlashAttribute("successMessage", "Existing images deleted. Uploading new images...");
	    }

	    // Step 10: Process image files
	    for (Map.Entry<String, List<MultipartFile>> entry : subfolderMap.entrySet()) {
	        productService.processMultipartFiles(entry.getKey(), entry.getValue());
	    }

	    productService.printFinalInsertCounts();

	    redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully. Thank you!");
	    return "redirect:/upload";
	}


}
