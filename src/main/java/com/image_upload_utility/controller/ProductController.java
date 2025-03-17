package com.image_upload_utility.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	
	@PostMapping("/uploadImage")
	public String uploadImages(@RequestParam("folderPath") String folderPath,
			@RequestParam(value = "confirmUpdate", required = false) Boolean confirmUpdate,
			RedirectAttributes redirectAttributes) {

		// Step 1: Validate folder path and size
		long folderSize = productService.getFolderSize(folderPath);

		if (folderSize == -1) {
			redirectAttributes.addFlashAttribute("errorMessage", "The folder does not exist. Please check the path.");
			return "redirect:/upload";
		}
		if (folderSize == 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "The folder is empty.");
			return "redirect:/upload";
		}
		if (folderSize > MAX_FILE_SIZE) {
			redirectAttributes.addFlashAttribute("errorMessage","The file size is too large. Only files up to 50MB are allowed!");
			return "redirect:/upload";
		}

		// Step 2: Validate folder image count
		List<String> invalidFolders = productService.validateSubfolderImageCount(folderPath);
		if (!invalidFolders.isEmpty()) {
		    redirectAttributes.addFlashAttribute("errorMessage",
		        "Error: The following subfolders do not have between 3 and 5 images: " + invalidFolders);
		    return "redirect:/upload";
		}


		// Step 3: Check for multiple _h.jpg images in subfolders
		Optional<String> violatingFolder = productService.findFolderWithMultipleHImages(folderPath);
		if (violatingFolder.isPresent()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Error: Folder '" + violatingFolder.get()
					+ "' contains more than one _h.jpg image. Only one _h.jpg is allowed per folder.");
			return "redirect:/upload";
		}

		// Step 4: Validate productDeptCode (folder names) exist in DB
		List<String> missingFolders = productService.getMissingFolderNamesInDB(folderPath);
		if (!missingFolders.isEmpty()) {
			String missingCodes = String.join(", ", missingFolders);
			redirectAttributes.addFlashAttribute("errorMessage",
					"The following productDeptCode(s) are missing in the database: <strong>" + missingCodes
							+ "</strong>");
			return "redirect:/upload";
		}

		// Step 5: Check images exist in images and image_details table
		if (productService.checkImagesExistsInDB(folderPath)) {
			if (Boolean.TRUE.equals(confirmUpdate)) {
				productService.deleteExistingImages(folderPath);
				redirectAttributes.addFlashAttribute("successMessage",
						"Existing images deleted. You can upload again.");
			} else {
				redirectAttributes.addFlashAttribute("confirmMessage",
						"Alert: This data already exists. Do you wish to update?");
				redirectAttributes.addFlashAttribute("folderPath", folderPath);
				return "redirect:/upload";
			}
		}
		
		// step 5: process image (size , thumbnail, compress)
		productService.processImages(folderPath);

		redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully, thank you!");
		return "redirect:/upload";
	}
	
	@PostMapping("/uploadFolder")
	public String handleFolderUpload(@RequestParam("files") MultipartFile[] files,
	                                 RedirectAttributes redirectAttributes) {

	    if (files.length == 0) {
	        redirectAttributes.addFlashAttribute("message", "No files selected.");
	        return "redirect:/upload";
	    }

	    // Map to group files by subfolder
	    Map<String, List<MultipartFile>> subfolderMap = new HashMap<>();

	    for (MultipartFile file : files) {
	        String relativePath = file.getOriginalFilename(); // e.g., FinalImages/gaha872/image1.jpg
	        if (relativePath == null || !relativePath.contains("/")) continue;

	        String[] pathParts = relativePath.split("/");

	        // Step 1: Extract Root Folder (first part)
	        String rootFolder = pathParts[0];  // FinalImages

	        // Step 2: Extract Subfolder (second part)
	        String subFolder = pathParts.length > 2 ? pathParts[1] : "UNKNOWN";

	        // Group by subfolder
	        subfolderMap.computeIfAbsent(subFolder, k -> new ArrayList<>()).add(file);
	    }

	    // Step 3: Print Folder Structure
	    for (Map.Entry<String, List<MultipartFile>> entry : subfolderMap.entrySet()) {
	        String subfolderName = entry.getKey();
	        List<MultipartFile> imageFiles = entry.getValue();

	       System.out.println("Subfolder: " + subfolderName + " | Image Count: " + imageFiles.size());
	        
	        System.out.println(subfolderName);

	        // Validate image count (3 to 5)
	        if (imageFiles.size() < 3 || imageFiles.size() > 5) {
	            redirectAttributes.addFlashAttribute("message",
	                    "Subfolder " + subfolderName + " has invalid image count: " + imageFiles.size());
	            return "redirect:/upload";
	        }

	        // TODO: Process imageFiles (save, compress, etc.)
	    }

	    redirectAttributes.addFlashAttribute("message", "Upload and validation complete.");
	    return "redirect:/upload";
	}







}
