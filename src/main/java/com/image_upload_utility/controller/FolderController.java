package com.image_upload_utility.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.io.IOException;

@Controller
public class FolderController {

	@GetMapping("/")
    public String index() {
        return "index";
    }



	@PostMapping("/processFolder")
    @ResponseBody
    public ResponseEntity<String> processFolder(@RequestParam("files") List<MultipartFile> files) {
        try {
            // Group by subfolder
            Map<String, List<String>> folderMap = new HashMap<>();
            for (MultipartFile file : files) {
                String path = file.getOriginalFilename(); // e.g., ProductA/image1.jpg
                String[] parts = path.split("/");
                String folder = (parts.length > 1) ? parts[0] : "Root";

                folderMap.computeIfAbsent(folder, k -> new ArrayList<>()).add(path);

                // TODO: Process/save file here
                // Example: file.transferTo(new File("uploads/" + path));
            }

            // Example: Log folder names and file counts
            folderMap.forEach((k, v) -> System.out.println("Folder: " + k + " | Files: " + v.size()));

            return ResponseEntity.ok("Processed " + files.size() + " files in " + folderMap.size() + " folders.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed.");
        }
	}


}

