package com.image_upload_utility.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.image_upload_utility.repository.ProductRepository;
import com.image_upload_utility.service.ProductService;

@Controller
public class ProductController {
	
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	ProductService productService;
	
	private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
	
	@GetMapping("/upload")
	public String showUploadPage(Model model) {
	return "upload";
	}
	
	@PostMapping("/uploadFolder")
	public String handleFolderUpload(@RequestParam("files") MultipartFile[] files,
	                                 @RequestParam(value = "confirmUpdate", required = false) Boolean confirmUpdate,
	                                 RedirectAttributes redirectAttributes,
	                                 Model model) {

	    List<String> errorMessages = new ArrayList<>();

	     // Step 1: Check no files are selected
	    if (files.length == 0) {
	        errorMessages.add("No files selected.");
	    }
	    
	   // Step 2: Cal total file size 
	    long totalSize = Arrays.stream(files)
	            .mapToLong(file -> file.getSize())
	            .sum();

	    if (totalSize > MAX_FILE_SIZE) {
	        redirectAttributes.addFlashAttribute("errorMessage",
	                "The file size is too large. Only folders up to 50MB are allowed!");
	        return "redirect:/upload";
	    }
	    
	    // Step 3: Group files by subfolder
	    Map<String, List<MultipartFile>> subfolderMap = new HashMap<>();
	    Set<String> allFolderNames = new HashSet<>();  // Unique folder names

	    for (MultipartFile file : files) {
	        String relativePath = file.getOriginalFilename();
	        if (relativePath == null || !relativePath.contains("/")) continue;

	        String[] pathParts = relativePath.split("/");
	        if (pathParts.length < 2) continue;

	        String folderName = pathParts[1].trim();
	        allFolderNames.add(folderName);  // Collect unique folder names

	        subfolderMap.computeIfAbsent(folderName, k -> new ArrayList<>()).add(file);
	    }

	    // check unique folder 
	    Map<String, List<String>> normalizedFolderMap = new HashMap<>();
	    for (String folderName : allFolderNames) {
	        String normalizedName = folderName.replaceAll(" - Copy( \\(\\d+\\))?$", "").trim();
	        normalizedFolderMap.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(folderName);
	    }

	    // Step 4: check for duplicates
	    Set<String> duplicateFolders = new HashSet<>();
	    for (Map.Entry<String, List<String>> entry : normalizedFolderMap.entrySet()) {
	        List<String> variants = entry.getValue();
	        if (variants.size() > 1) {
	            duplicateFolders.addAll(variants); 
	    }
	    }

	    System.out.println("Duplicate Folders Detected: " + duplicateFolders);



	    List<String> folderNames = new ArrayList<>(subfolderMap.keySet());

	    // Step 5: Validate image count (3 to 5) and single _h.jpg per folder
	    List<String> invalidImageCountFolders = new ArrayList<>();
	    List<String> multipleHImageFolders = new ArrayList<>();
	    List<String> doesnotExistHImageFolders = new ArrayList<>();

	    for (Map.Entry<String, List<MultipartFile>> entry1 : subfolderMap.entrySet()) {
	        String subfolderName = entry1.getKey();
	        List<MultipartFile> imageFiles = entry1.getValue();

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
	    
	    // collect all validations from the above method

	    if (!invalidImageCountFolders.isEmpty()) {
	        errorMessages.add("The following folders have an invalid image count (each must contain 3 to 5 images): " + invalidImageCountFolders);
	    }

	    if (!multipleHImageFolders.isEmpty()) {
	        errorMessages.add("Only one _h.jpg image is allowed per folder. The following folders contain more than one _h.jpg image: " + multipleHImageFolders);
	    }
	    
	    if (!duplicateFolders.isEmpty()) {
	        errorMessages.add("Duplicate folder names found: " + duplicateFolders + ". Please check, delete the duplicates, and upload again.");
	    }


	    // Step 6: Validate folder names in DB
	    List<String> missingFolders = productService.getMissingFolderNamesInDB(folderNames);
	    if (!missingFolders.isEmpty()) {
	        String missingCodes = String.join(", ", missingFolders);
	        errorMessages.add("Does not Exists productDeptCode(s) in DB: " + missingCodes);
	    }

	    // Step 6: Collect and display all errors 
	    if (!errorMessages.isEmpty()) {
	        String combinedErrors = errorMessages.stream()
	                .map(msg -> "<li>" + msg + "</li>")
	                .collect(Collectors.joining());
	        String formattedError = "<ul>" + combinedErrors + "</ul>";
	        redirectAttributes.addFlashAttribute("errorMessage", formattedError);
	        return "redirect:/upload";
	    }

	    // Step 7: Check existing images
	    boolean existingImagesFound = productService.checkImagesExistsInDB(folderNames);
	    if (existingImagesFound) {
	        productService.deleteExistingImages(folderNames);
	        redirectAttributes.addFlashAttribute("successMessage", "Existing images deleted. Uploading new images...");
	    }

	    // Step 8: Process images
	    for (Map.Entry<String, List<MultipartFile>> entry2 : subfolderMap.entrySet()) {
	        productService.processMultipartFiles(entry2.getKey(), entry2.getValue());
	    }

	    productService.printFinalInsertCounts();

	    redirectAttributes.addFlashAttribute("successMessage", "image uploaded successfully thank you!.");
	    return "redirect:/upload";
	}
	    
	    
	




	








}
